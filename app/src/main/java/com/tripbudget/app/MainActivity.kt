package com.tripbudget.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tripbudget.app.location.RouteTripSchedule
import com.tripbudget.app.notify.TripReminders
import com.tripbudget.app.ui.navigation.TripBudgetNavHost
import com.tripbudget.app.ui.screens.settings.SettingsViewModel
import com.tripbudget.app.ui.theme.TripBudgetTheme
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        var keepSplash by mutableStateOf(true)
        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition { keepSplash }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as TripBudgetApp
        // Hold splash until auth session is restored from storage (avoids Sign-in flash).
        lifecycleScope.launch {
            withTimeoutOrNull(3_000) {
                app.container.authRepository.isAuthResolving.first { resolving -> !resolving }
            }
            keepSplash = false
        }

        app.container.routeArrivalTracker.start()
        lifecycleScope.launch {
            combine(
                app.container.repository.observeTrips(),
                app.container.authRepository.sessionStatus,
            ) { _, _ ->
                app.container.authRepository.currentUserIdOrNull()
            }.collect { userId ->
                RouteTripSchedule.refresh(app, app.container.repository, userId)
                TripReminders.refresh(app, app.container.repository)
            }
        }

        setContent {
            val settingsVm: SettingsViewModel = viewModel(
                factory = SettingsViewModel.factory(
                    appContext = applicationContext,
                    themePreferences = app.container.themePreferences,
                    authRepository = app.container.authRepository,
                    syncEngine = app.container.syncEngine,
                    syncPreferences = app.container.syncPreferences,
                    tripRepository = app.container.repository,
                ),
            )
            val themeMode by settingsVm.themeMode.collectAsStateWithLifecycle()
            TripBudgetTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TripBudgetNavHost(
                        repository = app.container.repository,
                        themePreferences = app.container.themePreferences,
                        authRepository = app.container.authRepository,
                        syncEngine = app.container.syncEngine,
                        syncPreferences = app.container.syncPreferences,
                        routeArrivalTracker = app.container.routeArrivalTracker,
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        (application as TripBudgetApp).container.routeArrivalTracker.stop()
        super.onDestroy()
    }
}
