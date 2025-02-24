package io.github.romantsisyk.mastodon.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.romantsisyk.mastodon.utils.AppConstants.DATABASE_VERSION

@Database(
    entities = [TimelineItemEntity::class],
    version = DATABASE_VERSION
)
abstract class TimelineDatabase : RoomDatabase() {
    abstract fun timelineDao(): TimelineDao
}