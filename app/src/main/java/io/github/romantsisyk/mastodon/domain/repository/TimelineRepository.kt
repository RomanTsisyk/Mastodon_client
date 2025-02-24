package io.github.romantsisyk.mastodon.domain.repository

import io.github.romantsisyk.mastodon.domain.model.PostId
import io.github.romantsisyk.mastodon.domain.model.SearchQuery
import io.github.romantsisyk.mastodon.domain.model.TimelineItem
import kotlinx.coroutines.flow.Flow

interface TimelineRepository {
    fun getTimelineFlow(query: SearchQuery): Flow<List<TimelineItem>>
    suspend fun updateTimelineItems()
    suspend fun clearExpiredItems()
    suspend fun deleteItem(id: PostId)
}