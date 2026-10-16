package com.tripbudget.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tripbudget.app.domain.ExpenseCategory
import com.tripbudget.app.domain.PaymentSource

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["paidByMemberId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("tripId"), Index("paidByMemberId"), Index(value = ["remoteId"], unique = true)],
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val title: String,
    val amountPaise: Long,
    val category: ExpenseCategory,
    val dateEpochDay: Long,
    /** Set only when payment source is PERSONAL; null for SHARED fund. */
    val paidByMemberId: Long? = null,
    val paymentSource: PaymentSource,
    val notes: String? = null,
    val remoteId: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)
