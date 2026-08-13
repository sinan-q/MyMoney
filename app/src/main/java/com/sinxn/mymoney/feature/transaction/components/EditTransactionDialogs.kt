package com.sinxn.mymoney.feature.transaction.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import com.sinxn.mymoney.core.data.local.entity.EventEntity
import com.sinxn.mymoney.core.data.local.entity.PlaceEntity
import com.sinxn.mymoney.core.ui.components.CategorySelectionDialog
import com.sinxn.mymoney.core.ui.components.SelectionDialog
import com.sinxn.mymoney.core.ui.components.WalletSelectionDialog
import com.sinxn.mymoney.core.util.Direction
import com.sinxn.mymoney.feature.transaction.TransactionDetailsUiState
import com.sinxn.mymoney.feature.transaction.TransactionDetailsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionDialogs(
    uiState: TransactionDetailsUiState,
    viewModel: TransactionDetailsViewModel,
    showCategoryPicker: Boolean,
    showWalletPicker: Boolean,
    showTargetWalletPicker: Boolean,
    showPlacePicker: Boolean,
    showEventPicker: Boolean,
    showPeoplePicker: Boolean,
    showDatePicker: Boolean,
    onDismissCategoryPicker: () -> Unit,
    onDismissWalletPicker: () -> Unit,
    onDismissTargetWalletPicker: () -> Unit,
    onDismissPlacePicker: () -> Unit,
    onDismissEventPicker: () -> Unit,
    onDismissPeoplePicker: () -> Unit,
    onDismissDatePicker: () -> Unit
) {
    if (showCategoryPicker) {
        CategorySelectionDialog(
            showIncome = uiState.editDirection == Direction.INCOME,
            incomeCategories = uiState.availableIncomeCategories,
            expenseCategories = uiState.availableExpenseCategories,
            selectedCategoryId = uiState.editCategoryId,
            onCategorySelected = { category ->
                viewModel.onCategoryIdChange(category.id)
                onDismissCategoryPicker()
            },
            onDismissRequest = onDismissCategoryPicker
        )
    }

    if (showWalletPicker) {
        WalletSelectionDialog(
            title = "Select From Wallet",
            wallets = uiState.availableWallets,
            selectedWalletId = uiState.editWalletId,
            onWalletSelected = { wallet ->
                viewModel.onWalletIdChange(wallet.id)
                onDismissWalletPicker()
            },
            onDismissRequest = onDismissWalletPicker
        )
    }

    if (showTargetWalletPicker) {
        WalletSelectionDialog(
            title = "Select To Wallet",
            wallets = uiState.availableWallets.filter { it.id != uiState.editWalletId },
            selectedWalletId = uiState.targetWalletId ?: "",
            onWalletSelected = { wallet ->
                viewModel.onTargetWalletIdChange(wallet.id)
                onDismissTargetWalletPicker()
            },
            onDismissRequest = onDismissTargetWalletPicker
        )
    }

    if (showPlacePicker) {
        SelectionDialog(
            title = "Select Place",
            options = uiState.availablePlaces + PlaceEntity("null", "None", "", null, null, null, false, 0, null),
            onOptionSelected = {
                viewModel.onPlaceIdChange(if (it.id == "null") null else it.id)
                onDismissPlacePicker()
            },
            onDismissRequest = onDismissPlacePicker,
            labelProvider = { it.name }
        )
    }

    if (showEventPicker) {
        SelectionDialog(
            title = "Select Event",
            options = uiState.availableEvents + EventEntity("null", "None", "", null, "", "", false, 0, null),
            onOptionSelected = {
                viewModel.onEventIdChange(if (it.id == "null") null else it.id)
                onDismissEventPicker()
            },
            onDismissRequest = onDismissEventPicker,
            labelProvider = { it.name }
        )
    }

    if (showPeoplePicker) {
        SelectionDialog(
            title = "Select People",
            options = uiState.availablePeople,
            selectedOptions = uiState.availablePeople.filter { it.id in uiState.editPeopleIds }.toSet(),
            onOptionSelected = { viewModel.onPeopleToggle(it.id) },
            onDismissRequest = onDismissPeoplePicker,
            labelProvider = { it.name },
            multiSelect = true
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = onDismissDatePicker,
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        viewModel.onDateChange(it)
                        onDismissDatePicker()
                    }
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
