package io.github.romantsisyk.mastodon.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsTracker @Inject constructor(
    private val dispatcher: CoroutineDispatcher
) {
    private val analyticsScope = CoroutineScope(SupervisorJob() + dispatcher)

    fun trackSearch(query: String) {
        analyticsScope.launch {
            try {
                Timber.d("Analytics - Search query tracked: $query")
            } catch (e: Exception) {
                Timber.e(e, "Failed to track search event")
            }
        }
    }

    fun trackItemDismissed(itemId: String, reason: String = "") {
        analyticsScope.launch {
            try {
                Timber.d("Analytics - Item dismissed: $itemId, reason: $reason")
            } catch (e: Exception) {
                Timber.e(e, "Failed to track item dismissal event")
            }
        }
    }

    fun trackEvent(eventName: String, params: Map<String, Any> = emptyMap()) {
        analyticsScope.launch {
            try {
                val paramsString = if (params.isEmpty()) "" else
                    params.entries.joinToString(", ") { "${it.key}: ${it.value}" }

                Timber.d("Analytics - Event tracked: $eventName $paramsString")
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