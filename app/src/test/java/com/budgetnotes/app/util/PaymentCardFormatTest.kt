package com.budgetnotes.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentCardFormatTest {
    @Test
    fun detectBrand_visaAndMastercard() {
        assertEquals("Visa", PaymentCardFormat.detectBrand("4111111111111111"))
        assertEquals("Mastercard", PaymentCardFormat.detectBrand("5555555555554444"))
        assertEquals("Mastercard", PaymentCardFormat.detectBrand("2221000000000009"))
    }

    @Test
    fun luhnValid_acceptsKnownTestPan() {
        assertTrue(PaymentCardFormat.luhnValid("4111111111111111"))
    }

    @Test
    fun maskPan_showsLastFour() {
        assertEquals("•••• 1111", PaymentCardFormat.maskPan("4111111111111111"))
    }

    @Test
    fun formatPanForDisplay_groupsByFour() {
        assertEquals("4111 1111 1111 1111", PaymentCardFormat.formatPanForDisplay("4111111111111111"))
    }
}
