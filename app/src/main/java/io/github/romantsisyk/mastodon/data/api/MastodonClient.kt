package io.github.romantsisyk.mastodon.data.api

import io.github.romantsisyk.mastodon.domain.model.ConnectionState
import io.github.romantsisyk.mastodon.domain.model.SearchQuery
import io.github.romantsisyk.mastodon.domain.model.TimelineItem
import io.github.romantsisyk.mastodon.utils.NetworkMonitor
import io.github.romantsisyk.mastodon.utils.NetworkStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MastodonClient @Inject constructor(
    private val streamingService: MastodonStreamingService,
    private val networkMonitor: NetworkMonitor,
    private val dispatcher: CoroutineDispatcher
) {
    private val connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun streamTimeline(query: SearchQuery): Flow<TimelineItem> {
        if (query == SearchQuery.EMPTY) {
            Timber.d("Empty search query, returning empty flow")
            connectionState.value = ConnectionState.Disconnected
            return flow { }
        }
        Timber.d("Starting timeline stream for query: ${query.value}")

        return networkMonitor.networkStatus
            .filter { it == NetworkStatus.Available }
            .flatMapLatest {
                Timber.d("Network available, connecting to stream")

                streamingService.streamPublicTimeline(query.value)
                    .onStart {
                        Timber.d("Stream started")
                        connectionState.value = ConnectionState.Connected
                    }
                    .catch { error ->
                        Timber.e(error, "Stream error: ${getErrorMessage(error)}")
                        connectionState.value = ConnectionState.Error(getErrorMessage(error))
                        throw error
                    }
            }
            .onCompletion {
                Timber.d("Stream completed")
                connectionState.value = ConnectionState.Disconnected
            }
            .flowOn(dispatcher)
    }

    private fun getErrorMessage(throwable: Throwable): String {
        return when (throwable) {
            is retrofit2.HttpException -> {
                when (throwable.code()) {
                    400 -> "Bad Request: The request was invalid"
                    401 -> "Authentication required. Please check API access"
                    403 -> "Forbidden: Access denied"
                    404 -> "Not Found: The requested resource doesn't exist"
                    429 -> "Too Many Requests: Please wait before trying again"
                    500 -> "Internal Server Error: Please try again later"
                    502 -> "Bad Gateway: Server is temporarily unavailable"
                    503 -> "Service Unavailable: Server is overloaded"
                    504 -> "Gateway Timeout: Server took too long to respond"
                    in 400..499 -> "Client Error (${throwable.code()})"
                    in 500..599 -> "Server Error (${throwable.code()})"
                    else -> "HTTP Error: ${throwable.code()}"
                }
            }

            is java.net.UnknownHostException -> "Network Error: Unable to reach server"
            is java.net.SocketTimeoutException -> "Network Error: Connection timed out"
            is java.io.IOException -> "Network Error: ${throwable.message}"
            else -> throwable.message ?: "Unknown error occurred"
        }
    }
}