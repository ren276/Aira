package com.aira.health.presentation.train

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.aira.health.data.local.model.WorkoutSession
import com.aira.health.domain.repository.WorkoutRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import java.time.Instant

/**
 * Compose Instrumentation tests for the Train Quick-Add and History flow (D-13).
 */
class TrainFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun quickAdd_validInputs_savesWorkout() {
        val mockRepo = mockk<WorkoutRepository>(relaxed = true)
        // No history
        every { mockRepo.observeWorkouts(any(), any()) } returns flowOf(emptyList())

        composeTestRule.setContent {
            TrainScreen(
                viewModel = TrainViewModel(mockRepo),
                onNavigateToEdit = {}
            )
        }

        // Enter exercise
        composeTestRule.onNodeWithContentDescription("exercise input")
            .performTextClearance()
        composeTestRule.onNodeWithContentDescription("exercise input")
            .performTextInput("Yoga")

        // Enter duration
        composeTestRule.onNodeWithContentDescription("duration input")
            .performTextClearance()
        composeTestRule.onNodeWithContentDescription("duration input")
            .performTextInput("30")

        // Save
        composeTestRule.onNodeWithContentDescription("save quick add")
            .performClick()

        // Verify repo interaction
        coVerify(exactly = 1) { 
            mockRepo.addWorkout(match { it.exerciseType == "Yoga" && it.durationMin == 30 })
        }
    }

    @Test
    fun history_showsWorkouts_canEdit() {
        val mockRepo = mockk<WorkoutRepository>(relaxed = true)
        val testWorkouts = listOf(
            WorkoutSession(id = 1L, exerciseType = "CrossFit", durationMin = 45, startTime = Instant.now().toEpochMilli(), endTime = Instant.now().toEpochMilli(), sourcePackage = "test")
        )
        val flow = MutableStateFlow(testWorkouts)
        every { mockRepo.observeWorkouts(any(), any()) } returns flow

        var editClickedForId: Long? = null

        composeTestRule.setContent {
            TrainScreen(
                viewModel = TrainViewModel(mockRepo),
                onNavigateToEdit = { editClickedForId = it }
            )
        }

        // Wait to show "CrossFit" text
        composeTestRule.onNodeWithText("CrossFit").assertIsDisplayed()

        // Click edit
        composeTestRule.onNodeWithContentDescription("edit CrossFit").performClick()

        assert(editClickedForId == 1L) { "Should navigate to edit with correct ID" }
    }

    @Test
    fun history_clickDelete_showsConfirmation_deletesWorkout() {
        val mockRepo = mockk<WorkoutRepository>(relaxed = true)
        val testWorkouts = listOf(
            WorkoutSession(id = 2L, exerciseType = "Running", durationMin = 60, startTime = Instant.now().toEpochMilli(), endTime = Instant.now().toEpochMilli(), sourcePackage = "test")
        )
        every { mockRepo.observeWorkouts(any(), any()) } returns flowOf(testWorkouts)

        composeTestRule.setContent {
            TrainScreen(
                viewModel = TrainViewModel(mockRepo),
                onNavigateToEdit = {}
            )
        }

        // Click delete on item
        composeTestRule.onNodeWithContentDescription("delete Running").performClick()

        // Confirm dialog text (D-13 constraint check)
        composeTestRule.onNodeWithText("Delete Workout").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "Are you sure you want to delete this workout? This action cannot be undone and will recalculate your daily strain.",
            substring = true
        ).assertIsDisplayed()

        // Confirm deletion
        composeTestRule.onNodeWithContentDescription("confirm delete").performClick()

        // Verify repository deletion
        coVerify(exactly = 1) { mockRepo.deleteWorkout(2L) }
    }
}
