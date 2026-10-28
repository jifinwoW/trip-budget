package com.tripbudget.app.di

import android.content.Context
import com.tripbudget.app.TripBudgetApp
import com.tripbudget.app.cloud.AuthRepository
import com.tripbudget.app.cloud.SupabaseProvider
import com.tripbudget.app.cloud.SyncEngine
import com.tripbudget.app.data.SyncPreferences
import com.tripbudget.app.data.ThemePreferences
import com.tripbudget.app.data.TripBudgetDatabase
import com.tripbudget.app.data.TripRepository
import com.tripbudget.app.location.RouteArrivalTracker
import com.tripbudget.app.notify.TripNotifications
import io.github.jan.supabase.auth.auth

class AppContainer(context: Context) {
    private val database = TripBudgetDatabase.create(context)
    val repository = TripRepository(database)
    val themePreferences = ThemePreferences(context)
    val syncPreferences = SyncPreferences(context)

    private val supabase = SupabaseProvider.client

    val authRepository = AuthRepository(supabase?.auth)

    val syncEngine = SyncEngine(
        appContext = context.applicationContext,
        client = supabase,
        authRepository = authRepository,
        repository = repository,
        syncPreferences = syncPreferences,
        showNotification = { title, body, evenInForeground, tag ->
            val app = context.applicationContext as TripBudgetApp
            if (evenInForeground || !app.isInForeground) {
                TripNotifications.show(app, title, body, headsUp = evenInForeground, tag = tag)
            }
        },
    ).also { engine ->
        // Live upsert/delete after each local write when signed in.
        repository.liveCloudPush = engine
    }

    val routeArrivalTracker = RouteArrivalTracker(context, repository)
}
