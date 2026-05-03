package com.aira.health.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aira.health.presentation.assistant.AiraAssistantScreen
import com.aira.health.presentation.dashboard.body.BodyScreen
import com.aira.health.presentation.dashboard.coach.CoachScreen
import com.aira.health.presentation.dashboard.details.MetricDetailRoute
import com.aira.health.presentation.fueltrain.FuelTrainScreen
import com.aira.health.presentation.dashboard.home.HomeDashboardScreen
import com.aira.health.presentation.nutrition.NutritionEditScreen
import com.aira.health.presentation.settings.SettingsScreen
import com.aira.health.presentation.train.TrainEditScreen

@Composable
fun AiraNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val currentRoute = currentDestination?.route

    // Use Box to overlap the floating nav over the content
    Box(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = AiraRoutes.HOME,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(AiraRoutes.HOME) {
                // Ensure content pads the bottom for the floating nav
                HomeDashboardScreen(
                    onMetricTap = { metricId ->
                        navController.navigate(AiraRoutes.metricDetailRoute(metricId))
                    }
                )
            }

            composable(AiraRoutes.INSIGHTS) {
                BodyScreen(
                    onOpenCoach = { navController.navigate(AiraRoutes.COACH) }
                )
            }

            composable(AiraRoutes.COACH) {
                CoachScreen()
            }

            composable(
                route = AiraRoutes.INSIGHTS_DETAIL,
                arguments = listOf(navArgument("metricId") { type = NavType.StringType })
            ) {
                MetricDetailRoute(
                    onNavigateUp = { navController.popBackStack() }
                )
            }

            composable(AiraRoutes.FUEL_TRAIN) {
                FuelTrainScreen(
                    onNavigateToNutritionEdit = { entryId ->
                        navController.navigate(AiraRoutes.nutritionEditRoute(entryId))
                    },
                    onNavigateToTrainEdit = { workoutId ->
                        navController.navigate(AiraRoutes.trainEditRoute(workoutId))
                    }
                )
            }

            composable(
                route = AiraRoutes.TRAIN_EDIT,
                arguments = listOf(navArgument("workoutId") { type = NavType.LongType })
            ) { backStackEntry ->
                val workoutId = backStackEntry.arguments?.getLong("workoutId") ?: return@composable
                TrainEditScreen(
                    workoutId = workoutId,
                    onNavigateUp = { navController.popBackStack() }
                )
            }

            composable(AiraRoutes.ASSISTANT) {
                AiraAssistantScreen()
            }

            composable(
                route = AiraRoutes.NUTRITION_EDIT,
                arguments = listOf(navArgument("entryId") { type = NavType.LongType })
            ) { backStackEntry ->
                val entryId = backStackEntry.arguments?.getLong("entryId") ?: return@composable
                NutritionEditScreen(
                    entryId = entryId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(AiraRoutes.SETTINGS) {
                SettingsScreen(
                    onOpenAccount = { navController.navigate(AiraRoutes.ACCOUNT) },
                    onOpenDataConfidence = { navController.navigate(AiraRoutes.DATA_CONFIDENCE) },
                    onOpenCorrections = { navController.navigate(AiraRoutes.DATA_CORRECTIONS) },
                    onOpenPredictions = { navController.navigate(AiraRoutes.INSIGHTS_PREDICTIONS) },
                    onOpenWeeklyReport = { navController.navigate(AiraRoutes.WEEKLY_REPORT) },
                    onOpenWhatIfSimulator = { navController.navigate(AiraRoutes.WHAT_IF) }
                )
            }

            composable(AiraRoutes.ACCOUNT) {
                com.aira.health.presentation.supplementary.AccountScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(AiraRoutes.DATA_CONFIDENCE) {
                com.aira.health.presentation.supplementary.DataConfidenceScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(AiraRoutes.WEEKLY_REPORT) {
                com.aira.health.presentation.supplementary.WeeklyReportScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(AiraRoutes.WHAT_IF) {
                com.aira.health.presentation.supplementary.WhatIfSimulatorScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(AiraRoutes.DATA_CORRECTIONS) {
                com.aira.health.presentation.supplementary.DataCorrectionsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(AiraRoutes.INSIGHTS_PREDICTIONS) {
                com.aira.health.presentation.supplementary.InsightsPredictionsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        // Show navigation only on top-level routes
        val topLevelRoutes = listOf(
            AiraRoutes.HOME, 
            AiraRoutes.INSIGHTS,
            AiraRoutes.FUEL_TRAIN, 
            AiraRoutes.ASSISTANT,
            AiraRoutes.SETTINGS
        )
        
        if (currentRoute in topLevelRoutes) {
            AiraBottomNav(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
