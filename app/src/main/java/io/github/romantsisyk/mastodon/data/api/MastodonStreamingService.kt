package io.github.romantsisyk.mastodon.data.api

import com.google.gson.Gson
import io.github.romantsisyk.mastodon.data.database.TimelineItemDto
import io.github.romantsisyk.mastodon.domain.model.TimelineItem
import io.github.romantsisyk.mastodon.utils.AppConstants.ACCESS_TOKEN
import io.github.romantsisyk.mastodon.utils.AppConstants.BASE_URL
import io.github.romantsisyk.mastodon.utils.AppConstants.INITIAL_RETRY_DELAY_SECONDS
import io.github.romantsisyk.mastodon.utils.AppConstants.MAX_RETRIES
import io.github.romantsisyk.mastodon.utils.AppConstants.MAX_RETRY_DELAY_SECONDS
import io.github.romantsisyk.mastodon.utils.NetworkMonitor
import io.github.romantsisyk.mastodon.utils.NetworkStatus
import io.github.romantsisyk.mastodon.utils.RetryPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class MastodonStreamingService @Inject constructor(
    private val client: OkHttpClient,
    private val gson: Gson,
    private val networkMonitor: NetworkMonitor
) {
    private val retryPolicy = RetryPolicy(
        maxAttempts = MAX_RETRIES,
        initialDelay = INITIAL_RETRY_DELAY_SECONDS.seconds,
        maxDelay = MAX_RETRY_DELAY_SECONDS.seconds
    )

    fun streamPublicTimeline(query: String): Flow<TimelineItem> = callbackFlow {
        if (networkMonitor.networkStatus.first() == NetworkStatus.Unavailable) {
            Timber.d("Network unavailable, not starting EventSource")
            close()
            return@callbackFlow
        }

        retryPolicy.retry {
            streamWithEventSource(query, this)
        }
    }

    private suspend fun streamWithEventSource(
        query: String,
        channel: ProducerScope<TimelineItem>
    ) {
        var currentEventSource: EventSource? = null

        try {
            val request = buildStreamRequest(query)
            val listener = createEventSourceListener(query, channel)
            currentEventSource =
                EventSources.createFactory(client).newEventSource(request, listener)
            monitorNetworkStatus(currentEventSource, channel)
            channel.awaitClose {
                Timber.d("Closing EventSource for query: $query")
                currentEventSource.cancel()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in streaming connection for query: $query")
            currentEventSource?.cancel()
            throw e
        }
    }

    private fun buildStreamRequest(query: String): Request {
        return Request.Builder()
            .url(buildStreamingUrl(query))
            .addHeader("Accept", "text/event-stream")
            .build()
    }

    private fun createEventSourceListener(
        query: String,
        channel: ProducerScope<TimelineItem>
    ): EventSourceListener {
        return object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                Timber.d("EventSource opened for query: $query with response code: ${response.code}")
            }

            override fun onEvent(
                eventSource: EventSource, id: String?, type: String?, data: String
            ) {
                processEvent(type, data, channel)
            }

            override fun onClosed(eventSource: EventSource) {
                Timber.d("EventSource closed for query: $query")
                channel.close()
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?
            ) {
                handleFailure(t, response, query, channel)
            }
        }
    }

    private fun processEvent(
        type: String?,
        data: String,
        channel: ProducerScope<TimelineItem>
    ) {
        try {
            Timber.v("Received event type: $type")
            if (type == "update" || type == "status.update") {
                val timelineItem = gson.fromJson(data, TimelineItemDto::class.java)?.toDomainModel()
                timelineItem?.let { item ->
                    channel.trySend(item)
                    Timber.d("Processed timeline item: ${item.id}")
                } ?: Timber.w("Failed to parse timeline item from data: $data")
            } else {
                Timber.d("Unhandled event type: $type")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error processing event data: $data")
        }
    }

    private fun handleFailure(
        throwable: Throwable?,
        response: Response?,
        query: String,
        channel: ProducerScope<TimelineItem>
    ) {
        val errorCode = response?.code
        val errorBody = response?.body?.string()

        if (throwable is java.net.UnknownHostException) {
            Timber.d("Network unavailable, closing EventSource")
            channel.close()
            return
        }

        Timber.e(
            throwable,
            "EventSource failure for query: $query. Response code: $errorCode, body: $errorBody"
        )
        response?.close()

        throw throwable ?: IOException("Stream failed with code: $errorCode")
    }

    private fun monitorNetworkStatus(
        eventSource: EventSource,
        channel: ProducerScope<TimelineItem>
    ) {
        networkMonitor.networkStatus
            .onEach { status ->
                if (status == NetworkStatus.Unavailable) {
                    Timber.d("Network became unavailable, closing EventSource")
                    eventSource.cancel()
                    channel.close()
                }
            }
            .launchIn(CoroutineScope(Dispatchers.IO))
    }

    private fun buildStreamingUrl(query: String): String {
        return "$BASE_URL/api/v1/streaming/public?query=$query&access_token=$ACCESS_TOKEN"
    }
}