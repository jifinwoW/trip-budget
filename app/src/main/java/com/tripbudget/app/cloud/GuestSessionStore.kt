package com.tripbudget.app.cloud

import android.content.Context
import android.provider.Settings
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Remembers a guest session for this phone so sign-out does not create a new user. */
class GuestSessionStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun deviceId(): String? {
        return Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
            ?.takeIf { it.isNotBlank() }
    }

    fun remember(session: UserSession) {
        val id = deviceId() ?: return
        prefs.edit()
            .putString(KEY_DEVICE, id)
            .putString(KEY_SESSION, json.encodeToString(session))
            .apply()
    }

    fun savedSession(): UserSession? {
        val id = deviceId() ?: return null
        if (prefs.getString(KEY_DEVICE, null) != id) return null
        val raw = prefs.getString(KEY_SESSION, null) ?: return null
        return runCatching { json.decodeFromString<UserSession>(raw) }.getOrNull()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREFS = "guest_identity"
        const val KEY_DEVICE = "device_id"
        const val KEY_SESSION = "session"
    }
}
