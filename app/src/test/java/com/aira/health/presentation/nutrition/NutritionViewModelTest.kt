package com.aira.health.presentation.nutrition

import app.cash.turbine.test
import com.aira.health.data.local.model.NutritionLog
import com.aira.health.domain.repository.NutritionRepository
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
class NutritionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockRepository: NutritionRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockRepository = mockk(relaxed = true)
        every { mockRepository.observeNutrition(any(), any()) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `quick add validates empty food name`() = runTest(testDispatcher) {
        val viewModel = NutritionViewModel(mockRepository)
        viewModel.onFoodNameChange("")
        viewModel.saveQuickAdd()
        
        viewModel.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertNotNull("Should have input error for empty food name", state.inputError)
        }
        
        coVerify(exactly = 0) { mockRepository.addNutritionLog(any()) }
    }

    @Test
    fun `quick add succeeds with valid inputs mapped to repo`() = runTest(testDispatcher) {
        val viewModel = NutritionViewModel(mockRepository)
        viewModel.onFoodNameChange("Apple")
        viewModel.onCaloriesChange("95")
        viewModel.saveQuickAdd()

        advanceUntilIdle()

        coVerify(exactly = 1) { 
            mockRepository.addNutritionLog(withArg { 
                assertEquals("Apple", it.foodName)
                assertEquals(95f, it.calories)
                assertEquals("manual", it.logMethod)
            }) 
        }

        viewModel.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertNull("Input error should be cleared", state.inputError)
            assertEquals("Inputs should be cleared on success", "", state.quickAddFoodName)
            assertEquals("Inputs should be cleared on success", "", state.quickAddCalories)
        }
    }

    @Test
    fun `initiate delete prompts confirmation state`() = runTest(testDispatcher) {
        val viewModel = NutritionViewModel(mockRepository)
        viewModel.initiateDelete(123L)

        viewModel.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals("Should store ID to confirm delete", 123L, state.showDeleteConfirmationForId)
        }
    }

    @Test
    fun `confirm delete executes block and clears state`() = runTest(testDispatcher) {
        val viewModel = NutritionViewModel(mockRepository)
        viewModel.initiateDelete(456L)
        advanceUntilIdle()
        
        viewModel.confirmDelete()
        advanceUntilIdle()

        coVerify(exactly = 1) { mockRepository.deleteNutritionLog(456L) }

        viewModel.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertNull("Confirmation should be dismissed", state.showDeleteConfirmationForId)
        }
    }

    @Test
    fun `scanner draft populates editable state`() = runTest(testDispatcher) {
        val viewModel = NutritionViewModel(mockRepository)
        viewModel.onScannerDraftReceived(foodName = "Banana", calories = 105f)

        viewModel.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals("Banana", state.quickAddFoodName)
            assertEquals("105.0", state.quickAddCalories)
            assertEquals("barcode", state.currentLogMethod)
        }
    }
}
