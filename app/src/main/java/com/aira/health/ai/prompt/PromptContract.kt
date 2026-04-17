package com.aira.health.ai.prompt

/**
 * Strongly-typed prompt contract sections that together form one generation request.
 *
 * **Privacy (AIM-03):** All fields accept only aggregated/derived values — never raw
 * biometric event payloads. The [PromptAssembler] enforces this at the type level by
 * accepting only [com.aira.health.ai.prompt.MetricSnapshot] DTOs, not Room entities.
 *
 * @param systemGuidance     Fixed system-role instruction that anchors tone and claim boundaries.
 * @param userContext        Compressed daily athlete state derived from [MetricSnapshot].
 * @param outputPolicy       Explicit length and safety constraints appended to the user turn.
 * @param maxOutputTokens    Token budget hint; downstream PromptBudgeter uses this to prune.
 */
data class PromptContract(
    val systemGuidance: String,
    val userContext: String,
    val outputPolicy: String,
    val maxOutputTokens: Int = MAX_OUTPUT_TOKENS,
) {
    /**
     * Ordered list of chunks ready for [com.aira.health.ai.runtime.AiRuntimeRequest].
     * System chunk first, then user context, then output policy.
     */
    fun toChunks(): List<String> = listOf(
        "System: $systemGuidance",
        "User: $userContext",
        outputPolicy,
    )

    companion object {
        const val MAX_OUTPUT_TOKENS: Int = 120 // ≈ 120 words — concise coaching narrative
        const val SYSTEM_GUIDANCE: String =
            "You are a concise athlete recovery coach. " +
                "Provide actionable wellness guidance in plain language. " +
                "Stay within general wellness boundaries — do not diagnose, prescribe, or suggest medical treatment. " +
                "If sensor quality is low or data is missing, clearly qualify your response with uncertainty."
        const val OUTPUT_POLICY: String =
            "Respond in 3 sentences or fewer. " +
                "Keep language calm and specific to the data provided. " +
                "Do not reference medical conditions or diagnose symptoms."
    }
}

/**
 * Aggregated metric snapshot used as the only permissible prompt input.
 *
 * This DTO contains only derived scores, confidence, and recency signals.
 * Raw sensor records ([com.aira.health.data.local.model.HealthRecordRaw],
 * HRV samples, sleep session rows, etc.) must never appear here — AIM-03.
 *
 * @param date              ISO-8601 date string "YYYY-MM-DD".
 * @param recoveryScore     0–100 composite recovery score.
 * @param sleepScore        0–100 sleep quality score.
 * @param strainScore       0–100 training strain score.
 * @param stressScore       0–100 stress estimate.
 * @param dataConfidence    0.0–1.0 quality confidence across input sensors.
 * @param hrv               Morning HRV in milliseconds (nullable when sensor absent).
 * @param rhr               Morning resting HR in bpm (nullable when sensor absent).
 * @param sleepDurationMin  Total sleep duration in minutes (nullable).
 * @param compositeReadiness 0–100 composite readiness index.
 */
data class MetricSnapshot(
    val date: String,
    val recoveryScore: Int,
    val sleepScore: Int,
    val strainScore: Int,
    val stressScore: Int,
    val dataConfidence: Float,
    val hrv: Float? = null,
    val rhr: Float? = null,
    val sleepDurationMin: Int? = null,
    val compositeReadiness: Int = 0,
)
