package io.github.romantsisyk.mastodon.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.romantsisyk.mastodon.R
import io.github.romantsisyk.mastodon.domain.model.ConnectionState
import io.github.romantsisyk.mastodon.ui.state.TimelineViewModel
import io.github.romantsisyk.mastodon.ui.views.ErrorView
import io.github.romantsisyk.mastodon.ui.views.LoadingView
import io.github.romantsisyk.mastodon.ui.views.OfflineSearchIndicator
import io.github.romantsisyk.mastodon.ui.views.TimelineItem
import io.github.romantsisyk.mastodon.ui.views.TimelineSearchBar

@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    Column(modifier = Modifier.fillMaxSize()) {

        TimelineSearchBar(
            query = uiState.searchQuery,
            onQueryChange = viewModel::updateSearchQuery,
            onClearSearch = viewModel::clearSearch,
            isOffline = uiState.isOfflineMode,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(id = R.dimen.padding_large),
                    vertical = dimensionResource(id = R.dimen.padding_medium)
                )
        )

        OfflineSearchIndicator(
            visible = uiState.isOfflineMode,
            query = uiState.searchQuery,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(id = R.dimen.padding_large))
        )

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading -> {
                    LoadingView(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.shouldShowError -> {
                    ErrorView(
                        message = uiState.error ?: stringResource(R.string.unknown_error_occurred),
                        onRetry = viewModel::refresh,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.connectionState is ConnectionState.Error && uiState.items.isEmpty() -> {
                    ErrorView(
                        message = (uiState.connectionState as ConnectionState.Error).message,
                        onRetry = viewModel::refresh,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.shouldShowEmptyState -> {
                    EmptyStateMessage(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    TimelineContent(
                        uiState = uiState,
                        onItemExpired = viewModel::removeExpiredItem,
                        onItemDismiss = viewModel::dismissItem
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineContent(
    uiState: io.github.romantsisyk.mastodon.ui.state.TimelineUiState,
    onItemExpired: (io.github.romantsisyk.mastodon.domain.model.PostId) -> Unit,
    onItemDismiss: (io.github.romantsisyk.mastodon.domain.model.PostId) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            horizontal = dimensionResource(id = R.dimen.padding_large),
            vertical = dimensionResource(id = R.dimen.padding_medium)
        ),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_medium))
    ) {
        items(
            items = uiState.items,
            key = { it.id.value }
        ) { item ->
            TimelineItem(
                item = item,
                onExpired = { onItemExpired(item.id) },
                onDismiss = { onItemDismiss(item.id) }
            )
        }
    }
}

@Composable
private fun EmptyStateMessage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(dimensionResource(id = R.dimen.padding_large)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.no_items_found),
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_medium)))
        Text(
            text = stringResource(R.string.try_adjusting_your_search_or_check_back_later),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}