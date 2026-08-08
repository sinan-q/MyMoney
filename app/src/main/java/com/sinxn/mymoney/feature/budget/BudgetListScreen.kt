package com.sinxn.mymoney.feature.budget

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
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
import com.sinxn.mymoney.core.ui.components.NavigationMenuPage
import com.sinxn.mymoney.core.util.MoneyFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    BackHandler(enabled = pagerState.currentPage == 1) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(0)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = true
    ) { page ->
        if (page == 1) {
            NavigationMenuPage(
                selectedItemId = "budgets",
                onReturnToMain = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(0)
                    }
                },
                onWalletSelect = { wallet ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(0)
                    }
                    onNavigateToWallet(wallet.wallet.id)
                },
                onAddWallet = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(0)
                    }
                    onNavigateToWallet("new")
                },
                onManageWallets = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(0)
                    }
                    onNavigateToSettings()
                },
                onItemClick = { item ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(0)
                    }
                    onNavigateMenuItem(item.id)
                }
            )
        } else {
            BudgetListContent(
                uiState = uiState,
                onNavigateUp = onNavigateUp,
                onOpenDrawer = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(1)
                    }
                },
                onBudgetClick = onBudgetClick,
                onAddBudget = onAddBudget,
                onEditBudget = onEditBudget,
                onDeleteBudget = viewModel::deleteBudget
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetListContent(
    uiState: BudgetListUiState,
    onNavigateUp: () -> Unit,
    onOpenDrawer: () -> Unit,
    onBudgetClick: (String) -> Unit,
    onAddBudget: () -> Unit,
    onEditBudget: (String) -> Unit = onBudgetClick,
    onDeleteBudget: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budgets", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.budgets.isEmpty()) {
                EmptyBudgetsView(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.budgets, key = { it.budget.id }) { item ->
                        BudgetItemCard(
                            item = item,
                            onClick = { onBudgetClick(item.budget.id) },
                            onEdit = { onEditBudget(item.budget.id) },
                            onDelete = { onDeleteBudget(item.budget.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetItemCard(
    item: BudgetWithDetails,
    onClick: () -> Unit,
    onEdit: () -> Unit = onClick,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val budget = item.budget
    val targetAmount = budget.money
    val progressAmount = item.progress
    val currency = budget.currency

    val percentage = if (targetAmount > 0) {
        ((progressAmount.toDouble() / targetAmount.toDouble()) * 100.0).coerceIn(0.0, 100.0)
    } else 0.0

    val progressFraction = (percentage / 100.0).toFloat()

    val (typeName, icon, iconColor) = when (budget.type) {
        0 -> Triple("Expenses", Icons.Default.TrendingDown, Color(0xFFE53935))
        1 -> Triple("Incomes", Icons.Default.TrendingUp, Color(0xFF43A047))
        else -> Triple(item.categoryName ?: "Category", Icons.Default.Category, Color(0xFF1E88E5))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(iconColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = typeName, tint = iconColor, modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text(
                            text = typeName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${budget.startDate.take(10)} - ${budget.endDate.take(10)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }


            // Progress bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (progressFraction >= 1.0f) MaterialTheme.colorScheme.error else iconColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Spent/Earned: ${MoneyFormatter.format(progressAmount, currency)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Target: ${MoneyFormatter.format(targetAmount, currency)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "${percentage.toInt()}% used",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (percentage >= 100) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Linked Wallets
            if (item.wallets.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Wallets:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    item.wallets.forEach { wallet ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = wallet.name,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyBudgetsView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.PieChart,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Text(
            text = "No Budgets Found",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Create a budget to track your spending or income targets across wallets.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
