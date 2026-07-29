package com.budgetnotes.app.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

object MoneyFormat {
    private const val SCALE = 2
    private val hundred = BigDecimal(100)

    fun formatMinor(amountMinor: Long, locale: Locale = Locale.getDefault()): String {
        val value = BigDecimal(amountMinor).divide(hundred, SCALE, RoundingMode.HALF_UP)
        return NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = SCALE
            maximumFractionDigits = SCALE
        }.format(value)
    }

    /**
     * Parses a positive decimal amount string into minor units.
     * Returns null if invalid or not strictly positive.
     */
    fun parseToMinor(input: String): Long? {
        val trimmed = input.trim().replace(',', '.')
        if (trimmed.isEmpty()) return null
        return try {
            val value = BigDecimal(trimmed).setScale(SCALE, RoundingMode.HALF_UP)
            if (value <= BigDecimal.ZERO) null
            else value.multiply(hundred).longValueExact()
        } catch (_: Exception) {
            null
        }
    }
}
