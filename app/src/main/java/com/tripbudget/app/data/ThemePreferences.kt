package com.tripbudget.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tripbudget.app.places.WeatherKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/** Header atmosphere: live location weather, or a fixed scene for preview. */
enum class HeaderWeatherPreference {
    LIVE,
    SUNNY,
    CLOUDY,
    FOG,
    RAIN,
    STORM,
    SNOW,
    ;

    fun toWeatherKind(): WeatherKind? = when (this) {
        LIVE -> null
        SUNNY -> WeatherKind.SUNNY
        CLOUDY -> WeatherKind.CLOUDY
        FOG -> WeatherKind.FOG
        RAIN -> WeatherKind.RAIN
        STORM -> WeatherKind.STORM
        SNOW -> WeatherKind.SNOW
    }

    fun label(): String = when (this) {
        LIVE -> "Live weather"
        SUNNY -> "Sunny"
        CLOUDY -> "Cloudy"
        FOG -> "Fog"
        RAIN -> "Rain"
        STORM -> "Storm"
        SNOW -> "Snow"
    }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class ThemePreferences(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme_mode")
    private val headerWeatherKey = stringPreferencesKey("header_weather")

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        runCatching {
            ThemeMode.valueOf(prefs[themeKey] ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM)
    }

    val headerWeather: Flow<HeaderWeatherPreference> = context.dataStore.data.map { prefs ->
        runCatching {
            HeaderWeatherPreference.valueOf(
                prefs[headerWeatherKey] ?: HeaderWeatherPreference.LIVE.name,
            )
        }.getOrDefault(HeaderWeatherPreference.LIVE)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[themeKey] = mode.name
        }
    }

    suspend fun setHeaderWeather(preference: HeaderWeatherPreference) {
        context.dataStore.edit { prefs ->
            prefs[headerWeatherKey] = preference.name
        }
    }
}
