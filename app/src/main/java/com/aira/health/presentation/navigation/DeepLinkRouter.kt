package com.aira.health.presentation.navigation

object DeepLinkRouter {
    fun resolve(routeKey: String?): String {
        return when (routeKey) {
            AiraRoutes.HOME, AiraRoutes.INSIGHTS, AiraRoutes.TRAIN, AiraRoutes.NUTRITION, AiraRoutes.SETTINGS -> routeKey
            else -> AiraRoutes.HOME
        }
    }
}
