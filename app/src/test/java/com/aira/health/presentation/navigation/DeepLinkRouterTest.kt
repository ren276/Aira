package com.aira.health.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class DeepLinkRouterTest {
    
    @Test
    fun resolveKnownRoutes() {
        assertEquals(AiraRoutes.HOME, DeepLinkRouter.resolve(AiraRoutes.HOME))
        assertEquals(AiraRoutes.INSIGHTS, DeepLinkRouter.resolve(AiraRoutes.INSIGHTS))
        assertEquals(AiraRoutes.TRAIN, DeepLinkRouter.resolve(AiraRoutes.TRAIN))
        assertEquals(AiraRoutes.NUTRITION, DeepLinkRouter.resolve(AiraRoutes.NUTRITION))
        assertEquals(AiraRoutes.SETTINGS, DeepLinkRouter.resolve(AiraRoutes.SETTINGS))
    }

    @Test
    fun resolveUnknownFallsBackToHome() {
        assertEquals(AiraRoutes.HOME, DeepLinkRouter.resolve("some_unknown_route"))
        assertEquals(AiraRoutes.HOME, DeepLinkRouter.resolve(null))
        assertEquals(AiraRoutes.HOME, DeepLinkRouter.resolve(""))
    }
}
