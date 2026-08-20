package com.budgetnotes.app.ocr

/**
 * Parsed payment-card fields from OCR text. Empty strings mean "not found".
 */
data class ParsedPaymentFields(
    val cardNumber: String = "",
    val cardholderName: String = "",
    val expiryMonth: String = "",
    val expiryYear: String = "",
    val cvv: String = "",
    val brand: String = "",
)

/**
 * Best-effort parsing of ML Kit OCR output for credit/debit cards.
 *
 * Typical front layout (top → bottom): bank/issuer → brand → PAN → expiry → cardholder name.
 * Name extraction therefore prefers lines **below** the PAN and rejects bank/issuer labels.
 */
object PaymentCardParser {

    private val expiryRegex = Regex(
        """(?i)(?:0[1-9]|1[0-2])\s*[/\-]\s*(\d{2}|\d{4})""",
    )
    private val cvvRegex = Regex(
        """(?i)(?:cvv|cvc|cid|security\s*code)\s*[:\-]?\s*(\d{3,4})""",
    )
    /** Four digit groups often OCR'd with spaces or thin separators. */
    private val groupedPanRegex = Regex(
        """(?<!\d)((?:[\dOoIlSBZG]{4})(?:[\s\-./]*(?:[\dOoIlSBZG]{4})){2,4})(?!\d)""",
        RegexOption.IGNORE_CASE,
    )
    private val densePanRegex = Regex(
        """(?<!\d)([\dOoIlSBZG]{13,19})(?!\d)""",
        RegexOption.IGNORE_CASE,
    )

    private val bankOrIssuerTokens = setOf(
        "bank", "limited", "ltd", "plc", "corp", "corporation", "company", "co",
        "islamic", "commercial", "national", "united", "federal", "state",
        "credit", "debit", "card", "visa", "mastercard", "master", "amex",
        "american", "express", "discover", "unionpay", "rupay", "maestro",
        "platinum", "gold", "silver", "classic", "signature", "infinite",
        "world", "business", "corporate", "prepaid", "reward", "rewards",
        "member", "since", "valid", "thru", "from", "good", "expires", "expiry",
        "month", "year", "international", "worldwide", "financial", "finance",
        "services", "service", "trust", "society", "cooperative", "co-operative",
        "habib", "meezan", "allied", "askari", "faysal", "samba", "nift",
        "hbl", "ubl", "mcb", "nbp", "abl", "jsbl", "scb", "citi", "hsbc",
        "barclays", "chase", "wells", "fargo", "capital", "one",
        "standard", "chartered", "dubai", "emirates", "mashreq", "adcb",
        "al", "rajhi", "sabb", "riyad", "kuwait", "qatar", "rafidain",
        "electron", "paywave", "paypass", "contactless", "chip",
    )

    fun parse(ocrText: String): ParsedPaymentFields {
        val lines = normalizeLines(ocrText)
        val flat = lines.joinToString(" ")
        val panResult = findPan(lines, flat)
        val expiry = findExpiry(flat)
        val cvv = findCvv(ocrText)
        val name = findName(lines, panResult)
        val brand = detectBrand(panResult.digits).ifBlank {
            detectBrandFromText(flat)
        }
        return ParsedPaymentFields(
            cardNumber = panResult.digits,
            cardholderName = name,
            expiryMonth = expiry?.first.orEmpty(),
            expiryYear = expiry?.second.orEmpty(),
            cvv = cvv,
            brand = brand,
        )
    }

    private data class PanHit(
        val digits: String,
        /** Index of the line that best represents where the PAN appears; -1 if unknown. */
        val lineIndex: Int,
    )

    private fun normalizeLines(ocrText: String): List<String> {
        return ocrText
            .lines()
            .map { it.replace('\u00A0', ' ').trim() }
            .filter { it.isNotEmpty() }
    }

    private fun findPan(lines: List<String>, flat: String): PanHit {
        // 1) Prefer 4-group lines (most common plastic layout).
        val fromGroups = collectPanCandidates(lines, flat, groupedPanRegex)
        pickBestPan(fromGroups)?.let { return it }

        // 2) Dense digit runs (OCR collapsed spaces).
        val fromDense = collectPanCandidates(lines, flat, densePanRegex)
        pickBestPan(fromDense)?.let { return it }

        // 3) Stitch consecutive short digit-only lines (OCR split "4111\n1111\n1111\n1111").
        stitchDigitLines(lines)?.let { return it }

        return PanHit("", -1)
    }

    private data class RawPanCandidate(
        val raw: String,
        val lineIndex: Int,
    )

    private fun collectPanCandidates(
        lines: List<String>,
        flat: String,
        regex: Regex,
    ): List<RawPanCandidate> {
        val out = mutableListOf<RawPanCandidate>()
        lines.forEachIndexed { index, line ->
            regex.findAll(line).forEach { match ->
                out += RawPanCandidate(match.value, index)
            }
        }
        // Also scan flattened text in case PAN spans a soft line break mid-group.
        regex.findAll(flat).forEach { match ->
            out += RawPanCandidate(match.value, lineIndexForSpan(lines, match.value))
        }
        return out
    }

    private fun lineIndexForSpan(lines: List<String>, span: String): Int {
        val digits = normalizeOcrDigits(span)
        if (digits.isEmpty()) return -1
        lines.forEachIndexed { index, line ->
            val lineDigits = normalizeOcrDigits(line)
            if (lineDigits.contains(digits.take(8)) || digits.contains(lineDigits.take(8))) {
                return index
            }
        }
        return -1
    }

    private fun pickBestPan(rawCandidates: List<RawPanCandidate>): PanHit? {
        val scored = rawCandidates.mapNotNull { candidate ->
            val digits = normalizeOcrDigits(candidate.raw)
            if (digits.length !in 13..19) return@mapNotNull null
            if (!looksLikeCardPrefix(digits)) return@mapNotNull null
            val luhn = luhnValid(digits)
            // Prefer Luhn-valid; allow near-miss only for strong 16-digit Visa/MC shapes.
            val score = when {
                luhn && digits.length == 16 -> 100
                luhn && digits.length == 15 -> 95
                luhn -> 90
                digits.length == 16 && looksLikeCardPrefix(digits) -> 40
                else -> return@mapNotNull null
            }
            Triple(digits, candidate.lineIndex, score)
        }
        val best = scored.maxWithOrNull(
            compareBy<Triple<String, Int, Int>> { it.third }
                .thenBy { it.first.length },
        ) ?: return null
        return PanHit(best.first, best.second)
    }

    private fun stitchDigitLines(lines: List<String>): PanHit? {
        val groups = mutableListOf<Pair<Int, String>>() // startIndex to concatenated digits
        var i = 0
        while (i < lines.size) {
            val chunk = mutableListOf<String>()
            val start = i
            while (i < lines.size) {
                val digits = normalizeOcrDigits(lines[i])
                // A PAN group line is typically 3–5 digits (OCR may drop/add one).
                if (digits.length in 3..5 && lines[i].none { it.isLetter() && it !in "OoIlSBZsBz" }) {
                    chunk += digits
                    i++
                } else {
                    break
                }
            }
            if (chunk.size in 3..5) {
                groups += start to chunk.joinToString("")
            }
            if (chunk.isEmpty()) i++ else { /* i already advanced */ }
        }
        val candidates = groups.map { (start, digits) ->
            RawPanCandidate(digits, start)
        }
        return pickBestPan(candidates)
    }

    /** Map common OCR confusions to digits, then keep digits only. */
    fun normalizeOcrDigits(raw: String): String {
        val mapped = buildString(raw.length) {
            for (ch in raw) {
                when (ch) {
                    'O', 'o', 'D', 'Q' -> append('0')
                    'I', 'l', '|', '!', '/' -> append('1')
                    'Z', 'z' -> append('2')
                    'S', 's' -> append('5')
                    'B' -> append('8')
                    'G' -> append('6')
                    else -> if (ch.isDigit()) append(ch)
                }
            }
        }
        return mapped
    }

    private fun looksLikeCardPrefix(digits: String): Boolean {
        if (digits.isEmpty()) return false
        val p2 = digits.take(2).toIntOrNull() ?: return false
        return when {
            digits.startsWith('4') -> true // Visa
            digits.startsWith('5') -> true // Mastercard 51-55 (and some others)
            p2 in 22..27 -> true // Mastercard 2-series
            digits.startsWith("34") || digits.startsWith("37") -> true // Amex
            digits.startsWith('6') -> true // Discover / UnionPay / etc.
            digits.startsWith('3') && digits.length == 15 -> true
            else -> false
        }
    }

    private fun findExpiry(text: String): Pair<String, String>? {
        val match = expiryRegex.find(text) ?: return null
        val parts = match.value.split(Regex("""[/\-]""")).map { it.trim() }
        if (parts.size != 2) return null
        val month = parts[0].filter { it.isDigit() }.padStart(2, '0').takeLast(2)
        if (month.toIntOrNull() !in 1..12) return null
        var year = parts[1].filter { it.isDigit() }
        if (year.length == 2) year = "20$year"
        if (year.length != 4) return null
        return month to year
    }

    private fun findCvv(ocrText: String): String {
        return cvvRegex.find(ocrText)?.groupValues?.getOrNull(1).orEmpty()
    }

    private fun findName(lines: List<String>, pan: PanHit): String {
        data class NameCandidate(val text: String, val score: Int)

        val candidates = mutableListOf<NameCandidate>()
        lines.forEachIndexed { index, line ->
            val cleaned = line
                .replace(Regex("""[^A-Za-z\s'\-]"""), " ")
                .replace(Regex("""\s+"""), " ")
                .trim()
            if (cleaned.isEmpty()) return@forEachIndexed
            val words = cleaned.split(' ').filter { it.isNotEmpty() }
            if (words.size !in 2..4) return@forEachIndexed
            if (words.any { it.length < 2 }) return@forEachIndexed
            if (!words.all { word -> word.all { it.isLetter() || it == '-' || it == '\'' } }) {
                return@forEachIndexed
            }
            val lowerWords = words.map { it.lowercase() }
            if (lowerWords.any { it in bankOrIssuerTokens }) return@forEachIndexed
            if (isLikelyBankOrBrandLine(cleaned)) return@forEachIndexed

            var score = 10
            // Prefer below the PAN (cardholder is almost always under the number).
            if (pan.lineIndex >= 0) {
                when {
                    index > pan.lineIndex -> score += 50 + (index - pan.lineIndex).coerceAtMost(5)
                    index < pan.lineIndex -> score -= 30
                    else -> score -= 10
                }
            } else {
                // No PAN: prefer lower half of the card text.
                if (index >= lines.size / 2) score += 20
            }
            // Personal names are rarely ALL one short token-heavy brand phrase.
            if (words.size in 2..3) score += 5
            // Penalize lines that look like titles (all short ALL-CAPS banky words already filtered).
            if (cleaned.length > 28) score -= 5

            candidates += NameCandidate(cleaned.uppercase(), score)
        }

        return candidates.maxByOrNull { it.score }?.text.orEmpty()
    }

    private fun isLikelyBankOrBrandLine(line: String): Boolean {
        val lower = line.lowercase()
        if (bankOrIssuerTokens.any { token ->
                Regex("""\b${Regex.escape(token)}\b""").containsMatchIn(lower)
            }
        ) {
            return true
        }
        // Single-word "banks" already excluded by words.size >= 2; catch "BANK AL HABIB" etc.
        val banky = listOf("bank", "limited", "ltd", "islamic", "commercial")
        return banky.count { lower.contains(it) } >= 1 && lower.split(Regex("""\s+""")).size >= 2
    }

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

    private fun detectBrandFromText(text: String): String {
        val lower = text.lowercase()
        return when {
            "visa" in lower -> "Visa"
            "master card" in lower || "mastercard" in lower || "master" in lower -> "Mastercard"
            "american express" in lower || "amex" in lower -> "Amex"
            "discover" in lower -> "Discover"
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
