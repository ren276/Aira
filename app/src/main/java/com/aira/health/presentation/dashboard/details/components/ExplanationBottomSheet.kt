package com.aira.health.presentation.dashboard.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aira.health.presentation.theme.AiraSpacing
import com.aira.health.presentation.theme.Theme

/**
 * Reusable primitive for surfacing metric explanations (D-11, T-04-09).
 * 
 * Contract Invariants:
 *  - ALWAYS renders exactly three sections: "What changed", "Why it matters", "What to do next".
 *  - Headings are fixed and non-configurable to enforce consistency and mitigate UI divergence.
 *  - Uses ModalBottomSheet for interactive dismissal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplanationBottomSheet(
    whatChanged: String,
    whyItMatters: String,
    whatToDoNext: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Theme.colors.dominant,
        contentColor = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.semantics { contentDescription = "explanation bottom sheet" }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AiraSpacing.lg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AiraSpacing.md)
        ) {
            Text(
                text = "Metric Insight",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(AiraSpacing.sm))

            // Section 1
            ExplanationSection(
                title = "What changed",
                content = whatChanged
            )

            // Section 2
            ExplanationSection(
                title = "Why it matters",
                content = whyItMatters
            )

            // Section 3
            ExplanationSection(
                title = "What to do next",
                content = whatToDoNext
            )
            
            Spacer(modifier = Modifier.height(AiraSpacing.xxl))
        }
    }
}

@Composable
private fun ExplanationSection(title: String, content: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(AiraSpacing.xs),
        modifier = Modifier.semantics { contentDescription = "explanation section: $title" }
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = Theme.colors.accent,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )
    }
}
