package com.tripbudget.app.data

import com.tripbudget.app.cloud.LiveCloudPush
import com.tripbudget.app.data.entity.ContributionEntity
import com.tripbudget.app.data.entity.ExpenseEntity
import com.tripbudget.app.data.entity.MemberEntity
import com.tripbudget.app.data.entity.TripEntity
import com.tripbudget.app.domain.ExpenseAmount
import com.tripbudget.app.domain.TripTotals
import com.tripbudget.app.domain.TripTotalsCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class TripRepository(
    private val db: TripBudgetDatabase,
) {
    private val tripDao = db.tripDao()
    private val memberDao = db.memberDao()
    private val contributionDao = db.contributionDao()
    private val expenseDao = db.expenseDao()
    private val clearMutex = Mutex()

    /** Set from AppContainer after SyncEngine is created (avoids construct-time cycles). */
    var liveCloudPush: LiveCloudPush? = null

    fun observeTrips(): Flow<List<TripEntity>> = tripDao.observeAll()

    fun observeTrip(tripId: Long): Flow<TripEntity?> = tripDao.observeById(tripId)

    fun observeMembers(tripId: Long): Flow<List<MemberEntity>> = memberDao.observeByTrip(tripId)

    fun observeContributions(tripId: Long): Flow<List<ContributionEntity>> =
        contributionDao.observeByTrip(tripId)

    fun observeMemberContributions(memberId: Long): Flow<List<ContributionEntity>> =
        contributionDao.observeByMember(memberId)

    fun observeExpenses(tripId: Long): Flow<List<ExpenseEntity>> = expenseDao.observeByTrip(tripId)

    fun observeRecentExpenses(tripId: Long, limit: Int = 5): Flow<List<ExpenseEntity>> =
        expenseDao.observeRecent(tripId, limit)

    fun observeTripTotals(tripId: Long): Flow<TripTotals?> {
        return combine(
            tripDao.observeById(tripId),
            memberDao.observeByTrip(tripId),
            contributionDao.observeByTrip(tripId),
            expenseDao.observeByTrip(tripId),
        ) { trip, members, contributions, expenses ->
            if (trip == null) return@combine null
            val expected = members.associate { it.id to (it.name to it.expectedContributionPaise) }
            val paid = contributions
                .groupBy { it.memberId }
                .mapValues { (_, list) -> list.sumOf { it.amountPaise } }
            TripTotalsCalculator.calculate(
                plannedBudgetPaise = trip.plannedBudgetPaise,
                memberExpected = expected,
                contributionsByMember = paid,
                expenses = expenses.map {
                    ExpenseAmount(it.amountPaise, it.paymentSource)
                },
            )
        }
    }

    suspend fun insertTrip(trip: TripEntity): Long {
        val id = tripDao.insert(trip.copy(updatedAt = System.currentTimeMillis()))
        liveCloudPush?.onTripUpserted(id)
        return id
    }

    suspend fun updateTrip(trip: TripEntity) {
        tripDao.update(trip.copy(updatedAt = System.currentTimeMillis()))
        liveCloudPush?.onTripUpserted(trip.id)
    }

    suspend fun deleteTrip(trip: TripEntity) {
        val remoteId = trip.remoteId
        tripDao.delete(trip)
        liveCloudPush?.onTripDeleted(remoteId)
    }

    suspend fun insertMember(member: MemberEntity): Long {
        val id = memberDao.insert(member.copy(updatedAt = System.currentTimeMillis()))
        liveCloudPush?.onMemberUpserted(id)
        return id
    }

    suspend fun updateMember(member: MemberEntity) {
        memberDao.update(member.copy(updatedAt = System.currentTimeMillis()))
        liveCloudPush?.onMemberUpserted(member.id)
    }

    suspend fun deleteMember(member: MemberEntity) {
        val remoteId = member.remoteId
        memberDao.delete(member)
        liveCloudPush?.onMemberDeleted(remoteId)
    }

    suspend fun insertContribution(contribution: ContributionEntity): Long {
        val id = contributionDao.insert(contribution.copy(updatedAt = System.currentTimeMillis()))
        liveCloudPush?.onContributionUpserted(id)
        return id
    }

    suspend fun updateContribution(contribution: ContributionEntity) {
        contributionDao.update(contribution.copy(updatedAt = System.currentTimeMillis()))
        liveCloudPush?.onContributionUpserted(contribution.id)
    }

    suspend fun deleteContribution(contribution: ContributionEntity) {
        val remoteId = contribution.remoteId
        contributionDao.delete(contribution)
        liveCloudPush?.onContributionDeleted(remoteId)
    }

    suspend fun insertExpense(expense: ExpenseEntity): Long {
        val id = expenseDao.insert(expense.copy(updatedAt = System.currentTimeMillis()))
        liveCloudPush?.onExpenseUpserted(id)
        return id
    }

    suspend fun updateExpense(expense: ExpenseEntity) {
        expenseDao.update(expense.copy(updatedAt = System.currentTimeMillis()))
        liveCloudPush?.onExpenseUpserted(expense.id)
    }

    suspend fun deleteExpense(expense: ExpenseEntity) {
        val remoteId = expense.remoteId
        expenseDao.delete(expense)
        liveCloudPush?.onExpenseDeleted(remoteId)
    }

    suspend fun getTrip(id: Long): TripEntity? = tripDao.getById(id)

    suspend fun getMember(id: Long): MemberEntity? = memberDao.getById(id)

    suspend fun getContribution(id: Long): ContributionEntity? = contributionDao.getById(id)

    suspend fun getExpense(id: Long): ExpenseEntity? = expenseDao.getById(id)

    // --- Sync helpers (preserve timestamps / remote ids as provided) ---

    suspend fun getAllTrips(): List<TripEntity> = tripDao.getAll()

    suspend fun getAllMembers(): List<MemberEntity> = memberDao.getAll()

    suspend fun getAllContributions(): List<ContributionEntity> = contributionDao.getAll()

    suspend fun getAllExpenses(): List<ExpenseEntity> = expenseDao.getAll()

    suspend fun getTripByRemoteId(remoteId: String): TripEntity? = tripDao.getByRemoteId(remoteId)

    suspend fun getMemberByRemoteId(remoteId: String): MemberEntity? =
        memberDao.getByRemoteId(remoteId)

    suspend fun getContributionByRemoteId(remoteId: String): ContributionEntity? =
        contributionDao.getByRemoteId(remoteId)

    suspend fun getExpenseByRemoteId(remoteId: String): ExpenseEntity? =
        expenseDao.getByRemoteId(remoteId)

    /**
     * Persist sync fields without SQLite REPLACE.
     * REPLACE deletes the row first and CASCADE-wipes children (e.g. contributions).
     */
    suspend fun putTripSynced(trip: TripEntity): Long {
        return if (trip.id == 0L) {
            tripDao.insert(trip)
        } else {
            tripDao.update(trip)
            trip.id
        }
    }

    suspend fun putMemberSynced(member: MemberEntity): Long {
        return if (member.id == 0L) {
            memberDao.insert(member)
        } else {
            memberDao.update(member)
            member.id
        }
    }

    suspend fun putContributionSynced(contribution: ContributionEntity): Long {
        return if (contribution.id == 0L) {
            contributionDao.insert(contribution)
        } else {
            contributionDao.update(contribution)
            contribution.id
        }
    }

    suspend fun putExpenseSynced(expense: ExpenseEntity): Long {
        return if (expense.id == 0L) {
            expenseDao.insert(expense)
        } else {
            expenseDao.update(expense)
            expense.id
        }
    }

    suspend fun deleteSyncedTripsNotIn(keepRemoteIds: Collection<String>) {
        if (keepRemoteIds.isEmpty()) {
            tripDao.deleteAllSynced()
        } else {
            tripDao.deleteSyncedNotIn(keepRemoteIds.toList())
        }
    }

    suspend fun deleteSyncedMembersNotIn(keepRemoteIds: Collection<String>) {
        if (keepRemoteIds.isEmpty()) {
            memberDao.deleteAllSynced()
        } else {
            memberDao.deleteSyncedNotIn(keepRemoteIds.toList())
        }
    }

    suspend fun deleteSyncedContributionsNotIn(keepRemoteIds: Collection<String>) {
        if (keepRemoteIds.isEmpty()) {
            contributionDao.deleteAllSynced()
        } else {
            contributionDao.deleteSyncedNotIn(keepRemoteIds.toList())
        }
    }

    suspend fun deleteSyncedExpensesNotIn(keepRemoteIds: Collection<String>) {
        if (keepRemoteIds.isEmpty()) {
            expenseDao.deleteAllSynced()
        } else {
            expenseDao.deleteSyncedNotIn(keepRemoteIds.toList())
        }
    }

    /** Wipe all cached trip data (used on sign-out — cloud is source of truth). */
    suspend fun clearAllLocalData() {
        clearMutex.withLock {
            // Room.clearAllTables() must not run on the main thread.
            withContext(Dispatchers.IO) {
                db.clearAllTables()
            }
        }
    }
}
