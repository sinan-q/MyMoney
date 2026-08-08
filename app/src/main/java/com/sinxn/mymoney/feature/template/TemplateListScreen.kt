package com.sinxn.mymoney.feature.template

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sinxn.mymoney.core.data.local.model.TransactionModelWithDetails
import com.sinxn.mymoney.core.data.local.model.TransferModelWithDetails
import com.sinxn.mymoney.core.util.MoneyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateListScreen(
    onNavigateBack: () -> Unit,
    viewModel: TemplateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Transactions, 1: Transfers

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction Templates") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openCreateTxTemplateDialog(selectedTab == 1) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Template")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                if (selectedTab == 0) {
                    if (uiState.transactionTemplates.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No transaction templates saved")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.transactionTemplates, key = { it.model.id }) { item ->
                                TransactionTemplateCard(
                                    item = item,
                                    onApply = { viewModel.applyTransactionTemplate(item) },
                                    onEdit = { viewModel.openEditTxTemplateDialog(item) },
                                    onDelete = { viewModel.deleteTransactionTemplate(item) }
                                )
                            }
                        }
                    }
                } else {
                    if (uiState.transferTemplates.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No transfer templates saved")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.transferTemplates, key = { it.model.id }) { item ->
                                TransferTemplateCard(
                                    item = item,
                                    onApply = { viewModel.applyTransferTemplate(item) },
                                    onEdit = { viewModel.openEditTransferTemplateDialog(item) },
                                    onDelete = { viewModel.deleteTransferTemplate(item) }
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.isFormOpen) {
                TemplateEditDialog(
                    uiState = uiState,
                    onAmountChange = viewModel::onAmountChange,
                    onDescriptionChange = viewModel::onDescriptionChange,
                    onCategoryChange = viewModel::onCategoryChange,
                    onWalletChange = viewModel::onWalletChange,
                    onTargetWalletChange = viewModel::onTargetWalletChange,
                    onDismiss = viewModel::closeDialog,
                    onSave = viewModel::saveTemplate
                )
            }
        }
    }
}

@Composable
private fun TransactionTemplateCard(
    item: TransactionModelWithDetails,
    onApply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val formattedMoney = MoneyFormatter.format(amount = item.model.money, currencyCode = item.walletCurrency, decimals = item.walletDecimals)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.model.description.takeIf { !it.isNullOrBlank() } ?: (item.categoryName ?: "Template"),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "$formattedMoney • ${item.walletName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onApply, modifier = Modifier.padding(end = 8.dp)) {
                    Icon(Icons.Default.Check, contentDescription = "Apply")
                    Spacer(Modifier.width(4.dp))
                    Text("Apply")
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun TransferTemplateCard(
    item: TransferModelWithDetails,
    onApply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val formattedMoney = MoneyFormatter.format(amount = item.model.moneyFrom, currencyCode = item.walletFromCurrency, decimals = 2)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.model.description.takeIf { !it.isNullOrBlank() } ?: "Transfer Template",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "$formattedMoney: ${item.walletFromName} ➔ ${item.walletToName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onApply, modifier = Modifier.padding(end = 8.dp)) {
                    Icon(Icons.Default.Check, contentDescription = "Apply")
                    Spacer(Modifier.width(4.dp))
                    Text("Apply")
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun TemplateEditDialog(
    uiState: TemplateUiState,
    onAmountChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onWalletChange: (String) -> Unit,
    onTargetWalletChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (uiState.isTransferForm) "Transfer Template" else "Transaction Template") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = uiState.editAmount,
                    onValueChange = onAmountChange,
                    label = { Text("Amount") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.editDescription,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = uiState.editAmount.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
