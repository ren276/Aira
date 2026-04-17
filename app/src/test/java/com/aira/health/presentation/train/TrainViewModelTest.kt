package com.aira.health.presentation.train

import app.cash.turbine.test
import com.aira.health.data.local.model.WorkoutSession
import com.aira.health.domain.repository.WorkoutRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockRepository: WorkoutRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockRepository = mockk(relaxed = true)
        every { mockRepository.observeWorkouts(any(), any()) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `quick add validates empty duration`() = runTest(testDispatcher) {
        val viewModel = TrainViewModel(mockRepository)
        viewModel.onDurationChange("") // empty string
        viewModel.saveQuickAdd()
        
        viewModel.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertNotNull("Should have input error for empty duration", state.inputError)
        }
        
        coVerify(exactly = 0) { mockRepository.addWorkout(any()) }
    }

    @Test
    fun `quick add succeeds with valid inputs mapped to repo`() = runTest(testDispatcher) {
        val viewModel = TrainViewModel(mockRepository)
        viewModel.onExerciseChange("Cycling")
        viewModel.onDurationChange("60")
        viewModel.saveQuickAdd()

        advanceUntilIdle()

        coVerify(exactly = 1) { 
            mockRepository.addWorkout(withArg { 
                assertEquals("Cycling", it.exerciseType)
                assertEquals(60, it.durationMin)
                assertEquals("com.aira.health.manual", it.sourcePackage)
            }) 
        }

        viewModel.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertNull("Input error should be cleared", state.inputError)
            assertEquals("Inputs should be cleared on success", "", state.quickAddExercise)
            assertEquals("Inputs should be cleared on success", "", state.quickAddDurationMin)
        }
    }

    @Test
    fun `initiate delete prompts confirmation state`() = runTest(testDispatcher) {
        val viewModel = TrainViewModel(mockRepository)
        viewModel.initiateDelete(123L)

        viewModel.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals("Should store ID to confirm delete", 123L, state.showDeleteConfirmationForId)
        }
    }

    @Test
    fun `confirm delete executes block and clears state`() = runTest(testDispatcher) {
        val viewModel = TrainViewModel(mockRepository)
        viewModel.initiateDelete(456L)
        advanceUntilIdle()
        
        viewModel.confirmDelete()
        advanceUntilIdle()

        coVerify(exactly = 1) { mockRepository.deleteWorkout(456L) }

        viewModel.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertNull("Confirmation should be dismissed", state.showDeleteConfirmationForId)
        }
    }
}
