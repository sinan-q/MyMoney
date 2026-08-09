package com.sinxn.mymoney.feature.debt

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.util.MoneyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtDetailsScreen(
    onNavigateBack: () -> Unit,
    onRecordPayment: (debtId: String, walletId: String, debtAction: String) -> Unit = { _, _, _ -> },
    viewModel: DebtDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val formatterConfig = remember(uiState) {
        MoneyFormatter.Config(
            showCurrency = true,
            groupDigits = true,
            roundDecimals = false,
            showPlusMinus = false
        )
    }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isNewDebt) {
                            if (uiState.editType == 0) "New Debt" else "New Credit"
                        } else if (uiState.isEditMode) {
                            "Edit Debt"
                        } else {
                            uiState.debtDetails?.debt?.description?.ifBlank { "Debt Details" } ?: "Debt Details"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!uiState.isNewDebt) {
                        if (!uiState.isEditMode) {
                            IconButton(onClick = { viewModel.setEditMode(true) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = viewModel::toggleArchived) {
                                Icon(
                                    imageVector = if (uiState.debtDetails?.debt?.isArchived == true) Icons.Default.Unarchive else Icons.Default.Archive,
                                    contentDescription = "Archive"
                                )
                            }
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Debt") },
                text = { Text("Do you want to delete all associated transactions or keep them in history?") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        viewModel.deleteDebt(deleteTransactions = true, onSuccess = onNavigateBack)
                    }) {
                        Text("Delete All", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = {
                            showDeleteDialog = false
                            viewModel.deleteDebt(deleteTransactions = false, onSuccess = onNavigateBack)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.isEditMode || uiState.isNewDebt) {
                // Form Mode (Create / Edit)
                DebtFormContent(
                    uiState = uiState,
                    onTypeChange = viewModel::updateType,
                    onDescriptionChange = viewModel::updateDescription,
                    onAmountChange = viewModel::updateAmount,
                    onWalletChange = viewModel::updateWalletId,
                    onPlaceChange = viewModel::updatePlaceId,
                    onDateChange = viewModel::updateDate,
                    onExpirationDateChange = viewModel::updateExpirationDate,
                    onNoteChange = viewModel::updateNote,
                    onPersonToggle = viewModel::togglePersonSelection,
                    onInsertMasterTxChange = viewModel::updateInsertMasterTransaction,
                    onSave = { viewModel.saveDebt { onNavigateBack() } }
                )
            } else {
                // View Mode
                val debtDetails = uiState.debtDetails
                if (debtDetails != null) {
                    DebtViewContent(
                        debtDetails = debtDetails,
                        transactions = uiState.transactions,
                        formatterConfig = formatterConfig,
                        currencyCode = uiState.currencyCode,
                        onRecordPaymentClick = {
                            onRecordPayment(
                                debtDetails.debt.id,
                                debtDetails.debt.walletId,
                                if (debtDetails.debt.type == 0) "PAY" else "COLLECT"
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DebtViewContent(
    debtDetails: com.sinxn.mymoney.core.data.local.model.DebtWithDetails,
    transactions: List<TransactionWithCategory>,
    formatterConfig: MoneyFormatter.Config,
    currencyCode: String,
    onRecordPaymentClick: () -> Unit
) {
    val debt = debtDetails.debt
    val remaining = debtDetails.remainingMoney
    val totalMoney = debt.money
    val isFullyPaid = remaining == 0L && totalMoney > 0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (debt.type == 0) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (debt.type == 0) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = if (debt.type == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = if (debt.type == 0) "I Owe (Debt)" else "Owed to Me (Credit)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = debt.description.ifBlank { "Debt" },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Remaining Balance",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = MoneyFormatter.format(amount = remaining, currencyCode = debtDetails.walletCurrency, decimals = debtDetails.walletDecimals, config = formatterConfig),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (totalMoney > 0) {
                        val progressFraction = (kotlin.math.abs(debtDetails.progress).toFloat() / totalMoney.toFloat()).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Total: ${MoneyFormatter.format(amount = totalMoney, currencyCode = debtDetails.walletCurrency, decimals = debtDetails.walletDecimals, config = formatterConfig)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (!isFullyPaid) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onRecordPaymentClick,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (debt.type == 0) "Record Repayment" else "Record Collection")
                        }
                    }
                }
            }
        }

        // Information Details Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailRow(icon = Icons.Default.Event, label = "Date", value = debt.date)
                    if (!debt.expirationDate.isNullOrBlank()) {
                        DetailRow(icon = Icons.Default.Event, label = "Due Date", value = debt.expirationDate)
                    }
                    if (debtDetails.walletName.isNotBlank()) {
                        DetailRow(icon = Icons.Default.Wallet, label = "Wallet", value = debtDetails.walletName)
                    }
                    if (!debtDetails.placeName.isNullOrBlank()) {
                        DetailRow(icon = Icons.Default.Place, label = "Place", value = debtDetails.placeName)
                    }
                    if (debtDetails.people.isNotEmpty()) {
                        DetailRow(
                            icon = Icons.Default.Person,
                            label = "People",
                            value = debtDetails.people.joinToString(", ") { it.name }
                        )
                    }
                    if (!debt.note.isNullOrBlank()) {
                        DetailRow(icon = Icons.Default.Edit, label = "Note", value = debt.note)
                    }
                }
            }
        }

        // Linked Transactions Header
        item {
            Text(
                text = "Transaction History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (transactions.isEmpty()) {
            item {
                Text(
                    text = "No payments logged yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(transactions, key = { it.transaction.id }) { tx ->
                TransactionRowItem(
                    txWithCat = tx,
                    formatterConfig = formatterConfig,
                    currencyCode = currencyCode
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun TransactionRowItem(
    txWithCat: TransactionWithCategory,
    formatterConfig: MoneyFormatter.Config,
    currencyCode: String
) {
    val tx = txWithCat.transaction
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIcon(
                iconString = txWithCat.categoryIcon,
                categoryName = txWithCat.categoryName ?: "Transaction",
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.description?.ifBlank { txWithCat.categoryName ?: "Transaction" } ?: "Transaction",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = tx.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = (if (tx.direction == 1) "+" else "-") + MoneyFormatter.format(amount = tx.money, currencyCode = currencyCode, decimals = 2, config = formatterConfig),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (tx.direction == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebtFormContent(
    uiState: DebtDetailsUiState,
    onTypeChange: (Int) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onWalletChange: (String) -> Unit,
    onPlaceChange: (String?) -> Unit,
    onDateChange: (String) -> Unit,
    onExpirationDateChange: (String?) -> Unit,
    onNoteChange: (String) -> Unit,
    onPersonToggle: (String) -> Unit,
    onInsertMasterTxChange: (Boolean) -> Unit,
    onSave: () -> Unit
) {
    var walletExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Debt Type Switcher
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = uiState.editType == 0,
                    onClick = { onTypeChange(0) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("I Owe (Debt)")
                }
                SegmentedButton(
                    selected = uiState.editType == 1,
                    onClick = { onTypeChange(1) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Owed to Me (Credit)")
                }
            }
        }

        // Description Input
        item {
            OutlinedTextField(
                value = uiState.editDescription,
                onValueChange = onDescriptionChange,
                label = { Text("Description") },
                placeholder = { Text("e.g. Lunch money, Car loan") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        // Amount Input
        item {
            val selectedWallet = uiState.availableWallets.find { it.id == uiState.editWalletId }
            val formCurrency = selectedWallet?.currency ?: uiState.currencyCode
            OutlinedTextField(
                value = uiState.editAmount,
                onValueChange = onAmountChange,
                label = { Text("Total Amount") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                prefix = { Text(formCurrency + " ") }
            )
        }

        // Wallet Dropdown Selector
        item {
            ExposedDropdownMenuBox(
                expanded = walletExpanded,
                onExpandedChange = { walletExpanded = it }
            ) {
                val selectedWallet = uiState.availableWallets.find { it.id == uiState.editWalletId }
                OutlinedTextField(
                    value = selectedWallet?.name ?: "Select Wallet",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Associated Wallet") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = walletExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = walletExpanded,
                    onDismissRequest = { walletExpanded = false }
                ) {
                    uiState.availableWallets.forEach { w ->
                        DropdownMenuItem(
                            text = { Text(w.name) },
                            onClick = {
                                onWalletChange(w.id)
                                walletExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Dates Inputs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = uiState.editDate,
                    onValueChange = onDateChange,
                    label = { Text("Date") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = uiState.editExpirationDate ?: "",
                    onValueChange = { onExpirationDateChange(it.ifBlank { null }) },
                    label = { Text("Due Date (Optional)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }

        // Initial Master Transaction Toggle (Only for new debt)
        if (uiState.isNewDebt) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onInsertMasterTxChange(!uiState.editInsertMasterTransaction) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = uiState.editInsertMasterTransaction,
                        onCheckedChange = onInsertMasterTxChange
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Create initial transaction in wallet balance",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (uiState.editType == 0) "Adds income transaction when borrowing money" else "Adds expense transaction when lending money",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Linked People Chips
        if (uiState.availablePeople.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "Linked People",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        uiState.availablePeople.forEach { person ->
                            val isSelected = uiState.editPeopleIds.contains(person.id)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onPersonToggle(person.id) },
                                label = { Text(person.name) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }
        }

        // Note Input
        item {
            OutlinedTextField(
                value = uiState.editNote,
                onValueChange = onNoteChange,
                label = { Text("Note (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }

        // Save Button
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = uiState.editDescription.isNotBlank() && (uiState.editAmount.toDoubleOrNull() ?: 0.0) > 0 && !uiState.isSaving
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Save Debt")
                }
            }
        }
    }
}
