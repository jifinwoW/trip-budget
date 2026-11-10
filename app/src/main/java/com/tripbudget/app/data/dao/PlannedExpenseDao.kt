package com.tripbudget.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tripbudget.app.data.entity.PlannedExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannedExpenseDao {
    @Query(
        """
        SELECT * FROM planned_expenses
        WHERE tripId = :tripId
        ORDER BY id ASC
        """,
    )
    fun observeByTrip(tripId: Long): Flow<List<PlannedExpenseEntity>>

    @Query("SELECT * FROM planned_expenses WHERE tripId = :tripId")
    suspend fun listByTrip(tripId: Long): List<PlannedExpenseEntity>

    @Query("SELECT * FROM planned_expenses")
    suspend fun getAll(): List<PlannedExpenseEntity>

    @Query("SELECT * FROM planned_expenses WHERE id = :id")
    suspend fun getById(id: Long): PlannedExpenseEntity?

    @Query("SELECT * FROM planned_expenses WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): PlannedExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: PlannedExpenseEntity): Long

    @Update
    suspend fun update(entity: PlannedExpenseEntity)

    @Delete
    suspend fun delete(entity: PlannedExpenseEntity)

    @Query("DELETE FROM planned_expenses WHERE remoteId IS NOT NULL AND remoteId NOT IN (:keepRemoteIds)")
    suspend fun deleteSyncedNotIn(keepRemoteIds: List<String>)

    @Query("DELETE FROM planned_expenses WHERE remoteId IS NOT NULL")
    suspend fun deleteAllSynced()
}

