package com.sinxn.mymoney.feature.debt

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sinxn.mymoney.core.data.local.model.DebtWithDetails
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.PeopleSelectionDialog
import com.sinxn.mymoney.core.ui.components.PlaceSelectionDialog
import com.sinxn.mymoney.core.ui.components.WalletSelectionDialog
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MoneyFormatter
import com.sinxn.mymoney.feature.transaction.components.CleanListRow
import com.sinxn.mymoney.feature.transaction.components.TransactionCardContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DebtRoseColor = Color(0xFFE11D48)
private val CreditEmeraldColor = Color(0xFF10B981)

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

    val isDebt = if (uiState.isEditMode || uiState.isNewDebt) uiState.editType == 0
    else (uiState.debtDetails?.debt?.type ?: 0) == 0

    val accentColor = if (isDebt) DebtRoseColor else CreditEmeraldColor

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isNewDebt) {
                            if (uiState.editType == 0) "New Debt" else "New Credit"
                        } else if (uiState.isEditMode) {
                            if (uiState.editType == 0) "Edit Debt" else "Edit Credit"
                        } else {
                            uiState.debtDetails?.debt?.description?.ifBlank {
                                if (isDebt) "Debt Details" else "Credit Details"
                            } ?: if (isDebt) "Debt Details" else "Credit Details"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.isEditMode && !uiState.isNewDebt) {
                            viewModel.setEditMode(false)
                        } else {
                            onNavigateBack()
                        }
                    }) {
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
                                    contentDescription = "Archive",
                                    tint = if (uiState.debtDetails?.debt?.isArchived == true) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
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
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.isEditMode || uiState.isNewDebt) {
                DebtFormContent(
                    uiState = uiState,
                    accentColor = accentColor,
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
                val debtDetails = uiState.debtDetails
                if (debtDetails != null) {
                    DebtViewContent(
                        debtDetails = debtDetails,
                        transactions = uiState.transactions,
                        formatterConfig = formatterConfig,
                        currencyCode = uiState.currencyCode,
                        accentColor = accentColor,
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
    debtDetails: DebtWithDetails,
    transactions: List<TransactionWithCategory>,
    formatterConfig: MoneyFormatter.Config,
    currencyCode: String,
    accentColor: Color,
    onRecordPaymentClick: () -> Unit
) {
    val debt = debtDetails.debt
    val remaining = debtDetails.remainingMoney
    val totalMoney = debt.money
    val isDebt = debt.type == 0
    val isFullyPaid = remaining == 0L && totalMoney > 0

    val isOverdue = remember(debt.expirationDate) {
        debt.expirationDate?.let { exp ->
            try {
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val expDate = format.parse(exp)
                expDate != null && expDate.before(Date()) && !isFullyPaid
            } catch (e: Exception) {
                false
            }
        } ?: false
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Centered Hero Amount Section (Inspired by Transaction View Screen)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isFullyPaid) "Settled in Full" else "Remaining Balance",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                val formattedRemaining = MoneyFormatter.format(
                    amount = remaining,
                    currencyCode = debtDetails.walletCurrency,
                    decimals = debtDetails.walletDecimals,
                    config = formatterConfig
                )

                Text(
                    text = formattedRemaining,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isFullyPaid) MaterialTheme.colorScheme.primary else accentColor,
                    letterSpacing = (-1.2).sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Direction & Status Pill Chip
                Surface(
                    shape = CircleShape,
                    color = if (isFullyPaid) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else accentColor.copy(alpha = 0.12f),
                    border = BorderStroke(
                        1.dp,
                        if (isFullyPaid) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        else accentColor.copy(alpha = 0.25f)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (isFullyPaid) Icons.Default.CheckCircle
                            else if (isDebt) Icons.Default.ArrowDownward
                            else Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = if (isFullyPaid) MaterialTheme.colorScheme.primary else accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isFullyPaid) "Paid in Full"
                            else if (isDebt) "I Owe (Debt)"
                            else "Owed to Me (Credit)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isFullyPaid) MaterialTheme.colorScheme.primary else accentColor
                        )
                    }
                }
            }
        }

        // 2. Progress Card
        if (totalMoney > 0) {
            item {
                TransactionCardContainer(horizontalPadding = 0.dp) {
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
                                text = "Progress",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            val paidAmount = kotlin.math.abs(debtDetails.progress)
                            val progressPercent = ((paidAmount.toFloat() / totalMoney.toFloat()) * 100).toInt().coerceIn(0, 100)
                            Text(
                                text = "$progressPercent% Repaid",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val progressFraction = (kotlin.math.abs(debtDetails.progress).toFloat() / totalMoney.toFloat()).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = if (isFullyPaid) MaterialTheme.colorScheme.primary else accentColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Paid: ${MoneyFormatter.format(amount = kotlin.math.abs(debtDetails.progress), currencyCode = debtDetails.walletCurrency, decimals = debtDetails.walletDecimals, config = formatterConfig)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Total: ${MoneyFormatter.format(amount = totalMoney, currencyCode = debtDetails.walletCurrency, decimals = debtDetails.walletDecimals, config = formatterConfig)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 3. Description Block (if present)
        if (debt.description.isNotBlank()) {
            item {
                TransactionCardContainer(horizontalPadding = 0.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = debt.description,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // 4. Primary Details Card (Wallet, Dates, People, Place, Note)
        item {
            TransactionCardContainer(horizontalPadding = 0.dp) {
                Column {
                    // Wallet Row
                    if (debtDetails.walletName.isNotBlank()) {
                        CleanListRow(
                            icon = { Icon(Icons.Default.Wallet, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp)) },
                            label = "Wallet",
                            value = debtDetails.walletName
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))
                    }

                    // Creation Date Row
                    CleanListRow(
                        icon = { Icon(Icons.Default.Event, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp)) },
                        label = "Date Created",
                        value = debt.date.take(10)
                    )

                    // Due Date / Expiration Date Row
                    if (!debt.expirationDate.isNullOrBlank()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))
                        CleanListRow(
                            icon = { Icon(Icons.Default.Event, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp)) },
                            label = "Due Date",
                            value = debt.expirationDate.take(10),
                            trailingBadge = if (isOverdue) {
                                {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    ) {
                                        Text(
                                            text = "Overdue",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            } else null
                        )
                    }

                    // Linked People Row
                    if (debtDetails.people.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))
                        CleanListRow(
                            icon = { Icon(Icons.Default.Person, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp)) },
                            label = "People",
                            value = debtDetails.people.joinToString(", ") { it.name }
                        )
                    }

                    // Linked Place Row
                    if (!debtDetails.placeName.isNullOrBlank()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))
                        CleanListRow(
                            icon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp)) },
                            label = "Place",
                            value = debtDetails.placeName
                        )
                    }

                    // Notes Row
                    if (!debt.note.isNullOrBlank()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Notes, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Note",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = debt.note,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Quick Payment Button (if not fully paid)
        if (!isFullyPaid) {
            item {
                Button(
                    onClick = onRecordPaymentClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isDebt) "Record Repayment" else "Record Collection",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 6. Payment History Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Payment History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (transactions.isNotEmpty()) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "${transactions.size} payment" + if (transactions.size != 1) "s" else "",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // 7. Payment History Items
        if (transactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No payments have been recorded yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        } else {
            items(transactions, key = { it.transaction.id }) { tx ->
                DebtTransactionItem(
                    txWithCat = tx,
                    formatterConfig = formatterConfig,
                    currencyCode = currencyCode
                )
            }
        }
    }
}

@Composable
private fun DebtTransactionItem(
    txWithCat: TransactionWithCategory,
    formatterConfig: MoneyFormatter.Config,
    currencyCode: String
) {
    val tx = txWithCat.transaction
    val isIncome = tx.direction == 1
    val isTransfer = tx.direction == 2 || tx.type == 1 || tx.type == 2 || txWithCat.categoryName.equals("Transfer", ignoreCase = true)

    val categoryDisplayName = when {
        isTransfer -> "Transfer"
        !txWithCat.categoryName.isNullOrBlank() -> txWithCat.categoryName!!
        else -> "Payment"
    }

    val amountColor = when {
        isTransfer -> Color(0xFF0284C7)
        isIncome -> Color(0xFF10B981)
        else -> Color(0xFFE11D48)
    }

    val primaryTitle = if (!tx.description.isNullOrBlank()) tx.description!! else categoryDisplayName
    val subtitleText = if (!tx.description.isNullOrBlank()) categoryDisplayName else null
    val dateText = tx.date.take(10)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIcon(
                iconString = txWithCat.categoryIcon,
                categoryName = categoryDisplayName,
                modifier = Modifier.size(42.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = primaryTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitleText.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            val signedAmount = if (isIncome || isTransfer) tx.money else -tx.money
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = MoneyFormatter.format(
                        amount = signedAmount,
                        currencyCode = currencyCode,
                        decimals = 2,
                        config = formatterConfig
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebtFormContent(
    uiState: DebtDetailsUiState,
    accentColor: Color,
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
    var showWalletPicker by remember { mutableStateOf(false) }
    var showPlacePicker by remember { mutableStateOf(false) }
    var showPeoplePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showExpDatePicker by remember { mutableStateOf(false) }

    val selectedWallet = uiState.availableWallets.find { it.id == uiState.editWalletId }
    val selectedPlace = uiState.availablePlaces.find { it.id == uiState.editPlaceId }
    val formCurrency = selectedWallet?.currency ?: uiState.currencyCode

    val isSaveEnabled = uiState.editDescription.isNotBlank() &&
            (uiState.editAmount.toDoubleOrNull() ?: 0.0) > 0 &&
            uiState.editWalletId.isNotBlank() &&
            !uiState.isSaving

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mode Selector Tab Pill
        item {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                DebtFormTabPill(
                    selectedTab = uiState.editType,
                    onTabSelected = onTypeChange
                )
            }
        }

        // Amount Input Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Total Amount",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = formCurrency,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = uiState.editAmount,
                            onValueChange = onAmountChange,
                            placeholder = { Text("0.00", fontSize = 36.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)) },
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Start
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            modifier = Modifier.width(IntrinsicSize.Min)
                        )
                    }
                }
            }
        }

        // Description Input Card
        item {
            OutlinedTextField(
                value = uiState.editDescription,
                onValueChange = onDescriptionChange,
                label = { Text("Description") },
                placeholder = { Text("e.g. Lunch with team, Loan for car...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
        }

        // Form Fields (Wallet, Dates, People, Place)
        item {
            TransactionCardContainer(horizontalPadding = 0.dp) {
                Column {
                    // Wallet Selector Row
                    CleanListRow(
                        icon = { Icon(Icons.Default.Wallet, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp)) },
                        label = "Associated Wallet",
                        value = selectedWallet?.name ?: "Select Wallet",
                        onClick = { showWalletPicker = true }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Creation Date Row
                    CleanListRow(
                        icon = { Icon(Icons.Default.Event, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp)) },
                        label = "Date",
                        value = uiState.editDate.take(10),
                        onClick = { showDatePicker = true }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Expiration / Due Date Row
                    CleanListRow(
                        icon = { Icon(Icons.Default.Event, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp)) },
                        label = "Due Date (Optional)",
                        value = uiState.editExpirationDate?.take(10) ?: "Not Set",
                        onClick = { showExpDatePicker = true },
                        trailingBadge = if (!uiState.editExpirationDate.isNullOrBlank()) {
                            {
                                IconButton(
                                    onClick = { onExpirationDateChange(null) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear Due Date", modifier = Modifier.size(16.dp))
                                }
                            }
                        } else null
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Linked People Row
                    val linkedPeopleNames = uiState.availablePeople
                        .filter { uiState.editPeopleIds.contains(it.id) }
                        .joinToString(", ") { it.name }

                    CleanListRow(
                        icon = { Icon(Icons.Default.Person, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp)) },
                        label = "Linked People",
                        value = if (linkedPeopleNames.isNotBlank()) linkedPeopleNames else "None Selected",
                        onClick = { showPeoplePicker = true }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Linked Place Row
                    CleanListRow(
                        icon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp)) },
                        label = "Place",
                        value = selectedPlace?.name ?: "None",
                        onClick = { showPlacePicker = true }
                    )
                }
            }
        }

        // Note Input Card
        item {
            OutlinedTextField(
                value = uiState.editNote,
                onValueChange = onNoteChange,
                label = { Text("Note (Optional)") },
                placeholder = { Text("Additional notes...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                minLines = 3
            )
        }

        // Initial Master Transaction Toggle (Only for new debt)
        if (uiState.isNewDebt) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onInsertMasterTxChange(!uiState.editInsertMasterTransaction) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = uiState.editInsertMasterTransaction,
                            onCheckedChange = onInsertMasterTxChange
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Create Initial Wallet Transaction",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (uiState.editType == 0) "Adds income transaction when borrowing money"
                                else "Adds expense transaction when lending money",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Save Button
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onSave,
                enabled = isSaveEnabled,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (uiState.isNewDebt) {
                            if (uiState.editType == 0) "Save Debt" else "Save Credit"
                        } else "Save Changes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Dialog Pickers
    if (showWalletPicker) {
        WalletSelectionDialog(
            title = "Select Associated Wallet",
            wallets = uiState.availableWallets,
            selectedWalletId = uiState.editWalletId,
            onWalletSelected = { wallet ->
                onWalletChange(wallet.id)
                showWalletPicker = false
            },
            onDismissRequest = { showWalletPicker = false }
        )
    }

    if (showPlacePicker) {
        PlaceSelectionDialog(
            title = "Select Place",
            places = uiState.availablePlaces,
            selectedPlaceId = uiState.editPlaceId,
            onPlaceSelected = { place ->
                onPlaceChange(place?.id)
                showPlacePicker = false
            },
            onDismissRequest = { showPlacePicker = false }
        )
    }

    if (showPeoplePicker) {
        PeopleSelectionDialog(
            title = "Select People",
            people = uiState.availablePeople,
            selectedPeopleIds = uiState.editPeopleIds,
            onPersonToggle = { person -> onPersonToggle(person.id) },
            onDismissRequest = { showPeoplePicker = false }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        onDateChange(formatter.format(Date(millis)))
                        showDatePicker = false
                    }
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showExpDatePicker) {
        val expDatePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showExpDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    expDatePickerState.selectedDateMillis?.let { millis ->
                        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        onExpirationDateChange(formatter.format(Date(millis)))
                        showExpDatePicker = false
                    }
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = expDatePickerState)
        }
    }
}

@Composable
private fun DebtFormTabPill(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf(
        Triple("I Owe (Debt)", Icons.Default.ArrowDownward, DebtRoseColor),
        Triple("Owed to Me (Credit)", Icons.Default.ArrowUpward, CreditEmeraldColor)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = CircleShape
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEachIndexed { index, (title, icon, accent) ->
            val isSelected = selectedTab == index
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) accent else Color.Transparent,
                label = "FormTabBg_$index"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "FormTabText_$index"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(bgColor)
                    .clickable { onTabSelected(index) }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
