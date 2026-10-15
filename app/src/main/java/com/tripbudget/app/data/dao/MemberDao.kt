package com.tripbudget.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tripbudget.app.data.entity.MemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {
    @Query("SELECT * FROM members WHERE tripId = :tripId ORDER BY name COLLATE NOCASE ASC")
    fun observeByTrip(tripId: Long): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE tripId = :tripId ORDER BY name COLLATE NOCASE ASC")
    suspend fun getByTrip(tripId: Long): List<MemberEntity>

    @Query("SELECT * FROM members WHERE id = :id")
    suspend fun getById(id: Long): MemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(member: MemberEntity): Long

    @Update
    suspend fun update(member: MemberEntity)

    @Delete
    suspend fun delete(member: MemberEntity)
}
