package com.budgetnotes.app.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentCardParserTest {

    @Test
    fun parse_extractsVisaPanExpiryAndName() {
        val text = """
            VISA
            4111 1111 1111 1111
            GOOD THRU 12/27
            AHMAD KHAN
        """.trimIndent()

        val parsed = PaymentCardParser.parse(text)
        assertEquals("4111111111111111", parsed.cardNumber)
        assertEquals("12", parsed.expiryMonth)
        assertEquals("2027", parsed.expiryYear)
        assertEquals("AHMAD KHAN", parsed.cardholderName)
        assertEquals("Visa", parsed.brand)
    }

    @Test
    fun parse_prefersNameBelowPan_notBankAbove() {
        val text = """
            HABIB BANK LIMITED
            VISA Platinum
            4111 1111 1111 1111
            VALID THRU 08/28
            AHMAD RAZA
        """.trimIndent()

        val parsed = PaymentCardParser.parse(text)
        assertEquals("4111111111111111", parsed.cardNumber)
        assertEquals("AHMAD RAZA", parsed.cardholderName)
        assertEquals("08", parsed.expiryMonth)
        assertEquals("2028", parsed.expiryYear)
    }

    @Test
    fun parse_rejectsMeezanBankAsCardholder() {
        val text = """
            MEEZAN BANK
            5500 0000 0000 0004
            09/29
            SARA ALI
        """.trimIndent()

        val parsed = PaymentCardParser.parse(text)
        assertEquals("5500000000000004", parsed.cardNumber)
        assertEquals("SARA ALI", parsed.cardholderName)
    }

    @Test
    fun parse_stitchesPanSplitAcrossLines() {
        val text = """
            UNITED BANK LIMITED
            4111
            1111
            1111
            1111
            01/30
            BILAL AHMED
        """.trimIndent()

        val parsed = PaymentCardParser.parse(text)
        assertEquals("4111111111111111", parsed.cardNumber)
        assertEquals("BILAL AHMED", parsed.cardholderName)
    }

    @Test
    fun parse_normalizesOcrLetterDigitsInPan() {
        // l→1, O→0 confusions common in OCR (groups may start with letters after OCR)
        val confused = """
            HBL
            4lll llll llll llll
            12/27
            NADIA KHAN
        """.trimIndent()

        val parsed = PaymentCardParser.parse(confused)
        assertEquals("4111111111111111", parsed.cardNumber)
        assertEquals("NADIA KHAN", parsed.cardholderName)
    }

    @Test
    fun parse_extractsCvvWhenLabeled() {
        val text = "CVV: 123\n4111 1111 1111 1111"
        val parsed = PaymentCardParser.parse(text)
        assertEquals("123", parsed.cvv)
    }

    @Test
    fun normalizeOcrDigits_mapsCommonConfusions() {
        assertEquals("4111111111111111", PaymentCardParser.normalizeOcrDigits("4lll llll llll llll"))
        assertEquals("4000000000000000", PaymentCardParser.normalizeOcrDigits("4OOO OOOO OOOO OOOO"))
    }

    @Test
    fun luhnValid_acceptsKnownTestPan() {
        assertTrue(PaymentCardParser.luhnValid("4111111111111111"))
    }

    @Test
    fun maskPan_showsLastFour() {
        assertEquals("•••• 1111", PaymentCardParser.maskPan("4111111111111111"))
    }

    @Test
    fun formatPanForDisplay_groupsByFour() {
        assertEquals("4111 1111 1111 1111", PaymentCardParser.formatPanForDisplay("4111111111111111"))
    }
}
