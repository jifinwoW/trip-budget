package com.tripbudget.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tripbudget.app.data.entity.ContributionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContributionDao {
    @Query(
        """
        SELECT * FROM contributions
        WHERE tripId = :tripId
        ORDER BY paidAt DESC
        """
    )
    fun observeByTrip(tripId: Long): Flow<List<ContributionEntity>>

    @Query(
        """
        SELECT * FROM contributions
        WHERE memberId = :memberId
        ORDER BY paidAt DESC
        """
    )
    fun observeByMember(memberId: Long): Flow<List<ContributionEntity>>

    @Query("SELECT * FROM contributions WHERE id = :id")
    suspend fun getById(id: Long): ContributionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contribution: ContributionEntity): Long

    @Update
    suspend fun update(contribution: ContributionEntity)

    @Delete
    suspend fun delete(contribution: ContributionEntity)
}
