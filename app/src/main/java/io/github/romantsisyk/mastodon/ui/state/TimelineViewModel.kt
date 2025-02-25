package io.github.romantsisyk.mastodon.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.romantsisyk.mastodon.domain.model.ConnectionState
import io.github.romantsisyk.mastodon.domain.model.PostId
import io.github.romantsisyk.mastodon.domain.model.SearchQuery
import io.github.romantsisyk.mastodon.domain.usecase.DeleteItemUseCase
import io.github.romantsisyk.mastodon.domain.usecase.GetTimelineUseCase
import io.github.romantsisyk.mastodon.utils.AnalyticsTracker
import io.github.romantsisyk.mastodon.utils.AnalyticsTracker.Companion.EVENT_SEARCH_CLEARED
import io.github.romantsisyk.mastodon.utils.AnalyticsTracker.Companion.EVENT_SEARCH_OFFLINE_ATTEMPT
import io.github.romantsisyk.mastodon.utils.AnalyticsTracker.Companion.KEY_SEARCH_QUERY
import io.github.romantsisyk.mastodon.utils.AppConstants.MIN_SEARCH_QUERY_LENGTH
import io.github.romantsisyk.mastodon.utils.AppConstants.SEARCH_DEBOUNCE_DELAY
import io.github.romantsisyk.mastodon.utils.NetworkMonitor
import io.github.romantsisyk.mastodon.utils.NetworkStatus
import io.github.romantsisyk.mastodon.utils.SearchDebouncer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val getTimelineUseCase: GetTimelineUseCase,
    private val deleteItemUseCase: DeleteItemUseCase,
    private val analyticsTracker: AnalyticsTracker,
    networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimelineUiState())
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()

    private val networkStatusFlow = networkMonitor.networkStatus
        .onEach { status ->
            Timber.d("Network status changed: $status")
            val isOffline = status == NetworkStatus.Unavailable
            _uiState.update { currentState ->
                timelineUiState(currentState, isOffline, status)
            }

            if (status == NetworkStatus.Available) {
                val currentQuery = _uiState.value.searchQuery
                if (currentQuery.isNotEmpty()) {
                    Timber.d("Network restored, refreshing search: $currentQuery")
                    searchDebouncer.accept(currentQuery)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, NetworkStatus.Unavailable)

    private fun timelineUiState(
        currentState: TimelineUiState,
        isOffline: Boolean,
        status: NetworkStatus
    ) = currentState.copy(
        isOfflineMode = isOffline,
        error = if (!isOffline && currentState.error?.lowercase()?.let { error ->
                listOf("offline", "network", "internet").any { it in error }
            } == true) null else currentState.error,
        connectionState = when (status) {
            NetworkStatus.Available -> ConnectionState.Connected
            NetworkStatus.Unavailable -> ConnectionState.Disconnected
        }
    )

    private val searchDebouncer = SearchDebouncer(
        scope = viewModelScope,
        delayMillis = SEARCH_DEBOUNCE_DELAY
    )

    init {
        observeTimelineItems()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeTimelineItems() {
        viewModelScope.launch {
            searchDebouncer.flow
                .distinctUntilChanged()
                .bufferTimeout(300L)
                .onEach { _ ->
                    _uiState.update { it.copy(isLoading = true) }
                }
                .mapNotNull { query ->
                    SearchQuery.create(query)?.also {
                        Timber.d("Valid search query: $query")
                    } ?: run {
                        Timber.d("Invalid search query: $query")
                        _uiState.update { state ->
                            state.copy(
                                items = emptyList(),
                                isLoading = false,
                                connectionState = if (query.isEmpty()) {
                                    ConnectionState.Disconnected
                                } else {
                                    ConnectionState.Error(
                                        "Search query must be at least $MIN_SEARCH_QUERY_LENGTH characters"
                                    )
                                }
                            )
                        }
                        null
                    }
                }
                .flatMapLatest { query ->
                    getTimelineUseCase(query)
                        .map { items ->
                            Timber.d("Received ${items.size} items")
                            val isOffline = networkStatusFlow.value == NetworkStatus.Unavailable
                            Timber.d("Current network status is offline: $isOffline")

                            _uiState.value.copy(
                                items = items.map { it.toUiModel() },
                                isLoading = false,
                                connectionState = if (isOffline) ConnectionState.Disconnected else ConnectionState.Connected,
                                hasCache = items.isNotEmpty(),
                                isOfflineMode = isOffline
                            )
                        }
                        .catch { error ->
                            Timber.e(error, "Error in timeline flow")
                            val errorMessage = error.message ?: "Unknown error"

                            val isNetworkError = error is java.net.UnknownHostException ||
                                    error is java.io.IOException ||
                                    errorMessage.containsAnyOf("network")

                            emit(
                                _uiState.value.copy(
                                    isLoading = false,
                                    connectionState = ConnectionState.Error(errorMessage),
                                    isOfflineMode = isNetworkError && _uiState.value.hasCache
                                )
                            )
                        }
                }
                .collect { newState ->
                    Timber.d("Setting new state with ${newState.items.size} items")
                    _uiState.value = newState
                }
        }
    }

    fun updateSearchQuery(query: String) {
        val isOffline = networkStatusFlow.value == NetworkStatus.Unavailable
        _uiState.update { it.copy(searchQuery = query) }

        if (isOffline && query.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    error = "Search is limited in offline mode. Showing cached results.",
                    isLoading = false
                )
            }
            analyticsTracker.trackEvent(
                EVENT_SEARCH_OFFLINE_ATTEMPT,
                mapOf(KEY_SEARCH_QUERY to query)
            )
        } else {
            searchDebouncer.accept(query)
            analyticsTracker.trackSearch(query)
        }
    }

    fun clearSearch() {
        analyticsTracker.trackEvent(EVENT_SEARCH_CLEARED)
        viewModelScope.launch {
            searchDebouncer.accept("")
            delay(SEARCH_DEBOUNCE_DELAY * 3)
            _uiState.update {
                it.copy(
                    searchQuery = "",
                    isLoading = false,
                    items = emptyList(),
                    error = null
                )
            }
        }
    }

    fun dismissItem(id: PostId) {
        analyticsTracker.trackItemDismissed(id.value)
        viewModelScope.launch {
            try {
                _uiState.update { state ->
                    state.copy(items = state.items.filterNot { it.id == id })
                }

                deleteItemUseCase(id)
            } catch (e: Exception) {
                Timber.e(e, "Error deleting item ${id.value}")
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Error deleting item"
                    )
                }
            }
        }
    }

    fun removeExpiredItem(id: PostId) {
        dismissItem(id)
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        searchDebouncer.accept(_uiState.value.searchQuery)
    }

    private fun String.containsAnyOf(vararg keywords: String): Boolean =
        keywords.any { this.contains(it, ignoreCase = true) }
}

private fun <T> Flow<T>.bufferTimeout(
    timeoutMillis: Long = SEARCH_DEBOUNCE_DELAY,
    bufferSize: Int = DEFAULT_BUFFER_SIZE
): Flow<T> = buffer(bufferSize).onEach { delay(timeoutMillis) }
