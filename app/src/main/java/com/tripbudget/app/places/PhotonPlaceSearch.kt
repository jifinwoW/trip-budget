package com.tripbudget.app.places

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class PlaceSuggestion(
    val title: String,
    val subtitle: String?,
    val lat: Double,
    val lng: Double,
    val osmPlaceId: String?,
)

/**
 * Free place autocomplete via Komoot Photon (OpenStreetMap data).
 * Debounce in the UI; identify the app in User-Agent per OSM etiquette.
 */
class PhotonPlaceSearch(
    private val userAgent: String = "ChilavuTripBudget/1.0 (Android; trip route planner)",
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun search(query: String, limit: Int = 8): List<PlaceSuggestion> =
        withContext(Dispatchers.IO) {
            val q = query.trim()
            if (q.length < 2) return@withContext emptyList()
            val encoded = URLEncoder.encode(q, StandardCharsets.UTF_8)
            // Bias toward India (approx. geographic center) so Indian place names rank first.
            val url = URL(
                "https://photon.komoot.io/api/?q=$encoded&limit=$limit&lang=en&lat=20.5937&lon=78.9629",
            )
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("User-Agent", userAgent)
                setRequestProperty("Accept", "application/json")
            }
            try {
                if (connection.responseCode !in 200..299) {
                    return@withContext emptyList()
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val response = json.decodeFromString(PhotonResponse.serializer(), body)
                response.features.mapNotNull { feature ->
                    val coords = feature.geometry?.coordinates ?: return@mapNotNull null
                    if (coords.size < 2) return@mapNotNull null
                    val lng = coords[0]
                    val lat = coords[1]
                    val props = feature.properties ?: return@mapNotNull null
                    val title = props.name?.takeIf { it.isNotBlank() }
                        ?: props.city
                        ?: props.county
                        ?: props.state
                        ?: return@mapNotNull null
                    val parts = listOfNotNull(
                        props.city?.takeIf { it != title },
                        props.state,
                        props.country,
                    ).distinct()
                    val osmId = when {
                        props.osmType != null && props.osmId != null ->
                            "${props.osmType}:${props.osmId}"
                        else -> null
                    }
                    PlaceSuggestion(
                        title = title,
                        subtitle = parts.takeIf { it.isNotEmpty() }?.joinToString(", "),
                        lat = lat,
                        lng = lng,
                        osmPlaceId = osmId,
                    )
                }
            } finally {
                connection.disconnect()
            }
        }
}

@Serializable
private data class PhotonResponse(
    val features: List<PhotonFeature> = emptyList(),
)

@Serializable
private data class PhotonFeature(
    val geometry: PhotonGeometry? = null,
    val properties: PhotonProperties? = null,
)

@Serializable
private data class PhotonGeometry(
    val coordinates: List<Double> = emptyList(),
)

@Serializable
private data class PhotonProperties(
    val name: String? = null,
    val city: String? = null,
    val county: String? = null,
    val state: String? = null,
    val country: String? = null,
    @SerialName("osm_id") val osmId: Long? = null,
    @SerialName("osm_type") val osmType: String? = null,
)
