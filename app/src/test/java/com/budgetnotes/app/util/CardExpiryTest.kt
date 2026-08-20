package com.budgetnotes.app.util

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CardExpiryTest {
    private val today = LocalDate.of(2026, 8, 20)

    @Test
    fun payment_ok_when_more_than_30_days() {
        assertEquals(
            ExpiryStatus.OK,
            CardExpiry.statusForPayment("12", "2026", today),
        )
    }

    @Test
    fun payment_expiring_soon_within_30_days() {
        // Aug 2026 ends Aug 31 — 11 days after today (Aug 20).
        assertEquals(
            ExpiryStatus.EXPIRING_SOON,
            CardExpiry.statusForPayment("08", "2026", today),
        )
    }

    @Test
    fun payment_expired_after_month_end() {
        assertEquals(
            ExpiryStatus.EXPIRED,
            CardExpiry.statusForPayment("07", "2026", today),
        )
    }

    @Test
    fun payment_two_digit_year() {
        assertEquals(
            ExpiryStatus.EXPIRED,
            CardExpiry.statusForPayment("01", "25", today),
        )
    }

    @Test
    fun payment_blank_is_unknown() {
        assertEquals(ExpiryStatus.UNKNOWN, CardExpiry.statusForPayment("", "", today))
        assertEquals(ExpiryStatus.UNKNOWN, CardExpiry.statusForPayment("13", "2026", today))
    }

    @Test
    fun id_yyyy_mm() {
        assertEquals(
            ExpiryStatus.OK,
            CardExpiry.statusForIdDate("2030-01", today),
        )
        assertEquals(
            ExpiryStatus.EXPIRED,
            CardExpiry.statusForIdDate("2025-01", today),
        )
    }

    @Test
    fun id_yyyy_mm_dd() {
        assertEquals(
            ExpiryStatus.EXPIRING_SOON,
            CardExpiry.statusForIdDate("2026-09-01", today),
        )
        assertEquals(
            ExpiryStatus.EXPIRED,
            CardExpiry.statusForIdDate("2026-08-01", today),
        )
    }

    @Test
    fun id_mm_yyyy() {
        assertEquals(
            ExpiryStatus.OK,
            CardExpiry.statusForIdDate("12/2030", today),
        )
    }

    @Test
    fun id_unparseable_unknown() {
        assertEquals(ExpiryStatus.UNKNOWN, CardExpiry.statusForIdDate("soon", today))
        assertEquals(ExpiryStatus.UNKNOWN, CardExpiry.statusForIdDate("", today))
        assertNull(CardExpiry.parseIdExpiryEnd("n/a"))
    }

    @Test
    fun chip_and_sort_rank() {
        assertEquals("Expires soon", CardExpiry.chipLabel(ExpiryStatus.EXPIRING_SOON))
        assertEquals("Expired", CardExpiry.chipLabel(ExpiryStatus.EXPIRED))
        assertNull(CardExpiry.chipLabel(ExpiryStatus.OK))
        assertEquals(0, CardExpiry.sortRank(ExpiryStatus.EXPIRED))
        assertEquals(1, CardExpiry.sortRank(ExpiryStatus.EXPIRING_SOON))
        assertEquals(2, CardExpiry.sortRank(ExpiryStatus.OK))
    }
}
