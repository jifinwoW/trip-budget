package com.tripbudget.app.domain

data class MemberBalance(
    val memberId: Long,
    val name: String,
    val expectedPaise: Long,
    val paidPaise: Long,
) {
    val outstandingPaise: Long get() = expectedPaise - paidPaise
    val excessPaise: Long get() = if (outstandingPaise < 0) -outstandingPaise else 0L
    val hasExcess: Boolean get() = outstandingPaise < 0
}

data class TripTotals(
    val plannedBudgetPaise: Long,
    val contributionsCollectedPaise: Long,
    val totalExpensesPaise: Long,
    val sharedExpensesPaise: Long,
    val memberBalances: List<MemberBalance>,
) {
    val sharedCashRemainingPaise: Long
        get() = contributionsCollectedPaise - sharedExpensesPaise

    val budgetRemainingPaise: Long
        get() = plannedBudgetPaise - totalExpensesPaise

    val isOverBudget: Boolean
        get() = budgetRemainingPaise < 0

    /** Sum of positive member outstandings only. */
    val outstandingContributionsPaise: Long
        get() = memberBalances
            .map { it.outstandingPaise }
            .filter { it > 0 }
            .sum()
}

object TripTotalsCalculator {
    fun calculate(
        plannedBudgetPaise: Long,
        memberExpected: Map<Long, Pair<String, Long>>,
        contributionsByMember: Map<Long, Long>,
        expenses: List<ExpenseAmount>,
    ): TripTotals {
        val sharedExpenses = expenses
            .filter { it.paymentSource == PaymentSource.SHARED }
            .sumOf { it.amountPaise }
        val totalExpenses = expenses.sumOf { it.amountPaise }
        val contributionsCollected = contributionsByMember.values.sum()

        val balances = memberExpected.map { (memberId, nameAndExpected) ->
            val (name, expected) = nameAndExpected
            MemberBalance(
                memberId = memberId,
                name = name,
                expectedPaise = expected,
                paidPaise = contributionsByMember[memberId] ?: 0L,
            )
        }.sortedBy { it.name.lowercase() }

        return TripTotals(
            plannedBudgetPaise = plannedBudgetPaise,
            contributionsCollectedPaise = contributionsCollected,
            totalExpensesPaise = totalExpenses,
            sharedExpensesPaise = sharedExpenses,
            memberBalances = balances,
        )
    }
}

data class ExpenseAmount(
    val amountPaise: Long,
    val paymentSource: PaymentSource,
)
