package com.tripbudget.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TripTotalsCalculatorTest {
    @Test
    fun contributionTotals_andPartialPayments() {
        val totals = TripTotalsCalculator.calculate(
            plannedBudgetPaise = 10_000_00,
            memberExpected = mapOf(
                1L to ("Asha" to 5_000_00),
                2L to ("Ravi" to 5_000_00),
            ),
            contributionsByMember = mapOf(
                1L to 2_000_00,
                2L to 5_000_00,
            ),
            expenses = emptyList(),
        )
        assertEquals(7_000_00, totals.contributionsCollectedPaise)
        assertEquals(3_000_00, totals.memberBalances.first { it.memberId == 1L }.outstandingPaise)
        assertEquals(0L, totals.memberBalances.first { it.memberId == 2L }.outstandingPaise)
        assertEquals(3_000_00, totals.outstandingContributionsPaise)
    }

    @Test
    fun excessContributions_areVisible() {
        val totals = TripTotalsCalculator.calculate(
            plannedBudgetPaise = 5_000_00,
            memberExpected = mapOf(1L to ("Asha" to 1_000_00)),
            contributionsByMember = mapOf(1L to 1_500_00),
            expenses = emptyList(),
        )
        val balance = totals.memberBalances.single()
        assertTrue(balance.hasExcess)
        assertEquals(500_00, balance.excessPaise)
        assertEquals(0L, totals.outstandingContributionsPaise)
    }

    @Test
    fun sharedCash_ignoresPersonalExpenses() {
        val totals = TripTotalsCalculator.calculate(
            plannedBudgetPaise = 10_000_00,
            memberExpected = mapOf(1L to ("Asha" to 5_000_00)),
            contributionsByMember = mapOf(1L to 5_000_00),
            expenses = listOf(
                ExpenseAmount(2_000_00, PaymentSource.SHARED),
                ExpenseAmount(1_500_00, PaymentSource.PERSONAL),
            ),
        )
        assertEquals(3_500_00, totals.totalExpensesPaise)
        assertEquals(3_000_00, totals.sharedCashRemainingPaise)
        assertEquals(6_500_00, totals.budgetRemainingPaise)
    }

    @Test
    fun personalExpense_doesNotCountAsContribution() {
        val totals = TripTotalsCalculator.calculate(
            plannedBudgetPaise = 10_000_00,
            memberExpected = mapOf(1L to ("Asha" to 5_000_00)),
            contributionsByMember = emptyMap(),
            expenses = listOf(
                ExpenseAmount(1_000_00, PaymentSource.PERSONAL),
            ),
        )
        assertEquals(0L, totals.contributionsCollectedPaise)
        assertEquals(5_000_00, totals.outstandingContributionsPaise)
        assertEquals(0L, totals.sharedCashRemainingPaise)
    }

    @Test
    fun budgetRemaining_andOverspending() {
        val under = TripTotalsCalculator.calculate(
            plannedBudgetPaise = 1_000_00,
            memberExpected = emptyMap(),
            contributionsByMember = emptyMap(),
            expenses = listOf(ExpenseAmount(400_00, PaymentSource.SHARED)),
        )
        assertEquals(600_00, under.budgetRemainingPaise)
        assertFalse(under.isOverBudget)

        val over = TripTotalsCalculator.calculate(
            plannedBudgetPaise = 1_000_00,
            memberExpected = emptyMap(),
            contributionsByMember = mapOf(1L to 2_000_00),
            expenses = listOf(ExpenseAmount(1_500_00, PaymentSource.SHARED)),
        )
        assertEquals(-500_00, over.budgetRemainingPaise)
        assertTrue(over.isOverBudget)
    }

    @Test
    fun zeroBudget_handling() {
        val totals = TripTotalsCalculator.calculate(
            plannedBudgetPaise = 0L,
            memberExpected = mapOf(1L to ("Solo" to 0L)),
            contributionsByMember = mapOf(1L to 100_00),
            expenses = listOf(ExpenseAmount(50_00, PaymentSource.SHARED)),
        )
        assertEquals(-50_00, totals.budgetRemainingPaise)
        assertTrue(totals.isOverBudget)
        assertEquals(50_00, totals.sharedCashRemainingPaise)
        assertEquals(0L, totals.outstandingContributionsPaise)
    }

    @Test
    fun outstanding_sumsOnlyPositiveBalances() {
        val totals = TripTotalsCalculator.calculate(
            plannedBudgetPaise = 10_000_00,
            memberExpected = mapOf(
                1L to ("A" to 1_000_00),
                2L to ("B" to 1_000_00),
            ),
            contributionsByMember = mapOf(
                1L to 500_00,
                2L to 2_000_00,
            ),
            expenses = emptyList(),
        )
        assertEquals(500_00, totals.outstandingContributionsPaise)
    }
}
