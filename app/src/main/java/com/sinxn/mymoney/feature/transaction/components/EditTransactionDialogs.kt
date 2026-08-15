package com.sinxn.mymoney.feature.transaction.components

import androidx.compose.runtime.Composable
import com.sinxn.mymoney.core.ui.components.CategorySelectionDialog
import com.sinxn.mymoney.core.ui.components.EventSelectionDialog
import com.sinxn.mymoney.core.ui.components.FormDatePickerDialog
import com.sinxn.mymoney.core.ui.components.FormPicker
import com.sinxn.mymoney.core.ui.components.PeopleSelectionDialog
import com.sinxn.mymoney.core.ui.components.PlaceSelectionDialog
import com.sinxn.mymoney.core.ui.components.WalletSelectionDialog
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.Direction
import com.sinxn.mymoney.feature.transaction.TransactionDetailsUiState
import com.sinxn.mymoney.feature.transaction.TransactionDetailsViewModel

@Composable
fun EditTransactionDialogs(
    uiState: TransactionDetailsUiState,
    viewModel: TransactionDetailsViewModel,
    activePicker: FormPicker?,
    onDismiss: () -> Unit
) {
    when (activePicker) {
        FormPicker.Category -> {
            CategorySelectionDialog(
                showIncome = uiState.editDirection == Direction.INCOME,
                incomeCategories = uiState.availableIncomeCategories,
                expenseCategories = uiState.availableExpenseCategories,
                selectedCategoryId = uiState.editCategoryId,
                onCategorySelected = { category ->
                    viewModel.onCategoryIdChange(category.id)
                    onDismiss()
                },
                onDismissRequest = onDismiss
            )
        }
        FormPicker.Wallet -> {
            WalletSelectionDialog(
                title = "Select From Wallet",
                wallets = uiState.availableWallets,
                selectedWalletId = uiState.editWalletId,
                onWalletSelected = { wallet ->
                    viewModel.onWalletIdChange(wallet.id)
                    onDismiss()
                },
                onDismissRequest = onDismiss
            )
        }
        FormPicker.TargetWallet -> {
            WalletSelectionDialog(
                title = "Select To Wallet",
                wallets = uiState.availableWallets.filter { it.id != uiState.editWalletId },
                selectedWalletId = uiState.targetWalletId ?: "",
                onWalletSelected = { wallet ->
                    viewModel.onTargetWalletIdChange(wallet.id)
                    onDismiss()
                },
                onDismissRequest = onDismiss
            )
        }
        FormPicker.Place -> {
            PlaceSelectionDialog(
                title = "Select Place",
                places = uiState.availablePlaces,
                selectedPlaceId = uiState.editPlaceId,
                onPlaceSelected = { place ->
                    viewModel.onPlaceIdChange(place?.id)
                    onDismiss()
                },
                onDismissRequest = onDismiss
            )
        }
        FormPicker.Event -> {
            EventSelectionDialog(
                title = "Select Event",
                events = uiState.availableEvents,
                selectedEventId = uiState.editEventId,
                onEventSelected = { event ->
                    viewModel.onEventIdChange(event?.id)
                    onDismiss()
                },
                onDismissRequest = onDismiss
            )
        }
        FormPicker.People -> {
            PeopleSelectionDialog(
                title = "Select People",
                people = uiState.availablePeople,
                selectedPeopleIds = uiState.editPeopleIds,
                onPersonToggle = { person -> viewModel.onPeopleToggle(person.id) },
                onDismissRequest = onDismiss
            )
        }
        FormPicker.Date -> {
            val initialMillis = try {
                DateUtils.parseDate(uiState.editDate).time
            } catch (e: Exception) {
                null
            }
            FormDatePickerDialog(
                initialDateMillis = initialMillis,
                onDateSelected = { millis ->
                    viewModel.onDateChange(millis)
                    onDismiss()
                },
                onDismissRequest = onDismiss
            )
        }
        else -> Unit
    }
}

