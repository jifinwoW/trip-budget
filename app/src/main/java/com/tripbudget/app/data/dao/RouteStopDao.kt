package com.tripbudget.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tripbudget.app.data.entity.RouteStopEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteStopDao {
    @Query(
        """
        SELECT * FROM route_stops
        WHERE tripId = :tripId
        ORDER BY sortOrder ASC, id ASC
        """,
    )
    fun observeByTrip(tripId: Long): Flow<List<RouteStopEntity>>

    @Query(
        """
        SELECT * FROM route_stops
        WHERE tripId = :tripId
        ORDER BY sortOrder ASC, id ASC
        """,
    )
    suspend fun getByTrip(tripId: Long): List<RouteStopEntity>

    @Query("SELECT * FROM route_stops")
    suspend fun getAll(): List<RouteStopEntity>

    @Query("SELECT * FROM route_stops WHERE id = :id")
    suspend fun getById(id: Long): RouteStopEntity?

    @Query("SELECT * FROM route_stops WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): RouteStopEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM route_stops WHERE tripId = :tripId")
    suspend fun maxSortOrder(tripId: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(stop: RouteStopEntity): Long

    @Update
    suspend fun update(stop: RouteStopEntity)

    @Delete
    suspend fun delete(stop: RouteStopEntity)

    @Query("DELETE FROM route_stops WHERE remoteId IS NOT NULL AND remoteId NOT IN (:keepRemoteIds)")
    suspend fun deleteSyncedNotIn(keepRemoteIds: List<String>)

    @Query("DELETE FROM route_stops WHERE remoteId IS NOT NULL")
    suspend fun deleteAllSynced()
}
