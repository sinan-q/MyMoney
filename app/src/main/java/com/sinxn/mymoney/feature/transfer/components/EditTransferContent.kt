package com.sinxn.mymoney.feature.transfer.components

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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
import com.sinxn.mymoney.core.ui.components.CleanListRow
import com.sinxn.mymoney.core.ui.components.DescriptionEditForm
import com.sinxn.mymoney.core.ui.components.EditAmountHeader
import com.sinxn.mymoney.core.ui.components.EventSelectionDialog
import com.sinxn.mymoney.core.ui.components.FormCardContainer
import com.sinxn.mymoney.core.ui.components.FormDatePickerDialog
import com.sinxn.mymoney.core.ui.components.FormPicker
import com.sinxn.mymoney.core.ui.components.NumpadView
import com.sinxn.mymoney.core.ui.components.PeopleSelectionDialog
import com.sinxn.mymoney.core.ui.components.PlaceSelectionDialog
import com.sinxn.mymoney.core.ui.components.WalletSelectionDialog
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.feature.transfer.TransferAddEditUiState
import com.sinxn.mymoney.feature.transfer.TransferAddEditViewModel
import java.util.Date

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
    numpadOnNext: () -> Unit
) {
    val scrollState = rememberScrollState()
    var activePicker by remember { mutableStateOf<FormPicker?>(null) }

    val accentColor = Color(0xFF0284C7)

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

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Wallets Selection (From & To Wallets, Target Amount, Transfer Fee)
            EditTransferWallets(
                availableWallets = uiState.availableWallets,
                fromWalletId = uiState.editWalletId,
                toWalletId = uiState.targetWalletId,
                targetAmount = uiState.editTargetAmount,
                transferFee = uiState.editTransferFee,
                sourceCurrency = uiState.currencyCode,
                targetCurrency = uiState.targetWalletCurrency,
                accentColor = accentColor,
                onFromWalletClick = {
                    numpadDismiss()
                    activePicker = FormPicker.Wallet
                },
                onToWalletClick = {
                    numpadDismiss()
                    activePicker = FormPicker.TargetWallet
                },
                onSwapWallets = viewModel::swapWallets,
                onTargetAmountChange = viewModel::onTargetAmountChange,
                onTransferFeeChange = viewModel::onTransferFeeChange
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Options Card (Date, People, Place, Event, Confirmed, CountInTotal)
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
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Date Row
                    val parsedDate = DateUtils.parseDate(uiState.editDate)
                    val formattedDate = when {
                        DateUtils.isToday(parsedDate) -> "Today"
                        DateUtils.isYesterday(parsedDate) -> "Yesterday"
                        else -> DateUtils.formatDate(parsedDate, settings.dateFormat)
                    }
                    CleanListRow(
                        icon = {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = "Date",
                        value = formattedDate,
                        onClick = {
                            numpadDismiss()
                            activePicker = FormPicker.Date
                        }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    // People Row
                    val activePeopleCount = uiState.editPeopleIds.size
                    CleanListRow(
                        icon = {
                            Icon(
                                Icons.Default.Group,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = "People",
                        value = when (activePeopleCount) {
                            0 -> "None"
                            1 -> uiState.availablePeople.find { it.id == uiState.editPeopleIds.first() }?.name ?: "1 Person"
                            else -> "$activePeopleCount people"
                        },
                        onClick = {
                            numpadDismiss()
                            activePicker = FormPicker.People
                        }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    // Place Row
                    val activePlace = uiState.availablePlaces.find { it.id == uiState.editPlaceId }
                    CleanListRow(
                        icon = {
                            Icon(
                                Icons.Default.Place,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = "Place",
                        value = activePlace?.name ?: "None",
                        onClick = {
                            numpadDismiss()
                            activePicker = FormPicker.Place
                        }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    // Event Row
                    val activeEvent = uiState.availableEvents.find { it.id == uiState.editEventId }
                    CleanListRow(
                        icon = {
                            Icon(
                                Icons.Default.Flag,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = "Event",
                        value = activeEvent?.name ?: "None",
                        onClick = {
                            numpadDismiss()
                            activePicker = FormPicker.Event
                        }
                    )

                    if (!settings.hideStatusAndImpact) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        // Confirmed Switch Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Confirmed",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Switch(
                                checked = uiState.editConfirmed,
                                onCheckedChange = {
                                    numpadDismiss()
                                    viewModel.onConfirmedChange(it)
                                }
                            )
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        // Count in Total Switch Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Count in Total",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Switch(
                                checked = uiState.editCountInTotal,
                                onCheckedChange = {
                                    numpadDismiss()
                                    viewModel.onCountInTotalChange(it)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Docked Bottom Bar (Numpad View)
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
                        saveButtonText = if (uiState.isNewTransfer) "Add Transfer" else "Save Changes",
                        saveButtonColor = accentColor
                    )
                }
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
