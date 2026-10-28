package com.tripbudget.app.notify

import android.content.Context
import com.tripbudget.app.TripBudgetApp
import com.tripbudget.app.places.WeatherKind

/** Notifies when the trip place's live weather kind changes. The first reading is stored quietly. */
object TripWeatherAlerts {
    private const val PREFS = "weather_alerts"

    fun onFetched(
        context: Context,
        tripId: Long,
        remoteTripId: String?,
        tripName: String,
        kind: WeatherKind,
        label: String,
    ) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val kindKey = "kind_$tripId"
        val labelKey = "label_$tripId"
        val previous = prefs.getString(kindKey, null)
        val previousLabel = prefs.getString(labelKey, null)
        prefs.edit().putString(kindKey, kind.name).putString(labelKey, label).apply()
        if (previous == null || previous == kind.name) return
        val app = context.applicationContext as TripBudgetApp
        if (app.isInForeground) return
        val from = previousLabel ?: previous.lowercase().replaceFirstChar { it.uppercase() }
        TripNotifications.show(
            app,
            "Weather changed",
            "$tripName: $from to $label",
            tag = "weather:${remoteTripId ?: tripId}",
        )
    }
}
