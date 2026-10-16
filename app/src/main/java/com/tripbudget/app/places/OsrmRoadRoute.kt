package com.tripbudget.app.places

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

data class LatLng(
    val lat: Double,
    val lng: Double,
)

/**
 * Free driving route along roads via the public OSRM demo server (OpenStreetMap).
 * Fallback to straight segments if the request fails.
 */
object OsrmRoadRoute {
    private const val USER_AGENT = "ChilavuTripBudget/1.0 (Android; trip route planner)"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchDrivingPath(waypoints: List<LatLng>): List<LatLng> =
        withContext(Dispatchers.IO) {
            if (waypoints.size < 2) return@withContext waypoints
            val coords = waypoints.joinToString(";") { "${it.lng},${it.lat}" }
            val url = URL(
                "https://router.project-osrm.org/route/v1/driving/$coords" +
                    "?overview=full&geometries=geojson&steps=false",
            )
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12_000
                readTimeout = 12_000
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/json")
            }
            try {
                if (connection.responseCode !in 200..299) {
                    return@withContext waypoints
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val response = json.decodeFromString(OsrmResponse.serializer(), body)
                if (response.code != "Ok") return@withContext waypoints
                val geometry = response.routes.firstOrNull()?.geometry?.coordinates
                    ?: return@withContext waypoints
                // GeoJSON is [lng, lat]
                geometry.mapNotNull { pair ->
                    if (pair.size < 2) null else LatLng(lat = pair[1], lng = pair[0])
                }.ifEmpty { waypoints }
            } catch (_: Exception) {
                waypoints
            } finally {
                connection.disconnect()
            }
        }
}

@Serializable
private data class OsrmResponse(
    val code: String = "",
    val routes: List<OsrmRoute> = emptyList(),
)

@Serializable
private data class OsrmRoute(
    val geometry: OsrmGeometry? = null,
)

@Serializable
private data class OsrmGeometry(
    val coordinates: List<List<Double>> = emptyList(),
)
