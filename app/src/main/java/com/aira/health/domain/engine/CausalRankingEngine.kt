package com.aira.health.domain.engine

import com.aira.health.domain.model.CausalDirection
import com.aira.health.domain.model.CausalFactor
import javax.inject.Inject
import kotlin.math.abs

class CausalRankingEngine @Inject constructor() {

    data class Candidate(
        val key: String,
        val contribution: Float,
        val windowLabel: String,
        val windowTimestamp: Long
    )

    fun rankTopFactors(candidates: List<Candidate>): List<CausalFactor> {
        val validated = candidates
            .asSequence()
            .filter { it.key.isNotBlank() }
            .filter { it.windowLabel.isNotBlank() }
            .filter { it.windowTimestamp > 0L }
            .take(32)
            .toList()

        if (validated.isEmpty()) return emptyList()

        val maxAbs = validated.maxOfOrNull { abs(it.contribution) }?.takeIf { it > 0f } ?: 1f

        val normalized = validated.map { candidate ->
            CausalFactor(
                key = candidate.key,
                direction = toDirection(candidate.contribution),
                weight = (abs(candidate.contribution) / maxAbs).coerceIn(0f, 1f),
                windowLabel = candidate.windowLabel,
                windowTimestamp = candidate.windowTimestamp
            )
        }

        return normalized
            .sortedWith { first, second ->
                val weightDelta = second.weight - first.weight
                when {
                    abs(weightDelta) < 0.03f -> {
                        val recency = second.windowTimestamp.compareTo(first.windowTimestamp)
                        if (recency != 0) recency else first.key.compareTo(second.key)
                    }
                    else -> weightDelta.compareTo(0f)
                }
            }
            .take(3)
    }

    private fun toDirection(value: Float): CausalDirection {
        return when {
            value > 0.01f -> CausalDirection.INCREASED
            value < -0.01f -> CausalDirection.DECREASED
            else -> CausalDirection.NEUTRAL
        }
    }
}
