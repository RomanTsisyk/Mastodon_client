package io.github.romantsisyk.mastodon.data.database

import io.github.romantsisyk.mastodon.domain.model.Account
import io.github.romantsisyk.mastodon.domain.model.PostId
import io.github.romantsisyk.mastodon.domain.model.TimelineItem
import io.github.romantsisyk.mastodon.utils.AppConstants.DEFAULT_TIMELINE_ITEM_LIFESPAN
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

data class TimelineItemDto(
    val id: String,
    val content: String,
    val created_at: String,
    val account: AccountDto
) {
    fun toDomainModel() = TimelineItem(
        id = PostId(id),
        content = content,
        createdAt = OffsetDateTime.parse(created_at, DateTimeFormatter.ISO_DATE_TIME).toInstant(),
        lifespan = DEFAULT_TIMELINE_ITEM_LIFESPAN,
        account = account.toDomain()
    )
}

data class AccountDto(
    val username: String,
    val display_name: String,
    val avatar: String
) {
    fun toDomain() = Account(
        username = username,
        displayName = display_name,
        avatar = avatar
    )
}