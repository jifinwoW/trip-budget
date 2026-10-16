package com.tripbudget.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tripbudget.app.ui.theme.CoastalTokens
import com.tripbudget.app.ui.theme.floatingNavContentPadding

@Composable
fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerShift",
    )
    val base = if (CoastalTokens.isDark) Color(0xFF243835) else Color(0xFFE4EEEC)
    val highlight = if (CoastalTokens.isDark) Color(0xFF3A524C) else Color(0xFFF7FBFA)
    val x = shift * 1200f
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(x - 200f, 0f),
        end = Offset(x + 200f, 400f),
    )
}

@Composable
fun ShimmerBlock(
    brush: Brush,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(14.dp),
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(brush),
    )
}

/** Ghost Home layout while trip totals load. */
@Composable
fun DashboardShimmer(
    modifier: Modifier = Modifier,
) {
    val brush = rememberShimmerBrush()
    SoftScreenCanvas(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = floatingNavContentPadding()),
        ) {
            ShimmerBlock(
                brush = brush,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            )
            Spacer(Modifier.height(18.dp))
            ShimmerBlock(
                brush = brush,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(108.dp),
                shape = RoundedCornerShape(20.dp),
            )
            Spacer(Modifier.height(14.dp))
            ShimmerBlock(
                brush = brush,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                shape = RoundedCornerShape(20.dp),
            )
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ShimmerBlock(
                    brush = brush,
                    modifier = Modifier
                        .weight(1f)
                        .height(78.dp),
                    shape = RoundedCornerShape(22.dp),
                )
                ShimmerBlock(
                    brush = brush,
                    modifier = Modifier
                        .weight(1f)
                        .height(78.dp),
                    shape = RoundedCornerShape(22.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ShimmerBlock(
                    brush = brush,
                    modifier = Modifier
                        .weight(1f)
                        .height(78.dp),
                    shape = RoundedCornerShape(22.dp),
                )
                ShimmerBlock(
                    brush = brush,
                    modifier = Modifier
                        .weight(1f)
                        .height(78.dp),
                    shape = RoundedCornerShape(22.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ShimmerBlock(
                    brush = brush,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                )
                ShimmerBlock(
                    brush = brush,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                )
            }
            Spacer(Modifier.height(22.dp))
            ShimmerBlock(
                brush = brush,
                modifier = Modifier
                    .width(140.dp)
                    .height(22.dp),
                shape = RoundedCornerShape(8.dp),
            )
            Spacer(Modifier.height(12.dp))
            repeat(3) {
                ShimmerBlock(
                    brush = brush,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .height(72.dp),
                    shape = RoundedCornerShape(18.dp),
                )
            }
        }
    }
}

/** Ghost list rows for Spend / Crew while data loads. */
@Composable
fun ListShimmer(
    modifier: Modifier = Modifier,
    rows: Int = 6,
    rowHeight: Dp = 78.dp,
) {
    val brush = rememberShimmerBrush()
    SoftScreenCanvas(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .padding(bottom = floatingNavContentPadding()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ShimmerBlock(
                brush = brush,
                modifier = Modifier
                    .width(160.dp)
                    .height(28.dp),
                shape = RoundedCornerShape(8.dp),
            )
            ShimmerBlock(
                brush = brush,
                modifier = Modifier
                    .width(220.dp)
                    .height(16.dp),
                shape = RoundedCornerShape(6.dp),
            )
            Spacer(Modifier.height(4.dp))
            repeat(rows) {
                ShimmerBlock(
                    brush = brush,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight),
                    shape = RoundedCornerShape(18.dp),
                )
            }
        }
    }
}
