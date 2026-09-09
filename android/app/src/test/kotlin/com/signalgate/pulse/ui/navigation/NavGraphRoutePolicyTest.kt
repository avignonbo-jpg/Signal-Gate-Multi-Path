package com.signalgate.pulse.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NavGraphRoutePolicyTest {

    @Test
    fun initialRoute_waitsWhileCompletionStateIsUnresolved() {
        assertNull(initialRouteFor(null))
    }

    @Test
    fun initialRoute_opensOnboardingForIncompleteInstall() {
        assertEquals(Screen.Onboarding.route, initialRouteFor(false))
    }

    @Test
    fun initialRoute_opensDashboardForCompletedInstall() {
        assertEquals(Screen.Dashboard.route, initialRouteFor(true))
    }
}
