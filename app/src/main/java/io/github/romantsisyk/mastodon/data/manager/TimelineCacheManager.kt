package io.github.romantsisyk.mastodon.data.manager

import io.github.romantsisyk.mastodon.data.database.TimelineDatabase
import io.github.romantsisyk.mastodon.data.database.TimelineItemEntity.Companion.toEntity
import io.github.romantsisyk.mastodon.domain.model.PostId
import io.github.romantsisyk.mastodon.domain.model.TimelineItem
import io.github.romantsisyk.mastodon.utils.AppConstants.CLEANUP_INTERVAL_MS
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimelineCacheManager @Inject constructor(
    val database: TimelineDatabase,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val isCleanupRunning = AtomicBoolean(false)
    private var cleanupJob: Job? = null

    init {
        Timber.d("Initializing TimelineCacheManager")
        startCleanupJob()
    }

    private fun startCleanupJob() {
        if (isCleanupRunning.getAndSet(true)) {
            Timber.d("Cleanup job is already running")
            return
        }

        Timber.d("Starting cleanup job")
        cleanupJob = scope.launch(SupervisorJob() + dispatcher) {
            try {
                while (true) {
                    Timber.d("Running periodic cleanup")
                    try {
                        val currentTime = System.currentTimeMillis()
                        val deletedCount = database.timelineDao().deleteExpired(currentTime)
                        Timber.d("Cleanup complete. Removed $deletedCount expired items")
                    } catch (e: Exception) {
                        Timber.e(e, "Error during cleanup operation")
                        throw e
                    }
                    delay(CLEANUP_INTERVAL_MS)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error in cleanup job")
                isCleanupRunning.set(false)
                delay(CLEANUP_INTERVAL_MS * 2)
                startCleanupJob()
            } finally {
                isCleanupRunning.set(false)
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
            .flowOn(dispatcher)
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
            Timber.e(e, "Error caching items: ${e.message}")
            throw e
        }
    }

    suspend fun deleteItem(id: PostId) {
        Timber.d("Deleting item with id: ${id.value}")
        try {
            val deletedCount = database.timelineDao().deleteById(id.value)
            Timber.d("Successfully deleted $deletedCount items")
        } catch (e: Exception) {
            Timber.e(e, "Error deleting item: ${e.message}")
            throw e
        }
    }
}