package com.tripbudget.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tripbudget.app.data.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query(
        """
        SELECT * FROM expenses
        WHERE tripId = :tripId
        ORDER BY dateEpochDay DESC, id DESC
        """,
    )
    fun observeByTrip(tripId: Long): Flow<List<ExpenseEntity>>

    @Query(
        """
        SELECT * FROM expenses
        WHERE tripId = :tripId
        ORDER BY dateEpochDay DESC, id DESC
        LIMIT :limit
        """,
    )
    fun observeRecent(tripId: Long, limit: Int): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE tripId = :tripId")
    suspend fun listByTrip(tripId: Long): List<ExpenseEntity>

    @Query("SELECT * FROM expenses")
    suspend fun getAll(): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: Long): ExpenseEntity?

    @Query("SELECT * FROM expenses WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): ExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE remoteId IS NOT NULL AND remoteId NOT IN (:keepRemoteIds)")
    suspend fun deleteSyncedNotIn(keepRemoteIds: List<String>)

    @Query("DELETE FROM expenses WHERE remoteId IS NOT NULL")
    suspend fun deleteAllSynced()
}
