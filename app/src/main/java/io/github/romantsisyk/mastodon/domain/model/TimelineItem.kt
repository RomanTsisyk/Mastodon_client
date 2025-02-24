package io.github.romantsisyk.mastodon.domain.model

import io.github.romantsisyk.mastodon.utils.AppConstants.DEFAULT_TIMELINE_ITEM_LIFESPAN
import java.time.Duration
import java.time.Instant

data class TimelineUiItem(
    val id: PostId,
    val author: Account,
    val content: String,
    val createdAt: Instant,
    val lifespan: Duration = DEFAULT_TIMELINE_ITEM_LIFESPAN
)

data class Account(
    val username: String,
    val displayName: String,
    val avatar: String
)

data class TimelineItem(
    val id: PostId,
    val content: String,
    val createdAt: Instant,
    val account: Account,
    val lifespan: Duration = DEFAULT_TIMELINE_ITEM_LIFESPAN
) {
    fun isExpired(): Boolean =
        createdAt.plus(lifespan).isBefore(Instant.now())

    fun toUiModel() = TimelineUiItem(
        id = id,
        author = account,
        content = content,
        createdAt = createdAt,
        lifespan = lifespan
    )
}