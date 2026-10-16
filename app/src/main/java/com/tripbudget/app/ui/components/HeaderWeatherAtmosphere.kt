package com.tripbudget.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import com.tripbudget.app.places.WeatherKind
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Editorial weather wash for the app header — mood lighting, not a widget.
 * Sparse motion, soft gradients, text stays readable on top.
 */
@Composable
fun HeaderWeatherAtmosphere(
    kind: WeatherKind?,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val infinite = rememberInfiniteTransition(label = "headerWeather")
    val drift = infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "drift",
    )
    val rainPhase = infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rain",
    )
    // Storm flash clock — long dark gaps, sudden strikes.
    val flashPhase = infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4_200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "lightningFlash",
    )
    // Continuous seconds — never restarts (avoids the whole field hitching).
    var snowTimeSec by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(kind) {
        if (kind != WeatherKind.SNOW) return@LaunchedEffect
        var lastNanos = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastNanos != 0L) {
                    snowTimeSec += (now - lastNanos) / 1_000_000_000f
                }
                lastNanos = now
            }
        }
    }

    val rainDrops = remember {
        List(48) { i ->
            val rnd = Random(40 + i)
            RainDrop(
                x = rnd.nextFloat(),
                y = rnd.nextFloat(),
                len = 0.65f + rnd.nextFloat() * 0.55f,
                speed = 0.75f + rnd.nextFloat() * 0.7f,
                alpha = 0.55f + rnd.nextFloat() * 0.45f,
            )
        }
    }
    val lightningBolts = remember {
        listOf(
            buildLightningBolt(seed = 17, startX = 0.58f, endX = 0.42f),
            buildLightningBolt(seed = 41, startX = 0.78f, endX = 0.68f),
        )
    }
    val snowFlakes = remember {
        List(64) { i ->
            val rnd = Random(90 + i)
            val depth = rnd.nextFloat() // 0 = far dust, 1 = near flakes
            SnowFlake(
                x = rnd.nextFloat(),
                y = rnd.nextFloat(),
                size = 0.35f + depth * 1.35f + rnd.nextFloat() * 0.35f,
                speed = 0.35f + depth * 0.85f + rnd.nextFloat() * 0.25f,
                sway = 0.45f + rnd.nextFloat() * 0.9f,
                alpha = 0.35f + depth * 0.5f + rnd.nextFloat() * 0.15f,
                depth = depth,
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val t = drift.value
            val r = rainPhase.value
            val flash = flashPhase.value
            // Unbounded phase so sin/fall keep flowing; per-flake % wraps stagger.
            val snowT = snowTimeSec / 3.6f

            // Base sky wash — weather-keyed, soft enough for brand type on top
            val wash = headerWash(kind, isDark)
            drawRect(brush = Brush.verticalGradient(wash))

            when (kind) {
                WeatherKind.SUNNY -> drawSunny(w, h, t, isDark)
                WeatherKind.CLOUDY -> drawCloudy(w, h, t, isDark)
                WeatherKind.FOG -> drawFog(w, h, t, isDark)
                WeatherKind.RAIN -> {
                    drawRainSky(w, h, isDark)
                    drawRain(w, h, r, rainDrops, isDark)
                }
                WeatherKind.STORM -> {
                    drawStormSky(w, h, isDark)
                    drawRain(w, h, r, rainDrops, isDark, heavier = true)
                    drawLightning(w, h, flash, lightningBolts, isDark)
                }
                WeatherKind.SNOW -> {
                    drawSnowSky(w, h, snowT, isDark)
                    drawSnow(w, h, snowT, snowFlakes, isDark)
                }
                WeatherKind.UNKNOWN, null -> {
                    // Soft default coastal mist only
                    drawDefaultMist(w, h, t, isDark)
                }
            }
        }
    }
}

/** Gradient stops for the header background by weather. */
fun headerWashColors(kind: WeatherKind?, isDark: Boolean): List<Color> {
    if (isDark) {
        return when (kind) {
            WeatherKind.SUNNY -> listOf(Color(0xFF1A2820), Color(0xFF12201E), Color(0xFF0E1A18))
            WeatherKind.CLOUDY -> listOf(Color(0xFF1A2228), Color(0xFF141C22), Color(0xFF0E161C))
            WeatherKind.RAIN -> listOf(Color(0xFF162028), Color(0xFF101820), Color(0xFF0C141C))
            WeatherKind.STORM -> listOf(Color(0xFF181428), Color(0xFF120E1E), Color(0xFF0C0A16))
            WeatherKind.SNOW -> listOf(Color(0xFF1A222A), Color(0xFF141C24), Color(0xFF101820))
            WeatherKind.FOG -> listOf(Color(0xFF1C2224), Color(0xFF161C1E), Color(0xFF121618))
            else -> listOf(Color(0xFF12201E), Color(0xFF0E1A18), Color(0xFF0C1816))
        }
    }
    return when (kind) {
        WeatherKind.SUNNY -> listOf(Color(0xFFBFE8F2), Color(0xFFE8F6FA), Color(0xFFF8FCFD))
        WeatherKind.CLOUDY -> listOf(Color(0xFFC5D4DE), Color(0xFFDCE6EC), Color(0xFFEEF3F6))
        WeatherKind.RAIN -> listOf(Color(0xFF8FA8B8), Color(0xFFB0C4D0), Color(0xFFD0DCE4))
        WeatherKind.STORM -> listOf(Color(0xFF8FA0B4), Color(0xFFA8B4C4), Color(0xFFC8D0DC))
        // Cooler slate so white flakes read in light mode
        WeatherKind.SNOW -> listOf(Color(0xFF9AAEBE), Color(0xFFB8C8D4), Color(0xFFD2DCE4))
        WeatherKind.FOG -> listOf(Color(0xFFC8D0D4), Color(0xFFDCE2E4), Color(0xFFEEF0F0))
        else -> listOf(Color(0xFFD4EBF5), Color(0xFFE8F4FA), Color(0xFFF5FBFC))
    }
}

private fun headerWash(kind: WeatherKind?, isDark: Boolean): List<Color> =
    headerWashColors(kind, isDark)

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDefaultMist(
    w: Float,
    h: Float,
    t: Float,
    isDark: Boolean,
) {
    val scale = if (isDark) 0.14f else 1f
    softOrb(w * (0.15f + 0.02f * sin(t * 6.28f)), h * 0.55f, h * 0.9f, Color.White, 0.55f * scale)
    softOrb(w * 0.72f, h * 0.4f, h * 0.75f, Color.White, 0.4f * scale)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSunny(
    w: Float,
    h: Float,
    t: Float,
    isDark: Boolean,
) {
    val glow = if (isDark) Color(0xFFFFD89A) else Color(0xFFFFE8B0)
    val alpha = if (isDark) 0.12f else 0.55f
    // Soft sun glow — upper right, never a hard disc
    softOrb(w * 0.88f, h * -0.1f, h * 1.6f, glow, alpha)
    softOrb(w * 0.78f, h * 0.15f, h * 0.9f, Color.White, if (isDark) 0.06f else 0.35f)
    // Whisper of warm rays
    val rayAlpha = if (isDark) 0.04f else 0.08f
    for (i in 0 until 5) {
        val angle = -0.55f + i * 0.22f + sin(t * 6.28f) * 0.02f
        val x2 = w * 0.88f + cos(angle) * w * 0.55f
        val y2 = h * -0.05f + sin(angle) * h * 1.4f
        drawLine(
            color = glow.copy(alpha = rayAlpha),
            start = Offset(w * 0.88f, h * -0.05f),
            end = Offset(x2, y2),
            strokeWidth = h * 0.08f,
            cap = StrokeCap.Round,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCloudy(
    w: Float,
    h: Float,
    t: Float,
    isDark: Boolean,
) {
    val scale = if (isDark) 0.2f else 0.85f
    val tint = if (isDark) Color(0xFFB0C0D0) else Color.White
    // Soft elongated mist bands (not stacked white circles)
    val shift = sin(t * 6.28f) * 0.04f
    softOrb(w * (0.2f + shift), h * 0.35f, h * 1.1f, tint, 0.45f * scale)
    softOrb(w * (0.55f - shift * 0.5f), h * 0.55f, h * 0.95f, tint, 0.38f * scale)
    softOrb(w * (0.85f + shift * 0.3f), h * 0.3f, h * 0.85f, tint, 0.32f * scale)
    // Cool shadow veil
    drawRect(
        brush = Brush.verticalGradient(
            listOf(
                Color(0xFF6A7A8A).copy(alpha = if (isDark) 0.12f else 0.08f),
                Color.Transparent,
            ),
        ),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFog(
    w: Float,
    h: Float,
    t: Float,
    isDark: Boolean,
) {
    val a = if (isDark) 0.18f else 0.35f
    val shift = sin(t * 6.28f) * 0.03f
    softOrb(w * (0.3f + shift), h * 0.7f, h * 1.4f, Color.White, a)
    softOrb(w * (0.7f - shift), h * 0.5f, h * 1.2f, Color.White, a * 0.85f)
    drawRect(Color(0xFFD0D4D8).copy(alpha = if (isDark) 0.08f else 0.22f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRainSky(w: Float, h: Float, isDark: Boolean) {
    // Slightly deeper wash so white streaks pop against the header
    softOrb(w * 0.3f, h * 0.2f, h * 1.2f, Color(0xFF6A8498), if (isDark) 0.22f else 0.48f)
    softOrb(w * 0.75f, h * 0.35f, h * 1.0f, Color(0xFF5A7488), if (isDark) 0.18f else 0.4f)
    drawRect(Color(0xFF4A6070).copy(alpha = if (isDark) 0.1f else 0.12f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStormSky(w: Float, h: Float, isDark: Boolean) {
    softOrb(w * 0.4f, h * 0.1f, h * 1.4f, Color(0xFF5A6080), if (isDark) 0.22f else 0.4f)
    softOrb(w * 0.8f, h * 0.4f, h * 1.1f, Color(0xFF404868), if (isDark) 0.18f else 0.32f)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSnowSky(
    w: Float,
    h: Float,
    phase: Float,
    isDark: Boolean,
) {
    // Quiet sky; nudge only during occasional gusts
    val gust = snowGustStrength(phase) * 0.05f
    val mist = if (isDark) Color(0xFFB8C8D8) else Color(0xFF6A8498)
    softOrb(w * (0.2f + gust), h * 0.25f, h * 1.15f, mist, if (isDark) 0.16f else 0.42f)
    softOrb(w * (0.65f - gust * 0.6f), h * 0.4f, h * 1.0f, mist, if (isDark) 0.12f else 0.36f)
    softOrb(w * (0.9f + gust * 0.4f), h * 0.15f, h * 0.85f, Color.White, if (isDark) 0.08f else 0.22f)
    drawRect(
        brush = Brush.verticalGradient(
            listOf(
                Color(0xFF5A7088).copy(alpha = if (isDark) 0.14f else 0.16f),
                Color.Transparent,
            ),
        ),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRain(
    w: Float,
    h: Float,
    phase: Float,
    drops: List<RainDrop>,
    isDark: Boolean,
    heavier: Boolean = false,
) {
    clipRect {
        val tilt = 0.2f
        // Light header sky needs stronger streaks to read; dark mode stays a bit softer.
        val baseAlpha = if (isDark) 0.78f else 0.92f
        val count = if (heavier) drops.size else drops.size
        for (i in 0 until count) {
            val d = drops[i]
            val y = ((d.y + phase * d.speed) % 1.15f) - 0.08f
            val x = d.x * w
            val len = h * (0.22f + d.len * 0.28f) * if (heavier) 1.2f else 1f
            val x1 = x
            val y1 = y * h
            val x2 = x - tilt * len
            val y2 = y1 + len
            val a = (baseAlpha * d.alpha * if (heavier) 1.1f else 1f).coerceIn(0f, 1f)
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0f),
                        Color.White.copy(alpha = a),
                        Color.White.copy(alpha = a * 0.85f),
                        Color.White.copy(alpha = 0f),
                    ),
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                ),
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = if (heavier) 2.4f else 2.1f,
                cap = StrokeCap.Round,
            )
        }
    }
}

/** Mostly calm; brief gusts a few times per minute (phase ≈ seconds / 3.6). */
private fun snowGustStrength(phase: Float): Float {
    val cycle = 9.5f
    val local = ((phase % cycle) + cycle) % cycle
    fun pulse(start: Float, rise: Float, hold: Float, fall: Float, peak: Float): Float {
        val endRise = start + rise
        val endHold = endRise + hold
        val endFall = endHold + fall
        return when {
            local < start || local >= endFall -> 0f
            local < endRise -> ((local - start) / rise) * peak
            local < endHold -> peak
            else -> peak * (1f - (local - endHold) / fall).coerceAtLeast(0f)
        }
    }
    // Soft puff mid-cycle, stronger blow later — then long quiet fall.
    return maxOf(
        pulse(start = 2.5f, rise = 0.22f, hold = 0.28f, fall = 0.35f, peak = 0.4f),
        pulse(start = 6.9f, rise = 0.35f, hold = 0.75f, fall = 0.55f, peak = 1f),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSnow(
    w: Float,
    h: Float,
    phase: Float,
    flakes: List<SnowFlake>,
    isDark: Boolean,
) {
    clipRect {
        val gust = snowGustStrength(phase)

        for (f in flakes) {
            // Independent wrap per flake — no global restart hitch.
            val fall = ((f.y + phase * f.speed) % 1.25f + 1.25f) % 1.25f - 0.12f
            // Gentle flutter while falling; stronger sway only in a gust
            val flutter =
                sin(phase * 6.28f * f.sway + f.x * 10f) *
                    (0.018f + f.depth * 0.02f + gust * (0.04f + f.depth * 0.05f))
            // Temporary sideways push during gust (eases back when calm — no constant wind crawl)
            val gustPush = gust * (0.1f + f.depth * 0.18f)
            val xNorm =
                ((f.x + flutter + gustPush) % 1.2f + 1.2f) % 1.2f - 0.1f
            val x = xNorm * w
            val y = fall * h
            val r = (1.4f + f.size * 2.6f) * if (isDark) 1f else 1.15f
            val a = (f.alpha * if (isDark) 0.85f else 0.95f).coerceIn(0f, 1f)

            // Soft fade near recycle edges so wraps don't pop
            val edgeFade = when {
                fall < 0.02f -> ((fall + 0.12f) / 0.14f).coerceIn(0f, 1f)
                fall > 1.05f -> ((1.13f - fall) / 0.08f).coerceIn(0f, 1f)
                else -> 1f
            }
            val alpha = a * edgeFade
            if (alpha < 0.02f) continue

            // Blow streaks only when the wind actually kicks up
            if (gust > 0.35f && f.depth > 0.45f) {
                val streak = r * (1.6f + f.depth * 2.4f) * gust
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0f),
                            Color.White.copy(alpha = alpha * 0.5f * gust),
                            Color.White.copy(alpha = 0f),
                        ),
                        start = Offset(x - streak * 3.2f, y - streak * 0.25f),
                        end = Offset(x + streak * 0.1f, y + streak * 0.15f),
                    ),
                    start = Offset(x - streak * 3.2f, y - streak * 0.25f),
                    end = Offset(x + streak * 0.1f, y + streak * 0.15f),
                    strokeWidth = r * 0.75f,
                    cap = StrokeCap.Round,
                )
            }

            // Soft cool rim so flakes separate from light sky
            if (!isDark) {
                drawCircle(
                    color = Color(0xFF6A8498).copy(alpha = alpha * 0.35f),
                    radius = r * 1.35f,
                    center = Offset(x, y),
                )
            }
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = r,
                center = Offset(x, y),
            )
            // Tiny bright core on near flakes
            if (f.depth > 0.55f) {
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = r * 0.4f,
                    center = Offset(x - r * 0.15f, y - r * 0.15f),
                )
            }
        }
    }
}

/**
 * Real-looking strike: dark sky → white sheet → branched bolt (glow + core) →
 * brief blackout → return flash. Keeps type readable between strikes.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLightning(
    w: Float,
    h: Float,
    phase: Float,
    bolts: List<LightningBolt>,
    isDark: Boolean,
) {
    // Envelope: primary blast, fade, return flicker (common double-strike feel).
    val intensity = when {
        phase in 0.62f..0.645f -> ((phase - 0.62f) / 0.025f).coerceIn(0f, 1f) // rise
        phase in 0.645f..0.68f -> 1f - ((phase - 0.645f) / 0.035f) * 0.55f
        phase in 0.68f..0.71f -> 0.18f // afterglow
        phase in 0.74f..0.755f -> 0.85f // return strike
        phase in 0.755f..0.78f -> 0.35f * (1f - (phase - 0.755f) / 0.025f)
        else -> 0f
    }
    if (intensity <= 0.01f) return

    val boltIndex = if (phase < 0.72f) 0 else 1.coerceAtMost(bolts.lastIndex)
    val bolt = bolts[boltIndex]
    val sheetA = intensity * if (isDark) 0.42f else 0.48f
    val boltA = intensity * if (isDark) 0.95f else 0.9f

    // Sky sheet — cold blue-white
    drawRect(Color(0xFFDCE8FF).copy(alpha = sheetA * 0.55f))
    softOrb(
        cx = bolt.main.first().x * w,
        cy = h * 0.05f,
        radius = h * 1.8f,
        color = Color(0xFFEAF2FF),
        alpha = sheetA * 0.7f,
    )

    fun pathFrom(points: List<Offset>): Path = Path().apply {
        if (points.isEmpty()) return@apply
        moveTo(points.first().x * w, points.first().y * h)
        for (i in 1 until points.size) {
            lineTo(points[i].x * w, points[i].y * h)
        }
    }

    val mainPath = pathFrom(bolt.main)
    // Outer plasma glow
    drawPath(
        path = mainPath,
        color = Color(0xFF9EC8FF).copy(alpha = boltA * 0.35f),
        style = Stroke(width = h * 0.085f, cap = StrokeCap.Round),
    )
    drawPath(
        path = mainPath,
        color = Color(0xFFC8E0FF).copy(alpha = boltA * 0.55f),
        style = Stroke(width = h * 0.04f, cap = StrokeCap.Round),
    )
    // Hot white core
    drawPath(
        path = mainPath,
        color = Color.White.copy(alpha = boltA),
        style = Stroke(width = h * 0.012f, cap = StrokeCap.Round),
    )

    for (branch in bolt.branches) {
        val branchPath = pathFrom(branch)
        drawPath(
            path = branchPath,
            color = Color(0xFFB8D4FF).copy(alpha = boltA * 0.45f),
            style = Stroke(width = h * 0.028f, cap = StrokeCap.Round),
        )
        drawPath(
            path = branchPath,
            color = Color.White.copy(alpha = boltA * 0.75f),
            style = Stroke(width = h * 0.008f, cap = StrokeCap.Round),
        )
    }

    // Ground bloom where the bolt ends
    val tip = bolt.main.last()
    softOrb(
        cx = tip.x * w,
        cy = tip.y * h,
        radius = h * 0.55f,
        color = Color(0xFFE8F2FF),
        alpha = boltA * 0.4f,
    )
}

/** Fractal-ish bolt in normalized coords (0–1). */
private fun buildLightningBolt(
    seed: Int,
    startX: Float,
    endX: Float,
): LightningBolt {
    val rnd = Random(seed)
    val main = jaggedChannel(
        rnd = rnd,
        x0 = startX,
        y0 = -0.02f,
        x1 = endX,
        y1 = 0.92f,
        steps = 7,
        jag = 0.085f,
    )
    val branches = buildList {
        // Mid and lower forks peel off the main channel
        val forkAts = listOf(2, 4)
        for (at in forkAts) {
            if (at >= main.lastIndex) continue
            val origin = main[at]
            val dir = if (rnd.nextBoolean()) 1f else -1f
            add(
                jaggedChannel(
                    rnd = rnd,
                    x0 = origin.x,
                    y0 = origin.y,
                    x1 = (origin.x + dir * (0.12f + rnd.nextFloat() * 0.14f)).coerceIn(0.05f, 0.95f),
                    y1 = (origin.y + 0.18f + rnd.nextFloat() * 0.22f).coerceAtMost(1f),
                    steps = 4,
                    jag = 0.06f,
                ),
            )
        }
    }
    return LightningBolt(main = main, branches = branches)
}

private fun jaggedChannel(
    rnd: Random,
    x0: Float,
    y0: Float,
    x1: Float,
    y1: Float,
    steps: Int,
    jag: Float,
): List<Offset> {
    val points = ArrayList<Offset>(steps + 1)
    points.add(Offset(x0, y0))
    for (i in 1 until steps) {
        val t = i / steps.toFloat()
        val nx = x0 + (x1 - x0) * t + (rnd.nextFloat() - 0.5f) * 2f * jag
        val ny = y0 + (y1 - y0) * t
        points.add(Offset(nx.coerceIn(0.02f, 0.98f), ny))
    }
    points.add(Offset(x1, y1))
    return points
}

private data class LightningBolt(
    val main: List<Offset>,
    val branches: List<List<Offset>>,
)

private fun androidx.compose.ui.graphics.drawscope.DrawScope.softOrb(
    cx: Float,
    cy: Float,
    radius: Float,
    color: Color,
    alpha: Float,
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)),
            center = Offset(cx, cy),
            radius = radius,
        ),
        radius = radius,
        center = Offset(cx, cy),
    )
}

private data class RainDrop(
    val x: Float,
    val y: Float,
    val len: Float,
    val speed: Float,
    val alpha: Float,
)

private data class SnowFlake(
    val x: Float,
    val y: Float,
    val size: Float,
    val speed: Float,
    val sway: Float,
    val alpha: Float,
    val depth: Float,
)
