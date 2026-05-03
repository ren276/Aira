package com.aira.health.presentation.train

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aira.health.data.local.model.WorkoutSession
import androidx.hilt.navigation.compose.hiltViewModel
import com.aira.health.presentation.common.components.GlassContainer
import com.aira.health.presentation.common.components.PredictiveChip
import com.aira.health.presentation.common.components.SparklineChart
import com.aira.health.presentation.theme.AiraSpacing
import com.aira.health.presentation.theme.Theme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainScreen(
    modifier: Modifier = Modifier,
    viewModel: TrainViewModel = hiltViewModel(),
    onNavigateToEdit: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.showDeleteConfirmationForId != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            title = { Text("Delete Workout") },
            text = { Text("Are you sure you want to delete this workout? This action cannot be undone and will recalculate your daily strain.") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDelete() }) {
                    Text("Delete", color = Theme.colors.destructive)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) {
                    Text("Cancel", color = Theme.colors.onSurfaceVariant)
                }
            },
            containerColor = Theme.colors.surfaceContainerHigh
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                StrengthBuilderCard(
                    onAddSet = { /* TODO */ },
                    recentPr = "120kg Deadlift"
                )
            }

            if (uiState.history.isNotEmpty()) {
                item {
                    LiveSessionCard(session = uiState.history.firstOrNull())
                }
                item {
                    CardioLoadCard(history = uiState.history)
                }
                if (uiState.vo2Max != null) {
                    item {
                        Vo2MaxCard(vo2Max = uiState.vo2Max!!)
                    }
                }
            }

            item {
                Text(
                    text = "Activity Log",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            if (uiState.isHistoryLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Theme.colors.accent)
                    }
                }
            } else if (uiState.history.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "No workouts logged recently.",
                            color = Theme.colors.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(uiState.history, key = { it.id }) { session ->
                    HistoryItem(
                        session = session,
                        onEdit = { onNavigateToEdit(session.id) },
                        onDelete = { viewModel.initiateDelete(session.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StrengthBuilderCard(
    onAddSet: () -> Unit,
    recentPr: String
) {
    GlassContainer(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onAddSet),
        cornerRadius = 24.dp
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(
                        text = "STRENGTH BUILDER",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                        color = Theme.colors.accent
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Track your progressive overload and PRs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Theme.colors.accent.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Theme.colors.accent)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Theme.colors.surfaceContainerHighest)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "LATEST PR", style = MaterialTheme.typography.labelSmall, color = Theme.colors.onSurfaceVariant)
                Text(text = recentPr, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
            }
        }
    }
}

@Composable
private fun HistoryItem(
    session: WorkoutSession,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm")
    val dateStr = Instant.ofEpochMilli(session.startTime)
        .atZone(ZoneId.systemDefault())
        .format(formatter)

    GlassContainer(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Theme.colors.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        session.exerciseType.take(1).uppercase(),
                        color = Theme.colors.accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = session.exerciseType,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "$dateStr • ${session.durationMin} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = Theme.colors.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Theme.colors.destructive)
            }
        }
    }
}

@Composable
private fun LiveSessionCard(session: WorkoutSession?) {
    GlassContainer(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            PredictiveChip(text = "RECENT SESSION", stateColor = Theme.colors.tertiaryColor)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(
                        text = session?.exerciseType ?: "Active Recovery",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ZONE ${if ((session?.avgHr ?: 0) > 150) "4" else "2"} THRESHOLD",
                        style = MaterialTheme.typography.labelSmall,
                        color = Theme.colors.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${session?.maxHr ?: session?.avgHr ?: "--"}",
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = Theme.colors.tertiaryColor
                    )
                    Text(
                        text = " BPM",
                        style = MaterialTheme.typography.titleSmall,
                        color = Theme.colors.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CardioLoadCard(history: List<WorkoutSession>) {
    val cardioData = history.map { it.cardioLoadContribution }.take(14).reversed()
    val avgLoad = cardioData.average().toFloat()
    
    GlassContainer(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(
                        text = "Cardio Load",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = "Last 14 days",
                        style = MaterialTheme.typography.labelSmall,
                        color = Theme.colors.onSurfaceVariant
                    )
                }
                Text(
                    text = String.format("%.1f ACUTE", avgLoad),
                    style = MaterialTheme.typography.titleMedium,
                    color = Theme.colors.accent
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().height(60.dp)) {
                SparklineChart(
                    data = cardioData.ifEmpty { listOf(0f, 0f) },
                    color = Theme.colors.accent,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}


@Composable
private fun Vo2MaxCard(vo2Max: Double) {
    GlassContainer(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(
                        text = "VO2 MAX",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                        color = Theme.colors.tertiaryColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Cardiovascular Fitness",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format("%.0f", vo2Max),
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = Theme.colors.tertiaryColor
                    )
                    Text(
                        text = " ml/kg/min",
                        style = MaterialTheme.typography.labelSmall,
                        color = Theme.colors.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { (vo2Max.toFloat() / 60f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = Theme.colors.tertiaryColor,
                trackColor = Theme.colors.surfaceContainerHighest
            )
        }
    }
}
