package com.sinxn.mymoney.feature.overview.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.util.MoneyFormatter
import com.sinxn.mymoney.feature.overview.CashFlowFilter
import com.sinxn.mymoney.feature.overview.GroupType
import com.sinxn.mymoney.feature.overview.OverviewData
import com.sinxn.mymoney.feature.overview.OverviewSettings
import com.sinxn.mymoney.feature.overview.OverviewType
import kotlin.math.roundToInt

private val GreenPositive = Color(0xFF2E7D32)
private val RedNegative = Color(0xFFC62828)

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

    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    ) {
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
                    ExpensesOnlyLayout(
                        expenseAmount = expenseAmount,
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
                    IncomesOnlyLayout(
                        incomeAmount = incomeAmount,
                        currencyCode = currencyCode,
                        decimals = decimals,
                        configWithSign = configWithSign,
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
                        configNoSign = configNoSign
                    )
                }
            }
        }
    }
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
    configNoSign: MoneyFormatter.Config
) {
    val isPositive = netAmount > 0
    val isNegative = netAmount < 0
    val heroColor = when {
        isPositive -> GreenPositive
        isNegative -> RedNegative
        else -> MaterialTheme.colorScheme.onSurface
    }

    // Top Header: Label + Status Pill
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "NET CASH FLOW",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 0.8.sp
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = when {
                isPositive -> GreenPositive.copy(alpha = 0.12f)
                isNegative -> RedNegative.copy(alpha = 0.12f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ) {
            Text(
                text = when {
                    isPositive -> "Surplus"
                    isNegative -> "Deficit"
                    else -> "Balanced"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = when {
                    isPositive -> GreenPositive
                    isNegative -> RedNegative
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Hero Net Amount
    Text(
        text = MoneyFormatter.format(netAmount, currencyCode, decimals, configWithSign),
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.ExtraBold,
        color = heroColor
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Split Cards: Gross Incomes & Gross Expenses
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            label = "Total Inflow",
            value = "↑ " + MoneyFormatter.format(incomeAmount, currencyCode, decimals, configNoSign),
            valueColor = GreenPositive,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Total Outflow",
            value = "↓ " + MoneyFormatter.format(expenseAmount, currencyCode, decimals, configNoSign),
            valueColor = RedNegative,
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
                    color = if (savingsRate >= 0) GreenPositive else RedNegative
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
                        .background(RedNegative.copy(alpha = 0.8f))
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Mode B: Expenses Only
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ExpensesOnlyLayout(
    expenseAmount: Long,
    currencyCode: String,
    decimals: Int,
    configNoSign: MoneyFormatter.Config,
    txCount: Int,
    periodsCount: Int,
    periodUnit: String
) {
    val avgExpense = expenseAmount / periodsCount
    val formattedAvg = MoneyFormatter.format(avgExpense, currencyCode, decimals, configNoSign)

    // Top Header
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "TOTAL EXPENSES",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 0.8.sp
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = "$txCount Transactions",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Hero Total Expense (clean positive number)
    Text(
        text = MoneyFormatter.format(expenseAmount, currencyCode, decimals, configNoSign),
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.ExtraBold,
        color = RedNegative
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Secondary Metrics Row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            label = "Burn Rate",
            value = "$formattedAvg / $periodUnit",
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
// Mode C: Incomes Only
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun IncomesOnlyLayout(
    incomeAmount: Long,
    currencyCode: String,
    decimals: Int,
    configWithSign: MoneyFormatter.Config,
    configNoSign: MoneyFormatter.Config,
    txCount: Int,
    periodsCount: Int,
    periodUnit: String
) {
    val avgIncome = incomeAmount / periodsCount
    val formattedAvg = MoneyFormatter.format(avgIncome, currencyCode, decimals, configNoSign)

    // Top Header
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "TOTAL EARNINGS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 0.8.sp
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = "$txCount Transactions",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Hero Total Income
    Text(
        text = MoneyFormatter.format(incomeAmount, currencyCode, decimals, configWithSign),
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.ExtraBold,
        color = GreenPositive
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Secondary Metrics Row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            label = "Average Inflow",
            value = "$formattedAvg / $periodUnit",
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

    // Top Header: Category Icon + Title
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            CategoryIcon(
                iconString = null,
                categoryName = categoryName,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "SPENT ON " + categoryName.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = "$txCount Txns",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Hero Category Amount
    Text(
        text = MoneyFormatter.format(totalAmount, currencyCode, decimals, configNoSign),
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.ExtraBold,
        color = if (isExpense) RedNegative else GreenPositive
    )

    Spacer(modifier = Modifier.height(14.dp))

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
