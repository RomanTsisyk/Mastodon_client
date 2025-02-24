package io.github.romantsisyk.mastodon.data.repository

import io.github.romantsisyk.mastodon.data.api.MastodonClient
import io.github.romantsisyk.mastodon.data.manager.TimelineCacheManager
import io.github.romantsisyk.mastodon.domain.model.PostId
import io.github.romantsisyk.mastodon.domain.model.SearchQuery
import io.github.romantsisyk.mastodon.domain.model.TimelineItem
import io.github.romantsisyk.mastodon.domain.repository.TimelineRepository
import io.github.romantsisyk.mastodon.utils.NetworkMonitor
import io.github.romantsisyk.mastodon.utils.NetworkStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject

class TimelineRepositoryImpl @Inject constructor(
    private val client: MastodonClient,
    private val cacheManager: TimelineCacheManager,
    private val networkMonitor: NetworkMonitor
) : TimelineRepository {

    override suspend fun updateTimelineItems() {
//        TODO() updateTimelineItems not implemented
        Timber.d("Manual timeline update requested")
    }

    override suspend fun clearExpiredItems() {
//        TODO() clearExpiredItems not implemented
        Timber.d("Manual expired items cleanup requested")
    }

    override fun getTimelineFlow(query: SearchQuery): Flow<List<TimelineItem>> {
        Timber.d("Getting timeline flow for query: ${query.value}")

        return combine(
            networkMonitor.networkStatus,
            getCachedItems(),
            getNetworkItemsWithFallback(query)
        ) { networkStatus, cachedItems, networkItems ->
            Timber.d("Combining items. Network status: $networkStatus")

            when (networkStatus) {
                NetworkStatus.Available -> {
                    (networkItems + cachedItems)
                        .distinctBy { it.id }
                        .filterNot { it.isExpired() }
                        .also { Timber.d("Online mode: Showing ${it.size} items") }
                }

                NetworkStatus.Unavailable -> {
                    cachedItems
                        .filterNot { it.isExpired() }
                        .also { Timber.d("Offline mode: Showing ${it.size} cached items") }
                }
            }
        }
            .catch { error ->
                Timber.e(error, "Error in repository flow")
                emit(getCachedItems().first())
            }
    }

    private fun getNetworkItemsWithFallback(query: SearchQuery): Flow<List<TimelineItem>> {
        return client.streamTimeline(query)
            .map { listOf(it) }
            .onEach { items ->
                try {
                    cacheManager.cacheItems(items)
                    Timber.d("Cached ${items.size} new items")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to cache items")
                }
            }
            .catch { error ->
                Timber.e(error, "Network error, falling back to cache")
                emit(emptyList())
            }
    }

    private fun getCachedItems(): Flow<List<TimelineItem>> =
        cacheManager.getCachedItems()
            .catch { error ->
                Timber.e(error, "Error fetching cached items")
                emit(emptyList())
            }

    override suspend fun deleteItem(id: PostId) {
        Timber.d("Deleting item from repository: ${id.value}")
        try {
            cacheManager.deleteItem(id)
            Timber.d("Successfully deleted item from cache")
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete item from cache")
            throw e
        }
    }
}