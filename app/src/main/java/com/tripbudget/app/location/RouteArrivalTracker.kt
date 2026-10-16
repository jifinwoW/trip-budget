package com.tripbudget.app.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.tripbudget.app.data.TripRepository
import com.tripbudget.app.domain.RouteStopStatus
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/** Live distance from the phone to the next unreached stop. */
data class NextStopProximity(
    val tripId: Long,
    val stopTitle: String,
    val distanceMeters: Double,
)

/**
 * While any trip has [TripEntity.routeTrackingActive], listen for GPS and
 * auto-mark the next stop as reached when within [ARRIVAL_RADIUS_METERS].
 */
class RouteArrivalTracker(
    context: Context,
    private val repository: TripRepository,
) {
    private val appContext = context.applicationContext
    private val fused = LocationServices.getFusedLocationProviderClient(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observeJob: Job? = null
    private var updatesActive = false

    private val _proximity = MutableStateFlow<NextStopProximity?>(null)
    val proximity: StateFlow<NextStopProximity?> = _proximity.asStateFlow()

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            scope.launch(Dispatchers.IO) {
                processLocation(location)
            }
        }
    }

    fun start() {
        if (observeJob != null) return
        observeJob = scope.launch {
            repository.observeTripsWithRouteTracking().collectLatest { trips ->
                if (trips.isEmpty()) {
                    _proximity.value = null
                    stopLocationUpdates()
                } else if (hasLocationPermission()) {
                    startLocationUpdates()
                    checkNow()
                } else {
                    _proximity.value = null
                    stopLocationUpdates()
                }
            }
        }
    }

    /** Call after the user grants location permission while tracking may already be active. */
    fun onPermissionGranted() {
        scope.launch {
            val active = withContext(Dispatchers.IO) {
                repository.getTripsWithRouteTracking().isNotEmpty()
            }
            if (active && hasLocationPermission()) {
                startLocationUpdates()
                checkNow()
            }
        }
    }

    /**
     * Call when the user taps Start trip (permission already granted) so we don't
     * wait for the next periodic GPS tick.
     */
    fun nudgeCheck() {
        if (!hasLocationPermission()) return
        scope.launch {
            val active = withContext(Dispatchers.IO) {
                repository.getTripsWithRouteTracking().isNotEmpty()
            }
            if (!active) return@launch
            startLocationUpdates()
            checkNow()
        }
    }

    /**
     * Re-pin the next stop for [tripId] to the phone’s current GPS.
     * Use when search put the pin on a town center far from where you actually are.
     */
    fun pinNextStopToHere(tripId: Long, onResult: (Boolean) -> Unit = {}) {
        if (!hasLocationPermission()) {
            onResult(false)
            return
        }
        scope.launch {
            val location = fetchCurrentLocation()
            if (location == null) {
                onResult(false)
                return@launch
            }
            val ok = withContext(Dispatchers.IO) {
                val stops = repository.getRouteStopsForTrip(tripId).sortedBy { it.sortOrder }
                val target = stops.firstOrNull {
                    it.status == RouteStopStatus.CURRENT || it.status == RouteStopStatus.UPCOMING
                } ?: return@withContext false
                repository.updateRouteStopLocation(target.id, location.latitude, location.longitude)
                true
            }
            if (ok) {
                processLocation(location)
                startLocationUpdates()
            }
            onResult(ok)
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentLocation(): Location? {
        if (!hasLocationPermission()) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val cts = CancellationTokenSource()
                fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token).await()
            }.getOrNull()
                ?: runCatching { fused.lastLocation.await() }.getOrNull()
        }
    }

    fun stop() {
        observeJob?.cancel()
        observeJob = null
        _proximity.value = null
        stopLocationUpdates()
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (updatesActive || !hasLocationPermission()) return
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L)
            .setMinUpdateIntervalMillis(2_000L)
            .setMinUpdateDistanceMeters(0f)
            .setWaitForAccurateLocation(false)
            .build()
        runCatching {
            fused.requestLocationUpdates(request, callback, Looper.getMainLooper())
            updatesActive = true
            Log.i(TAG, "Route location updates started")
        }.onFailure { err ->
            Log.w(TAG, "Could not start location updates", err)
        }
    }

    private fun stopLocationUpdates() {
        if (!updatesActive) return
        fused.removeLocationUpdates(callback)
        updatesActive = false
        Log.i(TAG, "Route location updates stopped")
    }

    private fun checkNow() {
        scope.launch {
            val location = fetchCurrentLocation()
            if (location != null) {
                Log.i(
                    TAG,
                    "Immediate fix ${location.latitude},${location.longitude} ±${location.accuracy}m",
                )
                processLocation(location)
            } else {
                Log.w(TAG, "No immediate location available yet")
            }
        }
    }

    private suspend fun processLocation(location: Location) {
        val trips = repository.getTripsWithRouteTracking()
        if (trips.isEmpty()) {
            _proximity.value = null
            return
        }
        var published: NextStopProximity? = null
        for (trip in trips) {
            val stops = repository.getRouteStopsForTrip(trip.id)
                .sortedBy { it.sortOrder }
            val target = stops.firstOrNull {
                it.status == RouteStopStatus.CURRENT || it.status == RouteStopStatus.UPCOMING
            } ?: continue
            val distance = haversineMeters(
                location.latitude,
                location.longitude,
                target.lat,
                target.lng,
            )
            if (published == null) {
                published = NextStopProximity(
                    tripId = trip.id,
                    stopTitle = target.title,
                    distanceMeters = distance,
                )
            }
            Log.i(
                TAG,
                "Next stop “${target.title}” is ${distance.toInt()} m away " +
                    "(need ≤${ARRIVAL_RADIUS_METERS.toInt()} m) " +
                    "stop@${target.lat},${target.lng}",
            )
            if (distance <= ARRIVAL_RADIUS_METERS) {
                Log.i(TAG, "Auto-reached ${target.title} (${distance.toInt()} m)")
                repository.markRouteStopReached(target.id)
            }
        }
        _proximity.value = published
    }

    companion object {
        private const val TAG = "RouteArrivalTracker"
        const val ARRIVAL_RADIUS_METERS = 5_000.0

        fun formatDistance(meters: Double): String {
            return when {
                meters < 1000 -> "${meters.toInt()} m"
                else -> {
                    val km = meters / 1000.0
                    if (km < 10) String.format("%.1f km", km) else "${km.toInt()} km"
                }
            }
        }

        fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val r = 6_371_000.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return r * c
        }
    }
}
