package com.aira.health.presentation.navigation

import com.aira.health.domain.model.AuthState
import org.junit.Assert.assertEquals
import org.junit.Test

class AppEntryRouteTest {

    @Test
    fun resolveDestination_routesToStravaWhenReconnectRequiredEvenIfOnboardingComplete() {
        val state = AppEntryUiState(
            loading = false,
            onboardingCompleted = true,
            authStepCompleted = true,
            stravaConnected = false,
            stravaReconnectRequired = true,
            authState = AuthState.Guest
        )

        val destination = resolveAppEntryDestination(state)

        assertEquals(AppEntryDestination.STRAVA_ONBOARDING, destination)
    }

    @Test
    fun resolveDestination_routesToMainNavWhenOnboardingCompleteAndStravaHealthy() {
        val state = AppEntryUiState(
            loading = false,
            onboardingCompleted = true,
            authStepCompleted = true,
            stravaConnected = true,
            stravaReconnectRequired = false,
            authState = AuthState.Guest
        )

        val destination = resolveAppEntryDestination(state)

        assertEquals(AppEntryDestination.MAIN_NAV, destination)
    }
}
