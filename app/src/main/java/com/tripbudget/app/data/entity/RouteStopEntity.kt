package com.tripbudget.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tripbudget.app.domain.RouteStopStatus

@Entity(
    tableName = "route_stops",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("tripId"),
        Index(value = ["remoteId"], unique = true),
    ],
)
data class RouteStopEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val sortOrder: Int,
    val title: String,
    val subtitle: String? = null,
    val lat: Double,
    val lng: Double,
    val osmPlaceId: String? = null,
    val status: RouteStopStatus = RouteStopStatus.UPCOMING,
    val note: String? = null,
    val remoteId: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)
