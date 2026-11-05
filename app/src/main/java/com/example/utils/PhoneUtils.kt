package com.example.utils

data class ParsedPhone(
    val countryCode: String,
    val number: String
) {
    val fullE164: String
        get() = if (countryCode.isNotBlank() && number.isNotBlank()) "+${countryCode.removePrefix("+")}${number.trimStart('0')}" else ""

    val isValid: Boolean
        get() = countryCode.isNotBlank() && number.length >= 7
}

object PhoneUtils {
    /**
     * Parses a raw phone string into country code (e.g. "243") and clean number without leading zero (e.g. "812345678").
     */
    fun parse(rawPhone: String?, defaultCountryCode: String = "243"): ParsedPhone {
        if (rawPhone.isNullOrBlank()) {
            return ParsedPhone(countryCode = defaultCountryCode, number = "")
        }

        // Clean spaces, hyphens, parentheses
        val clean = rawPhone.trim()
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")
            .replace(".", "")

        // Case 1: Starts with '+'
        if (clean.startsWith("+")) {
            val digits = clean.substring(1)
            return when {
                digits.startsWith("243") -> ParsedPhone("243", digits.substring(3).trimStart('0'))
                digits.startsWith("242") -> ParsedPhone("242", digits.substring(3).trimStart('0'))
                digits.startsWith("237") -> ParsedPhone("237", digits.substring(3).trimStart('0'))
                digits.startsWith("225") -> ParsedPhone("225", digits.substring(3).trimStart('0'))
                digits.startsWith("221") -> ParsedPhone("221", digits.substring(3).trimStart('0'))
                digits.startsWith("33") -> ParsedPhone("33", digits.substring(2).trimStart('0'))
                digits.startsWith("32") -> ParsedPhone("32", digits.substring(2).trimStart('0'))
                digits.startsWith("1") -> ParsedPhone("1", digits.substring(1).trimStart('0'))
                digits.length > 9 -> ParsedPhone(digits.substring(0, 3), digits.substring(3).trimStart('0'))
                else -> ParsedPhone(defaultCountryCode, digits.trimStart('0'))
            }
        }

        // Case 2: Starts with "00"
        if (clean.startsWith("00")) {
            return parse("+" + clean.substring(2), defaultCountryCode)
        }

        // Case 3: Starts with explicit 243 followed by 9 digits
        if (clean.startsWith("243") && clean.length >= 11) {
            return ParsedPhone("243", clean.substring(3).trimStart('0'))
        }

        // Case 4: Starts with 0 (e.g. 0812345678 or 0991234567)
        if (clean.startsWith("0")) {
            return ParsedPhone(defaultCountryCode, clean.substring(1))
        }

        // Case 5: Direct number without leading 0 (e.g. 812345678)
        return ParsedPhone(defaultCountryCode, clean)
    }

    /**
     * Cleans up country code string (removes +, spaces, leading zeros).
     */
    fun cleanCountryCode(rawCountryCode: String, fallback: String = "243"): String {
        val clean = rawCountryCode.trim().removePrefix("+").replace(" ", "")
        return if (clean.isNotBlank()) clean else fallback
    }

    /**
     * Cleans up phone number string (removes leading zero, spaces, hyphens).
     */
    fun cleanNumber(rawNumber: String): String {
        return rawNumber.trim()
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")
            .trimStart('0')
    }
}
