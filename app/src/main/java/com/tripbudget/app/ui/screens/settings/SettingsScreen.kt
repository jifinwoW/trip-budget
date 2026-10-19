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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tripbudget.app.R
import com.tripbudget.app.data.HeaderWeatherPreference
import com.tripbudget.app.data.ThemeMode
import com.tripbudget.app.ui.components.EnterCascade
import com.tripbudget.app.ui.components.LivePullRefresh
import com.tripbudget.app.ui.components.SoftScreenCanvas
import com.tripbudget.app.ui.theme.CoastalTokens
import com.tripbudget.app.ui.theme.LoraFontFamily
import com.tripbudget.app.ui.theme.Teal600
import com.tripbudget.app.ui.theme.floatingNavContentPadding

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val headerWeather by viewModel.headerWeather.collectAsStateWithLifecycle()
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
                        color = CoastalTokens.muted,
                    )
                    Spacer(Modifier.height(8.dp))
                    AccountCard(
                        email = cloud.user?.email,
                        message = cloud.message,
                        busy = cloud.busy,
                        onSignOut = viewModel::signOut,
                    )
                }
                Spacer(Modifier.height(14.dp))
            }

            EnterCascade(80) {
                Text(
                    "Appearance",
                    style = MaterialTheme.typography.titleMedium,
                    color = CoastalTokens.muted,
                )
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(14.dp), clip = false)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CoastalTokens.card)
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

            EnterCascade(100) {
                Text(
                    "Header weather",
                    style = MaterialTheme.typography.titleMedium,
                    color = CoastalTokens.muted,
                )
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(14.dp), clip = false)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CoastalTokens.card)
                        .selectableGroup()
                        .padding(vertical = 2.dp),
                ) {
                    HeaderWeatherPreference.entries.forEach { preference ->
                        RowOption(
                            label = preference.label(),
                            selected = headerWeather == preference,
                            onClick = { viewModel.setHeaderWeather(preference) },
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
            // Room so the last cards clear the floating nav capsule.
            Spacer(Modifier.height(floatingNavContentPadding()))
        }
    }
    }
}

@Composable
private fun AccountCard(
    email: String?,
    message: String?,
    busy: Boolean,
    onSignOut: () -> Unit,
) {
    val hasEmail = !email.isNullOrBlank()
    val displayName = if (hasEmail) email!!.substringBefore("@") else "Guest"
    val initial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(14.dp), clip = false)
            .clip(RoundedCornerShape(14.dp))
            .background(CoastalTokens.card)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(CoastalTokens.tealSoft, CoastalTokens.softFillAlt),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (hasEmail) {
                    Text(
                        initial,
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = LoraFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = CoastalTokens.tealSoftFg,
                    )
                } else {
                    Icon(
                        Icons.Outlined.Person,
                        contentDescription = null,
                        tint = CoastalTokens.tealSoftFg,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (hasEmail) displayName.replaceFirstChar { it.titlecase() } else "Guest traveler",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = CoastalTokens.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        if (hasEmail) Icons.Outlined.MailOutline else Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = CoastalTokens.muted,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        if (hasEmail) email!! else "Joined without an email",
                        style = MaterialTheme.typography.bodySmall,
                        color = CoastalTokens.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                if (hasEmail) "Signed in" else "Guest",
                style = MaterialTheme.typography.labelMedium,
                color = if (hasEmail) CoastalTokens.settledFg else CoastalTokens.muted,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (hasEmail) CoastalTokens.settledSoft else CoastalTokens.softFill,
                    )
                    .padding(horizontal = 9.dp, vertical = 5.dp),
            )
        }

        message?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = CoastalTokens.accent,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, CoastalTokens.danger.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
                .background(CoastalTokens.dangerSoft)
                .clickable(enabled = !busy, onClick = onSignOut)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = CoastalTokens.danger,
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    "Signing out…",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = CoastalTokens.danger,
                )
            } else {
                Icon(
                    Icons.AutoMirrored.Outlined.Logout,
                    contentDescription = null,
                    tint = CoastalTokens.danger,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    "Sign out",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = CoastalTokens.danger,
                )
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
            .background(CoastalTokens.card)
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
                    .background(CoastalTokens.tealSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.AccountBalanceWallet,
                    contentDescription = null,
                    tint = CoastalTokens.tealSoftFg,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "About",
                    style = MaterialTheme.typography.labelMedium,
                    color = CoastalTokens.muted,
                )
                Text(
                    stringResource(R.string.brand_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = LoraFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = CoastalTokens.ink,
                )
            }
            Text(
                "v1.0.0",
                style = MaterialTheme.typography.labelMedium,
                color = Teal600,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(CoastalTokens.softFillAlt)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }

        Text(
            text = "Everything stays live in the cloud. Swipe down any page to refresh.",
            style = MaterialTheme.typography.bodySmall,
            color = CoastalTokens.muted,
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
            .background(CoastalTokens.softFill)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Teal600, modifier = Modifier.size(14.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = CoastalTokens.ink,
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
