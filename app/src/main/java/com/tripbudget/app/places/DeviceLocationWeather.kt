package com.tripbudget.app.places

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Current-location weather for ambient UI (header atmosphere).
 * Uses last/current GPS when permission is already granted — never prompts.
 */
object DeviceLocationWeather {
    @SuppressLint("MissingPermission")
    suspend fun fetch(context: Context): PlaceWeather? = withContext(Dispatchers.IO) {
        if (!hasLocationPermission(context)) return@withContext null
        val fused = LocationServices.getFusedLocationProviderClient(context.applicationContext)
        val location = runCatching {
            val cts = CancellationTokenSource()
            fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token).await()
        }.getOrNull()
            ?: runCatching { fused.lastLocation.await() }.getOrNull()
            ?: return@withContext null
        OpenMeteoWeather.fetchCurrent(
            listOf(LatLng(lat = location.latitude, lng = location.longitude)),
        ).firstOrNull()
    }

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }
}
