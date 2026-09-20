    package com.sinxn.mymoney.feature.overview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.IconData
import com.sinxn.mymoney.core.ui.components.TabPill
import com.sinxn.mymoney.core.ui.components.groupTransactionsIntoMonthGroups
import com.sinxn.mymoney.core.ui.components.monthGroupedTransactionItems
import com.sinxn.mymoney.core.util.MoneyFormatter
import com.sinxn.mymoney.ui.theme.ExpenseColor
import com.sinxn.mymoney.ui.theme.IncomeColor
import org.dmfs.jems2.iterable.Expanded

    @OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodDetailScreen(
    onNavigateBack: () -> Unit,
    onTransactionClick: (String) -> Unit = {},
    viewModel: PeriodDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var collapsedGroups by remember { mutableStateOf(setOf<String>()) }

    val config = remember(uiState.formattingSettings) {
        MoneyFormatter.Config(
            showCurrency = uiState.formattingSettings.showCurrency,
            groupDigits = uiState.formattingSettings.groupDigits,
            roundDecimals = uiState.formattingSettings.roundDecimals,
            showPlusMinus = false
        )
    }

    val groupedItems = remember(
        uiState.transactions,
        uiState.currencyDecimals,
        uiState.currencyCode,
        config,
        uiState.formattingSettings.dateFormat
    ) {
        groupTransactionsIntoMonthGroups(
            transactions = uiState.transactions,
            decimals = uiState.currencyDecimals,
            currencyCode = uiState.currencyCode,
            formatterConfig = config,
            dateFormat = uiState.formattingSettings.dateFormat
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0,0,0,0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Period Details",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${uiState.walletName} • ${uiState.startDate.take(10)} to ${uiState.endDate.take(10)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // Period Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Net Flow",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                val netColor = when {
                                    uiState.netTotal > 0 -> IncomeColor
                                    uiState.netTotal < 0 -> ExpenseColor
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                Text(
                                    text = MoneyFormatter.format(
                                        uiState.netTotal,
                                        uiState.currencyCode,
                                        uiState.currencyDecimals,
                                        config.copy(showPlusMinus = true)
                                    ),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = netColor
                                )
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = IncomeColor.copy(alpha = 0.15f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.ArrowUpward,
                                            contentDescription = null,
                                            tint = IncomeColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Incomes",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = MoneyFormatter.format(
                                            uiState.totalIncomes,
                                            uiState.currencyCode,
                                            uiState.currencyDecimals,
                                            config
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = IncomeColor

                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = ExpenseColor.copy(alpha = 0.15f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.ArrowDownward,
                                            contentDescription = null,
                                            tint = ExpenseColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Expenses",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = MoneyFormatter.format(
                                            uiState.totalExpenses,
                                            uiState.currencyCode,
                                            uiState.currencyDecimals,
                                            config
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ExpenseColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Tabs: Incomes / Expenses / Transactions
            item {
                TabPill(
                    tabs = listOf("Incomes", "Expenses", "Transactions").map { it to MaterialTheme.colorScheme.primary },
                    activeTab = uiState.selectedTab,
                ) {
                    viewModel.selectTab(it)
                }
            }

            // Content based on tab
            when (uiState.selectedTab) {
                0 -> {
                    // Incomes Category Breakdown
                    if (uiState.incomeCategories.isEmpty()) {
                        item {
                            EmptyStateMessage("No income categories recorded for this period")
                        }
                    } else {
                        items(
                            items = uiState.incomeCategories,
                            key = { it.categoryId ?: it.categoryName },
                            contentType = { "category_breakdown" }
                        ) { item ->
                            CategoryBreakdownGroup(
                                barColor = Color(0xFF2E7D32),
                                onToggleExpand = {
                                    viewModel.toggleParentExpanded(
                                        item.categoryId ?: item.categoryName
                                    )
                                },
                                iconData = item.iconData,
                                categoryName = item.categoryName,
                                percentageFormatted = item.percentageFormatted,
                                formattedAmount = item.formattedAmount,
                                percentage = item.percentage,
                                subcategories = item.subcategories,
                                isExpanded = item.isExpanded
                            )
                        }
                    }
                }
                1 -> {
                    // Expenses Category Breakdown
                    if (uiState.expenseCategories.isEmpty()) {
                        item {
                            EmptyStateMessage("No expense categories recorded for this period")
                        }
                    } else {
                        items(
                            items = uiState.expenseCategories,
                            key = { it.categoryId ?: it.categoryName },
                            contentType = { "category_breakdown" }
                        ) { item ->
                            CategoryBreakdownGroup(
                                iconData = item.iconData,
                                categoryName = item.categoryName,
                                percentageFormatted = item.percentageFormatted,
                                formattedAmount = item.formattedAmount,
                                percentage = item.percentage,
                                subcategories = item.subcategories,
                                isExpanded = item.isExpanded,
                                barColor = Color(0xFFC62828),
                                onToggleExpand = {
                                    viewModel.toggleParentExpanded(item.categoryId ?: item.categoryName)
                                }
                            )
                        }
                    }
                }
                2 -> {
                    // Transactions list
                    if (uiState.transactions.isEmpty()) {
                        item {
                            EmptyStateMessage("No transactions recorded for this period")
                        }
                    } else {
                        monthGroupedTransactionItems(
                            monthGroups = groupedItems,
                            collapsedGroups = collapsedGroups,
                            onToggleGroup = { groupKey ->
                                collapsedGroups = if (collapsedGroups.contains(groupKey)) {
                                    collapsedGroups - groupKey
                                } else {
                                    collapsedGroups + groupKey
                                }
                            },
                            onTransactionClick = onTransactionClick,
                            decimals = uiState.currencyDecimals,
                            currencyCode = uiState.currencyCode,
                            formatterConfig = config,
                            dateFormat = uiState.formattingSettings.dateFormat
                        )
                    }
                }
            }
        }
    }
}

private val BreakdownCardShape = RoundedCornerShape(14.dp)
private val ProgressClipShape = RoundedCornerShape(3.dp)

@Composable
private fun CategoryBreakdownGroup(
    iconData: IconData,
    categoryName: String,
    percentageFormatted: String,
    formattedAmount: String,
    percentage: Float,
    subcategories: List<SubcategoryBreakdownItem> = emptyList(),
    isExpanded: Boolean,
    barColor: Color,
    onToggleExpand: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIcon(iconData = iconData)
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Bottom
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(end = 8.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,) {
                            Row( verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = categoryName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = percentageFormatted,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = formattedAmount,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = barColor
                            )
                        }

                        LinearProgressIndicator(
                            progress = { (percentage / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(ProgressClipShape),
                            color = barColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }


                    Column (
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.size(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (subcategories.isNotEmpty()) {
                            IconButton(
                                onClick = onToggleExpand
                            ) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isExpanded && subcategories.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                subcategories.forEach { subItem ->
                    Column(Modifier.padding(start = 12.dp)) {
                        CategoryBreakdownGroup(
                            iconData = subItem.iconData,
                            categoryName = subItem.categoryName,
                            percentageFormatted = subItem.percentageFormatted,
                            formattedAmount = subItem.formattedAmount,
                            percentage = subItem.percentage,
                            isExpanded = false,
                            barColor = barColor,
                        ) { }
                    }
                }
            }
        }

        HorizontalDivider(thickness = 0.3.dp)
    }
}

@Composable
private fun EmptyStateMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
