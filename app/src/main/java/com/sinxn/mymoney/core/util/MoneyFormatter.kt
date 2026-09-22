package com.sinxn.mymoney.core.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
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

    /**
     * Flow mode controls the sign display.
     * Matches legacy MoneyFormatter.FlowMode.
     */
    enum class FlowMode {
        FORCE_POSITIVE,
        FORCE_NEGATIVE,
        AUTO_DETECT
    }

    /**
     * Tint mode for income/expense coloring.
     * Matches legacy MoneyFormatter.TintMode.
     */
    enum class TintMode {
        AUTO_DETECT,
        INCOME,
        EXPENSE
    }

    // Default colors matching legacy behavior
    private val DEFAULT_INCOME_COLOR = Color(0xFF4CAF50)   // Green
    private val DEFAULT_EXPENSE_COLOR = Color(0xFFF44336)  // Red
    private val DEFAULT_NEUTRAL_COLOR = Color.Unspecified   // Use theme default

    /**
     * REG-07: Normalize money amounts across different decimal systems.
     * Example: JPY stores 100 as 100 (0 decimals), USD stores $1.00 as 100 (2 decimals).
     * To display JPY in a USD context, normalize(100, 0, 2) -> 10000.
     */
    fun normalize(money: Long, fromDecimals: Int, toDecimals: Int): Long {
        val offset = toDecimals - fromDecimals
        val exponential = 10.0.pow(offset.toDouble())
        return (money * exponential).toLong()
    }

    /**
     * Normalize using a decimal offset directly.
     */
    fun normalize(money: Long, decimalOffset: Int): Long {
        val exponential = 10.0.pow(decimalOffset.toDouble())
        return (money * exponential).toLong()
    }

    private val threadLocalFormat = object : ThreadLocal<DecimalFormat>() {
        override fun initialValue(): DecimalFormat {
            return NumberFormat.getInstance() as DecimalFormat
        }
    }

    private val currencySymbolCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    fun getCurrencySymbol(currencyCode: String): String {
        return currencySymbolCache.getOrPut(currencyCode) {
            try {
                Currency.getInstance(currencyCode).symbol
            } catch (e: Exception) {
                currencyCode
            }
        }
    }

    fun getCurrencyDecimals(currencyCode: String): Int {
        return try {
            val digits = Currency.getInstance(currencyCode).defaultFractionDigits
            if (digits >= 0) digits else 2
        } catch (e: Exception) {
            2
        }
    }

    fun format(
        amount: Long, 
        currencyCode: String, 
        decimals: Int = 2, 
        config: Config = Config()
    ): String {
        val divider = 10.0.pow(decimals.toDouble())
        val value = amount.toDouble() / divider
        
        val format = threadLocalFormat.get() ?: (NumberFormat.getInstance() as DecimalFormat)
        // Round decimals: if true, show 0 fraction digits. Else usage passed decimals.
        val fractions = if (config.roundDecimals) 0 else decimals
        
        if (format.minimumFractionDigits != fractions) {
            format.minimumFractionDigits = fractions
        }
        if (format.maximumFractionDigits != fractions) {
            format.maximumFractionDigits = fractions
        }
        if (format.isGroupingUsed != config.groupDigits) {
            format.isGroupingUsed = config.groupDigits
        }
        
        var formattedValue = format.format(value)

        // Handle Plus/Minus
        if (config.showPlusMinus && value > 0) {
            formattedValue = "+$formattedValue"
        }
        
        if (config.showCurrency) {
            val symbol = getCurrencySymbol(currencyCode)
            return "$symbol $formattedValue"
        }

        return formattedValue
    }

    fun format(
        amount: Long, 
        currencyCode: String, 
        decimals: Int = 2, 
        config: FormattingSettings
    ): String = format(
        amount = amount,
        currencyCode = currencyCode,
        decimals = decimals,
        config = Config(
            showCurrency = config.showCurrency,
            groupDigits = config.groupDigits,
            roundDecimals = config.roundDecimals,
            showPlusMinus = config.showPlusMinus
        )
    )

    /**
     * REG-08: Format money with income/expense coloring for Compose.
     * Returns an AnnotatedString with appropriate ForegroundColorSpan equivalent.
     */
    fun formatColored(
        amount: Long,
        currencyCode: String,
        decimals: Int = 2,
        config: Config = Config(),
        tintMode: TintMode = TintMode.AUTO_DETECT,
        incomeColor: Color = DEFAULT_INCOME_COLOR,
        expenseColor: Color = DEFAULT_EXPENSE_COLOR,
        neutralColor: Color = DEFAULT_NEUTRAL_COLOR
    ): AnnotatedString {
        val text = format(amount, currencyCode, decimals, config)
        
        val color = when (tintMode) {
            TintMode.INCOME -> incomeColor
            TintMode.EXPENSE -> expenseColor
            TintMode.AUTO_DETECT -> when {
                amount > 0 -> incomeColor
                amount < 0 -> expenseColor
                else -> neutralColor
            }
        }

        return buildAnnotatedString {
            if (color != Color.Unspecified) {
                withStyle(SpanStyle(color = color)) {
                    append(text)
                }
            } else {
                append(text)
            }
        }
    }

    fun formatColored(
        amount: Long,
        currencyCode: String,
        decimals: Int = 2,
        config: FormattingSettings,
        tintMode: TintMode = TintMode.AUTO_DETECT,
        incomeColor: Color = DEFAULT_INCOME_COLOR,
        expenseColor: Color = DEFAULT_EXPENSE_COLOR,
        neutralColor: Color = DEFAULT_NEUTRAL_COLOR
    ): AnnotatedString = formatColored(
        amount = amount,
        currencyCode = currencyCode,
        decimals = decimals,
        config = Config(
            showCurrency = config.showCurrency,
            groupDigits = config.groupDigits,
            roundDecimals = config.roundDecimals,
            showPlusMinus = config.showPlusMinus
        ),
        tintMode = tintMode,
        incomeColor = incomeColor,
        expenseColor = expenseColor,
        neutralColor = neutralColor
    )
    fun formatBalanceWithNonBoldDecimals(
        formattedBalance: String,
        baseWeight: FontWeight = FontWeight.Black,
        decimalWeight: FontWeight = FontWeight.Normal
    ): AnnotatedString {
        val decimalSeparator = try {
            (NumberFormat.getInstance() as? DecimalFormat)?.decimalFormatSymbols?.decimalSeparator ?: '.'
        } catch (e: Exception) {
            '.'
        }

        val decimalIndex = formattedBalance.lastIndexOf(decimalSeparator)
        if (decimalIndex == -1) {
            return buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = baseWeight)) {
                    append(formattedBalance)
                }
            }
        }

        return buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = baseWeight)) {
                append(formattedBalance.substring(0, decimalIndex))
            }
            withStyle(SpanStyle(fontWeight = decimalWeight, fontSize = 0.65.em)) {
                append(formattedBalance.substring(decimalIndex))
            }
        }
    }
}

