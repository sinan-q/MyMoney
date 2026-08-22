package com.sinxn.mymoney.feature.transfer.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
import com.sinxn.mymoney.core.ui.components.CleanListRow
import com.sinxn.mymoney.core.ui.components.DescriptionEditForm
import com.sinxn.mymoney.core.ui.components.EditAmountHeader
import com.sinxn.mymoney.core.ui.components.EventSelectionDialog
import com.sinxn.mymoney.core.ui.components.FormCardContainer
import com.sinxn.mymoney.core.ui.components.FormDatePickerDialog
import com.sinxn.mymoney.core.ui.components.FormPicker
import com.sinxn.mymoney.core.ui.components.PeopleSelectionDialog
import com.sinxn.mymoney.core.ui.components.PlaceSelectionDialog
import com.sinxn.mymoney.core.ui.components.TransactionFormRowItem
import com.sinxn.mymoney.core.ui.components.WalletSelectionDialog
import com.sinxn.mymoney.core.ui.components.parseIconData
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.feature.settings.SettingsSwitchItem
import com.sinxn.mymoney.feature.transfer.TransferAddEditUiState
import com.sinxn.mymoney.feature.transfer.TransferAddEditViewModel
import java.util.Date
import kotlin.text.ifEmpty

@Composable
fun EditTransferContent(
    uiState: TransferAddEditUiState,
    settings: FormattingSettings,
    viewModel: TransferAddEditViewModel,
    onNavigateBack: () -> Unit,
    evaluatedAmountStr: String,
    hasOperatorInAmount: Boolean,
    isNumpadVisible: Boolean,
    showNumpad: () -> Unit,
    focusRequester: FocusRequester,
    onFocusField: () -> Unit,
    numpadDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var activePicker by remember { mutableStateOf<FormPicker?>(null) }

    val accentColor = Color(0xFF0284C7)
    val bottomPadding = if (isNumpadVisible) 320.dp else 80.dp

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = bottomPadding)
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
                value = uiState.editDescription,
                accentColor = accentColor,
                onValueChange = viewModel::onDescriptionChange,
                focusRequester = focusRequester,
                onFocusField = onFocusField,
                icon = Icons.Default.Description,
                label = "Description",
                placeHolder = "Add a description"
            )
        }

        // 3. Wallets Selection (From & To Wallets, Target Amount, Transfer Fee)
        val activeFromWallet = uiState.availableWallets.find { it.id == uiState.editWalletId }
        val activeToWallet = uiState.availableWallets.find { it.id == uiState.targetWalletId }

        // From Wallet Card
        val activeFromWalletIconData = remember(activeFromWallet?.icon, activeFromWallet?.name) {
            if (activeFromWallet != null) parseIconData(activeFromWallet.icon, activeFromWallet.name) else null
        }
        val activeToWalletIconData = remember(activeToWallet?.icon, activeToWallet?.name) {
            if (activeToWallet != null) parseIconData(activeToWallet.icon, activeToWallet.name) else null
        }

        FormCardContainer {
            TransactionFormRowItem(
                icon = Icons.Default.AccountBalanceWallet,
                accentColor = accentColor,
                label = "From Wallet",
                value = activeFromWallet?.name ?: "Select Source Wallet",
                trailingIconData = activeFromWalletIconData,
                onClick = {
                    numpadDismiss()
                    activePicker = FormPicker.Wallet
                }
            )
        }

        // Centered Swap Button
        CentreSwapButton(
            onSwapWallets = viewModel::swapWallets,
            icon = Icons.Default.SwapVert,
            accentColor = accentColor
        )

        // To Wallet Card
        FormCardContainer {
            TransactionFormRowItem(
                icon = Icons.Default.AccountBalanceWallet,
                accentColor = accentColor,
                label = "To Wallet",
                value = activeToWallet?.name ?: "Select Target Wallet",
                trailingIconData = activeToWalletIconData,
                onClick = {
                    activePicker = FormPicker.TargetWallet
                }
            )
        }

        // Multi-currency Destination Amount Field
        if (activeFromWallet != null && activeToWallet != null && !activeFromWallet.currency.equals(activeToWallet.currency, ignoreCase = true)) {
            FormCardContainer {
                DescriptionEditForm(
                    icon = Icons.Default.AccountBalanceWallet,
                    accentColor = accentColor,
                    label = "Destination Amount (${uiState.targetWalletCurrency})",
                    placeHolder = "Received in ${uiState.targetWalletCurrency}",
                    value = uiState.editTargetAmount,
                    onValueChange = viewModel::onTargetAmountChange,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        }

        // Transfer Fee / Tax Field
        FormCardContainer {
            DescriptionEditForm(
                icon = Icons.Default.Tune,
                accentColor = accentColor,
                label = "Transfer Fee / Tax (${uiState.currencyCode})",
                placeHolder = "0.00",
                value = uiState.editTransferFee,
                onValueChange = viewModel::onTransferFeeChange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        }

        // 4. Options Card (Date, People, Place, Event, Note)
        FormCardContainer {
            // Date Row
            val parsedDate = DateUtils.parseDate(uiState.editDate)
            val formattedDate = when {
                DateUtils.isToday(parsedDate) -> "Today"
                DateUtils.isYesterday(parsedDate) -> "Yesterday"
                else -> DateUtils.formatDate(parsedDate, settings.dateFormat)
            }
            TransactionFormRowItem(
                icon = Icons.Default.CalendarToday,
                accentColor = accentColor,
                label = "Date",
                value = formattedDate,
                onClick = {
                    activePicker = FormPicker.Date
                }
            )

            // People Row
            val selectedPeople = uiState.availablePeople.filter { it.id in uiState.editPeopleIds }
            val selectedPeopleNames = selectedPeople.joinToString { it.name }.ifEmpty { "None" }
            TransactionFormRowItem(
                icon = Icons.Default.People,
                active = selectedPeople.isNotEmpty(),
                accentColor = accentColor,
                label = "People",
                value = selectedPeopleNames,
                onClick = {
                    activePicker = FormPicker.People
                }
            )

            // Place Row
            val activePlace = uiState.availablePlaces.find { it.id == uiState.editPlaceId }
            val placeIconData = remember(activePlace?.icon, activePlace?.name) {
                if (activePlace != null && activePlace.icon.isNotBlank()) parseIconData(
                    activePlace.icon,
                    activePlace.name
                ) else null
            }
            TransactionFormRowItem(
                icon = Icons.Default.LocationOn,
                active = activePlace != null,
                accentColor = accentColor,
                label = "Place",
                value = activePlace?.name ?: "None",
                trailingIconData = placeIconData,
                onClick = {
                    activePicker = FormPicker.Place
                }
            )

            // Event Row
            val activeEvent = uiState.availableEvents.find { it.id == uiState.editEventId }
            val eventIconData = remember(activeEvent?.icon, activeEvent?.name) {
                if (activeEvent != null && activeEvent.icon.isNotBlank()) parseIconData(
                    activeEvent.icon,
                    activeEvent.name
                ) else null
            }
            TransactionFormRowItem(
                icon = Icons.Default.Flag,
                active = !activeEvent?.name.isNullOrEmpty(),
                accentColor = accentColor,
                label = "Event",
                value = activeEvent?.name ?: "None",
                trailingIconData = eventIconData,
                onClick = {
                    activePicker = FormPicker.Event
                }
            )
            // Note Field
            DescriptionEditForm(
                modifier = Modifier,
                focusRequester = remember { FocusRequester() },
                onFocusField = onFocusField,
                icon = Icons.AutoMirrored.Filled.Notes,
                accentColor = accentColor,
                value = uiState.editNote,
                onValueChange = viewModel::onNoteChange,
                label = "Note",
                placeHolder = "Add a note..."
            )
        }

        // 5. Status & Impact Card (Confirmed, Count in Total)
        if (!settings.hideStatusAndImpact) {
            FormCardContainer {
                // Confirmed Switch Row
                SettingsSwitchItem(
                    title = "Confirmed",
                    checked = uiState.editConfirmed,
                    onCheckedChange = viewModel::onConfirmedChange,
                    accentColor = accentColor,
                    horizontalPadding = 16.dp,
                )

                // Count in Total Switch Row
                SettingsSwitchItem(
                    title = "Count in Total",
                    checked = uiState.editCountInTotal,
                    onCheckedChange = viewModel::onCountInTotalChange,
                    accentColor = accentColor,
                    horizontalPadding = 16.dp,
                )
            }
        }
    }

    // Pickers Dialogs
    when (activePicker) {
        FormPicker.Wallet -> {
            WalletSelectionDialog(
                title = "Select From Wallet",
                wallets = uiState.availableWallets,
                selectedWalletId = uiState.editWalletId,
                onWalletSelected = { wallet ->
                    viewModel.onFromWalletSelect(wallet.id)
                    activePicker = null
                },
                onDismissRequest = { activePicker = null }
            )
        }
        FormPicker.TargetWallet -> {
            WalletSelectionDialog(
                title = "Select To Wallet",
                wallets = uiState.availableWallets.filter { it.id != uiState.editWalletId },
                selectedWalletId = uiState.targetWalletId ?: "",
                onWalletSelected = { wallet ->
                    viewModel.onToWalletSelect(wallet.id)
                    activePicker = null
                },
                onDismissRequest = { activePicker = null }
            )
        }
        FormPicker.Place -> {
            PlaceSelectionDialog(
                title = "Select Place",
                places = uiState.availablePlaces,
                selectedPlaceId = uiState.editPlaceId,
                onPlaceSelected = { place ->
                    viewModel.onPlaceSelect(place?.id)
                    activePicker = null
                },
                onDismissRequest = { activePicker = null }
            )
        }
        FormPicker.Event -> {
            EventSelectionDialog(
                title = "Select Event",
                events = uiState.availableEvents,
                selectedEventId = uiState.editEventId,
                onEventSelected = { event ->
                    viewModel.onEventSelect(event?.id)
                    activePicker = null
                },
                onDismissRequest = { activePicker = null }
            )
        }
        FormPicker.People -> {
            PeopleSelectionDialog(
                title = "Select People",
                people = uiState.availablePeople,
                selectedPeopleIds = uiState.editPeopleIds,
                onPersonToggle = { person ->
                    val current = uiState.editPeopleIds
                    val updated = if (current.contains(person.id)) current - person.id else current + person.id
                    viewModel.onPeopleChange(updated)
                },
                onDismissRequest = { activePicker = null }
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
                    val dateStr = DateUtils.getSQLDateTimeString(Date(millis))
                    viewModel.onDateChange(dateStr)
                    activePicker = null
                },
                onDismissRequest = { activePicker = null }
            )
        }
        else -> Unit
    }
}

