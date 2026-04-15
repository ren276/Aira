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
    fun appNavHostCompilesAndShowsPlaceholders() {
        composeTestRule.setContent {
            AiraNavHost()
        }
        
        // Home is the default destination, so its placeholder should exist initially
        composeTestRule.onNodeWithText("Home Placeholder").assertExists()
    }
}
