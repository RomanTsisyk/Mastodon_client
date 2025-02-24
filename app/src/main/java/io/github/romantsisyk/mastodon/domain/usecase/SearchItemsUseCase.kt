package io.github.romantsisyk.mastodon.domain.usecase

import io.github.romantsisyk.mastodon.domain.model.SearchQuery
import io.github.romantsisyk.mastodon.domain.model.TimelineUiItem
import io.github.romantsisyk.mastodon.domain.repository.TimelineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SearchItemsUseCase @Inject constructor(
    private val timelineRepository: TimelineRepository
) {
    operator fun invoke(query: String): Flow<List<TimelineUiItem>> {
        return SearchQuery.create(query)?.let { searchQuery ->
            timelineRepository
                .getTimelineFlow(searchQuery)
                .map { items -> items.map { it.toUiModel() } }
        } ?: emptyFlow()
    }
}