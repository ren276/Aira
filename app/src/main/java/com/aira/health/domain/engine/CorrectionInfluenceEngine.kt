package com.aira.health.domain.engine

import com.aira.health.data.local.model.UserCorrection
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.abs

class CorrectionInfluenceEngine @Inject constructor() {

    data class InfluenceDetail(
        val parameterKey: String,
        val correctionId: Long,
        val sourceFieldName: String,
        val ageDays: Long,
        val decayWeight: Float,
        val rawInfluence: Float,
        val decayedInfluence: Float,
        val cappedInfluence: Float,
        val createdAt: Long
    )

    data class InfluenceResult(
        val combinedByParameter: Map<String, Float>,
        val details: List<InfluenceDetail>
    )

    fun applyDecay(corrections: List<UserCorrection>, targetDate: String): InfluenceResult {
        val target = LocalDate.parse(targetDate)
        val grouped = linkedMapOf<String, MutableList<InfluenceDetail>>()

        corrections.forEach { correction ->
            val parameter = toParameterKey(correction.fieldName) ?: return@forEach
            val correctionDate = runCatching { LocalDate.parse(correction.recordDate) }.getOrNull() ?: return@forEach
            val ageDays = java.time.temporal.ChronoUnit.DAYS.between(correctionDate, target)
            if (ageDays < 0L || ageDays > 14L) return@forEach

            val decayWeight = ((14f - ageDays.toFloat()) / 14f).coerceIn(0f, 1f)
            val decayed = correction.confidenceDelta * decayWeight
            val detail = InfluenceDetail(
                parameterKey = parameter,
                correctionId = correction.id,
                sourceFieldName = correction.fieldName,
                ageDays = ageDays,
                decayWeight = decayWeight,
                rawInfluence = correction.confidenceDelta,
                decayedInfluence = decayed,
                cappedInfluence = decayed,
                createdAt = correction.createdAt
            )
            grouped.getOrPut(parameter) { mutableListOf() }.add(detail)
        }

        val cappedDetails = mutableListOf<InfluenceDetail>()
        val combined = mutableMapOf<String, Float>()

        grouped.forEach { (parameter, details) ->
            val total = details.sumOf { it.decayedInfluence.toDouble() }.toFloat()
            val capped = capInfluence(total)
            combined[parameter] = capped

            if (details.isNotEmpty()) {
                val share = if (abs(total) < 1e-6f) 0f else capped / total
                details.forEach { detail ->
                    cappedDetails += detail.copy(cappedInfluence = detail.decayedInfluence * share)
                }
            }
        }

        return InfluenceResult(combinedByParameter = combined, details = cappedDetails)
    }

    fun capInfluence(value: Float): Float = value.coerceIn(-0.2f, 0.2f)

    private fun toParameterKey(fieldName: String): String? {
        return when {
            fieldName.contains("sleep", ignoreCase = true) -> "sleepNeed"
            fieldName.contains("hrv", ignoreCase = true) ||
                fieldName.contains("recovery", ignoreCase = true) -> "recoverySpeed"
            fieldName.contains("stress", ignoreCase = true) -> "stressSensitivity"
            else -> null
        }
    }
}
