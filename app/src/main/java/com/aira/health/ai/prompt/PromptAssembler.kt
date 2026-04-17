package com.aira.health.ai.prompt

import com.aira.health.data.local.model.DailyMetrics
import javax.inject.Inject

/**
 * Builds privacy-safe [PromptContract] instances from aggregated [MetricSnapshot] inputs.
 *
 * **AIM-03 enforcement:**
 * - The public API accepts only [MetricSnapshot] (aggregated scores + confidence), never
 *   raw Room entities ([DailyMetrics] is only used for the [fromDailyMetrics] mapper helper,
 *   which is a pure conversion that strips raw event fields).
 * - Free-text user notes are sanitised through [redactFreeText] before any concatenation.
 * - No raw biometric event payloads (HR samples, sleep sessions, HRV arrays) ever reach
 *   the prompt — they must be aggregated into scores before calling this assembler.
 *
 * All functions are pure (no I/O, no side effects) to remain trivially unit-testable.
 */
class PromptAssembler @Inject constructor() {

    // ---------------------------------------------------------------------------
    // Primary public API
    // ---------------------------------------------------------------------------

    /**
     * Assemble a [PromptContract] from a [MetricSnapshot].
     *
     * @param snapshot  Aggregated daily metrics (AIM-03: must not contain raw records).
     * @param userNotes Optional free-text coach note from the user. Will be redacted before use.
     */
    fun assemble(snapshot: MetricSnapshot, userNotes: String? = null): PromptContract {
        val contextLines = buildList {
            add("Date: ${snapshot.date}")
            add("Recovery: ${snapshot.recoveryScore}/100")
            add("Sleep quality: ${snapshot.sleepScore}/100")
            add("Strain: ${snapshot.strainScore}/100")
            add("Stress: ${snapshot.stressScore}/100")
            add("Readiness: ${snapshot.compositeReadiness}/100")

            // Optional biometric aggregates — omit entirely when absent
            snapshot.hrv?.let { add("HRV (morning, ms): ${"%.1f".format(it)}") }
            snapshot.rhr?.let { add("Resting HR (bpm): ${"%.0f".format(it)}") }
            snapshot.sleepDurationMin?.let { add("Sleep duration: ${it / 60}h ${it % 60}m") }

            // Confidence qualifier — always included
            val confidenceLabel = confidenceLabel(snapshot.dataConfidence)
            add("Data confidence: $confidenceLabel (${(snapshot.dataConfidence * 100).toInt()}%)")

            // Sanitised user note — omit when blank after redaction
            val safeNote = userNotes?.let { redactFreeText(it) }?.takeIf { it.isNotBlank() }
            safeNote?.let { add("Athlete note: $it") }
        }

        val userContext = "Today's athlete data:\n${contextLines.joinToString("\n")}"

        return PromptContract(
            systemGuidance = PromptContract.SYSTEM_GUIDANCE,
            userContext = userContext,
            outputPolicy = PromptContract.OUTPUT_POLICY,
        )
    }

    /**
     * Convenience mapper: converts a [DailyMetrics] Room entity to a [MetricSnapshot].
     * This is the only permitted bridge point between Room entities and the prompt layer.
     * Raw sensor array fields are deliberately not forwarded.
     */
    fun fromDailyMetrics(metrics: DailyMetrics): MetricSnapshot = MetricSnapshot(
        date = metrics.date,
        recoveryScore = metrics.recoveryScore,
        sleepScore = metrics.sleepScore,
        strainScore = metrics.strainScore,
        stressScore = metrics.stressScore,
        dataConfidence = metrics.dataConfidence,
        hrv = metrics.hrvMorning,
        rhr = metrics.rhrMorning,
        sleepDurationMin = metrics.sleepDurationMin,
        compositeReadiness = metrics.compositeReadiness,
    )

    // ---------------------------------------------------------------------------
    // Redaction helpers (AIM-03)
    // ---------------------------------------------------------------------------

    /**
     * Sanitises user-supplied free-text by removing tokens that could carry biometric
     * identifiers or disallowed literal patterns before injecting into prompt chunks.
     *
     * Rules applied (in order):
     * 1. Trim to [MAX_NOTE_CHARS] characters.
     * 2. Strip tokens matching [BLOCKED_PATTERNS] (numeric medical values, email, URL).
     * 3. Collapse runs of whitespace.
     */
    fun redactFreeText(text: String): String {
        var sanitised = text.take(MAX_NOTE_CHARS)
        BLOCKED_PATTERNS.forEach { pattern ->
            sanitised = sanitised.replace(pattern, REDACT_REPLACEMENT)
        }
        return sanitised
            .replace(Regex("\\s{2,}"), " ")
            .trim()
    }

    // ---------------------------------------------------------------------------
    // Token budget pruning
    // ---------------------------------------------------------------------------

    /**
     * Prune [chunks] to fit within [maxTokens] using a deterministic priority order:
     * system guidance → current-day metrics → optional notes.
     *
     * Very rough budget: 1 token ≈ 4 characters (English). This is approximate and
     * intentionally conservative.
     */
    fun pruneToTokenBudget(chunks: List<String>, maxTokens: Int): List<String> {
        val result = mutableListOf<String>()
        var remaining = maxTokens * CHARS_PER_TOKEN_ESTIMATE
        for (chunk in chunks) {
            if (chunk.length <= remaining) {
                result.add(chunk)
                remaining -= chunk.length
            } else {
                // Include a truncated version of the chunk if at least half fits
                if (remaining > chunk.length / 2) {
                    result.add(chunk.take(remaining.toInt()))
                }
                break
            }
        }
        return result
    }

    // ---------------------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------------------

    private fun confidenceLabel(confidence: Float): String = when {
        confidence >= 0.85f -> "high"
        confidence >= 0.60f -> "medium"
        confidence >= 0.35f -> "low"
        else -> "very low — treat recommendations with caution"
    }

    companion object {
        private const val MAX_NOTE_CHARS = 200
        private const val REDACT_REPLACEMENT = "[redacted]"
        private const val CHARS_PER_TOKEN_ESTIMATE = 4L

        /**
         * Patterns that may indicate biometric literals, PII, or injection attempts.
         * These are removed from free-text before inclusion in prompt chunks.
         */
        val BLOCKED_PATTERNS: List<Regex> = listOf(
            Regex("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}"),   // email
            Regex("https?://\\S+"),                                            // URL
            Regex("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b"),       // IP address
            Regex("\\bBP\\s*\\d{2,3}/\\d{2,3}\\b", RegexOption.IGNORE_CASE), // blood pressure literal
            Regex("\\bglucose\\s*[=:]?\\s*\\d+", RegexOption.IGNORE_CASE),   // glucose reading
            Regex("\\b(diagnos|prescri|medica|treat)\\w*\\b", RegexOption.IGNORE_CASE), // medical terms
        )
    }
}
