package io.github.romantsisyk.mastodon.ui.state

import io.github.romantsisyk.mastodon.domain.model.ConnectionState
import io.github.romantsisyk.mastodon.domain.model.TimelineUiItem

data class TimelineUiState(
    val items: List<TimelineUiItem> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val isOfflineMode: Boolean = false,
    val hasCache: Boolean = false
) {
    val shouldShowError: Boolean
        get() = error != null && !isOfflineMode && connectionState is ConnectionState.Error

    val shouldShowEmptyState: Boolean
        get() = items.isEmpty() && !isLoading && error == null
}