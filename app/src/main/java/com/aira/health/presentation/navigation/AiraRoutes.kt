package com.aira.health.presentation.navigation

object AiraRoutes {
    const val HOME = "home"
    const val INSIGHTS = "insights"
    const val TRAIN = "train"
    const val NUTRITION = "nutrition"
    const val SETTINGS = "settings"
    const val FUEL_TRAIN = "fuel_train"
    const val ASSISTANT = "assistant"

    // Legacy aliases preserved for compatibility with older deep links.
    const val BODY = INSIGHTS
    const val EAT = NUTRITION
    const val COACH = "coach"
    
    // Detailed routes
    const val INSIGHTS_DETAIL = "insights/{metricId}"
    const val ENERGY_BANK = "energy_bank"
    const val RECOVERY_DETAIL = "recovery_detail"
    const val STRAIN_DETAIL = "strain_detail"
    const val STRESS_DETAIL = "stress_detail"
    
    const val TRAIN_EDIT = "train/edit/{workoutId}"
    const val NUTRITION_EDIT = "nutrition/edit/{entryId}"
    
    const val ONBOARDING_WEARABLES = "onboarding_wearables"
    const val WEEKLY_REPORT = "weekly_report"
    const val WHAT_IF = "what_if_simulator"
    const val DATA_CORRECTIONS = "data_corrections"
    const val DATA_CONFIDENCE = "data_confidence"
    const val INSIGHTS_PREDICTIONS = "insights_predictions"
    const val ACCOUNT = "account"

    fun metricDetailRoute(metricId: String): String = "insights/$metricId"
    fun trainEditRoute(workoutId: Long): String = "train/edit/$workoutId"
    fun nutritionEditRoute(entryId: Long): String = "nutrition/edit/$entryId"
}
