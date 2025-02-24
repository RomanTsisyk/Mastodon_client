package io.github.romantsisyk.mastodon.domain.usecase

import io.github.romantsisyk.mastodon.domain.model.PostId
import io.github.romantsisyk.mastodon.domain.repository.TimelineRepository
import javax.inject.Inject

class DeleteItemUseCase @Inject constructor(
    private val timelineRepository: TimelineRepository
) {
    suspend operator fun invoke(id: PostId) =
        timelineRepository.deleteItem(id)
}