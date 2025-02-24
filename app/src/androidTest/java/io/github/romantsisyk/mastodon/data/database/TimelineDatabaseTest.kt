package io.github.romantsisyk.mastodon.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jakewharton.threetenabp.AndroidThreeTen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class TimelineDatabaseRepositoryTest {

    private lateinit var timelineDao: TimelineDao
    private lateinit var db: TimelineDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AndroidThreeTen.init(context)

        db = Room.inMemoryDatabaseBuilder(
            context, TimelineDatabase::class.java
        ).build()
        timelineDao = db.timelineDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun given_new_timeline_items_when_inserting_items_then_items_are_successfully_stored_in_database() =
        runTest {
            // Given
            val currentTime = Instant.now().toEpochMilli()
            val timelineItems = listOf(
                TimelineItemEntity(
                    id = "test_id_1",
                    content = "Test content 1",
                    createdAt = currentTime,
                    expiresAt = currentTime + 300000,
                    accountId = "account1",
                    accountUsername = "user1",
                    accountDisplayName = "User 1",
                    accountAvatar = "avatar1"
                ),
                TimelineItemEntity(
                    id = "test_id_2",
                    content = "Test content 2",
                    createdAt = currentTime,
                    expiresAt = currentTime + 300000,
                    accountId = "account2",
                    accountUsername = "user2",
                    accountDisplayName = "User 2",
                    accountAvatar = "avatar2"
                )
            )

            // When
            timelineDao.insertAll(timelineItems)
            val storedItems = timelineDao.getAllFlow().first()

            // Then
            assertEquals(timelineItems.size, storedItems.size)
            assertTrue(storedItems.containsAll(timelineItems))
        }

    @Test
    fun given_multiple_timeline_items_with_expired_items_when_deleting_expired_items_then_only_non_expired_items_remain() =
        runTest {
            // Given
            val currentTime = Instant.now().toEpochMilli()
            val activeItem = TimelineItemEntity(
                id = "active_id",
                content = "Active content",
                createdAt = currentTime,
                expiresAt = currentTime + 300000,
                accountId = "active_account",
                accountUsername = "active_user",
                accountDisplayName = "Active User",
                accountAvatar = "active_avatar"
            )
            val expiredItem = TimelineItemEntity(
                id = "expired_id",
                content = "Expired content",
                createdAt = currentTime - 600000,
                expiresAt = currentTime - 300000,
                accountId = "expired_account",
                accountUsername = "expired_user",
                accountDisplayName = "Expired User",
                accountAvatar = "expired_avatar"
            )

            // When
            timelineDao.insertAll(listOf(activeItem, expiredItem))
            timelineDao.deleteExpired(currentTime)
            val remainingItems = timelineDao.getAllFlow().first()

            // Then
            assertEquals(1, remainingItems.size)
            assertEquals(activeItem, remainingItems[0])
        }

    @Test
    fun given_multiple_timeline_items_when_deleting_all_items_then_database_becomes_empty() =
        runTest {

            // Given
            val currentTime = Instant.now().toEpochMilli()
            val timelineItems = listOf(
                TimelineItemEntity(
                    id = "test_id_1",
                    content = "Test content 1",
                    createdAt = currentTime,
                    expiresAt = currentTime + 300000,
                    accountId = "account1",
                    accountUsername = "user1",
                    accountDisplayName = "User 1",
                    accountAvatar = "avatar1"
                ),
                TimelineItemEntity(
                    id = "test_id_2",
                    content = "Test content 2",
                    createdAt = currentTime,
                    expiresAt = currentTime + 300000,
                    accountId = "account2",
                    accountUsername = "user2",
                    accountDisplayName = "User 2",
                    accountAvatar = "avatar2"
                )
            )

            // When
            timelineDao.insertAll(timelineItems)
            timelineDao.deleteAll()
            val remainingItems = timelineDao.getAllFlow().first()

            // Then
            assertTrue(remainingItems.isEmpty())
        }
}