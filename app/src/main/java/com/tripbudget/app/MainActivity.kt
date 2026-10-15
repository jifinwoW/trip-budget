package com.tripbudget.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tripbudget.app.ui.navigation.TripBudgetNavHost
import com.tripbudget.app.ui.screens.settings.SettingsViewModel
import com.tripbudget.app.ui.theme.TripBudgetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as TripBudgetApp
        setContent {
            val settingsVm: SettingsViewModel = viewModel(
                factory = SettingsViewModel.factory(app.container.themePreferences),
            )
            val themeMode by settingsVm.themeMode.collectAsStateWithLifecycle()
            TripBudgetTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TripBudgetNavHost(
                        repository = app.container.repository,
                        themePreferences = app.container.themePreferences,
                    )
                }
            }
        }
    }
}
