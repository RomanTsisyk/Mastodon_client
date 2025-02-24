package io.github.romantsisyk.mastodon.domain.usecase

import io.github.romantsisyk.mastodon.domain.model.PostId
import io.github.romantsisyk.mastodon.domain.repository.TimelineRepository
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.io.IOException

@ExperimentalCoroutinesApi
class DeleteItemUseCaseTest {

    private lateinit var timelineRepository: TimelineRepository
    private lateinit var deleteItemUseCase: DeleteItemUseCase

    @Before
    fun setup() {
        timelineRepository = mockk()
        deleteItemUseCase = DeleteItemUseCase(timelineRepository)
    }

    @Test
    fun `Given a post ID When invoking delete use case Then repository delete operation is called`() =
        runTest {
            // Given
            val postId = PostId("test_post_id")
            coJustRun { timelineRepository.deleteItem(postId) }

            // When
            deleteItemUseCase(postId)

            // Then
            coVerify(exactly = 1) { timelineRepository.deleteItem(postId) }
        }

    @Test(expected = IOException::class)
    fun `Given a repository that throws exception When invoking delete use case Then exception is propagated`() =
        runTest {
            // Given
            val postId = PostId("test_post_id")
            coEvery { timelineRepository.deleteItem(postId) } throws IOException("Network error")

            // When
            deleteItemUseCase(postId)

            // Then: expected = IOException::class
        }

    @Test
    fun `Given multiple post IDs When invoking delete use case for each Then repository is called with correct IDs`() =
        runTest {
            // Given
            val postId1 = PostId("post_1")
            val postId2 = PostId("post_2")
            coJustRun { timelineRepository.deleteItem(any()) }

            // When
            deleteItemUseCase(postId1)
            deleteItemUseCase(postId2)

            // Then
            coVerify(exactly = 1) { timelineRepository.deleteItem(postId1) }
            coVerify(exactly = 1) { timelineRepository.deleteItem(postId2) }
        }
}