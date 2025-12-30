package com.sinxn.mymoney.core.util

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import kotlin.math.pow

object MoneyFormatter {
    data class Config(
        val showCurrency: Boolean = true,
        val groupDigits: Boolean = true,
        val roundDecimals: Boolean = false,
        val showPlusMinus: Boolean = false
    )

    fun format(
        amount: Long, 
        currencyCode: String, 
        decimals: Int, 
        config: Config = Config()
    ): String {
        val divider = 10.0.pow(decimals.toDouble())
        val value = amount.toDouble() / divider
        
        val format = NumberFormat.getInstance() as DecimalFormat
        // Round decimals: if true, show 0 fraction digits. Else usage passed decimals.
        val fractions = if (config.roundDecimals) 0 else decimals
        
        format.minimumFractionDigits = fractions
        format.maximumFractionDigits = fractions
        format.isGroupingUsed = config.groupDigits
        
        var formattedValue = format.format(value)
        
        // Handle Plus/Minus
        if (config.showPlusMinus && value > 0) {
            formattedValue = "+$formattedValue"
        }
        
        if (config.showCurrency) {
             val symbol = try {
                Currency.getInstance(currencyCode).symbol
            } catch (e: Exception) {
                currencyCode
            }
            return "$symbol $formattedValue"
        }
        
        return formattedValue
    }
}
