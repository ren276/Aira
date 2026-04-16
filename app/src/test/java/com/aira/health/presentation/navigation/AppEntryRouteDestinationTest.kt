package com.aira.health.presentation.navigation

import com.aira.health.domain.model.AuthState
import org.junit.Assert.assertEquals
import org.junit.Test

class AppEntryRouteDestinationTest {

    @Test
    fun resolveDestinationReturnsLoadingWhenStateIsLoading() {
        val state = AppEntryUiState(loading = true)

        val destination = resolveAppEntryDestination(state)

        assertEquals(AppEntryDestination.LOADING, destination)
    }

    @Test
    fun resolveDestinationReturnsMainNavWhenOnboardingIsComplete() {
        val state = AppEntryUiState(
            loading = false,
            onboardingCompleted = true,
            authStepCompleted = false,
            authState = AuthState.Guest
        )

        val destination = resolveAppEntryDestination(state)

        assertEquals(AppEntryDestination.MAIN_NAV, destination)
    }

    @Test
    fun resolveDestinationReturnsAuthOnboardingWhenAuthStepIsIncomplete() {
        val state = AppEntryUiState(
            loading = false,
            onboardingCompleted = false,
            authStepCompleted = false,
            authState = AuthState.Guest
        )

        val destination = resolveAppEntryDestination(state)

        assertEquals(AppEntryDestination.AUTH_ONBOARDING, destination)
    }

    @Test
    fun resolveDestinationReturnsPermissionOnboardingWhenAuthStepIsComplete() {
        val state = AppEntryUiState(
            loading = false,
            onboardingCompleted = false,
            authStepCompleted = true,
            authState = AuthState.Guest
        )

        val destination = resolveAppEntryDestination(state)

        assertEquals(AppEntryDestination.PERMISSION_ONBOARDING, destination)
    }
}
