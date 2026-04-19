package com.aira.health.data.local.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aira.health.domain.model.CausalDirection
import com.aira.health.domain.model.CausalFactor

@Entity(
    tableName = "causal_insights",
    indices = [Index(value = ["metricKey", "date"], unique = true)]
)
data class CausalInsight(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val metricKey: String,
    val confidence: Float,
    val factor1Key: String?,
    val factor1Direction: String?,
    val factor1Weight: Float?,
    val factor1WindowLabel: String?,
    val factor1WindowTimestamp: Long?,
    val factor2Key: String?,
    val factor2Direction: String?,
    val factor2Weight: Float?,
    val factor2WindowLabel: String?,
    val factor2WindowTimestamp: Long?,
    val factor3Key: String?,
    val factor3Direction: String?,
    val factor3Weight: Float?,
    val factor3WindowLabel: String?,
    val factor3WindowTimestamp: Long?,
    val calculatedAt: Long = System.currentTimeMillis()
) {
    fun toFactors(): List<CausalFactor> = listOfNotNull(
        toFactor(factor1Key, factor1Direction, factor1Weight, factor1WindowLabel, factor1WindowTimestamp),
        toFactor(factor2Key, factor2Direction, factor2Weight, factor2WindowLabel, factor2WindowTimestamp),
        toFactor(factor3Key, factor3Direction, factor3Weight, factor3WindowLabel, factor3WindowTimestamp)
    )

    private fun toFactor(
        key: String?,
        direction: String?,
        weight: Float?,
        windowLabel: String?,
        windowTimestamp: Long?
    ): CausalFactor? {
        if (key == null || direction == null || weight == null || windowLabel == null || windowTimestamp == null) {
            return null
        }
        return CausalFactor(
            key = key,
            direction = CausalDirection.valueOf(direction),
            weight = weight,
            windowLabel = windowLabel,
            windowTimestamp = windowTimestamp
        )
    }

    companion object {
        fun fromFactors(
            date: String,
            metricKey: String,
            confidence: Float,
            factors: List<CausalFactor>,
            calculatedAt: Long
        ): CausalInsight {
            val top = factors.take(3)
            fun at(index: Int): CausalFactor? = top.getOrNull(index)
            val first = at(0)
            val second = at(1)
            val third = at(2)

            return CausalInsight(
                date = date,
                metricKey = metricKey,
                confidence = confidence.coerceIn(0f, 1f),
                factor1Key = first?.key,
                factor1Direction = first?.direction?.name,
                factor1Weight = first?.weight,
                factor1WindowLabel = first?.windowLabel,
                factor1WindowTimestamp = first?.windowTimestamp,
                factor2Key = second?.key,
                factor2Direction = second?.direction?.name,
                factor2Weight = second?.weight,
                factor2WindowLabel = second?.windowLabel,
                factor2WindowTimestamp = second?.windowTimestamp,
                factor3Key = third?.key,
                factor3Direction = third?.direction?.name,
                factor3Weight = third?.weight,
                factor3WindowLabel = third?.windowLabel,
                factor3WindowTimestamp = third?.windowTimestamp,
                calculatedAt = calculatedAt
            )
        }
    }
}
