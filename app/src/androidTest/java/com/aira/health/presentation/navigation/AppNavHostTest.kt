package com.aira.health.presentation.navigation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppNavHostTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun appNavHostCompilesAndShowsHomeScreen() {
        composeTestRule.setContent {
            AiraNavHost()
        }

        // Bottom nav in locked IA should render these primary entries.
        composeTestRule.onNodeWithText("HOME").assertExists()
        composeTestRule.onNodeWithText("INSIGHTS").assertExists()
    }
}
