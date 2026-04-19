package com.aira.health.ai.prompt

/**
 * Prompt contract used for athlete guidance generation.
 */
data class AthleteGuidancePromptContract(
    val systemGuidance: String,
    val localStateContext: String,
    val outputPolicy: String,
    val citationKeys: List<String>,
    val lowConfidence: Boolean,
) {

    fun toChunks(): List<String> = listOf(
        "System: $systemGuidance",
        "User: $localStateContext",
        outputPolicy,
    )

    companion object {
        const val SYSTEM_GUIDANCE: String =
            "You are an on-device athlete guidance assistant. " +
                "Return practical daily guidance for training, recovery, and nutrition. " +
                "Use only provided local signals, avoid diagnosis, and avoid fabricated causality."

        const val OUTPUT_POLICY: String =
            "Output exactly four lines using these labels: " +
                "SUMMARY:, TRAINING:, RECOVERY:, NUTRITION:. " +
                "Keep each line concise and grounded in supplied local signals only."

        val ALLOWED_CITATION_KEYS: Set<String> = setOf(
            "recovery_score",
            "sleep_score",
            "strain_score",
            "stress_score",
            "energy_bank_score",
            "burnout_risk",
            "burnout_trajectory",
            "prediction_recovery_delta",
            "prediction_energy_delta",
            "data_confidence",
        )
    }
}
