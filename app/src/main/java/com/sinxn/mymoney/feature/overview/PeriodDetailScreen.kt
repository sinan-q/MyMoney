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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.IconData
import com.sinxn.mymoney.core.ui.components.TabPill
import com.sinxn.mymoney.core.ui.components.monthGroupedTransactionItems
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MoneyFormatter
import com.sinxn.mymoney.ui.theme.ExpenseColor
import com.sinxn.mymoney.ui.theme.IncomeColor

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodDetailScreen(
    onNavigateBack: () -> Unit,
    onTransactionClick: (String) -> Unit = {},
    viewModel: PeriodDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var collapsedGroups by rememberSaveable { mutableStateOf(setOf<String>()) }
    var expandedParentIds by rememberSaveable { mutableStateOf(setOf<String>()) }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    val incomeListState = rememberLazyListState()
    val expenseListState = rememberLazyListState()
    val transactionsListState = rememberLazyListState()

    val config = remember(uiState.formattingSettings) {
        MoneyFormatter.Config(
            showCurrency = uiState.formattingSettings.showCurrency,
            groupDigits = uiState.formattingSettings.groupDigits,
            roundDecimals = uiState.formattingSettings.roundDecimals,
            showPlusMinus = false
        )
    }

    val headerDateString = remember(uiState.startDate, uiState.endDate, uiState.formattingSettings.dateFormat) {
        val startParsed = DateUtils.parseDate(uiState.startDate)
        val endParsed = DateUtils.parseDate(uiState.endDate)
        val startFmt = DateUtils.formatDate(startParsed, uiState.formattingSettings.dateFormat)
        val endFmt = DateUtils.formatDate(endParsed, uiState.formattingSettings.dateFormat)
        
        if (uiState.startDate.isEmpty() || uiState.endDate.isEmpty()) {
            ""
        } else if (startFmt == endFmt) {
            startFmt
        } else {
            "$startFmt to $endFmt"
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val tabItems = remember(primaryColor) {
        listOf(
            "Incomes" to IncomeColor,
            "Expenses" to ExpenseColor,
            "Transactions" to primaryColor
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                            text = if (headerDateString.isNotEmpty()) "${uiState.walletName} • $headerDateString" else uiState.walletName,
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Period Summary Card
                    PeriodSummaryCard(
                        netTotal = uiState.netTotal,
                        totalIncomes = uiState.totalIncomes,
                        totalExpenses = uiState.totalExpenses,
                        currencyCode = uiState.currencyCode,
                        currencyDecimals = uiState.currencyDecimals,
                        config = config
                    )

                    // Tabs: Incomes / Expenses / Transactions
                    TabPill(
                        tabs = tabItems,
                        activeTab = pagerState.currentPage,
                        onTabChange = { index ->
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    )

                    // HorizontalPager across Incomes, Expenses, and Transactions
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        beyondViewportPageCount = 1
                    ) { page ->
                        when (page) {
                            0 -> {
                                // Incomes Category Breakdown
                                if (uiState.incomeCategories.isEmpty()) {
                                    EmptyStateMessage("No income categories recorded for this period")
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        state = incomeListState,
                                        contentPadding = PaddingValues(bottom = 24.dp)
                                    ) {
                                        items(
                                            items = uiState.incomeCategories,
                                            key = { "income_${it.categoryId ?: it.categoryName}" },
                                            contentType = { "category_breakdown" }
                                        ) { item ->
                                            val itemId = item.categoryId ?: item.categoryName
                                            CategoryBreakdownGroup(
                                                iconData = item.iconData,
                                                categoryName = item.categoryName,
                                                percentageFormatted = item.percentageFormatted,
                                                formattedAmount = item.formattedAmount,
                                                percentage = item.percentage,
                                                subcategories = item.subcategories,
                                                isExpanded = expandedParentIds.contains(itemId),
                                                barColor = IncomeColor,
                                                onToggleExpand = {
                                                    expandedParentIds = if (expandedParentIds.contains(itemId)) {
                                                        expandedParentIds - itemId
                                                    } else {
                                                        expandedParentIds + itemId
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            1 -> {
                                // Expenses Category Breakdown
                                if (uiState.expenseCategories.isEmpty()) {
                                    EmptyStateMessage("No expense categories recorded for this period")
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        state = expenseListState,
                                        contentPadding = PaddingValues(bottom = 24.dp)
                                    ) {
                                        items(
                                            items = uiState.expenseCategories,
                                            key = { "expense_${it.categoryId ?: it.categoryName}" },
                                            contentType = { "category_breakdown" }
                                        ) { item ->
                                            val itemId = item.categoryId ?: item.categoryName
                                            CategoryBreakdownGroup(
                                                iconData = item.iconData,
                                                categoryName = item.categoryName,
                                                percentageFormatted = item.percentageFormatted,
                                                formattedAmount = item.formattedAmount,
                                                percentage = item.percentage,
                                                subcategories = item.subcategories,
                                                isExpanded = expandedParentIds.contains(itemId),
                                                barColor = ExpenseColor,
                                                onToggleExpand = {
                                                    expandedParentIds = if (expandedParentIds.contains(itemId)) {
                                                        expandedParentIds - itemId
                                                    } else {
                                                        expandedParentIds + itemId
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            2 -> {
                                // Transactions list
                                if (uiState.groupedTransactions.isEmpty()) {
                                    EmptyStateMessage("No transactions recorded for this period")
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        state = transactionsListState,
                                        contentPadding = PaddingValues(bottom = 24.dp)
                                    ) {
                                        monthGroupedTransactionItems(
                                            monthGroups = uiState.groupedTransactions,
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
            }
        }
    }
}

@Composable
private fun PeriodSummaryCard(
    netTotal: Long,
    totalIncomes: Long,
    totalExpenses: Long,
    currencyCode: String,
    currencyDecimals: Int,
    config: MoneyFormatter.Config,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
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
                    netTotal > 0 -> IncomeColor
                    netTotal < 0 -> ExpenseColor
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                val configWithPlusMinus = remember(config) { config.copy(showPlusMinus = true) }
                Text(
                    text = MoneyFormatter.format(
                        netTotal,
                        currencyCode,
                        currencyDecimals,
                        configWithPlusMinus
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
                            totalIncomes,
                            currencyCode,
                            currencyDecimals,
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
                            totalExpenses,
                            currencyCode,
                            currencyDecimals,
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
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
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


                    Column(
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
                    SubcategoryBreakdownRow(
                        iconData = subItem.iconData,
                        categoryName = subItem.categoryName,
                        percentageFormatted = subItem.percentageFormatted,
                        formattedAmount = subItem.formattedAmount,
                        percentage = subItem.percentage,
                        barColor = barColor
                    )
                }
            }
        }

        HorizontalDivider(thickness = 0.3.dp)
    }
}

@Composable
private fun SubcategoryBreakdownRow(
    iconData: IconData,
    categoryName: String,
    percentageFormatted: String,
    formattedAmount: String,
    percentage: Float,
    barColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 28.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIcon(iconData = iconData)
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Bottom
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
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
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = barColor
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { (percentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
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
