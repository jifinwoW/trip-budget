package com.tripbudget.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripbudget.app.R
import com.tripbudget.app.domain.Money
import com.tripbudget.app.ui.theme.CoastalTokens

/** Clean dashboard canvas — subtle tinted wash with floating soft orbs. */
@Composable
fun SoftDashboardCanvas(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val canvas = CoastalTokens.canvas
    val orbAlpha = if (CoastalTokens.isDark) 0.10f else 0.22f
    val warmAlpha = if (CoastalTokens.isDark) 0.08f else 0.18f
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(canvas),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF7ED9C8).copy(alpha = orbAlpha), Color.Transparent),
                    center = Offset(size.width * 0.08f, size.height * 0.04f),
                    radius = size.minDimension * 0.45f,
                ),
                radius = size.minDimension * 0.45f,
                center = Offset(size.width * 0.08f, size.height * 0.04f),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFE8D4B8).copy(alpha = warmAlpha), Color.Transparent),
                    center = Offset(size.width * 0.95f, size.height * 0.12f),
                    radius = size.minDimension * 0.35f,
                ),
                radius = size.minDimension * 0.35f,
                center = Offset(size.width * 0.95f, size.height * 0.12f),
            )
        }
        content()
    }
}

/** Compact gradient hero card — tighter padding, refined typography. */
@Composable
fun GradientHeroCard(
    title: String,
    amountPaise: Long,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    amountColor: Color = Color.White,
    @DrawableRes backgroundRes: Int = R.drawable.bg_hero_ocean,
) {
    val oceanFallback = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0A6B60),
            Color(0xFF119584),
            Color(0xFF2DB8A8),
        ),
        start = Offset.Zero,
        end = Offset(900f, 600f),
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp), clip = false)
            .clip(RoundedCornerShape(20.dp))
            .background(oceanFallback),
    ) {
        Image(
            painter = painterResource(backgroundRes),
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
                            Color(0xFF0A6B60).copy(alpha = 0.60f),
                            Color(0xFF0A6B60).copy(alpha = 0.82f),
                        ),
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.85f),
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.60f),
                    fontSize = 11.sp,
                )
            }
            Text(
                text = Money.formatInr(amountPaise),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor,
            )
        }
    }
}

/** Compact metric tile — icon + label + amount in a tight horizontal card. */
@Composable
fun IconMetricTile(
    label: String,
    amountPaise: Long,
    icon: ImageVector,
    tint: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    amountColor: Color = CoastalTokens.ink,
    @DrawableRes backgroundRes: Int? = null,
) {
    val iconPlate = if (CoastalTokens.isDark) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color.White.copy(alpha = 0.78f)
    }
    Box(
        modifier = modifier
            .shadow(3.dp, RoundedCornerShape(16.dp), clip = false)
            .clip(RoundedCornerShape(16.dp))
            .background(tint),
    ) {
        if (backgroundRes != null) {
            Image(
                painter = painterResource(backgroundRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(tint.copy(alpha = 0.62f)),
            )
        }
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconPlate),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(17.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = amountColor.copy(alpha = 0.58f),
                )
                Text(
                    text = Money.formatInr(amountPaise),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor,
                )
            }
        }
    }
}

/** Inline summary chip — icon + label + value in a slim white row. */
@Composable
fun OverviewIconChip(
    label: String,
    value: String,
    icon: ImageVector,
    chipColor: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .shadow(2.dp, RoundedCornerShape(14.dp), clip = false)
            .clip(RoundedCornerShape(14.dp))
            .background(CoastalTokens.card)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(chipColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(15.dp),
            )
        }
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = CoastalTokens.muted,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = CoastalTokens.ink,
            )
        }
    }
}

/**
 * Showcase progress block: stacked pill bars (one under the other).
 * - Budget used = total expenses / planned budget
 * - Contributions collected = payments / sum of expected contributions
 * Bar fill clamps to 0–100%; the % label shows the true value (can exceed 100%).
 */
@Composable
fun SoftProgressCard(
    budgetUsed: Float,
    collectedProgress: Float,
    overBudget: Boolean,
    modifier: Modifier = Modifier,
) {
    val budgetPct = (budgetUsed * 100f).toInt().coerceAtLeast(0)
    val collectedPct = (collectedProgress * 100f).toInt().coerceAtLeast(0)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CoastalTokens.softFill)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ShowcaseProgressBar(
            label = "Budget used",
            percentLabel = "$budgetPct%",
            progress = budgetUsed.coerceIn(0f, 1f),
            fillColor = if (overBudget) CoastalTokens.danger else CoastalTokens.progressFill,
        )
        ShowcaseProgressBar(
            label = "Contributions collected",
            percentLabel = "$collectedPct%",
            progress = collectedProgress.coerceIn(0f, 1f),
            fillColor = CoastalTokens.progressFill,
        )
    }
}

/** Same teal used on dashboard progress fills (light-mode default). */
val CoastalProgressTeal: Color = Color(0xFF0D8B8A)

/**
 * Custom pill progress track (not Material LinearProgressIndicator).
 * Avoids M3 stop-indicator / clip glitches; fill clamps to 0–100%.
 */
@Composable
fun CoastalProgressTrack(
    progress: Float,
    fillColor: Color = CoastalTokens.progressFill,
    modifier: Modifier = Modifier,
    trackColor: Color = CoastalTokens.progressTrack,
    height: Dp = 8.dp,
) {
    // Weight-based fill (not fillMaxWidth(fraction)) so 75%/100% always lays out correctly.
    val clamped = progress.coerceIn(0f, 1f)
    val fillWeight = clamped.coerceAtLeast(0.0001f)
    val emptyWeight = (1f - clamped).coerceAtLeast(0.0001f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(999.dp))
            .background(trackColor),
    ) {
        if (clamped > 0f) {
            Box(
                modifier = Modifier
                    .weight(fillWeight)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(fillColor, fillColor.copy(alpha = 0.85f)),
                        ),
                    ),
            )
        }
        if (clamped < 1f) {
            Spacer(modifier = Modifier.weight(emptyWeight))
        }
    }
}

@Composable
private fun ShowcaseProgressBar(
    label: String,
    percentLabel: String,
    progress: Float,
    fillColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = CoastalTokens.muted,
                maxLines = 2,
            )
            Text(
                text = percentLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = CoastalTokens.ink,
            )
        }
        Spacer(Modifier.height(8.dp))
        CoastalProgressTrack(
            progress = progress,
            fillColor = fillColor,
        )
    }
}
