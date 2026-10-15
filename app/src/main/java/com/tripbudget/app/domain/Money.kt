package com.tripbudget.app.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/** Store and calculate money as integer paise (1 INR = 100 paise). */
object Money {
    fun rupeesToPaise(rupees: String): Long? {
        val trimmed = rupees.trim()
        if (trimmed.isEmpty()) return null
        return try {
            val value = BigDecimal(trimmed)
            if (value < BigDecimal.ZERO) return null
            value
                .movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()
        } catch (_: Exception) {
            null
        }
    }

    fun paiseToRupeesString(paise: Long): String {
        val abs = kotlin.math.abs(paise)
        val whole = abs / 100
        val fraction = abs % 100
        val sign = if (paise < 0) "-" else ""
        return "$sign$whole.${fraction.toString().padStart(2, '0')}"
    }

    fun formatInr(paise: Long): String {
        val rupees = BigDecimal(paise).movePointLeft(2)
        val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        return formatter.format(rupees)
    }
}
