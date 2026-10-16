package com.tripbudget.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.syncDataStore: DataStore<Preferences> by preferencesDataStore(name = "sync")

class SyncPreferences(private val context: Context) {
    private val lastSyncedKey = longPreferencesKey("last_synced_at")

    val lastSyncedAt: Flow<Long?> = context.syncDataStore.data.map { prefs ->
        prefs[lastSyncedKey]
    }

    suspend fun setLastSyncedAt(millis: Long) {
        context.syncDataStore.edit { prefs ->
            prefs[lastSyncedKey] = millis
        }
    }

    suspend fun clearLastSyncedAt() {
        context.syncDataStore.edit { prefs ->
            prefs.remove(lastSyncedKey)
        }
    }
}
