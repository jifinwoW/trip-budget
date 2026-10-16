package com.tripbudget.app.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tripbudget.app.ui.theme.Charcoal900
import com.tripbudget.app.ui.theme.Teal100
import com.tripbudget.app.ui.theme.Teal600
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val NavCapsule = RoundedCornerShape(999.dp)
private val NavBubble = RoundedCornerShape(999.dp)
private const val NavAnimMs = 340

private fun AppDestination.barLabel(): String = when (this) {
    AppDestination.DASHBOARD -> "Home"
    AppDestination.EXPENSES -> "Spend"
    AppDestination.MEMBERS -> "Crew"
    AppDestination.SETTINGS -> "More"
}

/**
 * Floating capsule matching the shared design:
 * dark pill bar, every item is icon + text side-by-side,
 * teal highlight pill wraps the active icon + label.
 */
@Composable
fun SlidingBottomBar(
    destinations: List<AppDestination>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val itemBounds = remember { mutableStateMapOf<Int, Rect>() }
    var barCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val bubbleX = remember { Animatable(0f) }
    val bubbleY = remember { Animatable(0f) }
    val bubbleW = remember { Animatable(0f) }
    val bubbleH = remember { Animatable(0f) }
    var placedOnce by remember { mutableStateOf(false) }

    val barColor = Charcoal900
    val bubbleColor = Teal600
    val selectedContent = Color.White
    val unselectedContent = Teal100.copy(alpha = 0.85f)

    val selectedBounds = itemBounds[selectedIndex]
    val slideSpec = tween<Float>(durationMillis = NavAnimMs, easing = FastOutSlowInEasing)

    LaunchedEffect(selectedIndex, selectedBounds) {
        val bounds = selectedBounds ?: return@LaunchedEffect
        // Tight padding so the pill hugs icon + text like the reference.
        val padH = with(density) { 2.dp.toPx() }
        val padV = with(density) { 2.dp.toPx() }
        val targetX = (bounds.left - padH).coerceAtLeast(0f)
        val targetY = (bounds.top - padV).coerceAtLeast(0f)
        val targetW = bounds.width + padH * 2
        val targetH = bounds.height + padV * 2

        if (!placedOnce) {
            bubbleX.snapTo(targetX)
            bubbleY.snapTo(targetY)
            bubbleW.snapTo(targetW)
            bubbleH.snapTo(targetH)
            placedOnce = true
            return@LaunchedEffect
        }

        launch { bubbleX.animateTo(targetX, slideSpec) }
        launch { bubbleY.animateTo(targetY, slideSpec) }
        launch { bubbleW.animateTo(targetW, slideSpec) }
        launch { bubbleH.animateTo(targetH, slideSpec) }
    }

    val bubbleWidthDp = with(density) { bubbleW.value.toDp() }
    val bubbleHeightDp = with(density) { bubbleH.value.toDp() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(18.dp, NavCapsule, clip = false)
                .clip(NavCapsule)
                .background(barColor)
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .onGloballyPositioned { barCoords = it },
        ) {
            if (placedOnce && bubbleWidthDp > 0.dp && bubbleHeightDp > 0.dp) {
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                bubbleX.value.roundToInt(),
                                bubbleY.value.roundToInt(),
                            )
                        }
                        .width(bubbleWidthDp)
                        .height(bubbleHeightDp)
                        .clip(NavBubble)
                        .background(bubbleColor),
                )
            }

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                destinations.forEachIndexed { index, dest ->
                    val selected = index == selectedIndex
                    val contentColor by animateColorAsState(
                        targetValue = if (selected) selectedContent else unselectedContent,
                        animationSpec = tween(NavAnimMs, easing = FastOutSlowInEasing),
                        label = "nav_content_$index",
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            modifier = Modifier
                                .wrapContentWidth()
                                .height(40.dp)
                                .onGloballyPositioned { coords ->
                                    val parent = barCoords ?: return@onGloballyPositioned
                                    if (parent.isAttached && coords.isAttached) {
                                        itemBounds[index] = parent.localBoundingBoxOf(coords)
                                    }
                                }
                                .clip(NavBubble)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onSelect(index) },
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = dest.icon,
                                contentDescription = dest.label,
                                tint = contentColor,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = dest.barLabel(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) {
                                    FontWeight.SemiBold
                                } else {
                                    FontWeight.Medium
                                },
                                color = contentColor,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}
