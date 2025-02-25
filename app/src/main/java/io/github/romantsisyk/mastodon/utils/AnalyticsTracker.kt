package io.github.romantsisyk.mastodon.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsTracker @Inject constructor(
    dispatcher: CoroutineDispatcher
) {
    private val analyticsScope = CoroutineScope(SupervisorJob() + dispatcher)
    private val sessionId = UUID.randomUUID().toString()
    private val pendingEvents = ConcurrentHashMap<String, Boolean>()

    init {
        Timber.d("Analytics tracker initialized with session ID: $sessionId")
    }

    fun trackSearch(query: String) {
        if (query.isBlank()) return
        analyticsScope.launch {
            try {
                Timber.d("Analytics - Search query tracked: $query")
                trackEvent("search_query", mapOf(KEY_SEARCH_QUERY to query))
            } catch (e: Exception) {
                Timber.e(e, "Failed to track search event")
            }
        }
    }

    fun trackItemDismissed(itemId: String, reason: String = "") {
        analyticsScope.launch {
            try {
                val params = buildMap {
                    put("item_id", itemId)
                    if (reason.isNotBlank()) {
                        put("reason", reason)
                    }
                }
                Timber.d("Analytics - Item dismissed: $itemId, reason: $reason")
                trackEvent("item_dismissed", params)
            } catch (e: Exception) {
                Timber.e(e, "Failed to track item dismissal event")
            }
        }
    }

    fun trackEvent(eventName: String, params: Map<String, Any> = emptyMap()) {
        val eventId = "${eventName}_${System.currentTimeMillis()}"
        pendingEvents[eventId] = true
        analyticsScope.launch {
            try {
                val enrichedParams = params.toMutableMap().apply {
                    put("session_id", sessionId)
                    put("timestamp", System.currentTimeMillis())
                }
                val paramsString = if (enrichedParams.isEmpty()) "" else
                    enrichedParams.entries.joinToString(", ") { "${it.key}: ${it.value}" }

                Timber.d("Analytics - Event tracked: $eventName { $paramsString }")
                pendingEvents.remove(eventId)
            } catch (e: Exception) {
                Timber.e(e, "Failed to track event: $eventName")
            }
        }
    }

    companion object {
        const val EVENT_SEARCH_OFFLINE_ATTEMPT = "search_offline_attempt"
        const val EVENT_SEARCH_CLEARED = "search_cleared"
        const val KEY_SEARCH_QUERY = "query"
    }
}