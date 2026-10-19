package com.tripbudget.app.ui.components

import android.os.Build.VERSION.SDK_INT
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.tripbudget.app.R
import com.tripbudget.app.ui.theme.Teal600

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
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 500f),
        label = "roadMapChipScale",
    )

    val subtitle = when {
        stopCount <= 0 -> "Plan your route"
        !nextStopTitle.isNullOrBlank() -> "Next: $nextStopTitle"
        stopCount == 1 -> "1 stop"
        else -> "$stopCount stops"
    }

    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(10.dp, RoundedCornerShape(28.dp), spotColor = Teal600.copy(alpha = 0.45f))
            .clip(RoundedCornerShape(28.dp))
            .background(Teal600)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .height(56.dp)
            .widthIn(min = 148.dp)
            .padding(start = 8.dp, end = 16.dp),
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
