package com.tripbudget.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tripbudget.app.domain.ExpenseCategory

@Entity(
    tableName = "planned_expenses",
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
data class PlannedExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val title: String,
    val amountPaise: Long,
    val category: ExpenseCategory,
    val notes: String? = null,
    val remoteId: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)

