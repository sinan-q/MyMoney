package com.sinxn.mymoney.feature.saving

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
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
import com.sinxn.mymoney.core.ui.components.AppExtendedFab
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.IconData
import com.sinxn.mymoney.core.ui.components.parseIconData
import com.sinxn.mymoney.core.ui.components.TabPill
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MoneyFormatter
import com.sinxn.mymoney.ui.theme.IncomeColor

private val InProgressColor = Color(0xFF3F51B5)
private val CompletedColor = IncomeColor

@Composable
fun SavingListScreen(
    onNavigateUp: () -> Unit,
    onSavingClick: (String) -> Unit,
    onAddSaving: () -> Unit,
    onEditSaving: (String) -> Unit = onSavingClick,
    onDeposit: (savingId: String) -> Unit,
    onWithdraw: (savingId: String) -> Unit,
    onWithdrawEverything: (savingId: String) -> Unit,
    onNavigateMenuItem: (String) -> Unit = {},
    onNavigateToWallet: (String) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: SavingListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var pendingDeleteSavingId by remember { mutableStateOf<String?>(null) }

    if (pendingDeleteSavingId != null) {
        val targetId = pendingDeleteSavingId!!
        AlertDialog(
            onDismissRequest = { pendingDeleteSavingId = null },
            title = { Text("Delete Saving Goal") },
            text = { Text("Do you want to delete all associated transactions or keep them in history?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSaving(targetId, deleteTransactions = true)
                    pendingDeleteSavingId = null
                }) {
                    Text("Delete All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        viewModel.deleteSaving(targetId, deleteTransactions = false)
                        pendingDeleteSavingId = null
                    }) {
                        Text("Keep Transactions")
                    }
                    TextButton(onClick = { pendingDeleteSavingId = null }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    val filteredSavings = remember(uiState.savings, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.savings
        } else {
            uiState.savings.filter { item ->
                val desc = item.saving.description ?: "Saving Goal"
                desc.contains(searchQuery, ignoreCase = true) ||
                        (!item.saving.note.isNullOrBlank() && item.saving.note.contains(searchQuery, ignoreCase = true)) ||
                        (!item.saving.tag.isNullOrBlank() && item.saving.tag.contains(searchQuery, ignoreCase = true)) ||
                        item.walletName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val listState = rememberLazyListState()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            AppExtendedFab(
                text = "New Goal",
                icon = Icons.Default.Add,
                onClick = onAddSaving,
                expanded = !listState.isScrollInProgress,
                containerColor = InProgressColor,
                contentColor = Color.White
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Pill Selector (In Progress vs Completed)
            TabPill(
                tabs = listOf("In Progress" to InProgressColor, "Completed" to CompletedColor),
                activeTab = uiState.selectedTab,
                onTabChange = viewModel::setSelectedTab
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Search bar (shown when > 5 items or actively searching)
            if (uiState.savings.size > 5 || searchQuery.isNotEmpty()) {
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
                                "Search savings goals...",
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
                } else if (filteredSavings.isEmpty()) {
                    EmptySavingsView(
                        isCompletedTab = uiState.selectedTab == 1,
                        isSearch = searchQuery.isNotEmpty(),
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = filteredSavings,
                            key = { it.saving.id },
                            contentType = { "saving_item" }
                        ) { item ->
                            SavingItemCard(
                                item = item,
                                formatterConfig = uiState.formatterConfig,
                                dateFormat = uiState.dateFormat,
                                onClick = { onSavingClick(item.saving.id) },
                                onEdit = { onEditSaving(item.saving.id) },
                                onDeposit = { onDeposit(item.saving.id) },
                                onWithdraw = { onWithdraw(item.saving.id) },
                                onWithdrawEverything = { onWithdrawEverything(item.saving.id) },
                                onToggleComplete = { viewModel.toggleComplete(item.saving.id, item.saving.isComplete) },
                                onDelete = { pendingDeleteSavingId = item.saving.id }
                            )
                        }
                    }
                }
            }
        }
    }
}

private val SavingCardShape = RoundedCornerShape(16.dp)

@Composable
fun SavingItemCard(
    item: SavingWithDetails,
    formatterConfig: MoneyFormatter.Config,
    dateFormat: Int,
    onClick: () -> Unit,
    onEdit: () -> Unit,
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
        ((currentAmount.toDouble() / targetAmount.toDouble()) * 100.0)
    } else 0.0

    val progressFraction = (percentage / 100.0).coerceIn(0.0, 1.0).toFloat()
    val isGoalReached = item.isGoalReached || saving.isComplete
    val baseColor = if (isGoalReached) CompletedColor else InProgressColor

    val targetDateLabel = remember(saving.endDate, dateFormat) {
        saving.endDate?.let { exp ->
            val parsed = DateUtils.parseDate(exp)
            DateUtils.formatDate(parsed, dateFormat)
        }
    }

    val iconData = remember(saving.icon, saving.description) {
        parseIconData(saving.icon.ifBlank { "ic_saving" }, saving.description ?: "Saving Goal")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = SavingCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Icon, Title, Wallet Subtitle, Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryIcon(iconData = iconData)
                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = saving.description?.takeIf { it.isNotBlank() } ?: "Saving Goal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Wallet: ${item.walletName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (targetDateLabel != null) {
                            Text(
                                text = "• Target: $targetDateLabel",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Percentage / Status Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isGoalReached) CompletedColor.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = if (isGoalReached) "100%" else "${percentage.toInt()}%",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isGoalReached) CompletedColor else MaterialTheme.colorScheme.onPrimaryContainer
                    )
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
                            text = { Text(if (saving.isComplete) "Mark In Progress" else "Mark Completed") },
                            leadingIcon = {
                                Icon(
                                    if (saving.isComplete) Icons.Default.Unarchive else Icons.Default.Archive,
                                    contentDescription = null
                                )
                            },
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

            // Progress Bar Section
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = baseColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Saved: ${MoneyFormatter.format(amount = currentAmount, currencyCode = currency, config = formatterConfig)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = baseColor
                    )
                    Text(
                        text = "Goal: ${MoneyFormatter.format(amount = targetAmount, currencyCode = currency, config = formatterConfig)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (!saving.isComplete && neededAmount > 0) {
                    Text(
                        text = "Needed: ${MoneyFormatter.format(amount = neededAmount, currencyCode = currency, config = formatterConfig)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick Actions (Deposit / Withdraw / Withdraw All)
            if (!saving.isComplete) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isGoalReached) {
                        Button(
                            onClick = onWithdrawEverything,
                            colors = ButtonDefaults.buttonColors(containerColor = CompletedColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Withdraw All", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onDeposit,
                            colors = ButtonDefaults.buttonColors(containerColor = CompletedColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Deposit", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onWithdraw,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Withdraw", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptySavingsView(
    isCompletedTab: Boolean,
    isSearch: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Savings,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Text(
            text = if (isSearch) "No Matching Savings Goals"
            else if (isCompletedTab) "No Completed Goals"
            else "No Active Savings Goals",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (isSearch) "Try a different search query"
            else if (isCompletedTab) "Goals that are fully reached or marked complete will appear here."
            else "Create a savings goal to track your progress and deposit money toward your targets.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
