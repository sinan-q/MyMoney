package com.sinxn.mymoney.feature.debt

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sinxn.mymoney.core.data.local.model.DebtWithDetails
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import com.sinxn.mymoney.core.ui.components.NumpadView
import com.sinxn.mymoney.core.ui.components.PeopleSelectionDialog
import com.sinxn.mymoney.core.ui.components.PlaceSelectionDialog
import com.sinxn.mymoney.core.ui.components.TabPill
import com.sinxn.mymoney.core.ui.components.WalletSelectionDialog
import com.sinxn.mymoney.core.ui.components.groupTransactionsByMonth
import com.sinxn.mymoney.core.ui.components.monthGroupedTransactionItems
import com.sinxn.mymoney.core.ui.components.rememberNumpadFormState
import com.sinxn.mymoney.core.util.MoneyFormatter
import com.sinxn.mymoney.core.ui.components.CleanListRow
import com.sinxn.mymoney.core.ui.components.EditAmountHeader
import com.sinxn.mymoney.core.ui.components.FormCardContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private val DebtRoseColor = Color(0xFFE11D48)
private val CreditEmeraldColor = Color(0xFF10B981)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtDetailsScreen(
    onNavigateBack: () -> Unit,
    onTransactionClick: (String) -> Unit = {},
    onRecordPayment: (debtId: String, walletId: String, debtAction: String) -> Unit = { _, _, _ -> },
    viewModel: DebtDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val formatterConfig = uiState.formatterConfig
    val numpadState = rememberNumpadFormState(initialNumpadVisible = uiState.isNewDebt)
    var showDeleteDialog by remember { mutableStateOf(false) }

    val isDebt = if (uiState.isEditMode || uiState.isNewDebt) uiState.editType == 0
    else (uiState.debtDetails?.debt?.type ?: 0) == 0

    val accentColor = if (isDebt) DebtRoseColor else CreditEmeraldColor

    val hasOperatorInAmount = remember(uiState.editAmount) {
        numpadState.hasOperator(uiState.editAmount)
    }

    val evaluatedAmountStr = remember(uiState.editAmount) {
        numpadState.getImmediateResult(uiState.editAmount, uiState.currencyDecimals)
    }
    val amountValue = remember(evaluatedAmountStr) {
        evaluatedAmountStr.toDoubleOrNull()
    }
    val selectedWallet = uiState.availableWallets.find { it.id == uiState.editWalletId }

    val formCurrency = remember(selectedWallet, uiState.currencyCode) {
        MoneyFormatter.getCurrencySymbol(selectedWallet?.currency ?: uiState.currencyCode)
    }

    val isSaveEnabled = uiState.editDescription.isNotBlank() &&
            (amountValue != null && amountValue > 0.0) &&
            uiState.editWalletId.isNotBlank() &&
            !uiState.isSaving

    val actionBtnText = if (uiState.isNewDebt) {
        if (uiState.editType == 0) "Save Debt" else "Save Credit"
    } else {
        "Save Changes"
    }

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
                            if (isDebt) "Debt Details" else "Credit Details"
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
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 16.dp)
                    ) {
                        if (uiState.isNewDebt) {
                            TabPill(
                                tabs = listOf("Debt" to DebtRoseColor, "Credit" to CreditEmeraldColor),
                                activeTab = uiState.editType,
                                onTabChange = viewModel::updateType
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        EditAmountHeader(
                            amountText = uiState.editAmount,
                            currencySymbol = formCurrency,
                            evaluatedResult = evaluatedAmountStr,
                            hasOperatorInAmount = hasOperatorInAmount,
                            accentColor = accentColor,
                            isNumpadVisible = numpadState.isNumpadVisible,
                            onHeaderClick = { numpadState.showNumpad() }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        DebtFormContent(
                            uiState = uiState,
                            accentColor = accentColor,
                            focusRequester = numpadState.focusRequester,
                            onDescriptionChange = viewModel::updateDescription,
                            onWalletChange = viewModel::updateWalletId,
                            onPlaceChange = viewModel::updatePlaceId,
                            onDateChange = viewModel::updateDate,
                            onExpirationDateChange = viewModel::updateExpirationDate,
                            onNoteChange = viewModel::updateNote,
                            onPersonToggle = viewModel::togglePersonSelection,
                            onInsertMasterTxChange = viewModel::updateInsertMasterTransaction,
                            onFocusField = { numpadState.onFocusField() },
                            onDismissKeyboardAndNumpad = { numpadState.dismiss() }
                        )
                    }

                    // Docked Bottom Bar (Numpad or Save Button)
                    Surface(
                        tonalElevation = 6.dp,
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    ) {
                        Column {
                            AnimatedVisibility(
                                visible = numpadState.isNumpadVisible,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                NumpadView(
                                    onKeyPress = viewModel::onNumpadKeyPress,
                                    onEvaluate = viewModel::evaluateMathExpression,
                                    onSave = {
                                        numpadState.dismiss()
                                        viewModel.saveDebt { onNavigateBack() }
                                    },
                                    onNext = { numpadState.onNext() },
                                    hasOperatorInAmount = hasOperatorInAmount,
                                    saveButtonText = actionBtnText,
                                    saveButtonColor = accentColor,
                                    isSaving = uiState.isSaving,
                                    isSaveEnabled = isSaveEnabled
                                )
                            }

                            if (!numpadState.isNumpadVisible) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            numpadState.dismiss()
                                            viewModel.saveDebt { onNavigateBack() }
                                        },
                                        enabled = isSaveEnabled,
                                        shape = RoundedCornerShape(20.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = accentColor,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(54.dp)
                                    ) {
                                        if (uiState.isSaving) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(22.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text(
                                                text = actionBtnText,
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                val debtDetails = uiState.debtDetails
                if (debtDetails != null) {
                    DebtViewContent(
                        debtDetails = debtDetails,
                        transactions = uiState.transactions,
                        formatterConfig = formatterConfig,
                        currencyCode = uiState.currencyCode,
                        currencyDecimals = uiState.currencyDecimals,
                        dateFormat = uiState.dateFormat,
                        accentColor = accentColor,
                        onTransactionClick = onTransactionClick,
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
    currencyDecimals: Int,
    dateFormat: Int,
    accentColor: Color,
    onTransactionClick: (String) -> Unit,
    onRecordPaymentClick: () -> Unit
) {
    val debt = debtDetails.debt
    val remaining = debtDetails.remainingMoney
    val totalMoney = debt.money
    val isDebt = debt.type == 0
    val isFullyPaid = remaining == 0L && totalMoney > 0

    var collapsedGroups by remember { mutableStateOf(setOf<String>()) }
    val groupedItems = remember(transactions) {
        groupTransactionsByMonth(transactions)
    }

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
        contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
    ) {
        // 1. Centered Hero Amount Section (Inspired by Transaction View Screen)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
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
                            else if (isDebt) "Debt"
                            else "Credit",
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
                                    text = "Progress",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                val paidAmount = abs(debtDetails.progress)
                                val progressPercent = ((paidAmount.toFloat() / totalMoney.toFloat()) * 100).toInt().coerceIn(0, 100)
                                Text(
                                    text = "$progressPercent% Repaid",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            val progressFraction = (abs(debtDetails.progress).toFloat() / totalMoney.toFloat()).coerceIn(0f, 1f)
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
                                    text = "Paid: ${MoneyFormatter.format(amount = abs(debtDetails.progress), currencyCode = debtDetails.walletCurrency, decimals = debtDetails.walletDecimals, config = formatterConfig)}",
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
        }

        // 3. Description Block (if present)
        if (debt.description.isNotBlank()) {
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
        }

        // 4. Primary Details Card (Wallet, Dates, People, Place, Note)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                FormCardContainer(horizontalPadding = 0.dp) {
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
        }

        // 5. Quick Payment Button (if not fully paid)
        if (!isFullyPaid) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
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
        }

        // 6. Payment History Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Payment History (${transactions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // 7. Month-grouped Transactions List with Sticky Headers
        monthGroupedTransactionItems(
            items = groupedItems,
            collapsedGroups = collapsedGroups,
            onToggleGroup = { headerKey ->
                collapsedGroups = if (headerKey in collapsedGroups) {
                    collapsedGroups - headerKey
                } else {
                    collapsedGroups + headerKey
                }
            },
            onTransactionClick = onTransactionClick,
            decimals = currencyDecimals,
            currencyCode = currencyCode,
            formatterConfig = formatterConfig,
            dateFormat = dateFormat,
            emptyMessage = "No payments have been recorded yet."
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebtFormContent(
    uiState: DebtDetailsUiState,
    accentColor: Color,
    focusRequester: FocusRequester,
    onDescriptionChange: (String) -> Unit,
    onWalletChange: (String) -> Unit,
    onPlaceChange: (String?) -> Unit,
    onDateChange: (String) -> Unit,
    onExpirationDateChange: (String?) -> Unit,
    onNoteChange: (String) -> Unit,
    onPersonToggle: (String) -> Unit,
    onInsertMasterTxChange: (Boolean) -> Unit,
    onFocusField: () -> Unit,
    onDismissKeyboardAndNumpad: () -> Unit
) {
    var showWalletPicker by remember { mutableStateOf(false) }
    var showPlacePicker by remember { mutableStateOf(false) }
    var showPeoplePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showExpDatePicker by remember { mutableStateOf(false) }

    val selectedWallet = uiState.availableWallets.find { it.id == uiState.editWalletId }
    val selectedPlace = uiState.availablePlaces.find { it.id == uiState.editPlaceId }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Description Input Card
        OutlinedTextField(
            value = uiState.editDescription,
            onValueChange = onDescriptionChange,
            label = { Text("Description") },
            placeholder = { Text("e.g. Lunch with team, Loan for car...") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        onFocusField()
                    }
                },
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        // Form Fields (Wallet, Dates, People, Place)
        FormCardContainer(horizontalPadding = 0.dp) {
            Column {
                // Wallet Selector Row
                CleanListRow(
                    icon = { Icon(Icons.Default.Wallet, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp)) },
                    label = "Associated Wallet",
                    value = selectedWallet?.name ?: "Select Wallet",
                    onClick = {
                        onDismissKeyboardAndNumpad()
                        showWalletPicker = true
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))

                // Creation Date Row
                CleanListRow(
                    icon = { Icon(Icons.Default.Event, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp)) },
                    label = "Date",
                    value = uiState.editDate.take(10),
                    onClick = {
                        onDismissKeyboardAndNumpad()
                        showDatePicker = true
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))

                // Expiration / Due Date Row
                CleanListRow(
                    icon = { Icon(Icons.Default.Event, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp)) },
                    label = "Due Date (Optional)",
                    value = uiState.editExpirationDate?.take(10) ?: "Not Set",
                    onClick = {
                        onDismissKeyboardAndNumpad()
                        showExpDatePicker = true
                    },
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
                    onClick = {
                        onDismissKeyboardAndNumpad()
                        showPeoplePicker = true
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))

                // Linked Place Row
                CleanListRow(
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp)) },
                    label = "Place",
                    value = selectedPlace?.name ?: "None",
                    onClick = {
                        onDismissKeyboardAndNumpad()
                        showPlacePicker = true
                    }
                )
            }
        }

        // Note Input Card
        OutlinedTextField(
            value = uiState.editNote,
            onValueChange = onNoteChange,
            label = { Text("Note (Optional)") },
            placeholder = { Text("Additional notes...") },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        onFocusField()
                    }
                },
            shape = RoundedCornerShape(16.dp),
            minLines = 3
        )

        // Initial Master Transaction Toggle (Only for new debt)
        if (uiState.isNewDebt) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDismissKeyboardAndNumpad()
                        onInsertMasterTxChange(!uiState.editInsertMasterTransaction)
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = uiState.editInsertMasterTransaction,
                        onCheckedChange = {
                            onDismissKeyboardAndNumpad()
                            onInsertMasterTxChange(it)
                        }
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

