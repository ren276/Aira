package com.aira.health.presentation.fueltrain

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aira.health.presentation.nutrition.NutritionScreen
import com.aira.health.presentation.theme.Theme
import com.aira.health.presentation.train.TrainScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelTrainScreen(
    onNavigateToNutritionEdit: (Long) -> Unit,
    onNavigateToTrainEdit: (Long) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Fuel", "Train")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Theme.colors.dominant,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Fuel & Train", color = Color.White, fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = Theme.colors.primaryContainer,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Theme.colors.primaryContainer
                        )
                    },
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { 
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            selectedContentColor = Theme.colors.primaryContainer,
                            unselectedContentColor = Theme.colors.onSurfaceVariant
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> NutritionScreen(onNavigateToEdit = onNavigateToNutritionEdit)
                1 -> TrainScreen(onNavigateToEdit = onNavigateToTrainEdit)
            }
        }
    }
}
