package com.tripbudget.app.ui.components

import android.os.Build.VERSION.SDK_INT
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.tripbudget.app.R
import kotlin.math.abs
import kotlin.math.roundToInt
import com.tripbudget.app.ui.theme.Teal600

private val PeekWidth = 16.dp
private val OpenCorner = 28.dp
private val DockedCorner = 8.dp

/**
 * Interactive dashboard CTA for the road-map / Plan screen.
 * Animated mountain-road GIF preview.
 */
@Composable
fun RoadMapRouteChip(
    stopCount: Int,
    nextStopTitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }
    var chipWidthPx by remember { mutableStateOf(0) }
    var chipHeightPx by remember { mutableStateOf(0) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 500f),
        label = "roadMapChipScale",
    )
    val motion = tween<Dp>(280, easing = FastOutSlowInEasing)
    // 0 parked on the edge, 1 fully open. Width is applied in the offset so the
    // first measurement does not animate the tab into place.
    val reveal by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "roadMapReveal",
    )
    // Open state matches the original pill: full rounding, inset from the edge.
    val corner by animateDpAsState(
        targetValue = if (expanded) OpenCorner else DockedCorner,
        animationSpec = motion,
        label = "roadMapCorner",
    )
    val endCorner by animateDpAsState(
        targetValue = if (expanded) OpenCorner else 0.dp,
        animationSpec = motion,
        label = "roadMapEndCorner",
    )
    val endInset by animateDpAsState(
        targetValue = if (expanded) 16.dp else 0.dp,
        animationSpec = motion,
        label = "roadMapInset",
    )
    val startPadding by animateDpAsState(
        targetValue = if (expanded) 8.dp else PeekWidth,
        animationSpec = motion,
        label = "roadMapStart",
    )
    val shape = RoundedCornerShape(
        topStart = corner,
        bottomStart = corner,
        topEnd = endCorner,
        bottomEnd = endCorner,
    )

    val subtitle = when {
        stopCount <= 0 -> "Plan your route"
        !nextStopTitle.isNullOrBlank() -> "Next: $nextStopTitle"
        stopCount == 1 -> "1 stop"
        else -> "$stopCount stops"
    }

    Box(
        modifier = modifier
            .padding(end = endInset)
            .onSizeChanged {
                chipWidthPx = it.width
                chipHeightPx = it.height
            }
            .offset {
                val hidden = if (chipWidthPx == 0) {
                    4000f
                } else {
                    (chipWidthPx - PeekWidth.toPx()).coerceAtLeast(0f)
                }
                IntOffset((hidden * (1f - reveal)).roundToInt(), 0)
            },
    ) {
    Row(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(10.dp, shape, spotColor = Teal600.copy(alpha = 0.45f))
            .clip(shape)
            .background(Teal600)
            .pointerInput(expanded, onClick) {
                detectRoadMapChipGesture(
                    expanded = expanded,
                    onPressed = { pressed = it },
                    onExpand = { expanded = true },
                    onCollapse = { expanded = false },
                    onOpen = onClick,
                )
            }
            .height(56.dp)
            .widthIn(min = 148.dp)
            .padding(start = startPadding, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MountainRoadGifPreview(
            modifier = Modifier.size(width = 64.dp, height = 44.dp),
        )
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = "Road map",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.82f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
        if (expanded && chipWidthPx > 0 && chipHeightPx > 0) {
            val density = LocalDensity.current
            Popup(
                alignment = Alignment.TopStart,
                onDismissRequest = { expanded = false },
                properties = PopupProperties(
                    focusable = true,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true,
                ),
            ) {
                Box(
                    Modifier
                        .size(
                            width = with(density) { chipWidthPx.toDp() },
                            height = with(density) { chipHeightPx.toDp() },
                        )
                        .pointerInput(onClick) {
                            detectRoadMapChipGesture(
                                expanded = true,
                                onPressed = { pressed = it },
                                onExpand = { expanded = true },
                                onCollapse = { expanded = false },
                                onOpen = onClick,
                            )
                        },
                )
            }
        }
    }
}

private suspend fun PointerInputScope.detectRoadMapChipGesture(
    expanded: Boolean,
    onPressed: (Boolean) -> Unit,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    onOpen: () -> Unit,
) {
    awaitEachGesture {
        awaitFirstDown()
        onPressed(true)
        var dragX = 0f
        var moved = false
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull() ?: break
            if (!change.pressed) break
            val dx = change.positionChange().x
            if (abs(dx) > 2f) {
                moved = true
                dragX += dx
                change.consume()
            }
        }
        onPressed(false)
        when {
            dragX < -36f -> onExpand()
            dragX > 36f -> onCollapse()
            !moved && expanded -> onOpen()
            !moved -> onExpand()
        }
    }
}

@Composable
private fun MountainRoadGifPreview(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val gifLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }
    val request = remember {
        ImageRequest.Builder(context)
            .data(R.raw.roadmap_chip)
            .crossfade(false)
            .build()
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1A2820)),
    ) {
        AsyncImage(
            model = request,
            imageLoader = gifLoader,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
