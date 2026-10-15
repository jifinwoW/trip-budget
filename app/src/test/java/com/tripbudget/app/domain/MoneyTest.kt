package com.tripbudget.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {
    @Test
    fun rupeesToPaise_parsesWholeAndFractional() {
        assertEquals(100_00L, Money.rupeesToPaise("100"))
        assertEquals(100_50L, Money.rupeesToPaise("100.5"))
        assertEquals(100_55L, Money.rupeesToPaise("100.55"))
        assertEquals(0L, Money.rupeesToPaise("0"))
    }

    @Test
    fun rupeesToPaise_rejectsInvalid() {
        assertNull(Money.rupeesToPaise(""))
        assertNull(Money.rupeesToPaise("-10"))
        assertNull(Money.rupeesToPaise("abc"))
    }

    @Test
    fun paiseToRupeesString_roundTrips() {
        assertEquals("100.00", Money.paiseToRupeesString(100_00))
        assertEquals("0.50", Money.paiseToRupeesString(50))
        assertEquals("-12.34", Money.paiseToRupeesString(-1234))
    }

    @Test
    fun formatInr_containsCurrencySymbol() {
        val formatted = Money.formatInr(150_075)
        assertTrue(formatted.contains("1") || formatted.contains("₹"))
    }
}
