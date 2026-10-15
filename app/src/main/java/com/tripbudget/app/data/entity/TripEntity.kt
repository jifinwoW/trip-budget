package com.tripbudget.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val destination: String? = null,
    val plannedBudgetPaise: Long,
    val createdAt: Long = System.currentTimeMillis(),
)
