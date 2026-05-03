package com.aira.health.presentation.dashboard.fueltrain

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aira.health.presentation.common.components.AiraHeader
import com.aira.health.presentation.nutrition.NutritionScreen
import com.aira.health.presentation.train.TrainScreen
import com.aira.health.presentation.theme.Theme

@Composable
fun FuelTrainScreen(
    onNavigateToNutritionEdit: (Long) -> Unit,
    onNavigateToTrainEdit: (Long) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Nutrition", "Training")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.dominant)
            .padding(top = 32.dp)
    ) {
        AiraHeader(
            title = if (selectedTab == 0) "Fuel" else "Train",
            subtitle = if (selectedTab == 0) "Metabolic Intake" else "Activity & Load",
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)
        )

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = Theme.colors.accent,
            edgePadding = 16.dp,
            divider = {},
            indicator = { tabPositions ->
                // Standard indicator or custom
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (selectedTab == index) Theme.colors.accent else Theme.colors.onSurfaceVariant
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> NutritionScreen(onNavigateToEdit = onNavigateToNutritionEdit)
            1 -> TrainScreen(onNavigateToEdit = onNavigateToTrainEdit)
        }
    }
}
