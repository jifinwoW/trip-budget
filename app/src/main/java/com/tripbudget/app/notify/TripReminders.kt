package com.tripbudget.app.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tripbudget.app.TripBudgetApp
import com.tripbudget.app.data.TripRepository
import com.tripbudget.app.location.RouteTripSchedule
import com.tripbudget.app.domain.RouteStopStatus
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 8:00 local reminder on the day a trip starts, and each later morning of the trip
 * while a stop is still ahead.
 */
object TripReminders {
    private const val PREFS = "trip_reminders"
    private const val REQUEST = 42
    private const val HOUR = 8

    suspend fun refresh(context: Context, repository: TripRepository) {
        deliverDue(context, repository)
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val whenMs = nextEight().toInstant().toEpochMilli()
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, pending(context))
    }

    suspend fun deliverDue(context: Context, repository: TripRepository) {
        val now = ZonedDateTime.now()
        if (now.hour < HOUR) return
        val today = now.toLocalDate()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        for (trip in repository.getAllTrips()) {
            val start = trip.startDateEpochDay?.let { LocalDate.ofEpochDay(it) } ?: continue
            val end = trip.endDateEpochDay?.let { LocalDate.ofEpochDay(it) } ?: start
            if (today == start) {
                val key = sentKey(trip.id, today, "start")
                if (!prefs.getBoolean(key, false)) {
                    val next = nextStopTitle(repository, trip.id)
                    val body = if (next == null) trip.name else "${trip.name}. First stop: $next"
                    TripNotifications.show(context, "Trip starts today", body)
                    editor.putBoolean(key, true)
                }
            } else if (today.isAfter(start) && !today.isAfter(end)) {
                val key = sentKey(trip.id, today, "stop")
                val next = nextStopTitle(repository, trip.id)
                if (next != null && !prefs.getBoolean(key, false)) {
                    TripNotifications.show(context, "Next stop", "${trip.name}: $next")
                    editor.putBoolean(key, true)
                }
            }
        }
        editor.commit()
    }

    private suspend fun nextStopTitle(repository: TripRepository, tripId: Long): String? {
        return repository.getRouteStopsForTrip(tripId)
            .sortedBy { it.sortOrder }
            .firstOrNull { it.status != RouteStopStatus.REACHED }
            ?.title
    }

    private fun sentKey(tripId: Long, day: LocalDate, kind: String): String =
        "sent:$tripId:$day:$kind"

    private fun nextEight(): ZonedDateTime {
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        val eight = now.withHour(HOUR).withMinute(0).withSecond(0).withNano(0)
        return if (now.isBefore(eight)) eight else eight.plusDays(1)
    }

    private fun pending(context: Context): PendingIntent {
        val intent = Intent(context, TripReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

class TripReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pending = goAsync()
        val app = context.applicationContext as TripBudgetApp
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = app.container.authRepository.currentUserIdOrNull()
                RouteTripSchedule.refresh(app, app.container.repository, userId)
                TripReminders.refresh(app, app.container.repository)
            } finally {
                pending.finish()
            }
        }
    }
}

class TripReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        val app = context.applicationContext as TripBudgetApp
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = app.container.authRepository.currentUserIdOrNull()
                RouteTripSchedule.refresh(app, app.container.repository, userId)
                TripReminders.refresh(app, app.container.repository)
            } finally {
                pending.finish()
            }
        }
    }
}
