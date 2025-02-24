package io.github.romantsisyk.mastodon.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.romantsisyk.mastodon.domain.model.Account
import io.github.romantsisyk.mastodon.domain.model.PostId
import io.github.romantsisyk.mastodon.domain.model.TimelineItem
import java.time.Duration
import java.time.Instant

@Entity(tableName = "timeline_items")
data class TimelineItemEntity(
    @PrimaryKey val id: String,
    val content: String,
    val createdAt: Long,
    val expiresAt: Long,
    val accountId: String,
    val accountUsername: String,
    val accountDisplayName: String,
    val accountAvatar: String
) {
    fun toDomain() = TimelineItem(
        id = PostId(id),
        content = content,
        createdAt = Instant.ofEpochMilli(createdAt),
        lifespan = Duration.ofMillis(expiresAt - createdAt),
        account = Account(
            username = accountUsername,
            displayName = accountDisplayName,
            avatar = accountAvatar
        )
    )

    companion object {
        fun TimelineItem.toEntity() = TimelineItemEntity(
            id = id.value,
            content = content,
            createdAt = createdAt.toEpochMilli(),
            expiresAt = createdAt.plus(lifespan).toEpochMilli(),
            accountId = account.username,
            accountUsername = account.username,
            accountDisplayName = account.displayName,
            accountAvatar = account.avatar
        )
    }
}