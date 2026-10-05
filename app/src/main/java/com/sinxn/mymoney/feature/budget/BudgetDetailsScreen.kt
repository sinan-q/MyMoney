package com.sinxn.mymoney.feature.budget

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sinxn.mymoney.core.data.repository.BudgetPeriod
import com.sinxn.mymoney.core.ui.components.AppDestructiveConfirmDialog
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.CleanListRow
import com.sinxn.mymoney.core.ui.components.FormCardContainer
import com.sinxn.mymoney.core.ui.components.groupTransactionsIntoMonthGroups
import com.sinxn.mymoney.core.ui.components.monthGroupedTransactionItems
import com.sinxn.mymoney.core.util.BudgetType
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MoneyFormatter
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDetailsScreen(
    onNavigateBack: () -> Unit,
    onEditBudgetClick: (String) -> Unit,
    onTransactionClick: (String) -> Unit = {},
    viewModel: BudgetDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                BudgetDetailsEvent.Deleted -> onNavigateBack()
            }
        }
    }

    if (showDeleteDialog) {
        AppDestructiveConfirmDialog(
            title = "Delete Budget",
            message = "Are you sure you want to delete this budget?",
            onConfirmDelete = {
                showDeleteDialog = false
                viewModel.deleteBudget(onSuccess = onNavigateBack)
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    val details = uiState.budgetWithDetails
    val budget = details?.budget

    val (typeName, baseColor, isCategoryType) = when (budget?.type) {
        BudgetType.EXPENSES -> Triple("Expenses Budget", Color(0xFFE53935), false)
        BudgetType.INCOMES -> Triple("Incomes Budget", Color(0xFF43A047), false)
        else -> Triple(details?.categoryName ?: "Category Budget", Color(0xFF1E88E5), true)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Budget Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (details != null) {
                        IconButton(onClick = { onEditBudgetClick(details.budget.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (details == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Budget not found", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                val targetAmount = budget!!.money
                val spentAmount = details.progress
                val remainingAmount = (targetAmount - spentAmount)
                val isOverBudget = spentAmount > targetAmount

                val percentage = if (targetAmount > 0) {
                    ((spentAmount.toDouble() / targetAmount.toDouble()) * 100.0)
                } else 0.0
                val progressFraction = (percentage / 100.0).coerceIn(0.0, 1.0).toFloat()

                var collapsedGroups by remember { mutableStateOf(setOf<String>()) }
                val groupedItems = remember(
                    uiState.transactions,
                    uiState.currencyDecimals,
                    uiState.currencyCode,
                    uiState.formatterConfig,
                    uiState.dateFormat
                ) {
                    groupTransactionsIntoMonthGroups(
                        transactions = uiState.transactions,
                        decimals = uiState.currencyDecimals,
                        currencyCode = uiState.currencyCode,
                        formatterConfig = uiState.formatterConfig,
                        dateFormat = uiState.dateFormat
                    )
                }

                val periodLabel = remember(budget.tag, budget.startDate, budget.endDate, uiState.dateFormat) {
                    val periodType = BudgetPeriod.fromTag(budget.tag)
                    val periodName = when (periodType) {
                        BudgetPeriod.WEEKLY -> "Weekly Auto-renew"
                        BudgetPeriod.MONTHLY -> "Monthly Auto-renew"
                        BudgetPeriod.ANNUAL -> "Annual Auto-renew"
                        else -> "Custom Fixed Dates"
                    }
                    val startObj = DateUtils.parseDate(budget.startDate)
                    val endObj = DateUtils.parseDate(budget.endDate)
                    val startStr = DateUtils.formatDate(startObj, uiState.dateFormat)
                    val endStr = DateUtils.formatDate(endObj, uiState.dateFormat)
                    val dateWindow = if (startStr == endStr) startStr else "$startStr - $endStr"
                    Pair(periodName, dateWindow)
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
                ) {
                    // 1. Centered Hero Amount Section
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isOverBudget) "Over Budget By" else "Remaining Limit",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            val formattedHeroAmount = MoneyFormatter.format(
                                amount = if (isOverBudget) (spentAmount - targetAmount) else remainingAmount,
                                currencyCode = uiState.currencyCode,
                                decimals = uiState.currencyDecimals,
                                config = uiState.formatterConfig
                            )

                            Text(
                                text = (if (isOverBudget) "+" else "") + formattedHeroAmount,
                                fontSize = 44.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isOverBudget) MaterialTheme.colorScheme.error else baseColor,
                                letterSpacing = (-1.2).sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Type Pill Chip
                            Surface(
                                shape = CircleShape,
                                color = baseColor.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, baseColor.copy(alpha = 0.25f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    if (isCategoryType) {
                                        CategoryIcon(
                                            iconString = details.categoryIcon ?: "ic_category",
                                            categoryName = details.categoryName ?: "Category",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (budget.type == BudgetType.EXPENSES) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
                                            contentDescription = null,
                                            tint = baseColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = typeName,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = baseColor
                                    )
                                }
                            }
                        }
                    }

                    // 2. Progress Card
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            FormCardContainer(horizontalPadding = 0.dp) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Budget Progress",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Text(
                                            text = "${percentage.toInt()}% Used",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isOverBudget) MaterialTheme.colorScheme.error else baseColor
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

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
                                            .clip(CircleShape),
                                        color = progressColor,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Spent: ${MoneyFormatter.format(amount = spentAmount, currencyCode = uiState.currencyCode, decimals = uiState.currencyDecimals, config = uiState.formatterConfig)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Limit: ${MoneyFormatter.format(amount = targetAmount, currencyCode = uiState.currencyCode, decimals = uiState.currencyDecimals, config = uiState.formatterConfig)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Key Metrics Cards (Days Remaining & Daily Allowance)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            BudgetMetricCard(
                                modifier = Modifier.weight(1f),
                                title = "Days Remaining",
                                value = if (uiState.daysRemaining > 0) "${uiState.daysRemaining} days left" else "Expired",
                                icon = Icons.Default.Schedule,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            BudgetMetricCard(
                                modifier = Modifier.weight(1f),
                                title = "Daily Allowance",
                                value = MoneyFormatter.format(
                                    amount = uiState.dailyAllowance,
                                    currencyCode = uiState.currencyCode,
                                    decimals = uiState.currencyDecimals,
                                    config = uiState.formatterConfig
                                ),
                                icon = Icons.Default.AccountBalance,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    // 4. Primary Details Card (Period, Dates, Associated Wallets, Tag)
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            FormCardContainer(horizontalPadding = 0.dp) {
                                Column {
                                    // Period Type Row
                                    CleanListRow(
                                        icon = {
                                            Icon(
                                                Icons.Default.Event,
                                                contentDescription = null,
                                                tint = baseColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        label = "Renewal Period",
                                        value = periodLabel.first
                                    )

                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )

                                    // Active Date Window Row
                                    CleanListRow(
                                        icon = {
                                            Icon(
                                                Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = baseColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        label = "Active Window",
                                        value = periodLabel.second
                                    )

                                    // Linked Wallets Row
                                    if (details.wallets.isNotEmpty()) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                        CleanListRow(
                                            icon = {
                                                Icon(
                                                    Icons.Default.Wallet,
                                                    contentDescription = null,
                                                    tint = baseColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            },
                                            label = "Linked Wallets",
                                            value = details.wallets.joinToString(", ") { it.name }
                                        )
                                    }

                                    // Custom Tag Row (if present and not period tag)
                                    val cleanTag = budget.tag?.takeIf { !it.startsWith("period::") }
                                    if (!cleanTag.isNullOrBlank()) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                        CleanListRow(
                                            icon = {
                                                Icon(
                                                    Icons.Default.Label,
                                                    contentDescription = null,
                                                    tint = baseColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            },
                                            label = "Tag",
                                            value = cleanTag
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 5. Contributing Transactions Header
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Contributing Transactions (${uiState.transactions.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // 6. Month-grouped Transactions List with Sticky Headers
                    monthGroupedTransactionItems(
                        monthGroups = groupedItems,
                        collapsedGroups = collapsedGroups,
                        onToggleGroup = { headerKey ->
                            collapsedGroups = if (headerKey in collapsedGroups) {
                                collapsedGroups - headerKey
                            } else {
                                collapsedGroups + headerKey
                            }
                        },
                        onTransactionClick = onTransactionClick,
                        decimals = uiState.currencyDecimals,
                        currencyCode = uiState.currencyCode,
                        formatterConfig = uiState.formatterConfig,
                        dateFormat = uiState.dateFormat,
                        emptyMessage = "No contributing transactions found in this period."
                    )
                }
            }
        }
    }
}

@Composable
fun BudgetMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
