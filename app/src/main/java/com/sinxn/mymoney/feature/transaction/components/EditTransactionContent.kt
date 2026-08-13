package com.sinxn.mymoney.feature.transaction.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.CategorySelectionDialog
import com.sinxn.mymoney.core.ui.components.NumpadView
import com.sinxn.mymoney.core.ui.components.SelectionDialog
import com.sinxn.mymoney.core.ui.components.WalletSelectionDialog
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.Direction
import com.sinxn.mymoney.feature.transaction.TransactionDetailsUiState
import com.sinxn.mymoney.feature.transaction.TransactionDetailsViewModel
import kotlin.collections.plus
import kotlin.text.ifEmpty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UltraCleanTransactionContent(
    uiState: TransactionDetailsUiState,
    settings: com.sinxn.mymoney.core.data.preferences.FormattingSettings,
    viewModel: TransactionDetailsViewModel,
    onNavigateBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var showCategoryPicker by remember { mutableStateOf(false) }
    var showWalletPicker by remember { mutableStateOf(false) }
    var showTargetWalletPicker by remember { mutableStateOf(false) }
    var showPlacePicker by remember { mutableStateOf(false) }
    var showEventPicker by remember { mutableStateOf(false) }
    var showPeoplePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isNumpadVisible by remember { mutableStateOf(true) }

    // Minimal Direction Accent
    val accentColor = when {
        uiState.isTransfer || uiState.editDirection == Direction.TRANSFER -> Color(0xFF0284C7) // Transfer Blue
        uiState.editDirection == Direction.INCOME -> Color(0xFF10B981) // Income Mint
        else -> Color(0xFFE11D48) // Expense Rose
    }

    val hasOperatorInAmount = run {
        val amountStr = uiState.editAmount.trim()
        val rest = if (amountStr.startsWith("-")) amountStr.substring(1) else amountStr
        rest.contains("+") || rest.contains("-") || rest.contains("×") || rest.contains("÷")
    }

    val evaluatedAmountStr = viewModel.getImmediateResult(uiState.editAmount)
    val amountValue = evaluatedAmountStr.toDoubleOrNull()
    val isCategorySelected = !uiState.editCategoryId.isNullOrBlank() || uiState.isTransfer
    val isWalletSelected = uiState.editWalletId.isNotBlank() && (!uiState.isTransfer || !uiState.targetWalletId.isNullOrBlank())
    val isAmountNonNegative = amountValue != null && amountValue >= 0.0

    val isSaveEnabled = !uiState.isSaving && isCategorySelected && isWalletSelected && isAmountNonNegative

    val actionBtnText = if (uiState.isNewTransaction) {
        val dirName = if (uiState.isTransfer || uiState.editDirection == Direction.TRANSFER) "Transfer" else if (uiState.editDirection == Direction.INCOME) "Income" else "Expense"
        "Add $dirName"
    } else {
        "Save Changes"
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Scrollable Form Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(bottom = 16.dp)
        ) {
            // Mode Switcher (Transaction vs Transfer) - Only shown for new entries
            if (uiState.isNewTransaction) {
                TransactionTransferSegmentedControl(
                    isTransfer = uiState.isTransfer,
                    onModeChange = viewModel::onTransferToggle
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // 1. Centered Large Amount Display (Click to open Numpad)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        isNumpadVisible = true
                    }
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val displayAmount = if (hasOperatorInAmount) {
                    viewModel.getImmediateResult(uiState.editAmount)
                } else {
                    if (uiState.editAmount.isEmpty()) "0" else uiState.editAmount
                }

                if (hasOperatorInAmount) {
                    Text(
                        text = uiState.editAmount,
                        style = MaterialTheme.typography.titleMedium,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = uiState.currencySymbol,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = displayAmount,
                        fontSize = 60.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-1.5).sp
                    )
                }

                if (!isNumpadVisible) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Amount",
                            tint = accentColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Tap to edit amount",
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Description Card (Top of inputs)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.editDescription,
                        onValueChange = viewModel::onDescriptionChange,
                        label = { Text("Description") },
                        placeholder = { Text("Add description...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    isNumpadVisible = false
                                }
                            },
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2.5 Transfer Wallets Section (Separate Cards with Swap Button in the Middle)
            if (uiState.isTransfer) {
                val activeFromWallet = uiState.availableWallets.find { it.id == uiState.editWalletId }
                val activeToWallet = uiState.availableWallets.find { it.id == uiState.targetWalletId }

                // From Wallet Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    CleanListRow(
                        icon = {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
                        },
                        label = "From Wallet",
                        value = activeFromWallet?.name ?: uiState.walletName.ifEmpty { "Select Source Wallet" },
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            isNumpadVisible = false
                            showWalletPicker = true
                        }
                    )
                }

                // Centered Swap Button in the Middle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 40.dp)
                    )
                    Surface(
                        onClick = { viewModel.swapTransferWallets() },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp,
                        shadowElevation = 2.dp,
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = "Swap Wallets",
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // To Wallet Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    CleanListRow(
                        icon = {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
                        },
                        label = "To Wallet",
                        value = activeToWallet?.name ?: uiState.targetWalletName.ifEmpty { "Select Target Wallet" },
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            isNumpadVisible = false
                            showTargetWalletPicker = true
                        }
                    )
                }

                // If wallets have different currencies, show Destination Amount field
                if (activeFromWallet != null && activeToWallet != null && !activeFromWallet.currency.equals(activeToWallet.currency, ignoreCase = true)) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
                            OutlinedTextField(
                                value = uiState.editTargetAmount,
                                onValueChange = viewModel::onTargetAmountChange,
                                label = { Text("Destination Amount (${uiState.targetWalletCurrency})") },
                                placeholder = { Text("Received in ${uiState.targetWalletCurrency}") },
                                leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                            )
                        }
                    }
                }

                // Transfer Fee / Tax Field
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
                        OutlinedTextField(
                            value = uiState.editTransferFee,
                            onValueChange = viewModel::onTransferFeeChange,
                            label = { Text("Transfer Fee / Tax (${uiState.currencyCode})") },
                            placeholder = { Text("0.00") },
                            leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // 3. Single Unified Options Card (Category to Count in Total)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column {
                    // Category Row (Hidden for transfers)
                    if (!uiState.isTransfer) {
                        val activeCategory = uiState.availableCategories.find { it.id == uiState.editCategoryId }
                        CleanListRow(
                            icon = {
                                if (activeCategory != null) {
                                    CategoryIcon(
                                        iconString = activeCategory.icon,
                                        categoryName = activeCategory.name,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Icon(Icons.Default.Category, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
                                }
                            },
                            label = "Category",
                            value = activeCategory?.name?.replace("  ↳ ", "") ?: "Select Category",
                            onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                isNumpadVisible = false
                                showCategoryPicker = true
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))

                        val activeWallet = uiState.availableWallets.find { it.id == uiState.editWalletId }
                        CleanListRow(
                            icon = {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
                            },
                            label = "Wallet",
                            value = activeWallet?.name ?: "Select Wallet",
                            onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                isNumpadVisible = false
                                showWalletPicker = true
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))
                    }

                    // Date Row
                    val parsedDate = DateUtils.parseDate(uiState.editDate)
                    val dateText = when {
                        DateUtils.isToday(parsedDate) -> "Today"
                        DateUtils.isYesterday(parsedDate) -> "Yesterday"
                        else -> DateUtils.formatDate(parsedDate, settings.dateFormat)
                    }

                    CleanListRow(
                        icon = {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                        },
                        label = "Date",
                        value = dateText,
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            isNumpadVisible = false
                            showDatePicker = true
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))

                    // People Row
                    val selectedPeopleNames = uiState.availablePeople
                        .filter { it.id in uiState.editPeopleIds }
                        .joinToString { it.name }
                        .ifEmpty { "None" }

                    CleanListRow(
                        icon = { Icon(Icons.Default.People, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp)) },
                        label = "People",
                        value = selectedPeopleNames,
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            isNumpadVisible = false
                            showPeoplePicker = true
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Place Row
                    val activePlace = uiState.availablePlaces.find { it.id == uiState.editPlaceId }
                    CleanListRow(
                        icon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp)) },
                        label = "Place",
                        value = activePlace?.name ?: "None",
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            isNumpadVisible = false
                            showPlacePicker = true
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Event Row
                    val activeEvent = uiState.availableEvents.find { it.id == uiState.editEventId }
                    CleanListRow(
                        icon = { Icon(Icons.Default.Event, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp)) },
                        label = "Event",
                        value = activeEvent?.name ?: "None",
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            isNumpadVisible = false
                            showEventPicker = true
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Note Field
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        OutlinedTextField(
                            value = uiState.editNote,
                            onValueChange = viewModel::onNoteChange,
                            label = { Text("Note") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        isNumpadVisible = false
                                    }
                                },
                            shape = RoundedCornerShape(14.dp),
                            leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null, tint = accentColor) }
                        )
                    }

                    if (!settings.hideStatusAndImpact) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))

                        // Confirmed Switch Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Confirmed", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Switch(
                                checked = uiState.editConfirmed,
                                onCheckedChange = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    viewModel.onConfirmedChange(it)
                                }
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))

                        // Count in Total Switch Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Count in Total", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Switch(
                                checked = uiState.editCountInTotal,
                                onCheckedChange = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    viewModel.onCountInTotalChange(it)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Docked Bottom Bar (Numpad or Save Button like IME sheet)
        Surface(
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column {
                AnimatedVisibility(
                    visible = isNumpadVisible,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    NumpadView(
                        onKeyPress = viewModel::onNumpadKeyPress,
                        onEvaluate = viewModel::evaluateMathExpression,
                        onSave = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            viewModel.saveChanges()
                            onNavigateBack()
                        },
                        onNext = {
                            isNumpadVisible = false
                            focusRequester.requestFocus()
                            keyboardController?.show()
                        },
                        hasOperatorInAmount = hasOperatorInAmount,
                        saveButtonText = actionBtnText,
                        saveButtonColor = accentColor,
                        isSaving = uiState.isSaving,
                        isSaveEnabled = isSaveEnabled
                    )
                }

                if (!isNumpadVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                viewModel.saveChanges()
                                onNavigateBack()
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

    // Modal Dialog Pickers
    if (showCategoryPicker) {
        CategorySelectionDialog(
            showIncome = uiState.editDirection == Direction.INCOME,
            incomeCategories = uiState.availableIncomeCategories,
            expenseCategories = uiState.availableExpenseCategories,
            selectedCategoryId = uiState.editCategoryId,
            onCategorySelected = { category -> viewModel.onCategoryIdChange(category.id) },
            onDismissRequest = { showCategoryPicker = false }
        )
    }

    if (showWalletPicker) {
        WalletSelectionDialog(
            title = "Select From Wallet",
            wallets = uiState.availableWallets,
            selectedWalletId = uiState.editWalletId,
            onWalletSelected = { wallet ->
                viewModel.onWalletIdChange(wallet.id)
                showWalletPicker = false
            },
            onDismissRequest = { showWalletPicker = false }
        )
    }

    if (showTargetWalletPicker) {
        WalletSelectionDialog(
            title = "Select To Wallet",
            wallets = uiState.availableWallets.filter { it.id != uiState.editWalletId },
            selectedWalletId = uiState.targetWalletId ?: "",
            onWalletSelected = { wallet ->
                viewModel.onTargetWalletIdChange(wallet.id)
                showTargetWalletPicker = false
            },
            onDismissRequest = { showTargetWalletPicker = false }
        )
    }

    if (showPlacePicker) {
        SelectionDialog(
            title = "Select Place",
            options = uiState.availablePlaces + com.sinxn.mymoney.core.data.local.entity.PlaceEntity("null", "None", "", null, null, null, false, 0, null),
            onOptionSelected = {
                viewModel.onPlaceIdChange(if (it.id == "null") null else it.id)
                showPlacePicker = false
            },
            onDismissRequest = { showPlacePicker = false },
            labelProvider = { it.name }
        )
    }

    if (showEventPicker) {
        SelectionDialog(
            title = "Select Event",
            options = uiState.availableEvents + com.sinxn.mymoney.core.data.local.entity.EventEntity("null", "None", "", null, "", "", false, 0, null),
            onOptionSelected = {
                viewModel.onEventIdChange(if (it.id == "null") null else it.id)
                showEventPicker = false
            },
            onDismissRequest = { showEventPicker = false },
            labelProvider = { it.name }
        )
    }

    if (showPeoplePicker) {
        SelectionDialog(
            title = "Select People",
            options = uiState.availablePeople,
            selectedOptions = uiState.availablePeople.filter { it.id in uiState.editPeopleIds }.toSet(),
            onOptionSelected = { viewModel.onPeopleToggle(it.id) },
            onDismissRequest = { showPeoplePicker = false },
            labelProvider = { it.name },
            multiSelect = true
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        viewModel.onDateChange(it)
                        showDatePicker = false
                    }
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
