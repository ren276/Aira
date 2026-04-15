package com.aira.health.presentation.dashboard.details

/**
 * Known metric IDs mapped from Home cards to the detail route (D-05, T-04-07).
 */
enum class MetricType(val id: String) {
    RECOVERY("recovery"),
    SLEEP("sleep"),
    STRAIN("strain"),
    STRESS("stress");

    companion object {
        /** Safe parser to mitigate T-04-07 (invalid route argument handling). */
        fun fromIdOrNull(id: String?): MetricType? = 
            values().find { it.id.equals(id, ignoreCase = true) }
    }
}
