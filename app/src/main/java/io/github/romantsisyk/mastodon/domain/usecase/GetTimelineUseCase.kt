package io.github.romantsisyk.mastodon.domain.usecase

import io.github.romantsisyk.mastodon.domain.model.SearchQuery
import io.github.romantsisyk.mastodon.domain.model.TimelineItem
import io.github.romantsisyk.mastodon.domain.repository.TimelineRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTimelineUseCase @Inject constructor(
    private val repository: TimelineRepository
) {
    operator fun invoke(query: SearchQuery): Flow<List<TimelineItem>> =
        repository.getTimelineFlow(query)
}