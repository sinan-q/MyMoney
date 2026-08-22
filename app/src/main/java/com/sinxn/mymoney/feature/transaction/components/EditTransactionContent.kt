package com.sinxn.mymoney.feature.transaction.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.sinxn.mymoney.core.util.Direction
import com.sinxn.mymoney.feature.transaction.TransactionAddEditUiState
import com.sinxn.mymoney.feature.transaction.TransactionAddEditViewModel
import com.sinxn.mymoney.ui.theme.ExpenseColor
import com.sinxn.mymoney.ui.theme.IncomeColor

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
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var activePicker by remember { mutableStateOf<FormPicker?>(null) }

    // Direction Accent Color
    val accentColor = remember(uiState.editDirection) {
        if (uiState.editDirection == Direction.INCOME) IncomeColor else ExpenseColor
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 80.dp)
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
            activePicker = { activePicker = it },
            onNoteChange = viewModel::onNoteChange,
            onConfirmedChange = { viewModel.onConfirmedChange(it) },
            onCountInTotalChange = { viewModel.onCountInTotalChange(it) },
            onFocusField = { onFocusField() }
        )
    }

    // Modal Dialog Pickers
    EditTransactionDialogs(
        uiState = uiState,
        viewModel = viewModel,
        activePicker = activePicker,
        onDismiss = { activePicker = null }
    )
}
