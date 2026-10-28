package com.tripbudget.app.location

import android.content.Context
import com.tripbudget.app.data.TripRepository
import com.tripbudget.app.data.entity.TripEntity
import java.time.LocalDate

/**
 * Turns route tracking on for the owner's trip once the start date arrives,
 * and off again after the end date. Ending the trip early stays off.
 */
object RouteTripSchedule {
    private const val PREFS = "route_trip_schedule"

    suspend fun refresh(context: Context, repository: TripRepository, userId: String?) {
        val today = LocalDate.now()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        var changed = false
        for (trip in repository.getAllTrips()) {
            if (!isOwner(trip, userId)) continue
            val start = trip.startDateEpochDay?.let { LocalDate.ofEpochDay(it) } ?: continue
            val end = trip.endDateEpochDay?.let { LocalDate.ofEpochDay(it) } ?: start
            if (today.isAfter(end)) {
                if (trip.routeTrackingActive) {
                    repository.setRouteTrackingActive(trip.id, false)
                }
                continue
            }
            if (today.isBefore(start)) continue
            val key = "started:${trip.id}"
            if (trip.routeTrackingActive) {
                if (!prefs.getBoolean(key, false)) {
                    editor.putBoolean(key, true)
                    changed = true
                }
                continue
            }
            if (!prefs.getBoolean(key, false)) {
                repository.setRouteTrackingActive(trip.id, true)
                editor.putBoolean(key, true)
                changed = true
            }
        }
        if (changed) editor.commit()
    }

    private fun isOwner(trip: TripEntity, userId: String?): Boolean {
        if (userId.isNullOrBlank()) return false
        val ownerId = trip.remoteOwnerId
        return when {
            ownerId != null -> ownerId == userId
            trip.remoteId == null -> true
            else -> false
        }
    }
}
