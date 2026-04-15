package com.aira.health.data.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ConfidenceRouterTest {

    @Test
    fun `returns expected confidence weights by tier`() {
        assertEquals(100, ConfidenceRouter.getConfidenceWeight("com.ouraring.oura"))
        assertEquals(85, ConfidenceRouter.getConfidenceWeight("com.garmin.android.apps.connectmobile"))
        assertEquals(65, ConfidenceRouter.getConfidenceWeight("com.samsung.android.app.shealth"))
        assertEquals(40, ConfidenceRouter.getConfidenceWeight("com.unknown.source"))
    }

    @Test
    fun `preferredSource returns package with higher weight`() {
        val preferred = ConfidenceRouter.preferredSource(
            "com.garmin.android.apps.connectmobile",
            "com.samsung.android.app.shealth"
        )

        assertEquals("com.garmin.android.apps.connectmobile", preferred)
    }
}
