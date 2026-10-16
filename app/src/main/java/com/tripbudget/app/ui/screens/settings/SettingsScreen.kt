package com.tripbudget.app.ui.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tripbudget.app.R
import com.tripbudget.app.data.ThemeMode
import com.tripbudget.app.ui.components.EnterCascade
import com.tripbudget.app.ui.components.LivePullRefresh
import com.tripbudget.app.ui.components.SoftScreenCanvas
import com.tripbudget.app.ui.theme.LoraFontFamily
import com.tripbudget.app.ui.theme.Teal100
import com.tripbudget.app.ui.theme.Teal600
import com.tripbudget.app.ui.theme.Teal700

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val cloud by viewModel.cloud.collectAsStateWithLifecycle()

    LivePullRefresh(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
    ) {
    SoftScreenCanvas {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            EnterCascade(0) {
                Text(
                    text = "Preferences",
                    style = MaterialTheme.typography.labelMedium,
                    color = Teal600,
                )
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            Spacer(Modifier.height(14.dp))

            if (cloud.user != null) {
                EnterCascade(40) {
                    Text(
                        "Account",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF5A6F6A),
                    )
                    Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(14.dp), clip = false)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            cloud.user?.email?.takeIf { it.isNotBlank() }
                                ?: "Joined as guest",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF1A2B28),
                        )
                        cloud.message?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = Teal700)
                        }
                        TextButton(
                            onClick = viewModel::signOut,
                            enabled = !cloud.busy,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        ) {
                            Text("Sign out")
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            EnterCascade(80) {
                Text(
                    "Appearance",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF5A6F6A),
                )
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(14.dp), clip = false)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .selectableGroup()
                        .padding(vertical = 2.dp),
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

            Spacer(Modifier.height(14.dp))

            EnterCascade(120) {
                AboutSection(
                    hasAccount = !cloud.user?.email.isNullOrBlank(),
                )
            }

            Spacer(Modifier.height(14.dp))

            EnterCascade(160) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(88.dp)
                        .clip(RoundedCornerShape(14.dp)),
                ) {
                    Image(
                        painter = painterResource(R.drawable.bg_hero_ocean),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.45f),
                                    ),
                                ),
                            ),
                    )
                    Text(
                        text = "Travel light. Track clearly.",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            // Room to scroll past the floating nav capsule.
            Spacer(Modifier.height(100.dp))
        }
    }
    }
}

@Composable
private fun AboutSection(
    hasAccount: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(14.dp), clip = false)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Teal100),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.AccountBalanceWallet,
                    contentDescription = null,
                    tint = Teal700,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "About",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF8A9E99),
                )
                Text(
                    stringResource(R.string.brand_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = LoraFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F2A26),
                )
            }
            Text(
                "v1.0.0",
                style = MaterialTheme.typography.labelMedium,
                color = Teal600,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFEAF5F4))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }

        Text(
            text = "Everything stays live in the cloud. Swipe down any page to refresh.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF5A6F6A),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AboutChip(
                icon = Icons.Outlined.Payments,
                label = "INR",
                modifier = Modifier.weight(1f),
            )
            AboutChip(
                icon = Icons.Outlined.Cloud,
                label = "Live",
                modifier = Modifier.weight(1f),
            )
            AboutChip(
                icon = if (hasAccount) Icons.Outlined.Cloud else Icons.Outlined.Lock,
                label = if (hasAccount) "Online" else "Join OK",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AboutChip(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF3F8F7))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Teal600, modifier = Modifier.size(14.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF1A2B28),
            maxLines = 1,
        )
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
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = Teal600),
        )
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
