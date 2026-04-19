package com.aira.health.domain.model

data class PredictionScenario(
    val targetDate: String,
    val sleepDeltaHours: Float,
    val trainingLoadDeltaPercent: Float
) {
    fun validate() {
        require(sleepDeltaHours in MIN_SLEEP_DELTA_HOURS..MAX_SLEEP_DELTA_HOURS) {
            "sleepDeltaHours must be between $MIN_SLEEP_DELTA_HOURS and $MAX_SLEEP_DELTA_HOURS"
        }
        require(trainingLoadDeltaPercent in MIN_TRAINING_LOAD_DELTA_PERCENT..MAX_TRAINING_LOAD_DELTA_PERCENT) {
            "trainingLoadDeltaPercent must be between $MIN_TRAINING_LOAD_DELTA_PERCENT and $MAX_TRAINING_LOAD_DELTA_PERCENT"
        }
    }

    companion object {
        const val MIN_SLEEP_DELTA_HOURS = -3f
        const val MAX_SLEEP_DELTA_HOURS = 3f
        const val MIN_TRAINING_LOAD_DELTA_PERCENT = -40f
        const val MAX_TRAINING_LOAD_DELTA_PERCENT = 40f
    }
}
