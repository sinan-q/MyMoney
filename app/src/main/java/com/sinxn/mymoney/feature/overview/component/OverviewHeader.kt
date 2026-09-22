package com.sinxn.mymoney.feature.overview.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCbrt
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.CategoryIconExtended
import com.sinxn.mymoney.core.ui.components.EditAmountHeader
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MoneyFormatter
import com.sinxn.mymoney.feature.overview.CashFlowFilter
import com.sinxn.mymoney.feature.overview.GroupType
import com.sinxn.mymoney.feature.overview.OverviewData
import com.sinxn.mymoney.feature.overview.OverviewSettings
import com.sinxn.mymoney.feature.overview.OverviewType
import com.sinxn.mymoney.ui.theme.ExpenseColor
import com.sinxn.mymoney.ui.theme.IncomeColor

@Composable
fun OverviewHeader(
    walletName: String,
    overviewData: OverviewData,
    settings: OverviewSettings?,
    formattingSettings: FormattingSettings,
    currencyCode: String,
    decimals: Int,
    selectedCategoryName: String?,
    onConfigureClick: () -> Unit,
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
    val txCount = overviewData.transactionCount
    val formattedDateRange = remember(settings?.startDate, settings?.endDate) {
        settings?.let {
            val startFormatted = DateUtils.formatMonthDayYear(settings.startDate)
            val endFormatted = DateUtils.formatMonthDayYear(settings.endDate)
            if (startFormatted == endFormatted) startFormatted else "$startFormatted - $endFormatted"
        }
    }
    val periodUnit = when (settings?.groupType) {
        GroupType.DAILY -> "day"
        GroupType.WEEKLY -> "week"
        GroupType.MONTHLY -> "month"
        GroupType.YEARLY -> "year"
        null -> "period"
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
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    // Date Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formattedDateRange?: "Error",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }



                // ── Top Row: View-Only Account Context + Settings Action ──
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
                            // ── View-Only Date Range ──

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
                
                Text(
                    text = MoneyFormatter.format(netAmount, currencyCode, decimals, configWithSign),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = heroColor
                )
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Grouping Badge (view-only tag)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = 8.dp,
                                    vertical = 4.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = settings.groupType.name.lowercase()
                                        .replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = 8.dp,
                                    vertical = 4.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = walletName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,

                                    )
                            }
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
                                modifier = Modifier.padding(
                                    horizontal = 8.dp,
                                    vertical = 3.dp
                                )
                            )
                        }
                    }
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
