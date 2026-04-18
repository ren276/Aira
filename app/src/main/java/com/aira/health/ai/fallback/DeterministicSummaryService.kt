package com.aira.health.ai.fallback

import com.aira.health.ai.prompt.MetricSnapshot
import javax.inject.Inject

/**
 * Produces deterministic, wellness-safe summary text when the AI runtime is unavailable
 * or when data quality is insufficient for AI-driven output.
 *
 * **AIM-04 contract:**
 * - For an identical [MetricSnapshot] AND [FallbackReason] pair, this service always
 *   returns the same text (deterministic, no randomness).
 * - Text is generated from score thresholds — no model call is ever made here.
 * - Language stays within general wellness boundaries (no diagnosis, no treatment claims).
 *
 * All functions are pure (no I/O) for trivial unit testability.
 */
class DeterministicSummaryService @Inject constructor() {

    // ---------------------------------------------------------------------------
    // Primary API
    // ---------------------------------------------------------------------------

    /**
     * Build a deterministic summary from [snapshot] aggregates when AI is unavailable.
     *
     * @param snapshot  Aggregated daily metrics (AIM-03: no raw records).
     * @param reason    Why AI was bypassed — influences the qualifier sentence.
     * @return          A [FallbackSummary] with wellness-safe text, metadata, and reason.
     */
    fun buildSummary(
        snapshot: MetricSnapshot,
        reason: FallbackReason,
    ): FallbackSummary {
        val recoveryLabel = scoreLabel(snapshot.recoveryScore)
        val sleepLabel = scoreLabel(snapshot.sleepScore)
        val strainLabel = strainLabel(snapshot.strainScore)
        val confidenceQualifier = confidenceQualifier(snapshot.dataConfidence, reason)

        val text = buildString {
            append("Your recovery looks $recoveryLabel today, ")
            append("with $sleepLabel sleep quality. ")
            when {
                snapshot.strainScore > STRAIN_HIGH_THRESHOLD ->
                    append("Recent strain is $strainLabel — consider prioritising recovery activities. ")
                snapshot.recoveryScore < RECOVERY_LOW_THRESHOLD ->
                    append("Readiness is on the lower side — lighter activity may suit you better today. ")
                else ->
                    append("Overall readiness is $recoveryLabel. ")
            }
            append(confidenceQualifier)
        }.trim()

        return FallbackSummary(
            text = text,
            reason = reason,
            recoveryScore = snapshot.recoveryScore,
            dataConfidence = snapshot.dataConfidence,
        )
    }

    /**
     * Specialised shorthand for timeout path — always deterministic for the same snapshot.
     */
    fun buildTimeoutSummary(snapshot: MetricSnapshot): FallbackSummary =
        buildSummary(snapshot, FallbackReason.TIMEOUT)

    /**
     * Specialised shorthand for low-confidence data path.
     */
    fun buildLowConfidenceSummary(snapshot: MetricSnapshot): FallbackSummary =
        buildSummary(snapshot, FallbackReason.LOW_CONFIDENCE)

    // ---------------------------------------------------------------------------
    // Pure label helpers (deterministic for all inputs)
    // ---------------------------------------------------------------------------

    internal fun scoreLabel(score: Int): String = when {
        score >= 80 -> "strong"
        score >= 60 -> "moderate"
        score >= 40 -> "low"
        else        -> "poor"
    }

    internal fun strainLabel(strainScore: Int): String = when {
        strainScore >= 80 -> "very high"
        strainScore >= 60 -> "high"
        strainScore >= 40 -> "moderate"
        else              -> "low"
    }

    internal fun confidenceQualifier(confidence: Float, reason: FallbackReason): String = when {
        reason == FallbackReason.LOW_CONFIDENCE || reason == FallbackReason.STALE_DATA ->
            "Note: sensor data quality is limited — use this as a rough guide."
        confidence < LOW_CONFIDENCE_THRESHOLD ->
            "Confidence in today's data is low; results may not fully reflect your current state."
        reason == FallbackReason.TIMEOUT ->
            "AI response timed out — this summary was generated from your most recent metrics."
        reason == FallbackReason.MODEL_UNAVAILABLE ->
            "AI features are initialising — this summary is based on your stored metrics."
        else ->
            "This is a data-based summary generated without AI assistance."
    }

    companion object {
        internal const val RECOVERY_LOW_THRESHOLD = 45
        internal const val STRAIN_HIGH_THRESHOLD = 65
        internal const val LOW_CONFIDENCE_THRESHOLD = 0.40f
    }
}

/**
 * Deterministic fallback result returned by [DeterministicSummaryService].
 *
 * @param text           Wellness-safe summary text ready for display.
 * @param reason         Why the fallback path was taken.
 * @param recoveryScore  Recovery score from the input snapshot (for UI display).
 * @param dataConfidence Confidence level from the input snapshot.
 */
data class FallbackSummary(
    val text: String,
    val reason: FallbackReason,
    val recoveryScore: Int,
    val dataConfidence: Float,
)
