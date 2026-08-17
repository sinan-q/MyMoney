package com.sinxn.mymoney.feature.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sinxn.mymoney.core.data.local.model.BudgetWithDetails
import com.sinxn.mymoney.core.data.repository.BudgetPeriod
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.util.BudgetType
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MoneyFormatter

@Composable
fun BudgetListScreen(
    onNavigateUp: () -> Unit,
    onBudgetClick: (String) -> Unit,
    onAddBudget: () -> Unit,
    onEditBudget: (String) -> Unit = onBudgetClick,
    onNavigateMenuItem: (String) -> Unit = {},
    onNavigateToWallet: (String) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: BudgetListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filteredBudgets = remember(uiState.budgets, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.budgets
        } else {
            uiState.budgets.filter { item ->
                val typeName = when (item.budget.type) {
                    BudgetType.EXPENSES -> "Expenses"
                    BudgetType.INCOMES -> "Incomes"
                    BudgetType.CATEGORY -> item.categoryName ?: "Category"
                    else -> "Budget"
                }
                typeName.contains(searchQuery, ignoreCase = true) ||
                        (!item.categoryName.isNullOrBlank() && item.categoryName.contains(searchQuery, ignoreCase = true)) ||
                        (!item.budget.tag.isNullOrBlank() && item.budget.tag.contains(searchQuery, ignoreCase = true)) ||
                        item.wallets.any { it.name.contains(searchQuery, ignoreCase = true) }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddBudget,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Budget")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search field (shown when > 5 budgets or actively searching)
            if (uiState.budgets.size > 5 || searchQuery.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                "Search budgets...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        } else null,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (filteredBudgets.isEmpty()) {
                    EmptyBudgetsView(
                        isSearch = searchQuery.isNotEmpty(),
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredBudgets, key = { it.budget.id }) { item ->
                            BudgetItemCard(
                                item = item,
                                formatterConfig = uiState.formatterConfig,
                                dateFormat = uiState.dateFormat,
                                onClick = { onBudgetClick(item.budget.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetItemCard(
    item: BudgetWithDetails,
    formatterConfig: MoneyFormatter.Config,
    dateFormat: Int,
    onClick: () -> Unit
) {
    val budget = item.budget
    val targetAmount = budget.money
    val progressAmount = item.progress
    val currency = budget.currency

    val percentage = if (targetAmount > 0) {
        ((progressAmount.toDouble() / targetAmount.toDouble()) * 100.0)
    } else 0.0

    val progressFraction = (percentage / 100.0).coerceIn(0.0, 1.0).toFloat()
    val isOverBudget = percentage >= 100.0

    val (typeName, iconView, baseColor) = when (budget.type) {
        BudgetType.EXPENSES -> Triple(
            "Expenses",
            @Composable {
                Icon(
                    Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(24.dp)
                )
            },
            Color(0xFFE53935)
        )
        BudgetType.INCOMES -> Triple(
            "Incomes",
            @Composable {
                Icon(
                    Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = Color(0xFF43A047),
                    modifier = Modifier.size(24.dp)
                )
            },
            Color(0xFF43A047)
        )
        else -> Triple(
            item.categoryName ?: "Category",
            @Composable {
                CategoryIcon(
                    iconString = item.categoryIcon ?: "ic_category",
                    categoryName = item.categoryName ?: "Category",
                    modifier = Modifier.size(44.dp)
                )
            },
            Color(0xFF1E88E5)
        )
    }

    val periodLabel = remember(budget.tag, budget.startDate, budget.endDate, dateFormat) {
        val periodType = BudgetPeriod.fromTag(budget.tag)
        val periodName = when (periodType) {
            BudgetPeriod.WEEKLY -> "Weekly"
            BudgetPeriod.MONTHLY -> "Monthly"
            BudgetPeriod.ANNUAL -> "Annual"
            else -> "Custom"
        }
        val startObj = DateUtils.parseDate(budget.startDate)
        val endObj = DateUtils.parseDate(budget.endDate)
        val startStr = DateUtils.formatDate(startObj, dateFormat)
        val endStr = DateUtils.formatDate(endObj, dateFormat)
        val dateWindow = if (startStr == endStr) startStr else "$startStr - $endStr"
        if (periodType == BudgetPeriod.CUSTOM) dateWindow else "$periodName • $dateWindow"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Avatar, Title, Period Subtitle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(baseColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    iconView()
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = typeName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = periodLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Status / percentage badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isOverBudget) MaterialTheme.colorScheme.errorContainer
                    else if (percentage >= 80) Color(0xFFFFE082)
                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "${percentage.toInt()}%",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverBudget) MaterialTheme.colorScheme.onErrorContainer
                        else if (percentage >= 80) Color(0xFFE65100)
                        else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Progress Bar Section
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val progressColor = when {
                    isOverBudget -> MaterialTheme.colorScheme.error
                    percentage >= 80 -> Color(0xFFFB8C00)
                    else -> baseColor
                }

                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Spent: ${MoneyFormatter.format(amount = progressAmount, currencyCode = currency, config = formatterConfig)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Limit: ${MoneyFormatter.format(amount = targetAmount, currencyCode = currency, config = formatterConfig)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Linked Wallets
            if (item.wallets.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Wallets:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    item.wallets.take(3).forEach { wallet ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = wallet.name,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1
                            )
                        }
                    }
                    if (item.wallets.size > 3) {
                        Text(
                            text = "+${item.wallets.size - 3}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyBudgetsView(
    isSearch: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.PieChart,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Text(
            text = if (isSearch) "No Matching Budgets" else "No Budgets Found",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (isSearch) "Try a different search term"
            else "Create a budget to track your spending limits or income targets across wallets.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
