package com.sinxn.mymoney.feature.overview.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.CategoryIconExtended
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MoneyFormatter
import com.sinxn.mymoney.feature.overview.CashFlowFilter
import com.sinxn.mymoney.feature.overview.FilterPill
import com.sinxn.mymoney.feature.overview.MultiCurrencyMoney
import com.sinxn.mymoney.feature.overview.OverviewSettings
import com.sinxn.mymoney.feature.overview.OverviewType
import com.sinxn.mymoney.ui.theme.ExpenseColor
import com.sinxn.mymoney.ui.theme.IncomeColor

@Composable
fun OverviewHeader(
    walletName: String,
    totalNetIncomes: MultiCurrencyMoney,
    settings: OverviewSettings?,
    formattingSettings: FormattingSettings,
    currencyCode: String,
    decimals: Int,
    selectedCategoryName: String?,
    onConfigureClick: () -> Unit,
    modifier: Modifier = Modifier,
    comparisonText: String,
    comparisonIsPostivie: Boolean,
    insightsText: String
) {
    val configWithSign = remember(formattingSettings) {
        MoneyFormatter.Config(
            showCurrency = formattingSettings.showCurrency,
            groupDigits = formattingSettings.groupDigits,
            roundDecimals = formattingSettings.roundDecimals,
            showPlusMinus = true
        )
    }

    val netAmount = totalNetIncomes.getMoney(currencyCode)
    val formattedDateRange = remember(settings?.startDate, settings?.endDate) {
        settings?.let {
            val startFormatted = DateUtils.formatMonthDayYear(settings.startDate)
            val endFormatted = DateUtils.formatMonthDayYear(settings.endDate)
            if (startFormatted == endFormatted) startFormatted else "$startFormatted - $endFormatted"
        }
    }

    val isPositive = netAmount > 0
    val isNegative = netAmount < 0
    val heroColor = when {
        isPositive -> IncomeColor
        isNegative -> ExpenseColor
        else -> MaterialTheme.colorScheme.onSurface
    }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (settings != null) {
                // Scope / Filter Badge (view-only tag)
                val filterLabel = when (settings.overviewType) {
                    OverviewType.CASH_FLOW -> when (settings.cashFlowFilter) {
                        CashFlowFilter.NET_INCOMES -> "Net Income"
                        CashFlowFilter.INCOMES -> "Incomes Only"
                        CashFlowFilter.EXPENSES -> "Expenses Only"
                    }

                    OverviewType.CATEGORY -> selectedCategoryName ?: "All"
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // View-Only Account Info (no dropdown, purely informative)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (settings.overviewType == OverviewType.CASH_FLOW) {
                            CategoryIconExtended(
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                icon = Icons.Default.SwapVert
                            )
                        } else {
                            selectedCategoryName?.let {
                                CategoryIcon(
                                    iconString = null,
                                    categoryName = it
                                )
                            }
                            Icons.Default.Category
                        }
                        Column {
                            Text(
                                text = filterLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                        }
                    }


                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    FilterPill(icon = Icons.Default.AccountBalanceWallet, text = walletName) {
                        onConfigureClick()
                    }
                    FilterPill(icon = Icons.Default.CalendarToday, text = formattedDateRange?: "Error") { }
                }


                // ── Top Row: View-Only Account Context + Settings Action ──

                val formattedAmount = MoneyFormatter.format(netAmount, currencyCode, decimals, configWithSign)
                Text(
                    text = MoneyFormatter.formatBalanceWithNonBoldDecimals(formattedAmount, ) ,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (comparisonIsPostivie) Color(0xFF2E7D32) else Color(0xFFC62828),
                        contentColor = Color.White
                    ) {
                        Text(
                            text = comparisonText.substring(0,5),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = comparisonText.substring(6),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = insightsText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        // Settings Action to open OverviewSettingsSheet
                        FilledTonalIconButton(
                            onClick = onConfigureClick,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Overview Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

            }

        }
}
