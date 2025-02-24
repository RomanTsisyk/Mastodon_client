package io.github.romantsisyk.mastodon.data.manager

import io.github.romantsisyk.mastodon.data.database.TimelineDatabase
import io.github.romantsisyk.mastodon.data.database.TimelineItemEntity.Companion.toEntity
import io.github.romantsisyk.mastodon.domain.model.PostId
import io.github.romantsisyk.mastodon.domain.model.TimelineItem
import io.github.romantsisyk.mastodon.utils.AppConstants.CLEANUP_INTERVAL_MS
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimelineCacheManager @Inject constructor(
    private val database: TimelineDatabase,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    init {
        Timber.d("Initializing TimelineCacheManager")
        startCleanupJob()
    }

    private fun startCleanupJob() {
        Timber.d("Starting cleanup job")
        scope.launch(dispatcher) {
            try {
                while (true) {
                    Timber.d("Running periodic cleanup")
                    val currentTime = System.currentTimeMillis()
                    val deletedCount = database.timelineDao().deleteExpired(currentTime)
                    Timber.d("Cleanup complete. Removed $deletedCount expired items")
                    kotlinx.coroutines.delay(CLEANUP_INTERVAL_MS)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error in cleanup job")
                startCleanupJob()
            }
        }
    }

    fun getCachedItems(): Flow<List<TimelineItem>> {
        Timber.d("Getting cached items flow")
        return database.timelineDao()
            .getAllFlow()
            .map { entities ->
                entities.map { it.toDomain() }
            }
            .catch { e ->
                Timber.e(e, "Error mapping cached items")
                emit(emptyList())
            }
    }

    suspend fun cacheItems(items: List<TimelineItem>) {
        if (items.isEmpty()) return

        Timber.d("Caching ${items.size} items")
        try {
            database.timelineDao().insertAll(
                items.map { item -> item.toEntity() }
            )
            Timber.d("Successfully cached ${items.size} items")
        } catch (e: Exception) {
            Timber.e(e, "Error caching items")
            throw e
        }
    }

    suspend fun deleteItem(id: PostId) {
        Timber.d("Deleting item with id: ${id.value}")
        try {
            val deletedCount = database.timelineDao().deleteById(id.value)
            Timber.d("Successfully deleted $deletedCount items")
        } catch (e: Exception) {
            Timber.e(e, "Error deleting item")
            throw e
        }
    }
}