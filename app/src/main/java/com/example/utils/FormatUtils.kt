package com.example.utils

import java.text.DecimalFormat

object FormatUtils {
    fun formatCount(count: Long): String {
        if (count < 1000) return count.toString()
        val format = DecimalFormat("#.#")
        return when {
            count < 1_000_000 -> "${format.format(count / 1000.0)}k"
            count < 1_000_000_000 -> "${format.format(count / 1_000_000.0)}M"
            else -> "${format.format(count / 1_000_000_000.0)}B"
        }
    }
    fun formatCount(count: Int): String = formatCount(count.toLong())
}
