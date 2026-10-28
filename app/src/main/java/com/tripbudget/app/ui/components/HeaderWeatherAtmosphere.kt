package com.tripbudget.app.ui.components

import android.os.Build.VERSION.SDK_INT
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import coil.request.repeatCount
import com.tripbudget.app.R
import com.tripbudget.app.places.RainIntensity
import com.tripbudget.app.places.WeatherKind
import kotlin.math.abs
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
    /** From Open-Meteo when live; light vs heavy rain change density/speed. */
    rainIntensity: RainIntensity = RainIntensity.MEDIUM,
    /**
     * Where rain hits (0–1 from top). Dashboard’s taller under-hero strip uses a
     * higher line (~0.72) so splashes sit above the hero corners; other pages ~0.88.
     */
    impactHorizonRatio: Float = 0.88f,
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
    var rainTimeSec by remember { mutableFloatStateOf(0f) }
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
    LaunchedEffect(kind) {
        if (kind != WeatherKind.RAIN && kind != WeatherKind.STORM) return@LaunchedEffect
        var lastNanos = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastNanos != 0L) {
                    rainTimeSec += (now - lastNanos) / 1_000_000_000f
                }
                lastNanos = now
            }
        }
    }

    // Pluvia-style layered rain: far / mid / near parallax (dense enough to read in a short header).
    val rainDrops = remember {
        List(180) { i ->
            val rnd = Random(40 + i)
            val layerRoll = rnd.nextFloat()
            val layer = when {
                layerRoll < 0.30f -> 0
                layerRoll < 0.70f -> 1
                else -> 2
            }
            val z = when (layer) {
                0 -> 0.40f
                1 -> 0.72f
                else -> 1f
            }
            RainDrop(
                x = rnd.nextFloat(),
                y = rnd.nextFloat(),
                len = (0.65f + rnd.nextFloat() * 0.75f) * z,
                speed = (1.0f + rnd.nextFloat() * 0.65f) * z,
                alpha = (0.28f + rnd.nextFloat() * 0.48f) * z,
                thickness = (0.85f + rnd.nextFloat() * 1.25f) * z,
                layer = layer,
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
        // Header scenes are GIFs from res/raw.
        val animatedSceneRes = when (kind) {
            WeatherKind.SUNNY -> R.raw.header_weather_sunny
            WeatherKind.CLOUDY -> R.raw.header_weather_cloudy
            WeatherKind.OVERCAST -> R.raw.header_weather_overcast
            WeatherKind.RAIN -> R.raw.header_weather_rain
            WeatherKind.STORM -> R.raw.storm
            WeatherKind.FOG -> R.raw.header_weather_fog
            WeatherKind.SNOW -> R.raw.header_weather_snow
            else -> null
        }
        val sceneRes = when (kind) {
            WeatherKind.SUNNY -> null
            WeatherKind.CLOUDY -> null
            WeatherKind.OVERCAST -> null
            WeatherKind.FOG -> null
            WeatherKind.RAIN -> null
            WeatherKind.STORM -> null
            WeatherKind.SNOW -> null
            else -> null
        }
        if (animatedSceneRes != null || sceneRes != null) {
            // Wash fills clear top; scene art sits across the header.
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(brush = Brush.verticalGradient(headerWash(kind, isDark)))
            }
            val sceneAlignment = Alignment.BottomStart
            if (animatedSceneRes != null) {
                HeaderWeatherAnimatedScene(
                    rawRes = animatedSceneRes,
                    alignment = sceneAlignment,
                    modifier = Modifier.fillMaxSize(),
                )
            } else if (sceneRes != null) {
                Image(
                    painter = painterResource(sceneRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    // Left-weighted cottage scenes — keep Start when cropping.
                    alignment = sceneAlignment,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDark) 0.06f else 0.14f),
                            Color.Transparent,
                            Color.Transparent,
                        ),
                    ),
                )
                // GIFs already include rain, lightning, and snow.
                when (kind) {
                    WeatherKind.SUNNY, WeatherKind.CLOUDY, WeatherKind.OVERCAST, WeatherKind.FOG,
                    WeatherKind.RAIN, WeatherKind.STORM, WeatherKind.SNOW,
                    WeatherKind.UNKNOWN, null -> Unit
                }
            }
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val t = drift.value

                // Base sky wash — weather-keyed, soft enough for brand type on top
                val wash = headerWash(kind, isDark)
                drawRect(brush = Brush.verticalGradient(wash))

                when (kind) {
                    WeatherKind.SUNNY, WeatherKind.CLOUDY, WeatherKind.OVERCAST, WeatherKind.FOG,
                    WeatherKind.RAIN, WeatherKind.STORM, WeatherKind.SNOW -> Unit
                    WeatherKind.UNKNOWN, null -> {
                        // Soft default coastal mist only
                        drawDefaultMist(w, h, t, isDark)
                    }
                }
            }
        }
    }
}

/** GIF/WebP/JPEG scene from res/raw — Coil so cloudy (and later others) can animate. */
@Composable
private fun HeaderWeatherAnimatedScene(
    rawRes: Int,
    alignment: Alignment,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val gifLoader = remember {
        ImageLoader.Builder(context)
            .components {
                // Prefer GifDecoder — ImageDecoder + hardware bitmaps often show a still frame.
                add(GifDecoder.Factory())
                if (SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                }
            }
            .build()
    }
    val request = remember(rawRes) {
        ImageRequest.Builder(context)
            .data(rawRes)
            .allowHardware(false)
            .repeatCount(-1) // loop forever
            .crossfade(false)
            .build()
    }
    AsyncImage(
        model = request,
        imageLoader = gifLoader,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alignment = alignment,
        modifier = modifier,
    )
}

/**
 * Brand / header title color that stays readable on the weather wash.
 * Uses relative luminance of the top sky stop (and rain art left-side tones).
 */
fun headerBrandInk(kind: WeatherKind?, isDark: Boolean): Color {
    val top = headerWashColors(kind, isDark).first()
    return if (relativeLuminance(top) < 0.48f) {
        Color(0xFFF5FBFA)
    } else {
        Color(0xFF12201E)
    }
}

private fun relativeLuminance(color: Color): Float {
    fun channel(c: Float): Float {
        val v = c.coerceIn(0f, 1f)
        return if (v <= 0.03928f) v / 12.92f else ((v + 0.055f) / 1.055f)
            .toDouble()
            .let { Math.pow(it, 2.4) }
            .toFloat()
    }
    val r = channel(color.red)
    val g = channel(color.green)
    val b = channel(color.blue)
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}

/** Gradient stops for the header background by weather. */
fun headerWashColors(kind: WeatherKind?, isDark: Boolean): List<Color> {
    if (isDark) {
        return when (kind) {
            WeatherKind.SUNNY -> listOf(Color(0xFF1A2820), Color(0xFF12201E), Color(0xFF0E1A18))
            WeatherKind.CLOUDY, WeatherKind.OVERCAST -> listOf(Color(0xFF1A2228), Color(0xFF141C22), Color(0xFF0E161C))
            // Match frog-rain banner (moss / mist)
            WeatherKind.RAIN -> listOf(Color(0xFF1A2824), Color(0xFF14201E), Color(0xFF101818))
            WeatherKind.STORM -> listOf(Color(0xFF181428), Color(0xFF120E1E), Color(0xFF0C0A16))
            WeatherKind.SNOW -> listOf(Color(0xFF1A222A), Color(0xFF141C24), Color(0xFF101820))
            WeatherKind.FOG -> listOf(Color(0xFF1C2224), Color(0xFF161C1E), Color(0xFF121618))
            else -> listOf(Color(0xFF12201E), Color(0xFF0E1A18), Color(0xFF0C1816))
        }
    }
    return when (kind) {
        WeatherKind.SUNNY -> listOf(Color(0xFFBFE8F2), Color(0xFFE8F6FA), Color(0xFFF8FCFD))
        WeatherKind.CLOUDY, WeatherKind.OVERCAST -> listOf(Color(0xFFC5D4DE), Color(0xFFDCE6EC), Color(0xFFEEF3F6))
        // Soft grey-green to blend under the rain illustration
        WeatherKind.RAIN -> listOf(Color(0xFF9BB0A8), Color(0xFFB8C8C0), Color(0xFFD0DCD4))
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

/**
 * Atmospheric rain inspired by Pluvia-style simulation:
 * 3 depth layers, wind angle, soft gradient streaks, and an impact horizon
 * (wet ground band + splash/ripple) near the bottom of the header strip.
 * [timeSec] is unbounded seconds so the field never hitch-restarts.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRain(
    w: Float,
    h: Float,
    timeSec: Float,
    drops: List<RainDrop>,
    isDark: Boolean,
    intensity: RainIntensity = RainIntensity.MEDIUM,
    impactHorizonRatio: Float = 0.88f,
) {
    clipRect {
        // Density / speed / wind scale with light → heavy → storm.
        val windDeg = when (intensity) {
            RainIntensity.LIGHT -> 10f
            RainIntensity.MEDIUM -> 16f
            RainIntensity.HEAVY -> 24f
            RainIntensity.STORM -> 30f
        }
        val speedMul = when (intensity) {
            RainIntensity.LIGHT -> 0.95f
            RainIntensity.MEDIUM -> 1.45f
            RainIntensity.HEAVY -> 1.75f
            RainIntensity.STORM -> 1.95f
        }
        // Draw every Nth drop — light rain is sparse, storm uses the full set.
        val dropStride = when (intensity) {
            RainIntensity.LIGHT -> 3
            RainIntensity.MEDIUM -> 2
            RainIntensity.HEAVY -> 1
            RainIntensity.STORM -> 1
        }
        val lengthMul = when (intensity) {
            RainIntensity.LIGHT -> 0.85f
            RainIntensity.MEDIUM -> 1.12f
            RainIntensity.HEAVY -> 1.28f
            RainIntensity.STORM -> 1.35f
        }
        val strokeMul = when (intensity) {
            RainIntensity.LIGHT -> 0.95f
            RainIntensity.MEDIUM -> 1.25f
            RainIntensity.HEAVY -> 1.45f
            RainIntensity.STORM -> 1.55f
        }
        val alphaMul = when (intensity) {
            RainIntensity.LIGHT -> 0.88f
            RainIntensity.MEDIUM -> 1.08f
            RainIntensity.HEAVY -> 1.18f
            RainIntensity.STORM -> 1.25f
        }
        val rad = Math.toRadians(windDeg.toDouble()).toFloat()
        val sinW = sin(rad)
        val cosW = cos(rad)
        val tint = Color(0xFFD7EBFF) // cool rain tint (not pure white)
        val baseBoost = (if (isDark) 1.15f else 1.25f) * alphaMul
        // Fall units: ~1 full header height per second at speed 1.
        val fallRate = 1.35f * speedMul

        // Impact horizon — Pluvia-style ground line (higher on Dashboard’s tall strip).
        val impactRatio = impactHorizonRatio.coerceIn(0.55f, 0.95f)
        val impactY = h * impactRatio

        // Wet ground plane: mist rising into the horizon + thin reflection sheen.
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFF1E293B).copy(alpha = if (isDark) 0.14f else 0.10f),
                    Color(0xFF0F172A).copy(alpha = if (isDark) 0.22f else 0.16f),
                ),
                startY = impactY - h * 0.22f,
                endY = h,
            ),
        )
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    tint.copy(alpha = if (isDark) 0.22f else 0.28f),
                    tint.copy(alpha = if (isDark) 0.18f else 0.22f),
                    Color.Transparent,
                ),
            ),
            start = Offset(0f, impactY),
            end = Offset(w, impactY),
            strokeWidth = 1.2f,
            cap = StrokeCap.Round,
        )

        for ((index, d) in drops.withIndex()) {
            if (index % dropStride != 0) continue
            // Continuous wrap on Y; slight X drift from wind so columns don't stack.
            val fall = ((d.y + timeSec * d.speed * fallRate) % 1.35f + 1.35f) % 1.35f - 0.12f
            val windDrift = (timeSec * d.speed * fallRate * sinW * 0.22f + d.x * 3.1f) % 1.2f
            val nx = ((d.x + windDrift) % 1.2f + 1.2f) % 1.2f - 0.1f

            // Parallax surface: near drops hit a touch lower, far drops a touch higher.
            val layerImpactY = impactY + when (d.layer) {
                2 -> h * 0.03f
                1 -> 0f
                else -> -h * 0.025f
            }

            val rawHeadX = nx * w
            val rawHeadY = fall * h
            // Streaks stop at the horizon — they don't fall through the ground.
            val hit = rawHeadY >= layerImpactY
            val headY = rawHeadY.coerceAtMost(layerImpactY)
            val streakLen = h * (0.22f + d.len * 0.48f) * lengthMul
            val headX = rawHeadX
            val tailX = headX - sinW * streakLen
            val tailY = headY - cosW * streakLen
            val a = (d.alpha * baseBoost).coerceIn(0f, 0.95f)
            val stroke = (d.thickness * strokeMul).coerceIn(0.55f, 3.2f)

            // Only draw the falling streak while it's still above / just hitting the horizon.
            if (!hit || rawHeadY < layerImpactY + h * 0.05f) {
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            tint.copy(alpha = 0f),
                            tint.copy(alpha = a * 0.55f),
                            tint.copy(alpha = a),
                        ),
                        start = Offset(tailX, tailY),
                        end = Offset(headX, headY),
                    ),
                    start = Offset(tailX, tailY),
                    end = Offset(headX, headY),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }

            // Splash + ripple on the impact horizon (mid/near layers).
            if (d.layer >= 1 && hit) {
                val past = ((rawHeadY - layerImpactY) / (h * 0.12f)).coerceIn(0f, 1f)
                val life = (1f - past)
                if (life > 0.05f) {
                    val sx = headX
                    val sy = layerImpactY
                    val splashA = a * 0.9f * life
                    // Upward splash droplets (Pluvia-style bounce).
                    val lift = past * (6f + d.layer * 4f)
                    val spread = past * (5f + d.layer * 3f)
                    drawCircle(
                        color = tint.copy(alpha = splashA),
                        radius = 1.4f + d.layer * 0.7f,
                        center = Offset(sx - spread, sy - lift),
                    )
                    drawCircle(
                        color = tint.copy(alpha = splashA * 0.75f),
                        radius = 1.1f + d.layer * 0.5f,
                        center = Offset(sx + spread * 0.85f, sy - lift * 0.75f),
                    )
                    drawCircle(
                        color = tint.copy(alpha = splashA * 0.55f),
                        radius = 0.9f + d.layer * 0.35f,
                        center = Offset(sx + spread * 0.15f, sy - lift * 1.15f),
                    )
                    // Expanding squashed ripple on the wet plane.
                    val rippleR = 3f + past * (10f + d.layer * 6f)
                    drawOval(
                        color = tint.copy(alpha = splashA * 0.4f * life),
                        topLeft = Offset(sx - rippleR, sy - rippleR * 0.28f),
                        size = Size(rippleR * 2f, rippleR * 0.56f),
                        style = Stroke(width = 0.9f),
                    )
                }
            }
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
    val thickness: Float,
    /** 0 = far haze, 1 = mid, 2 = near / foreground. */
    val layer: Int,
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
