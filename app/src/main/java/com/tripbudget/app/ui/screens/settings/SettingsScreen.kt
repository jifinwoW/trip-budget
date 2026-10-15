package com.tripbudget.app.ui.screens.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tripbudget.app.data.ThemeMode
import com.tripbudget.app.ui.components.EnterCascade
import com.tripbudget.app.ui.components.LedgerAtmosphere
import com.tripbudget.app.ui.components.LedgerRule

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    LedgerAtmosphere {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
        ) {
            EnterCascade(0) {
                Text(
                    text = "PREFERENCES",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(Modifier.height(28.dp))
            EnterCascade(60) {
                Text(
                    text = "APPEARANCE",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                            MaterialTheme.shapes.small,
                        )
                        .padding(vertical = 4.dp),
                ) {
                    ThemeMode.entries.forEach { mode ->
                        RowOption(
                            label = when (mode) {
                                ThemeMode.SYSTEM -> "System default"
                                ThemeMode.LIGHT -> "Light"
                                ThemeMode.DARK -> "Dark"
                            },
                            selected = themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            EnterCascade(120) {
                LedgerRule()
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "ABOUT",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "TripBudget",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    text = "Offline personal trip cashbox.\nVersion 1.0.0 · Currency INR",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun RowOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
            ),
        )
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
