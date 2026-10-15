package com.tripbudget.app.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/** Store and calculate money as integer paise (1 INR = 100 paise). Display without decimals. */
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
        return BigDecimal(paise)
            .movePointLeft(2)
            .setScale(0, RoundingMode.HALF_UP)
            .toPlainString()
    }

    fun formatInr(paise: Long): String {
        val rupees = BigDecimal(paise)
            .movePointLeft(2)
            .setScale(0, RoundingMode.HALF_UP)
        val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }
        return formatter.format(rupees)
    }
}
