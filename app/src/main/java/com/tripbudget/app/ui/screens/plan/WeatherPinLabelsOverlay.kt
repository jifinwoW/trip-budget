package com.tripbudget.app.ui.screens.plan

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

data class WeatherPinLabel(
    val point: GeoPoint,
    val text: String,
)

/**
 * Compact weather chips above each stop pin — labels only, no particle effects.
 */
class WeatherPinLabelsOverlay(
    private var labels: List<WeatherPinLabel> = emptyList(),
) : Overlay() {
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xE6121820.toInt()
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x55FFFFFF
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    fun setLabels(next: List<WeatherPinLabel>) {
        labels = next
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow || labels.isEmpty()) return
        val projection = mapView.projection
        val density = mapView.resources.displayMetrics.density
        textPaint.textSize = 11f * density
        val padH = 8f * density
        val padV = 5f * density
        val radius = 10f * density
        val lift = 46f * density
        val px = android.graphics.Point()

        for (label in labels) {
            if (label.text.isBlank()) continue
            projection.toPixels(label.point, px)
            val tw = textPaint.measureText(label.text)
            val th = textPaint.fontMetrics.let { it.descent - it.ascent }
            val cx = px.x.toFloat()
            val cy = px.y.toFloat() - lift
            val rect = RectF(
                cx - tw / 2f - padH,
                cy - th / 2f - padV,
                cx + tw / 2f + padH,
                cy + th / 2f + padV,
            )
            canvas.drawRoundRect(rect, radius, radius, bgPaint)
            canvas.drawRoundRect(rect, radius, radius, strokePaint)
            val textY = cy - (textPaint.fontMetrics.ascent + textPaint.fontMetrics.descent) / 2f
            canvas.drawText(label.text, cx, textY, textPaint)
        }
    }
}
