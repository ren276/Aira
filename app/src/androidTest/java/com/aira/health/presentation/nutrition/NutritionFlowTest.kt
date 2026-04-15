package com.aira.health.presentation.nutrition

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.aira.health.data.local.model.NutritionLog
import com.aira.health.domain.repository.NutritionRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class NutritionFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun quickAdd_validInputs_savesNutrition() {
        val mockRepo = mockk<NutritionRepository>(relaxed = true)
        every { mockRepo.observeNutrition(any(), any()) } returns flowOf(emptyList())

        composeTestRule.setContent {
            NutritionScreen(
                viewModel = NutritionViewModel(mockRepo),
                onNavigateToEdit = {}
            )
        }

        // Enter food name
        composeTestRule.onNodeWithText("Food Name")
            .performTextClearance()
        composeTestRule.onNodeWithText("Food Name")
            .performTextInput("Oatmeal")

        // Enter calories
        composeTestRule.onNodeWithText("Calories")
            .performTextClearance()
        composeTestRule.onNodeWithText("Calories")
            .performTextInput("250")

        // Save
        composeTestRule.onNodeWithText("Save Entry")
            .performClick()

        // Verify repo interaction
        coVerify(exactly = 1) { 
            mockRepo.addNutritionLog(match { it.foodName == "Oatmeal" && it.calories == 250f && it.logMethod == "manual" })
        }
    }

    @Test
    fun scannerDraft_populatesFields_savesNutrition() {
        val mockRepo = mockk<NutritionRepository>(relaxed = true)
        every { mockRepo.observeNutrition(any(), any()) } returns flowOf(emptyList())

        composeTestRule.setContent {
            NutritionScreen(
                viewModel = NutritionViewModel(mockRepo),
                onNavigateToEdit = {}
            )
        }

        // Click scan barcode (which our UI simulates by populating fields)
        composeTestRule.onNodeWithContentDescription("Scan Barcode")
            .performClick()

        // Save
        composeTestRule.onNodeWithText("Save Entry")
            .performClick()

        // Verify repo interaction with scanned data
        coVerify(exactly = 1) { 
            mockRepo.addNutritionLog(match { it.foodName == "Scanned Food" && it.calories == 200f && it.logMethod == "barcode" })
        }
    }

    @Test
    fun history_clickDelete_showsConfirmation_deletesEntry() {
        val mockRepo = mockk<NutritionRepository>(relaxed = true)
        val flow = MutableStateFlow(emptyList<NutritionLog>())
        every { mockRepo.observeNutrition(any(), any()) } returns flow

        composeTestRule.setContent {
            NutritionScreen(
                viewModel = NutritionViewModel(mockRepo),
                onNavigateToEdit = {}
            )
        }

        // Click delete on the static dummy item in the History section
        composeTestRule.onNodeWithContentDescription("Delete").performClick()

        // Confirm dialog text (from UI-SPEC)
        composeTestRule.onNodeWithText("Delete nutrition entry").assertIsDisplayed()
        composeTestRule.onNodeWithText("Remove this meal log permanently? This action cannot be undone.").assertIsDisplayed()

        // Confirm deletion
        composeTestRule.onNodeWithText("Delete").performClick()

        // Verify repository deletion
        coVerify(exactly = 1) { mockRepo.deleteNutritionLog(1L) }
    }
}
