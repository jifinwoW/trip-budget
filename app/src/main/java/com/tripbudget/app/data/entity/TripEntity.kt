package com.tripbudget.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val destination: String? = null,
    val plannedBudgetPaise: Long,
    /** Inclusive trip start, as LocalDate epoch day. Null for trips created before date range existed. */
    val startDateEpochDay: Long? = null,
    /** Inclusive trip end, as LocalDate epoch day. Null for trips created before date range existed. */
    val endDateEpochDay: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
