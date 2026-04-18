package com.aira.health.presentation.navigation

object DeepLinkRouter {
    fun resolve(routeKey: String?): String {
        if (routeKey.isNullOrBlank()) {
            return AiraRoutes.HOME
        }

        return when {
            routeKey == AiraRoutes.HOME -> routeKey
            routeKey == AiraRoutes.INSIGHTS -> routeKey
            routeKey.startsWith("insights/") -> routeKey
            routeKey == AiraRoutes.TRAIN -> routeKey
            routeKey.startsWith("train/edit/") -> routeKey
            routeKey == AiraRoutes.NUTRITION -> routeKey
            routeKey.startsWith("nutrition/edit/") -> routeKey
            routeKey == AiraRoutes.SETTINGS -> routeKey
            routeKey == AiraRoutes.DATA_CONFIDENCE -> routeKey

            // Legacy aliases from earlier IA versions.
            routeKey == AiraRoutes.BODY -> AiraRoutes.INSIGHTS
            routeKey == AiraRoutes.EAT -> AiraRoutes.NUTRITION
            routeKey == AiraRoutes.COACH -> AiraRoutes.COACH

            else -> AiraRoutes.HOME
        }
    }
}
