package com.tripbudget.app.cloud

import com.tripbudget.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

object SupabaseProvider {
    val isConfigured: Boolean
        get() = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    val client: SupabaseClient? by lazy {
        if (!isConfigured) return@lazy null
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL.trim(),
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY.trim(),
        ) {
            install(Auth) {
                // Keep the session in memory when the app is backgrounded so reopen
                // resumes the same account instead of looking signed-out.
                enableLifecycleCallbacks = false
                alwaysAutoRefresh = true
                autoLoadFromStorage = true
                autoSaveToStorage = true
            }
            install(Postgrest)
            install(Realtime)
        }
    }
}
