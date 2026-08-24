package com.sinxn.mymoney.feature.transaction.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
import com.sinxn.mymoney.core.ui.components.CleanListRow
import com.sinxn.mymoney.core.ui.components.DescriptionEditForm
import com.sinxn.mymoney.core.ui.components.EditAmountHeader
import com.sinxn.mymoney.core.ui.components.FormCardContainer
import com.sinxn.mymoney.core.ui.components.FormPicker
import com.sinxn.mymoney.core.ui.components.TransactionFormRowItem
import com.sinxn.mymoney.core.ui.components.parseIconData
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.Direction
import com.sinxn.mymoney.feature.settings.SettingsSwitchItem
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
                label = "Description",
                placeHolder = "Add Description"
            )
        }

        // 3. Options Card
        Column {
            FormCardContainer {
                // Category Row
                val activeCategory =
                    uiState.availableCategories.find { it.id == uiState.editCategoryId }
                val iconData = remember(activeCategory?.icon, activeCategory?.name) {
                    if (activeCategory != null) parseIconData(
                        activeCategory.icon,
                        activeCategory.name
                    ) else null
                }
                TransactionFormRowItem(
                    icon = Icons.Default.Category,
                    active = true,
                    accentColor = accentColor,
                    label = "Category",
                    value = activeCategory?.name ?: "Select Category",
                    trailingIconData = iconData,
                    onClick = { activePicker = FormPicker.Category }
                )

                // Wallet Row
                val activeWallet = uiState.availableWallets.find { it.id == uiState.editWalletId }
                val walletIconData = remember(activeWallet?.icon, activeWallet?.name) {
                    if (activeWallet != null) parseIconData(
                        activeWallet.icon,
                        activeWallet.name
                    ) else null
                }
                TransactionFormRowItem(
                    icon = Icons.Default.AccountBalanceWallet,
                    active = true,
                    accentColor = accentColor,
                    label = "Wallet",
                    value = activeWallet?.name ?: "Select Wallet",
                    trailingIconData = walletIconData,
                    onClick = { activePicker = FormPicker.Wallet }
                )

                // Date Row
                val parsedDate = DateUtils.parseDate(uiState.editDate)
                val dateText = when {
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
                        )
                    },
                    label = "Date",
                    value = dateText,
                    onClick = { activePicker = FormPicker.Date }
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
                    onClick = { activePicker = FormPicker.People }
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
                    onClick = { activePicker = FormPicker.Place }
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
                    onClick = { activePicker = FormPicker.Event }
                )

                // Note Field
                DescriptionEditForm(
                    modifier = Modifier,
                    focusRequester = remember { FocusRequester() },
                    icon = Icons.AutoMirrored.Filled.Notes,
                    accentColor = accentColor,
                    value = uiState.editNote,
                    onValueChange = viewModel::onNoteChange,
                    singleLine = false,
                    label = "Note",
                    placeHolder = "Add a note..."
                )
            }
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
    }

    // Modal Dialog Pickers
    EditTransactionDialogs(
        uiState = uiState,
        viewModel = viewModel,
        activePicker = activePicker,
        onDismiss = { activePicker = null }
    )
}
