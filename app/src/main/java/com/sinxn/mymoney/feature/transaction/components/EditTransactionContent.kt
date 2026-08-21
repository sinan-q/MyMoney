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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
    onNavigateBack: () -> Unit,
    evaluatedAmountStr: String,
    hasOperatorInAmount: Boolean,
    isNumpadVisible: Boolean,
    showNumpad: () -> Unit,
    focusRequester: FocusRequester,
    onFocusField: () -> Unit,
    numpadDismiss: () -> Unit,
    numpadOnNext: () -> Unit
) {
    val scrollState = rememberScrollState()
    var activePicker by remember { mutableStateOf<FormPicker?>(null) }

    // Direction Accent Color
    val accentColor = remember(uiState.editDirection) {
        if (uiState.editDirection == Direction.INCOME) Color(0xFF10B981) else Color(0xFFE11D48)
    }

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
                isNumpadVisible = isNumpadVisible,
                onHeaderClick = showNumpad
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Description Card
            FormCardContainer {
                DescriptionEditForm(
                    icon = Icons.Default.Description,
                    value = uiState.editDescription,
                    accentColor = accentColor,
                    onValueChange = viewModel::onDescriptionChange,
                    focusRequester = focusRequester,
                    onFocusField = onFocusField,
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
                    numpadDismiss()
                    activePicker = FormPicker.Category
                },
                onWalletClick = {
                    numpadDismiss()
                    activePicker = FormPicker.Wallet
                },
                onDateClick = {
                    numpadDismiss()
                    activePicker = FormPicker.Date
                },
                onPeopleClick = {
                    numpadDismiss()
                    activePicker = FormPicker.People
                },
                onPlaceClick = {
                    numpadDismiss()
                    activePicker = FormPicker.Place
                },
                onEventClick = {
                    numpadDismiss()
                    activePicker = FormPicker.Event
                },
                onNoteChange = viewModel::onNoteChange,
                onConfirmedChange = {
                    numpadDismiss()
                    viewModel.onConfirmedChange(it)
                },
                onCountInTotalChange = {
                    numpadDismiss()
                    viewModel.onCountInTotalChange(it)
                },
                onFocusField = { onFocusField() }
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
                        onNext = numpadOnNext,
                        hasOperatorInAmount = hasOperatorInAmount,
                        saveButtonText = actionBtnText,
                        saveButtonColor = accentColor
                    )
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
