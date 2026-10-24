package com.tripbudget.app.places

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

enum class WeatherKind {
    SUNNY,
    CLOUDY,
    RAIN,
    STORM,
    SNOW,
    FOG,
    UNKNOWN,
}

/** How hard rain draws in the header — from Open-Meteo codes (or storm). */
enum class RainIntensity {
    LIGHT,
    MEDIUM,
    HEAVY,
    STORM,
}

data class PlaceWeather(
    val lat: Double,
    val lng: Double,
    val kind: WeatherKind,
    val label: String,
    val temperatureC: Double?,
    val weatherCode: Int,
) {
    /** Compact label for map pins, e.g. "Rain · 28°C". */
    fun pinLabel(): String {
        val temp = temperatureC?.let { "${it.toInt()}°" } ?: return label
        return "$label · $temp"
    }
}

/**
 * Free weather by coordinates via Open-Meteo (no API key / no Google billing).
 * https://open-meteo.com/
 */
object OpenMeteoWeather {
    private const val USER_AGENT = "ChilavuTripBudget/1.0 (Android; trip weather)"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchCurrent(points: List<LatLng>): List<PlaceWeather> =
        withContext(Dispatchers.IO) {
            if (points.isEmpty()) return@withContext emptyList()
            val lats = points.joinToString(",") { it.lat.toString() }
            val lngs = points.joinToString(",") { it.lng.toString() }
            val url = URL(
                "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$lats&longitude=$lngs" +
                    "&current=weather_code,temperature_2m" +
                    "&timezone=auto",
            )
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/json")
            }
            try {
                if (connection.responseCode !in 200..299) return@withContext emptyList()
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val trimmed = body.trim()
                val responses: List<OpenMeteoResponse> = if (trimmed.startsWith("[")) {
                    json.decodeFromString(OpenMeteoListSerializer, trimmed)
                } else {
                    listOf(json.decodeFromString(OpenMeteoResponse.serializer(), trimmed))
                }
                responses.mapIndexedNotNull { index, response ->
                    val point = points.getOrNull(index) ?: return@mapIndexedNotNull null
                    val current = response.current ?: return@mapIndexedNotNull null
                    val code = current.weatherCode ?: return@mapIndexedNotNull null
                    val kind = kindFromCode(code)
                    PlaceWeather(
                        lat = point.lat,
                        lng = point.lng,
                        kind = kind,
                        label = labelFor(kind, code),
                        temperatureC = current.temperature,
                        weatherCode = code,
                    )
                }
            } catch (_: Exception) {
                emptyList()
            } finally {
                connection.disconnect()
            }
        }

    fun kindFromCode(code: Int): WeatherKind = when (code) {
        0 -> WeatherKind.SUNNY
        1, 2, 3 -> WeatherKind.CLOUDY
        45, 48 -> WeatherKind.FOG
        in 51..67, in 80..82 -> WeatherKind.RAIN
        in 71..77, 85, 86 -> WeatherKind.SNOW
        in 95..99 -> WeatherKind.STORM
        else -> WeatherKind.UNKNOWN
    }

    /** Light / medium / heavy rain from WMO code; storms use [RainIntensity.STORM] separately. */
    fun rainIntensityFromCode(code: Int?): RainIntensity = when (code) {
        null -> RainIntensity.MEDIUM
        51, 56, 61, 66, 80 -> RainIntensity.LIGHT
        55, 57, 65, 67, 82 -> RainIntensity.HEAVY
        in 51..67, in 80..82 -> RainIntensity.MEDIUM
        else -> RainIntensity.MEDIUM
    }

    fun labelFor(kind: WeatherKind, code: Int): String = when (kind) {
        WeatherKind.SUNNY -> "Sunny"
        WeatherKind.CLOUDY -> when (code) {
            1 -> "Partly cloudy"
            2 -> "Cloudy"
            else -> "Overcast"
        }
        WeatherKind.RAIN -> when (code) {
            51, 56, 61, 66, 80 -> "Light rain"
            55, 57, 65, 67, 82 -> "Heavy rain"
            else -> "Rain"
        }
        WeatherKind.STORM -> "Storm"
        WeatherKind.SNOW -> "Snow"
        WeatherKind.FOG -> "Fog"
        WeatherKind.UNKNOWN -> "Weather"
    }
}

@Serializable
private data class OpenMeteoResponse(
    val current: OpenMeteoCurrent? = null,
)

@Serializable
private data class OpenMeteoCurrent(
    @SerialName("weather_code") val weatherCode: Int? = null,
    @SerialName("temperature_2m") val temperature: Double? = null,
)

private val OpenMeteoListSerializer =
    kotlinx.serialization.builtins.ListSerializer(OpenMeteoResponse.serializer())
