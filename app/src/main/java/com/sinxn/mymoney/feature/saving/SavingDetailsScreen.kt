package com.sinxn.mymoney.feature.saving

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sinxn.mymoney.core.data.local.model.SavingWithDetails
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.CleanListRow
import com.sinxn.mymoney.core.ui.components.FormCardContainer
import com.sinxn.mymoney.core.ui.components.groupTransactionsIntoMonthGroups
import com.sinxn.mymoney.core.ui.components.monthGroupedTransactionItems
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MoneyFormatter
import com.sinxn.mymoney.ui.theme.IncomeColor

private val InProgressColor = Color(0xFF3F51B5)
private val CompletedColor = IncomeColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingDetailsScreen(
    onNavigateBack: () -> Unit,
    onEditSavingClick: (String) -> Unit = {},
    onTransactionClick: (String) -> Unit = {},
    onDeposit: (String) -> Unit = {},
    onWithdraw: (String) -> Unit = {},
    onWithdrawEverything: (String) -> Unit = {},
    viewModel: SavingDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Saving Goal") },
            text = { Text("Do you want to delete all associated transactions or keep them in history?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteSaving(deleteTransactions = true, onSuccess = onNavigateBack)
                }) {
                    Text("Delete All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        viewModel.deleteSaving(deleteTransactions = false, onSuccess = onNavigateBack)
                    }) {
                        Text("Keep Transactions")
                    }
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    val savingDetails = uiState.savingWithDetails
    val saving = savingDetails?.saving
    val isComplete = saving?.isComplete == true || (savingDetails != null && savingDetails.isGoalReached)
    val accentColor = if (isComplete) CompletedColor else InProgressColor

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = saving?.description?.takeIf { it.isNotBlank() } ?: "Savings Goal",
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
                    if (saving != null) {
                        IconButton(onClick = { onEditSavingClick(saving.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = viewModel::toggleComplete) {
                            Icon(
                                imageVector = if (saving.isComplete) Icons.Default.Unarchive else Icons.Default.Archive,
                                contentDescription = if (saving.isComplete) "Mark In Progress" else "Mark Complete"
                            )
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
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (savingDetails == null || saving == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Savings goal not found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                val targetAmount = saving.endMoney
                val currentAmount = savingDetails.currentMoney
                val neededAmount = savingDetails.neededMoney
                val currency = savingDetails.walletCurrency

                val percentage = if (targetAmount > 0) {
                    ((currentAmount.toDouble() / targetAmount.toDouble()) * 100.0)
                } else 0.0
                val progressFraction = (percentage / 100.0).coerceIn(0.0, 1.0).toFloat()

                var collapsedGroups by remember { mutableStateOf(setOf<String>()) }
                val groupedItems = remember(
                    uiState.transactions,
                    currency,
                    uiState.formatterConfig,
                    uiState.dateFormat
                ) {
                    groupTransactionsIntoMonthGroups(
                        transactions = uiState.transactions,
                        currencyCode = currency,
                        formatterConfig = uiState.formatterConfig,
                        dateFormat = uiState.dateFormat
                    )
                }

                val targetDateLabel = remember(saving.endDate, uiState.dateFormat) {
                    saving.endDate?.let { exp ->
                        val parsed = DateUtils.parseDate(exp)
                        DateUtils.formatDate(parsed, uiState.dateFormat)
                    } ?: "No target date"
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
                                text = if (isComplete) "Goal Reached!" else "Saved So Far",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = MoneyFormatter.format(
                                    amount = currentAmount,
                                    currencyCode = currency,
                                    config = uiState.formatterConfig
                                ),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = accentColor,
                                fontSize = 34.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = accentColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (saving.isComplete) "Completed" else if (savingDetails.isGoalReached) "Goal Reached" else "In Progress",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                            }
                        }
                    }

                    // 2. Goal Progress Card
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            FormCardContainer(horizontalPadding = 0.dp) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            CategoryIcon(
                                                iconString = saving.icon.ifBlank { "ic_saving" },
                                                categoryName = saving.description ?: "Saving Goal",
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Text(
                                                text = "Goal Progress",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = accentColor.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "${percentage.toInt()}%",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = accentColor
                                            )
                                        }
                                    }

                                    LinearProgressIndicator(
                                        progress = { progressFraction },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(5.dp)),
                                        color = accentColor,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = "Saved",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = MoneyFormatter.format(amount = currentAmount, currencyCode = currency, config = uiState.formatterConfig),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = accentColor
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "Target Goal",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = MoneyFormatter.format(amount = targetAmount, currencyCode = currency, config = uiState.formatterConfig),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    // Quick deposit / withdraw buttons
                                    if (!saving.isComplete) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (savingDetails.isGoalReached) {
                                                Button(
                                                    onClick = { onWithdrawEverything(saving.id) },
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
                                                    onClick = { onDeposit(saving.id) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = CompletedColor),
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    Text("Deposit", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                }
                                                OutlinedButton(
                                                    onClick = { onWithdraw(saving.id) },
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
                    }

                    // 3. Key Metrics Cards (2-column grid)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Target Date Card
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Event,
                                            contentDescription = null,
                                            tint = accentColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Target Date",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = targetDateLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Needed Amount Card
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.TrackChanges,
                                            contentDescription = null,
                                            tint = accentColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Remaining Needed",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (neededAmount > 0) MoneyFormatter.format(amount = neededAmount, currencyCode = currency, config = uiState.formatterConfig) else "Goal Reached",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (neededAmount > 0) MaterialTheme.colorScheme.onSurface else CompletedColor
                                    )
                                }
                            }
                        }
                    }

                    // 4. Primary Details Card
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            FormCardContainer(horizontalPadding = 0.dp) {
                                Column {
                                    CleanListRow(
                                        icon = {
                                            Icon(
                                                Icons.Default.Wallet,
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        label = "Linked Wallet",
                                        value = "${savingDetails.walletName} (${savingDetails.walletCurrency})"
                                    )

                                    if (saving.startMoney > 0) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                        CleanListRow(
                                            icon = {
                                                Icon(
                                                    Icons.Default.Savings,
                                                    contentDescription = null,
                                                    tint = accentColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            },
                                            label = "Initial Starting Deposit",
                                            value = MoneyFormatter.format(amount = saving.startMoney, currencyCode = currency, config = uiState.formatterConfig)
                                        )
                                    }

                                    if (!saving.note.isNullOrBlank()) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                        CleanListRow(
                                            icon = {
                                                Icon(
                                                    Icons.Default.Notes,
                                                    contentDescription = null,
                                                    tint = accentColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            },
                                            label = "Note",
                                            value = saving.note
                                        )
                                    }

                                    if (!saving.tag.isNullOrBlank()) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                        CleanListRow(
                                            icon = {
                                                Icon(
                                                    Icons.Default.Label,
                                                    contentDescription = null,
                                                    tint = accentColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            },
                                            label = "Tag / Group",
                                            value = saving.tag
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 5. Contributing Transactions Header
                    item {
                        Text(
                            text = "Contributing Transactions (${uiState.transactions.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp)
                        )
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
                        decimals = 2,
                        currencyCode = currency,
                        formatterConfig = uiState.formatterConfig,
                        dateFormat = uiState.dateFormat,
                        emptyMessage = "No contributing transactions found."
                    )
                }
            }
        }
    }
}
