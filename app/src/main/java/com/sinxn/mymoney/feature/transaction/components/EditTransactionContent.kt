package com.sinxn.mymoney.feature.transaction.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
import com.sinxn.mymoney.core.ui.components.EditAmountHeader
import com.sinxn.mymoney.core.ui.components.NumpadView
import com.sinxn.mymoney.core.ui.components.TabPill
import com.sinxn.mymoney.core.ui.components.rememberNumpadFormState
import com.sinxn.mymoney.core.util.Direction
import com.sinxn.mymoney.feature.transaction.TransactionDetailsUiState
import com.sinxn.mymoney.feature.transaction.TransactionDetailsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionContent(
    uiState: TransactionDetailsUiState,
    settings: FormattingSettings,
    viewModel: TransactionDetailsViewModel,
    onNavigateBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val numpadState = rememberNumpadFormState(initialNumpadVisible = uiState.isNewTransaction)

    var showCategoryPicker by remember { mutableStateOf(false) }
    var showWalletPicker by remember { mutableStateOf(false) }
    var showTargetWalletPicker by remember { mutableStateOf(false) }
    var showPlacePicker by remember { mutableStateOf(false) }
    var showEventPicker by remember { mutableStateOf(false) }
    var showPeoplePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Direction Accent Color
    val accentColor = remember(uiState.isTransfer, uiState.editDirection) {
        when {
            uiState.isTransfer || uiState.editDirection == Direction.TRANSFER -> Color(0xFF0284C7)
            uiState.editDirection == Direction.INCOME -> Color(0xFF10B981)
            else -> Color(0xFFE11D48)
        }
    }

    val hasOperatorInAmount = remember(uiState.editAmount) {
        numpadState.hasOperator(uiState.editAmount)
    }

    val evaluatedAmountStr = remember(uiState.editAmount) {
        numpadState.getImmediateResult(uiState.editAmount, uiState.currencyDecimals)
    }
    val amountValue = remember(evaluatedAmountStr) {
        evaluatedAmountStr.toDoubleOrNull()
    }
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

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(bottom = 16.dp)
        ) {
            // Mode Switcher (Transaction vs Transfer)
            if (uiState.isNewTransaction) {
                TabPill(
                    tabs = listOf("Transaction" to accentColor, "Transfer" to Color(0xFF0284C7)),
                    activeTab = if (uiState.isTransfer) 1 else 0,
                    onTabChange = { index ->
                        viewModel.onTransferToggle(index == 1)
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // 1. Amount Header Display
            EditAmountHeader(
                amountText = uiState.editAmount,
                currencySymbol = uiState.currencySymbol,
                evaluatedResult = evaluatedAmountStr,
                hasOperatorInAmount = hasOperatorInAmount,
                accentColor = accentColor,
                isNumpadVisible = numpadState.isNumpadVisible,
                onHeaderClick = { numpadState.showNumpad() }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Description Card
            TransactionDescriptionCard(
                description = uiState.editDescription,
                accentColor = accentColor,
                isEditable = true,
                onDescriptionChange = viewModel::onDescriptionChange,
                focusRequester = numpadState.focusRequester,
                onFocusField = { numpadState.onFocusField() }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2.5 Transfer Wallets Section
            if (uiState.isTransfer) {
                EditTransactionTransferWallets(
                    uiState = uiState,
                    accentColor = accentColor,
                    onFromWalletClick = {
                        numpadState.dismiss()
                        showWalletPicker = true
                    },
                    onToWalletClick = {
                        numpadState.dismiss()
                        showTargetWalletPicker = true
                    },
                    onSwapWallets = viewModel::swapTransferWallets,
                    onTargetAmountChange = viewModel::onTargetAmountChange,
                    onTransferFeeChange = viewModel::onTransferFeeChange
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 3. Options Card
            EditTransactionFormOptions(
                uiState = uiState,
                settings = settings,
                accentColor = accentColor,
                onCategoryClick = {
                    numpadState.dismiss()
                    showCategoryPicker = true
                },
                onWalletClick = {
                    numpadState.dismiss()
                    showWalletPicker = true
                },
                onDateClick = {
                    numpadState.dismiss()
                    showDatePicker = true
                },
                onPeopleClick = {
                    numpadState.dismiss()
                    showPeoplePicker = true
                },
                onPlaceClick = {
                    numpadState.dismiss()
                    showPlacePicker = true
                },
                onEventClick = {
                    numpadState.dismiss()
                    showEventPicker = true
                },
                onNoteChange = viewModel::onNoteChange,
                onConfirmedChange = {
                    numpadState.dismiss()
                    viewModel.onConfirmedChange(it)
                },
                onCountInTotalChange = {
                    numpadState.dismiss()
                    viewModel.onCountInTotalChange(it)
                },
                onFocusField = { numpadState.onFocusField() }
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
                            viewModel.saveChanges()
                            onNavigateBack()
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
    EditTransactionDialogs(
        uiState = uiState,
        viewModel = viewModel,
        showCategoryPicker = showCategoryPicker,
        showWalletPicker = showWalletPicker,
        showTargetWalletPicker = showTargetWalletPicker,
        showPlacePicker = showPlacePicker,
        showEventPicker = showEventPicker,
        showPeoplePicker = showPeoplePicker,
        showDatePicker = showDatePicker,
        onDismissCategoryPicker = { showCategoryPicker = false },
        onDismissWalletPicker = { showWalletPicker = false },
        onDismissTargetWalletPicker = { showTargetWalletPicker = false },
        onDismissPlacePicker = { showPlacePicker = false },
        onDismissEventPicker = { showEventPicker = false },
        onDismissPeoplePicker = { showPeoplePicker = false },
        onDismissDatePicker = { showDatePicker = false }
    )
}
