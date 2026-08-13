package com.sinxn.mymoney.core.util

import java.util.Locale
import kotlin.math.pow

object AmountUtils {
    /**
     * Converts a double amount (e.g. 12.34) to base units (e.g. 1234 for 2 decimals).
     */
    fun Double.toBaseUnits(decimals: Int): Long {
        val multiplier = 10.0.pow(decimals.toDouble())
        return (this * multiplier).toLong()
    }

    /**
     * Converts base units (e.g. 1234) to a formatted decimal string (e.g. "12.34").
     */
    fun Long.toDecimalString(decimals: Int): String {
        val divider = 10.0.pow(decimals.toDouble())
        val amount = this.toDouble() / divider
        return "%.${decimals}f".format(Locale.US, amount)
    }

    /**
     * Safely parses a user-entered amount string to base units Long. Returns 0L if invalid.
     */
    fun parseAmountToLong(amountStr: String, decimals: Int): Long {
        return try {
            val cleanStr = amountStr.replace(",", ".").trim()
            val value = cleanStr.toDouble()
            value.toBaseUnits(decimals)
        } catch (e: Exception) {
            0L
        }
    }
}
