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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
import com.sinxn.mymoney.core.ui.components.NumpadView
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
    var isNumpadVisible by remember(uiState.isNewTransaction) { mutableStateOf(uiState.isNewTransaction) }

    // Direction Accent Color
    val accentColor = remember(uiState.isTransfer, uiState.editDirection) {
        when {
            uiState.isTransfer || uiState.editDirection == Direction.TRANSFER -> Color(0xFF0284C7)
            uiState.editDirection == Direction.INCOME -> Color(0xFF10B981)
            else -> Color(0xFFE11D48)
        }
    }

    val hasOperatorInAmount = remember(uiState.editAmount) {
        val amountStr = uiState.editAmount.trim()
        val rest = if (amountStr.startsWith("-")) amountStr.substring(1) else amountStr
        rest.contains("+") || rest.contains("-") || rest.contains("×") || rest.contains("÷")
    }

    val evaluatedAmountStr = remember(uiState.editAmount) {
        viewModel.getImmediateResult(uiState.editAmount)
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

    val dismissKeyboardAndNumpad = {
        focusManager.clearFocus()
        keyboardController?.hide()
        isNumpadVisible = false
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
                TransactionTransferSegmentedControl(
                    isTransfer = uiState.isTransfer,
                    onModeChange = viewModel::onTransferToggle
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // 1. Amount Header Display
            EditTransactionAmountHeader(
                amountText = uiState.editAmount,
                currencySymbol = uiState.currencySymbol,
                evaluatedResult = evaluatedAmountStr,
                hasOperatorInAmount = hasOperatorInAmount,
                accentColor = accentColor,
                isNumpadVisible = isNumpadVisible,
                onHeaderClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    isNumpadVisible = true
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Description Card
            TransactionDescriptionCard(
                description = uiState.editDescription,
                accentColor = accentColor,
                isEditable = true,
                onDescriptionChange = viewModel::onDescriptionChange,
                focusRequester = focusRequester,
                onFocusField = { isNumpadVisible = false }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2.5 Transfer Wallets Section
            if (uiState.isTransfer) {
                EditTransactionTransferWallets(
                    uiState = uiState,
                    accentColor = accentColor,
                    onFromWalletClick = {
                        dismissKeyboardAndNumpad()
                        showWalletPicker = true
                    },
                    onToWalletClick = {
                        dismissKeyboardAndNumpad()
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
                    dismissKeyboardAndNumpad()
                    showCategoryPicker = true
                },
                onWalletClick = {
                    dismissKeyboardAndNumpad()
                    showWalletPicker = true
                },
                onDateClick = {
                    dismissKeyboardAndNumpad()
                    showDatePicker = true
                },
                onPeopleClick = {
                    dismissKeyboardAndNumpad()
                    showPeoplePicker = true
                },
                onPlaceClick = {
                    dismissKeyboardAndNumpad()
                    showPlacePicker = true
                },
                onEventClick = {
                    dismissKeyboardAndNumpad()
                    showEventPicker = true
                },
                onNoteChange = viewModel::onNoteChange,
                onConfirmedChange = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    viewModel.onConfirmedChange(it)
                },
                onCountInTotalChange = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    viewModel.onCountInTotalChange(it)
                },
                onFocusField = { isNumpadVisible = false }
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
