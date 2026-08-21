package com.budgetnotes.app.util

/**
 * Display/validation helpers for payment card numbers (no OCR).
 */
object PaymentCardFormat {
    fun detectBrand(cardNumber: String): String {
        val digits = cardNumber.filter { it.isDigit() }
        val prefix2 = digits.take(2).toIntOrNull()
        return when {
            digits.startsWith("4") -> "Visa"
            digits.startsWith("5") -> "Mastercard"
            prefix2 != null && prefix2 in 22..27 -> "Mastercard"
            digits.startsWith("34") || digits.startsWith("37") -> "Amex"
            digits.startsWith("6") -> "Discover"
            else -> ""
        }
    }

    fun luhnValid(digits: String): Boolean {
        if (digits.any { !it.isDigit() }) return false
        if (digits.length < 13) return false
        var sum = 0
        var alternate = false
        for (i in digits.indices.reversed()) {
            var n = digits[i] - '0'
            if (alternate) {
                n *= 2
                if (n > 9) n -= 9
            }
            sum += n
            alternate = !alternate
        }
        return sum % 10 == 0
    }

    fun maskPan(cardNumber: String): String {
        val digits = cardNumber.filter { it.isDigit() }
        if (digits.length < 4) return "••••"
        return "•••• " + digits.takeLast(4)
    }

    fun formatPanForDisplay(cardNumber: String): String {
        val digits = cardNumber.filter { it.isDigit() }
        return digits.chunked(4).joinToString(" ")
    }
}
