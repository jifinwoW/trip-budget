package com.tripbudget.app.data

import com.tripbudget.app.cloud.LiveCloudPush
import com.tripbudget.app.data.entity.ContributionEntity
import com.tripbudget.app.data.entity.ExpenseEntity
import com.tripbudget.app.data.entity.MemberEntity
import com.tripbudget.app.data.entity.RouteStopEntity
import com.tripbudget.app.data.entity.TripEntity
import com.tripbudget.app.domain.ExpenseAmount
import com.tripbudget.app.domain.RouteStopStatus
import com.tripbudget.app.domain.TripTotals
import com.tripbudget.app.domain.TripTotalsCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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
    private val routeStopDao = db.routeStopDao()
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

    fun observeRouteStops(tripId: Long): Flow<List<RouteStopEntity>> =
        routeStopDao.observeByTrip(tripId)

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

    suspend fun insertRouteStop(stop: RouteStopEntity): Long {
        val id = routeStopDao.insert(stop)
        liveCloudPush?.onRouteStopUpserted(id)
        return id
    }

    suspend fun updateRouteStop(stop: RouteStopEntity) {
        routeStopDao.update(stop)
        liveCloudPush?.onRouteStopUpserted(stop.id)
    }

    suspend fun deleteRouteStop(stop: RouteStopEntity) {
        val remoteId = stop.remoteId
        routeStopDao.delete(stop)
        liveCloudPush?.onRouteStopDeleted(remoteId)
    }

    suspend fun addRouteStopFromPlace(
        tripId: Long,
        title: String,
        subtitle: String?,
        lat: Double,
        lng: Double,
        osmPlaceId: String?,
    ): Long {
        val nextOrder = routeStopDao.maxSortOrder(tripId) + 1
        val existing = routeStopDao.getByTrip(tripId)
        val status = if (existing.isEmpty()) {
            RouteStopStatus.CURRENT
        } else {
            RouteStopStatus.UPCOMING
        }
        return insertRouteStop(
            RouteStopEntity(
                tripId = tripId,
                sortOrder = nextOrder,
                title = title,
                subtitle = subtitle,
                lat = lat,
                lng = lng,
                osmPlaceId = osmPlaceId,
                status = status,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    /** Mark stop reached and promote the next upcoming stop to current. */
    suspend fun markRouteStopReached(stopId: Long) {
        val stop = routeStopDao.getById(stopId) ?: return
        if (stop.status == RouteStopStatus.REACHED) return
        val now = System.currentTimeMillis()
        updateRouteStop(stop.copy(status = RouteStopStatus.REACHED, updatedAt = now))
        val remaining = routeStopDao.getByTrip(stop.tripId)
            .filter { it.status != RouteStopStatus.REACHED }
            .sortedBy { it.sortOrder }
        val next = remaining.firstOrNull()
        if (next != null && next.status != RouteStopStatus.CURRENT) {
            updateRouteStop(next.copy(status = RouteStopStatus.CURRENT, updatedAt = now))
        }
        // Clear other CURRENT flags if any.
        remaining.drop(1).filter { it.status == RouteStopStatus.CURRENT }.forEach { extra ->
            updateRouteStop(extra.copy(status = RouteStopStatus.UPCOMING, updatedAt = now))
        }
    }

    suspend fun setRouteTrackingActive(tripId: Long, active: Boolean) {
        val trip = tripDao.getById(tripId) ?: return
        updateTrip(
            trip.copy(
                routeTrackingActive = active,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun moveRouteStop(stopId: Long, direction: Int) {
        val stop = routeStopDao.getById(stopId) ?: return
        val all = routeStopDao.getByTrip(stop.tripId).sortedBy { it.sortOrder }
        val index = all.indexOfFirst { it.id == stopId }
        val targetIndex = index + direction
        if (index < 0 || targetIndex !in all.indices) return
        val other = all[targetIndex]
        val now = System.currentTimeMillis()
        updateRouteStop(stop.copy(sortOrder = other.sortOrder, updatedAt = now))
        updateRouteStop(other.copy(sortOrder = stop.sortOrder, updatedAt = now))
    }

    /** Move a stop’s map pin to the given GPS (search pins are often town centers). */
    suspend fun updateRouteStopLocation(stopId: Long, lat: Double, lng: Double) {
        val stop = routeStopDao.getById(stopId) ?: return
        updateRouteStop(
            stop.copy(
                lat = lat,
                lng = lng,
                updatedAt = System.currentTimeMillis(),
            ),
        )
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

    suspend fun getAllRouteStops(): List<RouteStopEntity> = routeStopDao.getAll()

    suspend fun getRouteStop(id: Long): RouteStopEntity? = routeStopDao.getById(id)

    suspend fun getRouteStopByRemoteId(remoteId: String): RouteStopEntity? =
        routeStopDao.getByRemoteId(remoteId)

    suspend fun getRouteStopsForTrip(tripId: Long): List<RouteStopEntity> =
        routeStopDao.getByTrip(tripId)

    /** Trips currently running GPS route tracking. */
    suspend fun getTripsWithRouteTracking(): List<TripEntity> =
        tripDao.getAll().filter { it.routeTrackingActive }

    fun observeTripsWithRouteTracking(): Flow<List<TripEntity>> =
        tripDao.observeAll().map { trips -> trips.filter { it.routeTrackingActive } }

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

    suspend fun putRouteStopSynced(stop: RouteStopEntity): Long {
        return if (stop.id == 0L) {
            routeStopDao.insert(stop)
        } else {
            routeStopDao.update(stop)
            stop.id
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

    suspend fun deleteSyncedRouteStopsNotIn(keepRemoteIds: Collection<String>) {
        if (keepRemoteIds.isEmpty()) {
            routeStopDao.deleteAllSynced()
        } else {
            routeStopDao.deleteSyncedNotIn(keepRemoteIds.toList())
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
