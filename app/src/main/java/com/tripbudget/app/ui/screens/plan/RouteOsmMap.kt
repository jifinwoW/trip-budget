package com.tripbudget.app.ui.screens.plan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tripbudget.app.R
import com.tripbudget.app.data.entity.RouteStopEntity
import com.tripbudget.app.domain.RouteStopStatus
import com.tripbudget.app.places.LatLng
import com.tripbudget.app.places.OpenMeteoWeather
import com.tripbudget.app.places.OsrmRoadRoute
import com.tripbudget.app.places.PlaceWeather
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/** Soft route — remaining path + brighter completed progress. */
private val RouteRemainingSoft = AndroidColor.parseColor("#4490A090")
private val RouteRemainingCore = AndroidColor.parseColor("#B8C8C0")
private val RouteDoneSoft = AndroidColor.parseColor("#66C8F5B8")
private val RouteDoneCore = AndroidColor.parseColor("#E8FFF0")

/** Free dark satellite-style basemap (Esri World Imagery) — no API key. */
private val SatelliteTiles: OnlineTileSourceBase = object : XYTileSource(
    "EsriWorldImagery",
    1,
    19,
    256,
    ".jpg",
    arrayOf(
        "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/",
    ),
    "© Esri",
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        return baseUrl +
            MapTileIndex.getZoom(pMapTileIndex) + "/" +
            MapTileIndex.getY(pMapTileIndex) + "/" +
            MapTileIndex.getX(pMapTileIndex) + mImageFilenameEnding
    }
}

/**
 * Satellite map + road path (OSRM) + soft pulse marker.
 * Pinch zoom only — no built-in +/- chrome.
 */
@Composable
fun RouteOsmMap(
    stops: List<RouteStopEntity>,
    showUserLocation: Boolean,
    fullBleed: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val ordered = remember(stops) { stops.sortedBy { it.sortOrder } }
    val stopKey = remember(ordered) {
        ordered.joinToString("|") { "${it.id}:${it.lat},${it.lng}" }
    }

    // Empty until OSRM returns — never draw straight stop-to-stop as a stand-in.
    var pathPoints by remember(stopKey) { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var loadingRoute by remember(stopKey) { mutableStateOf(ordered.size >= 2) }
    var weatherByStopId by remember { mutableStateOf<Map<Long, PlaceWeather>>(emptyMap()) }

    LaunchedEffect(stopKey) {
        if (ordered.size < 2) {
            pathPoints = ordered.map { GeoPoint(it.lat, it.lng) }
            loadingRoute = false
        } else {
            loadingRoute = true
            pathPoints = emptyList()
            val waypoints = ordered.map { LatLng(it.lat, it.lng) }
            val road = OsrmRoadRoute.fetchDrivingPath(waypoints)
            pathPoints = road.map { GeoPoint(it.lat, it.lng) }
            loadingRoute = false
        }
        val points = ordered.map { LatLng(it.lat, it.lng) }
        val weather = OpenMeteoWeather.fetchCurrent(points)
        weatherByStopId = ordered.mapIndexedNotNull { index, stop ->
            weather.getOrNull(index)?.let { stop.id to it }
        }.toMap()
    }

    val carIcon = remember(context) { loadDrawableBitmap(context, R.drawable.ic_map_car, 52) }
    val pinIcon = remember(context) {
        loadDrawableBitmap(context, R.drawable.ic_map_pin_red, widthDp = 30, heightDp = 40)
    }

    val mapView = remember {
        val app = context.applicationContext
        Configuration.getInstance().apply {
            load(app, app.getSharedPreferences("osmdroid", 0))
            userAgentValue = "ChilavuTripBudget/1.0"
        }
        MapView(context).apply {
            setTileSource(SatelliteTiles)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(10.0)
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false
            setBackgroundColor(AndroidColor.parseColor("#0B1412"))
            overlays.add(WeatherPinLabelsOverlay())
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        mapView.onResume()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    val shape = if (fullBleed) {
        RoundedCornerShape(0.dp)
    } else {
        RoundedCornerShape(22.dp)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (fullBleed) Modifier else Modifier.height(280.dp))
            .clip(shape)
            .background(Color(0xFF0B1412)),
    ) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { map ->
                val weatherLabels = map.overlays.filterIsInstance<WeatherPinLabelsOverlay>().firstOrNull()
                map.overlays.removeAll {
                    it is Marker || it is Polyline || it is MyLocationNewOverlay
                }
                if (ordered.isEmpty()) {
                    weatherLabels?.setLabels(emptyList())
                    map.invalidate()
                    return@AndroidView
                }

                // Only draw after road geometry is ready (avoids straight-line flash).
                val linePoints = pathPoints
                if (linePoints.size >= 2) {
                    val lastReachedIdx = ordered.indexOfLast { it.status == RouteStopStatus.REACHED }
                    val (donePoints, restPoints) = splitRouteProgress(linePoints, ordered, lastReachedIdx)

                    if (restPoints.size >= 2) {
                        map.overlays.add(glowingPolyline(restPoints, RouteRemainingSoft, strokeWidth = 7f))
                        map.overlays.add(glowingPolyline(restPoints, RouteRemainingCore, strokeWidth = 2.8f))
                    }
                    if (donePoints.size >= 2) {
                        map.overlays.add(glowingPolyline(donePoints, RouteDoneSoft, strokeWidth = 8f))
                        map.overlays.add(glowingPolyline(donePoints, RouteDoneCore, strokeWidth = 3.2f))
                    } else if (donePoints.isEmpty() && restPoints.size < 2) {
                        map.overlays.add(glowingPolyline(linePoints, RouteRemainingSoft, strokeWidth = 7f))
                        map.overlays.add(glowingPolyline(linePoints, RouteRemainingCore, strokeWidth = 2.8f))
                    }
                }

                ordered.forEachIndexed { index, stop ->
                    val weather = weatherByStopId[stop.id]
                    val statusLabel = when (stop.status) {
                        RouteStopStatus.REACHED -> "Reached"
                        RouteStopStatus.CURRENT -> "Next stop"
                        RouteStopStatus.UPCOMING -> "Upcoming · #${index + 1}"
                    }
                    map.overlays.add(
                        Marker(map).apply {
                            position = GeoPoint(stop.lat, stop.lng)
                            title = stop.title
                            snippet = listOfNotNull(statusLabel, weather?.pinLabel()).joinToString(" · ")
                            if (pinIcon != null) {
                                icon = BitmapDrawable(map.context.resources, pinIcon)
                            }
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        },
                    )
                }

                weatherLabels?.setLabels(
                    ordered.mapNotNull { stop ->
                        val weather = weatherByStopId[stop.id] ?: return@mapNotNull null
                        WeatherPinLabel(
                            point = GeoPoint(stop.lat, stop.lng),
                            text = weather.pinLabel(),
                        )
                    },
                )

                if (showUserLocation) {
                    map.overlays.add(
                        MyLocationNewOverlay(GpsMyLocationProvider(map.context), map).apply {
                            if (carIcon != null) {
                                setPersonIcon(carIcon)
                                setDirectionIcon(carIcon)
                                setPersonAnchor(0.5f, 0.5f)
                                setDirectionAnchor(0.5f, 0.5f)
                            }
                            enableMyLocation()
                        },
                    )
                }

                // Keep weather chips above pins / location
                weatherLabels?.let { overlay ->
                    map.overlays.remove(overlay)
                    map.overlays.add(overlay)
                }

                map.post {
                    // Fit to road path when ready; otherwise pins so the map isn't empty.
                    val fit = if (linePoints.size >= 2) {
                        linePoints
                    } else {
                        ordered.map { GeoPoint(it.lat, it.lng) }
                    }
                    if (fit.size == 1) {
                        map.controller.setZoom(14.0)
                        map.controller.setCenter(fit.first())
                    } else if (fit.isNotEmpty()) {
                        map.zoomToBoundingBox(
                            BoundingBox.fromGeoPoints(fit).increaseByScale(1.2f),
                            true,
                            56,
                        )
                    }
                    map.invalidate()
                }
            },
        )

        if (loadingRoute && ordered.size >= 2) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(20.dp),
                strokeWidth = 2.dp,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
    }
}

/**
 * Split the road path into completed (through last reached stop) vs remaining.
 * Progress climbs with each reached stop (e.g. 3 of 4 → ~75% of the line).
 */
private fun splitRouteProgress(
    path: List<GeoPoint>,
    stops: List<RouteStopEntity>,
    lastReachedIdx: Int,
): Pair<List<GeoPoint>, List<GeoPoint>> {
    if (path.size < 2 || stops.isEmpty() || lastReachedIdx < 0) {
        return emptyList<GeoPoint>() to path
    }
    if (lastReachedIdx >= stops.lastIndex) {
        return path to emptyList()
    }
    val reachedStop = stops[lastReachedIdx]
    val target = GeoPoint(reachedStop.lat, reachedStop.lng)
    val cut = path.indices.minByOrNull { i ->
        val p = path[i]
        val dLat = p.latitude - target.latitude
        val dLon = p.longitude - target.longitude
        dLat * dLat + dLon * dLon
    } ?: 0
    val end = cut.coerceIn(0, path.lastIndex)
    // Include the cut point in both so the join is continuous
    val done = path.subList(0, end + 1)
    val rest = path.subList(end, path.size)
    return done to rest
}

private fun glowingPolyline(
    points: List<GeoPoint>,
    color: Int,
    strokeWidth: Float,
): Polyline {
    return Polyline().apply {
        setPoints(points)
        outlinePaint.color = color
        outlinePaint.strokeWidth = strokeWidth
        outlinePaint.strokeCap = Paint.Cap.ROUND
        outlinePaint.strokeJoin = Paint.Join.ROUND
        outlinePaint.isAntiAlias = true
        outlinePaint.style = Paint.Style.STROKE
    }
}

private fun loadDrawableBitmap(
    context: Context,
    resId: Int,
    widthDp: Int,
    heightDp: Int = widthDp,
): Bitmap? {
    val drawable = ContextCompat.getDrawable(context, resId) ?: return null
    val density = context.resources.displayMetrics.density
    val widthPx = (widthDp * density).toInt().coerceAtLeast(widthDp)
    val heightPx = (heightDp * density).toInt().coerceAtLeast(heightDp)
    return runCatching {
        drawable.toBitmap(width = widthPx, height = heightPx)
    }.getOrElse {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bitmap
    }
}
