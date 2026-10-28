package com.tripbudget.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "trips",
    indices = [Index(value = ["remoteId"], unique = true)],
)
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Place name chosen for this trip. Weather uses [placeLat] and [placeLng]. */
    val destination: String? = null,
    val placeLat: Double? = null,
    val placeLng: Double? = null,
    val plannedBudgetPaise: Long,
    /** Inclusive trip start, as LocalDate epoch day. Null for trips created before date range existed. */
    val startDateEpochDay: Long? = null,
    /** Inclusive trip end, as LocalDate epoch day. Null for trips created before date range existed. */
    val endDateEpochDay: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    /** Supabase UUID when synced; null until first successful push. */
    val remoteId: String? = null,
    /** Cloud owner user id — preserved so shared editors don't steal ownership on upsert. */
    val remoteOwnerId: String? = null,
    /** Short code friends enter to join this trip. */
    val inviteCode: String? = null,
    /** When true, GPS auto-marks route stops as reached. */
    val routeTrackingActive: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    /** Cached hero photo on this phone. The shared copy lives in the uploads bucket. */
    val coverImagePath: String? = null,
    /** Object key already uploaded or downloaded, such as `{trip remote id}/cover.jpg`. */
    val coverObjectKey: String? = null,
)
