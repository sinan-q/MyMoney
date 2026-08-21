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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
import com.sinxn.mymoney.core.ui.components.DescriptionEditForm
import com.sinxn.mymoney.core.ui.components.EditAmountHeader
import com.sinxn.mymoney.core.ui.components.FormCardContainer
import com.sinxn.mymoney.core.ui.components.FormPicker
import com.sinxn.mymoney.core.ui.components.NumpadView
import com.sinxn.mymoney.core.ui.components.rememberNumpadFormState
import com.sinxn.mymoney.core.util.Direction
import com.sinxn.mymoney.feature.transaction.TransactionAddEditUiState
import com.sinxn.mymoney.feature.transaction.TransactionAddEditViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionContent(
    uiState: TransactionAddEditUiState,
    settings: FormattingSettings,
    viewModel: TransactionAddEditViewModel,
    onNavigateBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val numpadState = rememberNumpadFormState(initialNumpadVisible = uiState.isNewTransaction)
    var activePicker by remember { mutableStateOf<FormPicker?>(null) }

    // Direction Accent Color
    val accentColor = remember(uiState.editDirection) {
        if (uiState.editDirection == Direction.INCOME) Color(0xFF10B981) else Color(0xFFE11D48)
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
    val isCategorySelected = !uiState.editCategoryId.isNullOrBlank()
    val isWalletSelected = uiState.editWalletId.isNotBlank()
    val isAmountNonNegative = amountValue != null && amountValue >= 0.0

    val isSaveEnabled = !uiState.isSaving && isCategorySelected && isWalletSelected && isAmountNonNegative

    val actionBtnText = if (uiState.isNewTransaction) {
        val dirName = if (uiState.editDirection == Direction.INCOME) "Income" else "Expense"
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
            FormCardContainer {
                DescriptionEditForm(
                    icon = Icons.Default.Description,
                    value = uiState.editDescription,
                    accentColor = accentColor,
                    onValueChange = viewModel::onDescriptionChange,
                    focusRequester = numpadState.focusRequester,
                    onFocusField = { numpadState.onFocusField() },
                    label = "Description",
                    placeHolder = "Add Description"
                )
            }


            // 3. Options Card
            EditTransactionFormOptions(
                uiState = uiState,
                settings = settings,
                accentColor = accentColor,
                onCategoryClick = {
                    numpadState.dismiss()
                    activePicker = FormPicker.Category
                },
                onWalletClick = {
                    numpadState.dismiss()
                    activePicker = FormPicker.Wallet
                },
                onDateClick = {
                    numpadState.dismiss()
                    activePicker = FormPicker.Date
                },
                onPeopleClick = {
                    numpadState.dismiss()
                    activePicker = FormPicker.People
                },
                onPlaceClick = {
                    numpadState.dismiss()
                    activePicker = FormPicker.Place
                },
                onEventClick = {
                    numpadState.dismiss()
                    activePicker = FormPicker.Event
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
                focusRequester = numpadState.focusRequester,
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
                            viewModel.saveChanges {
                                onNavigateBack()
                            }
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
                                viewModel.saveChanges {
                                    onNavigateBack()
                                }
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
        activePicker = activePicker,
        onDismiss = { activePicker = null }
    )
}
