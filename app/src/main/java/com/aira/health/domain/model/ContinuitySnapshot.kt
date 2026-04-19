package com.aira.health.domain.model

import kotlinx.serialization.Serializable

/**
 * Compact derived-only continuity payload for cloud restore.
 * Raw biometric rows are intentionally excluded.
 */
@Serializable
data class ContinuitySnapshot(
    val snapshotId: String,
    val capturedAtEpochMs: Long,
    val recoveryScore: Int,
    val sleepScore: Int,
    val strainScore: Int,
    val stressScore: Int,
    val energyBankScore: Int,
    val burnoutRiskIndex: Float,
    val dataConfidence: Float,
    val predictedRecoveryDelta: Int? = null,
    val predictedEnergyDelta: Int? = null,
    val guidanceSummary: String? = null,
    val weeklyHighlights: String? = null,
    val cloudBackupEnabled: Boolean
)
