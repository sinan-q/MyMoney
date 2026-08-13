package com.sinxn.mymoney.feature.recurrence

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sinxn.mymoney.R
import com.sinxn.mymoney.core.data.local.model.RecurrentTransactionWithDetails
import com.sinxn.mymoney.core.data.local.model.RecurrentTransferWithDetails
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.RecurrenceSetting

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurrenceScreen(
    onNavigateUp: () -> Unit,
    onAddRecurrentTransaction: () -> Unit,
    onAddRecurrentTransfer: () -> Unit,
    onRecurrentTransactionClick: (String) -> Unit,
    onRecurrentTransferClick: (String) -> Unit,
    viewModel: RecurrenceViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val transactions by viewModel.recurrentTransactions.collectAsState()
    val transfers by viewModel.recurrentTransfers.collectAsState()
    var itemToDelete by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // id to isTransfer

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) onAddRecurrentTransaction() else onAddRecurrentTransfer()
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Recurrent Item")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Transactions") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Transfers") }
                )
            }

            if (selectedTab == 0) {
                if (transactions.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.message_no_recurrence_found),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(transactions, key = { it.recurrentTransaction.id }) { item ->
                            RecurrentTransactionCard(
                                item = item,
                                onClick = { onRecurrentTransactionClick(item.recurrentTransaction.id) },
                                onDelete = { itemToDelete = Pair(item.recurrentTransaction.id, false) }
                            )
                        }
                    }
                }
            } else {
                if (transfers.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.message_no_recurrence_found),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(transfers, key = { it.recurrentTransfer.id }) { item ->
                            RecurrentTransferCard(
                                item = item,
                                onClick = { onRecurrentTransferClick(item.recurrentTransfer.id) },
                                onDelete = { itemToDelete = Pair(item.recurrentTransfer.id, true) }
                            )
                        }
                    }
                }
            }
        }
    }

    itemToDelete?.let { (id, isTransfer) ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete Recurrence?") },
            text = { Text("Historical transactions will be preserved without recurrence link.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (isTransfer) viewModel.deleteRecurrentTransfer(id) else viewModel.deleteRecurrentTransaction(id)
                        itemToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun RecurrentTransactionCard(
    item: RecurrentTransactionWithDetails,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val rt = item.recurrentTransaction
    val startDate = remember(rt.startDate) { DateUtils.parseDate(rt.startDate) }
    val setting = remember(rt.startDate, rt.rule) {
        RecurrenceSetting.fromStringOrFallback(startDate, rt.rule)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Repeat,
                contentDescription = null,
                tint = if (rt.direction == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rt.description?.takeIf { it.isNotBlank() } ?: item.category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${item.category.name} • ${item.wallet.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = setting.getUserReadableString(context),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                rt.nextOccurrence?.let { next ->
                    val nextDate = DateUtils.parseDate(next)
                    Text(
                        text = "Next: ${DateUtils.formatDate(nextDate, 2)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } ?: Text(
                    text = stringResource(R.string.hint_recurrence_finished),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun RecurrentTransferCard(
    item: RecurrentTransferWithDetails,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val rtf = item.recurrentTransfer
    val startDate = remember(rtf.startDate) { DateUtils.parseDate(rtf.startDate) }
    val setting = remember(rtf.startDate, rtf.rule) {
        RecurrenceSetting.fromStringOrFallback(startDate, rtf.rule)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Repeat,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.walletFrom.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(horizontal = 4.dp)
                    )
                    Text(
                        text = item.walletTo.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                rtf.description?.takeIf { it.isNotBlank() }?.let { desc ->
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = setting.getUserReadableString(context),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                rtf.nextOccurrence?.let { next ->
                    val nextDate = DateUtils.parseDate(next)
                    Text(
                        text = "Next: ${DateUtils.formatDate(nextDate, 2)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } ?: Text(
                    text = stringResource(R.string.hint_recurrence_finished),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
