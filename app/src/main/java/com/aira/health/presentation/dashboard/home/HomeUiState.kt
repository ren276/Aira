package com.aira.health.presentation.dashboard.home

/**
 * UI state contract for the Home dashboard.
 *
 * Design decisions:
 *  - Loading only shown on first open when NO cached data is available (D-08)
 *  - When cached values exist the UI renders immediately; sync updates produce [ScoreDelta] payloads
 *  - [confidence] and [lastUpdated] are mandatory non-null fields — never hidden (D-08, D-09)
 *  - Anomaly card is represented by nullable [anomaly]; null = show forecast/prevention fallback (D-09)
 */
sealed interface HomeUiState {

    /** Initial load with no cached data available yet. */
    data object Loading : HomeUiState

    /** Empty state shown when no health data is available yet. */
    data class Empty(
        val message: String,
        val userName: String = "Athlete",
        val greeting: String = "Morning",
        val statusHeadline: String = "Sync required",
        val isSyncing: Boolean = false
    ) : HomeUiState

    /** Error state — only emitted if no cached values could be read at all. */
    data class Error(val message: String) : HomeUiState

    /** Stable state with all four clinical scores present. */
    data class Success(
        // Clinical scores — fixed order: Recovery, Sleep, Strain, Stress
        val recoveryScore: Int,
        val sleepScore: Int,
        val strainScore: Int,
        val stressScore: Int,

        // Mandatory metadata — always shown (D-08)
        val confidence: Float,           // 0f-1f
        val lastUpdated: Long,           // epoch-millis

        // Silent sync state
        val isSyncing: Boolean = false,

        // Home Header Greeting
        val userName: String = "",
        val greeting: String = "",
        val statusHeadline: String = "",

        // Energy Bank
        val energyBankPct: Int = 0,
        val energyBankDelta: Int? = null,

        // Movement snapshot
        val totalSteps: Int? = null,
        val activeCalories: Int? = null,

        // Vitals
        val rhr: Int? = null,
        val hrv: Int? = null,
        val spo2: Int? = null,
        val temp: Float? = null,
        val sleepDurationHours: Float? = null,

        // Vitals Sparklines (Real history)
        val rhrHistory: List<Float> = emptyList(),
        val hrvHistory: List<Float> = emptyList(),
        val spo2History: List<Float> = emptyList(),
        val tempHistory: List<Float> = emptyList(),
        val recoveryHistory: List<Float> = emptyList(),
        val sleepHistory: List<Float> = emptyList(),
        val strainHistory: List<Float> = emptyList(),
        val stressHistory: List<Float> = emptyList(),

        // Delta animation payloads produced after a background sync (null = no animation pending)
        val recoveryDelta: ScoreDelta? = null,
        val sleepDelta: ScoreDelta? = null,
        val strainDelta: ScoreDelta? = null,
        val stressDelta: ScoreDelta? = null,

        // Causal anomaly — null means fallback forecast guidance should be shown (D-09)
        val anomaly: AnomalyPayload? = null
    ) : HomeUiState
}

/**
 * Represents a before/after score change produced by [HomeDeltaAnimator] after a background sync.
 */
data class ScoreDelta(
    val previous: Int,
    val current: Int
) {
    val direction: DeltaDirection = when {
        current > previous -> DeltaDirection.UP
        current < previous -> DeltaDirection.DOWN
        else -> DeltaDirection.NONE
    }
}

enum class DeltaDirection { UP, DOWN, NONE }

/**
 * Minimal anomaly payload surfaced to the Home card.
 *
 * When null, the [CausalAnomalyCard] renders prevention/forecast guidance instead.
 */
data class AnomalyPayload(
    val title: String,
    val description: String,
    /** Severity in 0f..1f — drives colour tinting on the card. */
    val severity: Float = 0.5f
)
