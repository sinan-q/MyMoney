package com.sinxn.mymoney.feature.overview.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
import com.sinxn.mymoney.core.util.MoneyFormatter
import com.sinxn.mymoney.feature.overview.CashFlowFilter
import com.sinxn.mymoney.feature.overview.GroupType
import com.sinxn.mymoney.feature.overview.OverviewData
import com.sinxn.mymoney.feature.overview.OverviewSettings
import com.sinxn.mymoney.feature.overview.OverviewType
import com.sinxn.mymoney.ui.theme.ExpenseColor
import com.sinxn.mymoney.ui.theme.IncomeColor
import kotlin.math.roundToInt

/**
 * Context-Adaptive summary card for the Overview screen.
 * Intelligently changes metrics, terminology, and layout depending on whether
 * Cash Flow (Net, Expenses, Incomes) or a Category drilldown is being viewed.
 */
@Composable
fun OverviewTotalSummaryCard(
    overviewData: OverviewData,
    settings: OverviewSettings?,
    currencyCode: String,
    decimals: Int,
    formattingSettings: FormattingSettings,
    selectedCategoryName: String?,
    modifier: Modifier = Modifier
) {
    val configWithSign = remember(formattingSettings) {
        MoneyFormatter.Config(
            showCurrency = formattingSettings.showCurrency,
            groupDigits = formattingSettings.groupDigits,
            roundDecimals = formattingSettings.roundDecimals,
            showPlusMinus = true
        )
    }
    val configNoSign = remember(configWithSign) {
        configWithSign.copy(showPlusMinus = false)
    }

    val netAmount = overviewData.totalNetIncomes.getMoney(currencyCode)
    val incomeAmount = overviewData.totalIncomes.getMoney(currencyCode)
    val expenseAmount = overviewData.totalExpenses.getMoney(currencyCode)
    val periodsCount = maxOf(1, overviewData.periods.size)
    val txCount = overviewData.transactionCount

    val periodUnit = when (settings?.groupType) {
        GroupType.DAILY -> "day"
        GroupType.WEEKLY -> "week"
        GroupType.MONTHLY -> "month"
        GroupType.YEARLY -> "year"
        null -> "period"
    }


        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            when {
                // ── Mode D: Category Spotlight ──
                settings?.overviewType == OverviewType.CATEGORY -> {
                    CategorySpotlightLayout(
                        categoryName = selectedCategoryName ?: "Category",
                        expenseAmount = expenseAmount,
                        incomeAmount = incomeAmount,
                        globalTotalExpenses = overviewData.globalTotalExpenses,
                        currencyCode = currencyCode,
                        decimals = decimals,
                        configNoSign = configNoSign,
                        txCount = txCount,
                        periodsCount = periodsCount,
                        periodUnit = periodUnit
                    )
                }

                // ── Mode B: Cash Flow — Expenses Only ──
                settings?.cashFlowFilter == CashFlowFilter.EXPENSES -> {
                    SingleCashFlowLayout(
                        isExpense = true,
                        amount = expenseAmount,
                        currencyCode = currencyCode,
                        decimals = decimals,
                        configNoSign = configNoSign,
                        txCount = txCount,
                        periodsCount = periodsCount,
                        periodUnit = periodUnit
                    )
                }

                // ── Mode C: Cash Flow — Incomes Only ──
                settings?.cashFlowFilter == CashFlowFilter.INCOMES -> {
                    SingleCashFlowLayout(
                        isExpense = false,
                        amount = incomeAmount,
                        currencyCode = currencyCode,
                        decimals = decimals,
                        configNoSign = configNoSign,
                        txCount = txCount,
                        periodsCount = periodsCount,
                        periodUnit = periodUnit
                    )
                }

                // ── Mode A: Cash Flow — Net Incomes (Default) ──
                else -> {
                    NetCashFlowLayout(
                        netAmount = netAmount,
                        incomeAmount = incomeAmount,
                        expenseAmount = expenseAmount,
                        currencyCode = currencyCode,
                        decimals = decimals,
                        configWithSign = configWithSign,
                        configNoSign = configNoSign,
                        txCount = txCount,
                    )
                }
            }
        }

}

@Composable
private fun Header(
    headerText: String,
    txCount: Int,
    amountText: String,
    amountColour: Color
) {

    // Hero Net Amount
    Text(
        text = amountText,
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.ExtraBold,
        color = amountColour
    )

    Spacer(modifier = Modifier.height(14.dp))
}

// ─────────────────────────────────────────────────────────────────────────────
// Mode A: Net Cash Flow
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun NetCashFlowLayout(
    netAmount: Long,
    incomeAmount: Long,
    expenseAmount: Long,
    currencyCode: String,
    decimals: Int,
    configWithSign: MoneyFormatter.Config,
    configNoSign: MoneyFormatter.Config,
    txCount: Int
) {
    val isPositive = netAmount > 0
    val isNegative = netAmount < 0
    val heroColor = when {
        isPositive -> IncomeColor
        isNegative -> ExpenseColor
        else -> MaterialTheme.colorScheme.onSurface
    }

    // Top Header: Label + Status Pill
    Header(
        headerText = "NET SPENT ",
        txCount = txCount,
        amountText = MoneyFormatter.format(netAmount, currencyCode, decimals, configWithSign),
        amountColour = heroColor,
    )

    // Split Cards: Gross Incomes & Gross Expenses
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            label = "Total Inflow",
            value = "↑ " + MoneyFormatter.format(incomeAmount, currencyCode, decimals, configNoSign),
            valueColor = IncomeColor,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Total Outflow",
            value = "↓ " + MoneyFormatter.format(expenseAmount, currencyCode, decimals, configNoSign),
            valueColor = ExpenseColor,
            modifier = Modifier.weight(1f)
        )
    }

    // Savings Rate Indicator (when income > 0)
    if (incomeAmount > 0) {
        Spacer(modifier = Modifier.height(12.dp))
        val savingsRate = ((incomeAmount - expenseAmount).toFloat() / incomeAmount) * 100f
        val spentRatio = (expenseAmount.toFloat() / incomeAmount).coerceIn(0f, 1f)

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (savingsRate >= 0) "${savingsRate.roundToInt()}% Saved" else "${(-savingsRate).roundToInt()}% Deficit",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (savingsRate >= 0) IncomeColor else ExpenseColor
                )
                Text(
                    text = "${(spentRatio * 100).roundToInt()}% Spent",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(spentRatio)
                        .clip(RoundedCornerShape(3.dp))
                        .background(ExpenseColor.copy(alpha = 0.8f))
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Mode C: SingleCashFlowLayout
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SingleCashFlowLayout(
    isExpense: Boolean,
    amount: Long,
    currencyCode: String,
    decimals: Int,
    configNoSign: MoneyFormatter.Config,
    txCount: Int,
    periodsCount: Int,
    periodUnit: String
) {
    val avgAmount = amount / periodsCount
    val formattedAvg = MoneyFormatter.format(avgAmount, currencyCode, decimals, configNoSign)

    // Top Header
    Header(
        headerText = "TOTAL" + if (isExpense) "EXPENSE" else "EARNINGS",
        txCount = txCount,
        amountText = MoneyFormatter.format(amount, currencyCode, decimals, configNoSign),
        amountColour = if (isExpense) ExpenseColor else IncomeColor,
    )

    // Secondary Metrics Row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            label = (if (isExpense) "Burn Rate" else "Average Inflow") + "/ $periodUnit",
            value = formattedAvg,
            valueColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Active Span",
            value = "$periodsCount ${periodUnit}s",
            valueColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Mode D: Category Spotlight
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CategorySpotlightLayout(
    categoryName: String,
    expenseAmount: Long,
    incomeAmount: Long,
    globalTotalExpenses: Long,
    currencyCode: String,
    decimals: Int,
    configNoSign: MoneyFormatter.Config,
    txCount: Int,
    periodsCount: Int,
    periodUnit: String
) {
    val totalAmount = if (expenseAmount > 0) expenseAmount else incomeAmount
    val isExpense = expenseAmount > 0 || incomeAmount == 0L
    val avgAmount = totalAmount / periodsCount
    val formattedAvg = MoneyFormatter.format(avgAmount, currencyCode, decimals, configNoSign)

    val sharePercent = if (isExpense && globalTotalExpenses > 0 && totalAmount > 0) {
        (totalAmount.toFloat() / globalTotalExpenses) * 100f
    } else null

    // Top Header
    Header(
        headerText = (if (isExpense) "SPENT ON " else "EARNINGS FROM") + categoryName.uppercase(),
        txCount = txCount,
        amountText = MoneyFormatter.format(totalAmount, currencyCode, decimals, configNoSign),
        amountColour = if (isExpense) ExpenseColor else IncomeColor,
    )

    // Secondary Metrics Row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            label = "Pacing",
            value = "$formattedAvg / $periodUnit",
            valueColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = if (sharePercent != null) "Share of Expenses" else "Active Span",
            value = if (sharePercent != null) "${String.format("%.1f", sharePercent)}% of total" else "$periodsCount ${periodUnit}s",
            valueColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared Helper: Reusable Mini Stat Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StatCard(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
