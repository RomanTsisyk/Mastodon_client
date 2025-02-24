package io.github.romantsisyk.mastodon.ui.state

import io.github.romantsisyk.mastodon.domain.model.Account
import io.github.romantsisyk.mastodon.domain.model.PostId
import io.github.romantsisyk.mastodon.domain.model.TimelineItem
import io.github.romantsisyk.mastodon.domain.usecase.DeleteItemUseCase
import io.github.romantsisyk.mastodon.domain.usecase.GetTimelineUseCase
import io.github.romantsisyk.mastodon.utils.AnalyticsTracker
import io.github.romantsisyk.mastodon.utils.NetworkMonitor
import io.github.romantsisyk.mastodon.utils.NetworkStatus
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class TimelineViewModelTest {

    @MockK
    private lateinit var getTimelineUseCase: GetTimelineUseCase

    @MockK
    private lateinit var deleteItemUseCase: DeleteItemUseCase

    @RelaxedMockK
    private lateinit var analyticsTracker: AnalyticsTracker

    @MockK
    private lateinit var networkMonitor: NetworkMonitor

    private val networkStatusFlow = MutableStateFlow<NetworkStatus>(NetworkStatus.Available)

    private lateinit var viewModel: TimelineViewModel
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        every { networkMonitor.networkStatus } returns networkStatusFlow

        // Default mock behavior
        coEvery {
            getTimelineUseCase(any())
        } returns flowOf(emptyList())

        viewModel = TimelineViewModel(
            getTimelineUseCase,
            deleteItemUseCase,
            analyticsTracker,
            networkMonitor
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `Given a new ViewModel When initializing Then state has correct default values`() =
        runTest {
            // Given

            // When
            val initialState = viewModel.uiState.value

            // Then
            assertEquals("", initialState.searchQuery)
            assertTrue(initialState.items.isEmpty())
            assertFalse(initialState.isLoading)
            assertEquals(null, initialState.error)
            assertFalse(initialState.isOfflineMode)
            assertFalse(initialState.hasCache)
        }

    @Test
    fun `Given online status When updating search query Then state updates and search is triggered`() =
        runTest {
            // Given
            val query = "test_query"
            val mockItems = listOf(
                TestDataFactory.createTimelineItem(id = "1"),
                TestDataFactory.createTimelineItem(id = "2")
            )

            coEvery {
                getTimelineUseCase(any())
            } returns flowOf(mockItems)

            // When
            viewModel.updateSearchQuery(query)
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertEquals(query, state.searchQuery)
            assertEquals(2, state.items.size)
            verify { analyticsTracker.trackSearch(query) }
        }

    @Test
    fun `Given an active search When clearing search Then state resets and event is tracked`() =
        runTest {
            // Given
            viewModel.updateSearchQuery("test_query")
            advanceUntilIdle()

            // When
            viewModel.clearSearch()
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertEquals("", state.searchQuery)
            assertTrue(state.items.isEmpty())
            verify { analyticsTracker.trackEvent(AnalyticsTracker.EVENT_SEARCH_CLEARED) }
        }

    @Test
    fun `Given timeline with items When dismissing an item Then item is removed and delete use case is called`() =
        runTest {
            // Given
            val mockItems = listOf(
                TestDataFactory.createTimelineItem(id = "1"),
                TestDataFactory.createTimelineItem(id = "2")
            )

            coEvery {
                getTimelineUseCase(any())
            } returns flowOf(mockItems)

            viewModel.updateSearchQuery("test")
            advanceUntilIdle()

            coJustRun { deleteItemUseCase(any()) }

            // When
            viewModel.dismissItem(PostId("1"))
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertEquals(1, state.items.size)
            assertEquals("2", state.items[0].id.value)
            coVerify { deleteItemUseCase(PostId("1")) }
            verify { analyticsTracker.trackItemDismissed("1") }
        }

    @Test
    fun `Given timeline with expired items When removing expired item Then expired item is removed`() =
        runTest {
            // Given
            val pastItem = TestDataFactory.createTimelineItem(
                id = "expired",
                createdAt = Instant.now().minus(Duration.ofMinutes(10)),
                lifespan = Duration.ofMinutes(5)
            )

            val validItem = TestDataFactory.createTimelineItem(
                id = "valid",
                createdAt = Instant.now(),
                lifespan = Duration.ofMinutes(30)
            )

            val mockItems = listOf(pastItem, validItem)

            coEvery {
                getTimelineUseCase(any())
            } returns flowOf(mockItems)

            viewModel.updateSearchQuery("test")
            advanceUntilIdle()

            coJustRun { deleteItemUseCase(any()) }

            // When
            viewModel.removeExpiredItem(PostId("expired"))
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertEquals(1, state.items.size)
            assertEquals("valid", state.items[0].id.value)
            coVerify { deleteItemUseCase(PostId("expired")) }
        }
}

object TestDataFactory {
    private fun createAccount(
        username: String = "testuser",
        displayName: String = "Test User",
        avatar: String = "https://example.com/avatar.jpg"
    ) = Account(
        username = username,
        displayName = displayName,
        avatar = avatar
    )

    fun createTimelineItem(
        id: String = "test_post",
        content: String = "Test content",
        createdAt: Instant = Instant.now(),
        account: Account = createAccount(),
        lifespan: Duration = Duration.ofMinutes(5)
    ) = TimelineItem(
        id = PostId(id),
        content = content,
        createdAt = createdAt,
        account = account,
        lifespan = lifespan
    )
}