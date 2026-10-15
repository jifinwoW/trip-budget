package com.tripbudget.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp
import com.tripbudget.app.ui.theme.Sea600
import com.tripbudget.app.ui.theme.Spruce800

@Composable
fun LedgerAtmosphere(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val bg = MaterialTheme.colorScheme.background
    val wash = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
    val spruceWash = Spruce800.copy(alpha = 0.05f)
    val line = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bg),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Soft diagonal field wash — cartographic depth without flat fill.
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(wash, Color.Transparent, spruceWash),
                    start = Offset.Zero,
                    end = Offset(size.width * 1.1f, size.height * 0.85f),
                ),
            )
            val step = 28.dp.toPx()
            val effect = PathEffect.dashPathEffect(floatArrayOf(2f, 10f), 0f)
            var x = -size.height
            while (x < size.width + size.height) {
                drawLine(
                    color = line,
                    start = Offset(x, 0f),
                    end = Offset(x + size.height, size.height),
                    strokeWidth = 1f,
                    pathEffect = effect,
                    alpha = 0.45f,
                )
                x += step
            }
            // Sea-glass glow in the upper-right — anchors the cash hero.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Sea600.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width * 0.88f, size.height * 0.08f),
                    radius = size.minDimension * 0.55f,
                ),
                radius = size.minDimension * 0.55f,
                center = Offset(size.width * 0.88f, size.height * 0.08f),
            )
        }
        content()
    }
}
