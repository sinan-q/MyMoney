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
import com.sinxn.mymoney.core.ui.components.EventSelectionDialog
import com.sinxn.mymoney.core.ui.components.PeopleSelectionDialog
import com.sinxn.mymoney.core.ui.components.PlaceSelectionDialog
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
        PlaceSelectionDialog(
            title = "Select Place",
            places = uiState.availablePlaces,
            selectedPlaceId = uiState.editPlaceId,
            onPlaceSelected = { place ->
                viewModel.onPlaceIdChange(place?.id)
                onDismissPlacePicker()
            },
            onDismissRequest = onDismissPlacePicker
        )
    }

    if (showEventPicker) {
        EventSelectionDialog(
            title = "Select Event",
            events = uiState.availableEvents,
            selectedEventId = uiState.editEventId,
            onEventSelected = { event ->
                viewModel.onEventIdChange(event?.id)
                onDismissEventPicker()
            },
            onDismissRequest = onDismissEventPicker
        )
    }

    if (showPeoplePicker) {
        PeopleSelectionDialog(
            title = "Select People",
            people = uiState.availablePeople,
            selectedPeopleIds = uiState.editPeopleIds,
            onPersonToggle = { person -> viewModel.onPeopleToggle(person.id) },
            onDismissRequest = onDismissPeoplePicker
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
