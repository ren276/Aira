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
import com.aira.health.presentation.nutrition.scanner.BarcodeScannerGateway
import com.aira.health.presentation.nutrition.scanner.ScannerResult
import io.mockk.coEvery
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
        val mockScanner = mockk<BarcodeScannerGateway>(relaxed = true)
        every { mockRepo.observeNutrition(any(), any()) } returns flowOf(emptyList())
        coEvery { mockScanner.scanBarcode() } returns null

        composeTestRule.setContent {
            NutritionScreen(
                viewModel = NutritionViewModel(mockRepo, mockScanner),
                onNavigateToEdit = {}
            )
        }

        composeTestRule.onNodeWithText("Food name")
            .performTextInput("Oatmeal")

        composeTestRule.onNodeWithText("kcal")
            .performTextInput("250")

        composeTestRule.onNodeWithText("Save Entry")
            .performClick()

        coVerify(exactly = 1) {
            mockRepo.addNutritionLog(match { it.foodName == "Oatmeal" && it.calories == 250f && it.logMethod == "manual" })
        }
    }

    @Test
    fun scannerUnavailable_showsErrorAndDoesNotSave() {
        val mockRepo = mockk<NutritionRepository>(relaxed = true)
        val mockScanner = mockk<BarcodeScannerGateway>(relaxed = true)
        every { mockRepo.observeNutrition(any(), any()) } returns flowOf(emptyList())
        coEvery { mockScanner.scanBarcode() } returns null

        composeTestRule.setContent {
            NutritionScreen(
                viewModel = NutritionViewModel(mockRepo, mockScanner),
                onNavigateToEdit = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Scan")
            .performClick()

        composeTestRule.onNodeWithText("Scanner is unavailable in this build. Enter food manually.")
            .assertIsDisplayed()

        coVerify(exactly = 0) {
            mockRepo.addNutritionLog(any())
        }
    }

    @Test
    fun scannerDraft_populatesFields_savesNutrition() {
        val mockRepo = mockk<NutritionRepository>(relaxed = true)
        val mockScanner = mockk<BarcodeScannerGateway>(relaxed = true)
        every { mockRepo.observeNutrition(any(), any()) } returns flowOf(emptyList())
        coEvery { mockScanner.scanBarcode() } returns ScannerResult(
            barcode = "123",
            foodName = "Scanned Food",
            calories = 200f,
            proteinG = null,
            carbsG = null,
            fatG = null
        )

        composeTestRule.setContent {
            NutritionScreen(
                viewModel = NutritionViewModel(mockRepo, mockScanner),
                onNavigateToEdit = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Scan")
            .performClick()

        composeTestRule.onNodeWithText("Save Entry")
            .performClick()

        coVerify(exactly = 1) {
            mockRepo.addNutritionLog(match { it.foodName == "Scanned Food" && it.calories == 200f && it.logMethod == "barcode" })
        }
    }

    @Test
    fun history_clickDelete_showsConfirmation_deletesEntry() {
        val mockRepo = mockk<NutritionRepository>(relaxed = true)
        val mockScanner = mockk<BarcodeScannerGateway>(relaxed = true)
        val flow = MutableStateFlow(
            listOf(
                NutritionLog(
                    id = 1L,
                    timestamp = Instant.now().toEpochMilli(),
                    foodName = "Dummy Food",
                    calories = 100f,
                    proteinG = 0f,
                    carbsG = 0f,
                    fatG = 0f,
                    logMethod = "manual"
                )
            )
        )
        every { mockRepo.observeNutrition(any(), any()) } returns flow

        composeTestRule.setContent {
            NutritionScreen(
                viewModel = NutritionViewModel(mockRepo, mockScanner),
                onNavigateToEdit = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Delete").performClick()

        composeTestRule.onNodeWithText("Delete meal log").assertIsDisplayed()
        composeTestRule.onNodeWithText("Remove this log permanently?").assertIsDisplayed()

        composeTestRule.onNodeWithText("Delete").performClick()

        coVerify(exactly = 1) { mockRepo.deleteNutritionLog(1L) }
    }
}
