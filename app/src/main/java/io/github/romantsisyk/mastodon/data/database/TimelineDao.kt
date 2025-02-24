package io.github.romantsisyk.mastodon.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TimelineDao {
    @Query("SELECT * FROM timeline_items ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<TimelineItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TimelineItemEntity>)

    @Query("DELETE FROM timeline_items WHERE expiresAt <= :currentTime")
    suspend fun deleteExpired(currentTime: Long)

    @Query("DELETE FROM timeline_items")
    suspend fun deleteAll()

    @Query("DELETE FROM timeline_items WHERE id = :id")
    suspend fun deleteById(id: String)
}