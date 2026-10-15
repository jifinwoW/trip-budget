package com.tripbudget.app.data

import com.tripbudget.app.data.entity.ContributionEntity
import com.tripbudget.app.data.entity.ExpenseEntity
import com.tripbudget.app.data.entity.MemberEntity
import com.tripbudget.app.data.entity.TripEntity
import com.tripbudget.app.domain.ExpenseAmount
import com.tripbudget.app.domain.TripTotals
import com.tripbudget.app.domain.TripTotalsCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class TripRepository(
    private val db: TripBudgetDatabase,
) {
    private val tripDao = db.tripDao()
    private val memberDao = db.memberDao()
    private val contributionDao = db.contributionDao()
    private val expenseDao = db.expenseDao()

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

    suspend fun insertTrip(trip: TripEntity): Long = tripDao.insert(trip)

    suspend fun updateTrip(trip: TripEntity) = tripDao.update(trip)

    suspend fun deleteTrip(trip: TripEntity) = tripDao.delete(trip)

    suspend fun insertMember(member: MemberEntity): Long = memberDao.insert(member)

    suspend fun updateMember(member: MemberEntity) = memberDao.update(member)

    suspend fun deleteMember(member: MemberEntity) = memberDao.delete(member)

    suspend fun insertContribution(contribution: ContributionEntity): Long =
        contributionDao.insert(contribution)

    suspend fun updateContribution(contribution: ContributionEntity) =
        contributionDao.update(contribution)

    suspend fun deleteContribution(contribution: ContributionEntity) =
        contributionDao.delete(contribution)

    suspend fun insertExpense(expense: ExpenseEntity): Long = expenseDao.insert(expense)

    suspend fun updateExpense(expense: ExpenseEntity) = expenseDao.update(expense)

    suspend fun deleteExpense(expense: ExpenseEntity) = expenseDao.delete(expense)

    suspend fun getMember(id: Long): MemberEntity? = memberDao.getById(id)
}
