package com.budgetnotes.app.util

import com.budgetnotes.app.data.CardType
import com.budgetnotes.app.data.SavedCard
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

enum class ExpiryStatus {
    OK,
    EXPIRING_SOON,
    EXPIRED,
    UNKNOWN,
}

object CardExpiry {
    const val WARN_WITHIN_DAYS = 30L

    fun statusFor(card: SavedCard, today: LocalDate = LocalDate.now()): ExpiryStatus {
        return when (card.type) {
            CardType.PAYMENT -> statusForPayment(card.expiryMonth, card.expiryYear, today)
            CardType.ID -> statusForIdDate(card.expiryDate, today)
        }
    }

    fun statusForPayment(
        monthRaw: String,
        yearRaw: String,
        today: LocalDate = LocalDate.now(),
    ): ExpiryStatus {
        val month = monthRaw.filter { it.isDigit() }.toIntOrNull() ?: return ExpiryStatus.UNKNOWN
        val yearDigits = yearRaw.filter { it.isDigit() }
        val year = when (yearDigits.length) {
            2 -> 2000 + yearDigits.toInt()
            4 -> yearDigits.toInt()
            else -> return ExpiryStatus.UNKNOWN
        }
        if (month !in 1..12) return ExpiryStatus.UNKNOWN
        val end = YearMonth.of(year, month).atEndOfMonth()
        return statusAgainst(end, today)
    }

    fun statusForIdDate(
        raw: String,
        today: LocalDate = LocalDate.now(),
    ): ExpiryStatus {
        val end = parseIdExpiryEnd(raw) ?: return ExpiryStatus.UNKNOWN
        return statusAgainst(end, today)
    }

    fun chipLabel(status: ExpiryStatus): String? = when (status) {
        ExpiryStatus.EXPIRING_SOON -> "Expires soon"
        ExpiryStatus.EXPIRED -> "Expired"
        else -> null
    }

    /** Lower sorts first on the Cards home grid. */
    fun sortRank(status: ExpiryStatus): Int = when (status) {
        ExpiryStatus.EXPIRED -> 0
        ExpiryStatus.EXPIRING_SOON -> 1
        ExpiryStatus.OK -> 2
        ExpiryStatus.UNKNOWN -> 3
    }

    private fun statusAgainst(end: LocalDate, today: LocalDate): ExpiryStatus {
        if (today.isAfter(end)) return ExpiryStatus.EXPIRED
        val daysLeft = ChronoUnit.DAYS.between(today, end)
        return if (daysLeft <= WARN_WITHIN_DAYS) ExpiryStatus.EXPIRING_SOON else ExpiryStatus.OK
    }

    /**
     * Best-effort parse of freeform ID expiry into the last valid day.
     * Supports YYYY-MM-DD, YYYY-MM, MM/YYYY, MM/YY, YYYY/MM/DD.
     */
    fun parseIdExpiryEnd(raw: String): LocalDate? {
        val text = raw.trim()
        if (text.isEmpty()) return null

        val dateFormats = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        )
        for (fmt in dateFormats) {
            try {
                return LocalDate.parse(text, fmt)
            } catch (_: DateTimeParseException) {
            }
        }

        val yearMonthPatterns = listOf(
            Regex("""^(\d{4})[-/](\d{1,2})$"""),
            Regex("""^(\d{1,2})[-/](\d{4})$"""),
            Regex("""^(\d{1,2})[-/](\d{2})$"""),
        )
        for (pattern in yearMonthPatterns) {
            val m = pattern.matchEntire(text) ?: continue
            val (a, b) = m.groupValues[1] to m.groupValues[2]
            val (year, month) = when {
                a.length == 4 -> a.toInt() to b.toInt()
                b.length == 4 -> b.toInt() to a.toInt()
                else -> {
                    // MM/YY
                    val yy = b.toInt()
                    val yyyy = if (yy >= 70) 1900 + yy else 2000 + yy
                    yyyy to a.toInt()
                }
            }
            if (month !in 1..12) continue
            return YearMonth.of(year, month).atEndOfMonth()
        }
        return null
    }
}
