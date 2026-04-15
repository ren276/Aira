package com.aira.health.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AiraNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            // Placeholder for AiraBottomBar which will be implemented later
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AiraRoutes.HOME,
            modifier = modifier.padding(innerPadding)
        ) {
            composable(AiraRoutes.HOME) { Text("Home Placeholder") }
            composable(AiraRoutes.INSIGHTS) { Text("Insights Placeholder") }
            composable(AiraRoutes.TRAIN) { Text("Train Placeholder") }
            composable(AiraRoutes.NUTRITION) { Text("Nutrition Placeholder") }
            composable(AiraRoutes.SETTINGS) { Text("Settings Placeholder") }
        }
    }
}
