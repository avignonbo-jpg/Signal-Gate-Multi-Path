package com.signalgate.pulse.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.signalgate.pulse.ui.dashboard.DashboardViewModel
import com.signalgate.pulse.ui.digest.DigestScreen
import com.signalgate.pulse.ui.onboarding.OnboardingWizardScreen
import com.signalgate.pulse.ui.screens.BlockAllowListScreen
import com.signalgate.pulse.ui.screens.CallLogScreen
import com.signalgate.pulse.ui.screens.ConsumerDashboardScreen
import com.signalgate.pulse.ui.screens.LogcatViewerScreen
import com.signalgate.pulse.ui.screens.PermissionSettingsScreen
import com.signalgate.pulse.ui.screens.SettingsScreen
import com.signalgate.pulse.ui.screens.SourcesScreen
import org.koin.androidx.compose.koinViewModel

private const val STARTUP_ROUTE = "startup"

/**
 * Resolves the app's first visible destination only after persistent completion
 * state is known. `null` deliberately remains on the startup surface so a first
 * install can never fall through to the dashboard and bypass required consent.
 */
internal fun initialRouteFor(onboardingComplete: Boolean?): String? = when (onboardingComplete) {
    true -> Screen.Dashboard.route
    false -> Screen.Onboarding.route
    null -> null
}

@Composable
fun SignalGateNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") onOpenDrawer: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = STARTUP_ROUTE,
        modifier = modifier.fillMaxSize()
    ) {
        composable(STARTUP_ROUTE) {
            FirstInstallRoute(navController)
        }

        composable(Screen.Dashboard.route) {
            ConsumerDashboardScreen(
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToPermissionSettings = {
                    navController.navigate(Screen.PermissionSettings.route)
                },
                onLaunchOnboarding = {
                    navController.navigate(Screen.Onboarding.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Sources.route) {
            SourcesScreen()
        }

        composable(Screen.CallLog.route) {
            CallLogScreen()
        }

        composable(Screen.BlockAllowList.route) {
            BlockAllowListScreen()
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToLogcat = { navController.navigate(Screen.Logcat.route) }
            )
        }

        composable(Screen.Logcat.route) {
            LogcatViewerScreen()
        }

        composable(Screen.Onboarding.route) {
            OnboardingWizardScreen(navController)
        }

        // Contextual entry point only: the dashboard exposes this destination
        // when the call-screening role is inactive; it is not a permanent menu item.
        composable(Screen.PermissionSettings.route) {
            PermissionSettingsScreen()
        }

        /**
         * Screen.Digest — blocked call review queue.
         *
         * Reached two ways:
         *   1. Deep link: notification tap fires signalgate://digest PendingIntent
         *      → MainActivity receives → NavController routes here automatically.
         *   2. Direct navigation: navController.navigate(Screen.Digest.route)
         *      from the nav drawer's "Blocked Calls" item (GlassmorphicDrawerContent.kt).
         *
         * The uriPattern must match android:scheme + android:host declared in
         * AndroidManifest.xml MainActivity intent-filter.
         */
        composable(
            route = Screen.Digest.route,
            deepLinks = listOf(navDeepLink { uriPattern = "signalgate://digest" })
        ) {
            DigestScreen()
        }
    }
}

@Composable
private fun FirstInstallRoute(
    navController: NavHostController,
    viewModel: DashboardViewModel = koinViewModel()
) {
    val onboardingComplete by viewModel.isOnboardingComplete.collectAsState()
    val destination = initialRouteFor(onboardingComplete)

    LaunchedEffect(destination) {
        destination?.let { route ->
            navController.navigate(route) {
                popUpTo(STARTUP_ROUTE) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
