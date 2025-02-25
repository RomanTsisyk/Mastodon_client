package io.github.romantsisyk.mastodon.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import io.github.romantsisyk.mastodon.utils.AppConstants.MIN_SEARCH_QUERY_LENGTH
import io.github.romantsisyk.mastodon.utils.AppConstants.SEARCH_DEBOUNCE_DELAY

class SearchDebouncer(
    private val scope: CoroutineScope,
    delayMillis: Long = SEARCH_DEBOUNCE_DELAY,
    private val minQueryLength: Int = MIN_SEARCH_QUERY_LENGTH,
    private val analyticsTracker: AnalyticsTracker? = null
) {
    private val queryFlow = MutableSharedFlow<String>(
        replay = 1,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val flow: Flow<String> = queryFlow
        .debounce(delayMillis)
        .distinctUntilChanged()
        .filter { query ->
            query.isEmpty() || query.length >= minQueryLength
        }
        .onEach { query ->
            Timber.d("Debounced search query: '$query'")
            if (query.isEmpty()) {
                analyticsTracker?.trackEvent(AnalyticsTracker.EVENT_SEARCH_CLEARED)
            } else {
                analyticsTracker?.trackSearch(query)
            }
        }

    fun accept(query: String) {
        Timber.v("Accepting query: '$query'")
        scope.launch {
            queryFlow.emit(query.trim())
        }
    }
}