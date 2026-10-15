package com.tripbudget.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tripbudget.app.data.entity.TripEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :id")
    fun observeById(id: Long): Flow<TripEntity?>

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun getById(id: Long): TripEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trip: TripEntity): Long

    @Update
    suspend fun update(trip: TripEntity)

    @Delete
    suspend fun delete(trip: TripEntity)

    @Query("SELECT COUNT(*) FROM trips")
    suspend fun count(): Int
}
