package com.sinxn.mymoney.core.util

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import kotlin.math.pow

object MoneyFormatter {
    fun format(amount: Long, currencyCode: String, decimals: Int): String {
        val divider = 10.0.pow(decimals.toDouble())
        val value = amount.toDouble() / divider
        
        val format = NumberFormat.getInstance() as DecimalFormat
        format.minimumFractionDigits = decimals
        format.maximumFractionDigits = decimals
        format.isGroupingUsed = true
        
        val symbol = try {
            Currency.getInstance(currencyCode).symbol
        } catch (e: Exception) {
            currencyCode
        }
        
        return "$symbol ${format.format(value)}"
    }
}
