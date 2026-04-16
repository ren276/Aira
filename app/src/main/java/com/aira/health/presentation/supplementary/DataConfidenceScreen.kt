package com.aira.health.presentation.supplementary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aira.health.data.local.dao.DailyMetricsDao
import com.aira.health.presentation.theme.Theme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class DataConfidenceUiState(
    val percent: Int? = null,
    val sourceSummary: String = "Awaiting synced data",
    val guidance: String = "Sync at least one full day to compute confidence."
)

@HiltViewModel
class DataConfidenceViewModel @Inject constructor(
    private val dailyMetricsDao: DailyMetricsDao
) : ViewModel() {

    val uiState: StateFlow<DataConfidenceUiState> = dailyMetricsDao.observeRecent(7)
        .map { recent ->
            val latest = recent.firstOrNull()
            val confidence = latest?.dataConfidence?.times(100f)?.toInt()?.coerceIn(0, 100)
            if (confidence == null) {
                DataConfidenceUiState()
            } else {
                val guidance = when {
                    confidence >= 80 -> "Confidence is high. Current guidance can be used for training decisions."
                    confidence >= 50 -> "Confidence is moderate. Keep syncing and avoid aggressive load jumps."
                    else -> "Confidence is low. Prioritize consistent wearable sync and sleep logs."
                }
                val sourceSummary = "Derived from your recent local metrics (last ${recent.size.coerceAtMost(7)} day window)."
                DataConfidenceUiState(
                    percent = confidence,
                    sourceSummary = sourceSummary,
                    guidance = guidance
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DataConfidenceUiState()
        )
}

@Composable
fun DataConfidenceScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DataConfidenceViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.dominant),
        contentPadding = PaddingValues(top = 48.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = "Data Confidence",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Theme.colors.surfaceContainerLow)
                    .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = Theme.colors.accent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Model Confidence",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(start = 0.dp)
                        )
                    }

                    Text(
                        text = state.percent?.let { "$it%" } ?: "--",
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = Theme.colors.accent
                    )

                    Text(
                        text = state.sourceSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Theme.colors.onSurfaceVariant
                    )

                    Text(
                        text = state.guidance,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}
