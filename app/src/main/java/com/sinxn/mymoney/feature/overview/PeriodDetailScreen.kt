    package com.sinxn.mymoney.feature.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.TransactionItem
import com.sinxn.mymoney.core.ui.components.groupTransactionsIntoMonthGroups
import com.sinxn.mymoney.core.ui.components.monthGroupedTransactionItems
import com.sinxn.mymoney.core.util.MoneyFormatter

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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Period Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                                    uiState.netTotal > 0 -> Color(0xFF2E7D32)
                                    uiState.netTotal < 0 -> Color(0xFFC62828)
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
                                    color = Color(0xFF2E7D32).copy(alpha = 0.15f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.ArrowUpward,
                                            contentDescription = null,
                                            tint = Color(0xFF2E7D32),
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
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFC62828).copy(alpha = 0.15f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.ArrowDownward,
                                            contentDescription = null,
                                            tint = Color(0xFFC62828),
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
                                        color = Color(0xFFC62828)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Tabs: Incomes / Expenses / Transactions
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val tabs = listOf("Incomes", "Expenses", "Transactions")
                    tabs.forEachIndexed { index, label ->
                        SegmentedButton(
                            selected = uiState.selectedTab == index,
                            onClick = { viewModel.selectTab(index) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = tabs.size
                            )
                        ) {
                            Text(text = label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
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
                            CategoryBreakdownRow(
                                item = item,
                                barColor = Color(0xFF2E7D32)
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
                            CategoryBreakdownRow(
                                item = item,
                                barColor = Color(0xFFC62828)
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
private fun CategoryBreakdownRow(
    item: CategoryBreakdownItem,
    barColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = BreakdownCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CategoryIcon(iconData = item.iconData)

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.categoryName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.percentageFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = item.formattedAmount,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = barColor
                )
            }

            LinearProgressIndicator(
                progress = { (item.percentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(ProgressClipShape),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
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
