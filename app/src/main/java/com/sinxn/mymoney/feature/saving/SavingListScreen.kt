package com.sinxn.mymoney.feature.saving

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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Unarchive
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
import com.sinxn.mymoney.core.data.local.model.SavingWithDetails
import com.sinxn.mymoney.core.ui.components.NavigationMenuPage
import com.sinxn.mymoney.core.util.MoneyFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingListScreen(
    onNavigateUp: () -> Unit,
    onSavingClick: (String) -> Unit,
    onAddSaving: () -> Unit,
    onDeposit: (savingId: String) -> Unit,
    onWithdraw: (savingId: String) -> Unit,
    onWithdrawEverything: (savingId: String) -> Unit,
    onNavigateMenuItem: (String) -> Unit = {},
    onNavigateToWallet: (String) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: SavingListViewModel = hiltViewModel()
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
                selectedItemId = "savings",
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
            SavingListContent(
                uiState = uiState,
                onNavigateUp = onNavigateUp,
                onOpenDrawer = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(1)
                    }
                },
                onTabSelected = viewModel::setSelectedTab,
                onSavingClick = onSavingClick,
                onAddSaving = onAddSaving,
                onDeposit = onDeposit,
                onWithdraw = onWithdraw,
                onWithdrawEverything = onWithdrawEverything,
                onToggleComplete = viewModel::toggleComplete,
                onDeleteSaving = viewModel::deleteSaving
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingListContent(
    uiState: SavingListUiState,
    onNavigateUp: () -> Unit,
    onOpenDrawer: () -> Unit,
    onTabSelected: (Int) -> Unit,
    onSavingClick: (String) -> Unit,
    onAddSaving: () -> Unit,
    onDeposit: (String) -> Unit,
    onWithdraw: (String) -> Unit,
    onWithdrawEverything: (String) -> Unit,
    onToggleComplete: (String, Boolean) -> Unit,
    onDeleteSaving: (String) -> Unit
) {
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Savings Goals", fontWeight = FontWeight.Bold) },
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
                TabRow(selectedTabIndex = uiState.selectedTab) {
                    Tab(
                        selected = uiState.selectedTab == 0,
                        onClick = { onTabSelected(0) },
                        text = { Text("In Progress", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = uiState.selectedTab == 1,
                        onClick = { onTabSelected(1) },
                        text = { Text("Completed", fontWeight = FontWeight.Bold) }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddSaving,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Saving Goal")
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
            } else if (uiState.savings.isEmpty()) {
                EmptySavingsView(isCompletedTab = uiState.selectedTab == 1, modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.savings, key = { it.saving.id }) { item ->
                        SavingItemCard(
                            item = item,
                            onClick = { onSavingClick(item.saving.id) },
                            onDeposit = { onDeposit(item.saving.id) },
                            onWithdraw = { onWithdraw(item.saving.id) },
                            onWithdrawEverything = { onWithdrawEverything(item.saving.id) },
                            onToggleComplete = { onToggleComplete(item.saving.id, item.saving.isComplete) },
                            onDelete = { onDeleteSaving(item.saving.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SavingItemCard(
    item: SavingWithDetails,
    onClick: () -> Unit,
    onDeposit: () -> Unit,
    onWithdraw: () -> Unit,
    onWithdrawEverything: () -> Unit,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val saving = item.saving
    val targetAmount = saving.endMoney
    val currentAmount = item.currentMoney
    val neededAmount = item.neededMoney
    val currency = item.walletCurrency

    val percentage = if (targetAmount > 0) {
        ((currentAmount.toDouble() / targetAmount.toDouble()) * 100.0).coerceIn(0.0, 100.0)
    } else 0.0

    val progressFraction = (percentage / 100.0).toFloat()

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
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Savings, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text(
                            text = saving.description ?: "Saving Goal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Wallet: ${item.walletName}",
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
                                onClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (saving.isComplete) "Mark Incomplete" else "Mark Complete") },
                            leadingIcon = { Icon(if (saving.isComplete) Icons.Default.Unarchive else Icons.Default.Archive, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onToggleComplete()
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

            // Progress bar & money amounts
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (saving.isComplete || percentage >= 100) Color(0xFF43A047) else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Saved: ${MoneyFormatter.format(currentAmount, currency)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF43A047)
                    )
                    Text(
                        text = "Goal: ${MoneyFormatter.format(targetAmount, currency)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!saving.isComplete && neededAmount > 0) {
                    Text(
                        text = "Needed: ${MoneyFormatter.format(neededAmount, currency)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Expiration Date
            saving.endDate?.let { exp ->
                Text(
                    text = "Target Date: ${exp.take(10)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Quick action buttons for active saving goals
            if (!saving.isComplete) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (neededAmount == 0L || item.isGoalReached) {
                        Button(
                            onClick = onWithdrawEverything,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Withdraw All", fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = onDeposit,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Deposit", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = onWithdraw,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Withdraw", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptySavingsView(isCompletedTab: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Savings,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Text(
            text = if (isCompletedTab) "No Completed Goals" else "No Active Savings Goals",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (isCompletedTab) "Completed goals will appear here." else "Create a savings goal to track deposits and reach target amounts.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
