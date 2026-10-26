package com.tripbudget.app.location

import android.content.Context
import android.content.IntentSender
import android.location.LocationManager
import android.os.Build
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority

fun isDeviceLocationEnabled(context: Context): Boolean {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        manager.isLocationEnabled
    } else {
        runCatching { manager.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false) ||
            runCatching { manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)
    }
}

/** Shows the system "turn on location" dialog when the phone can resolve it. */
fun promptEnableDeviceLocation(
    context: Context,
    onResolution: (IntentSender) -> Unit,
    onUnavailable: () -> Unit,
) {
    val request = LocationSettingsRequest.Builder()
        .addLocationRequest(
            LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10_000L).build(),
        )
        .setAlwaysShow(true)
        .build()
    LocationServices.getSettingsClient(context)
        .checkLocationSettings(request)
        .addOnFailureListener { error ->
            val resolvable = error as? ResolvableApiException
            if (resolvable != null) {
                onResolution(resolvable.resolution.intentSender)
            } else {
                onUnavailable()
            }
        }
}
