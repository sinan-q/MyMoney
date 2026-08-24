package com.sinxn.mymoney.feature.recurrence

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sinxn.mymoney.core.ui.components.*
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.RecurrenceSetting
import com.sinxn.mymoney.ui.theme.IncomeColor
import com.sinxn.mymoney.ui.theme.TransferColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurrentTransferAddEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: RecurrentTransferAddEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val formattingSettings by viewModel.formattingSettings.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val numpadState = rememberNumpadFormState(initialNumpadVisible = uiState.isNew)

    var activePicker by remember { mutableStateOf<FormPicker?>(null) }
    var activeWalletPickerForTarget by remember { mutableStateOf(false) }
    var showRecurrencePicker by remember { mutableStateOf(false) }

    val accentColor = TransferColor

    val hasOperatorInAmount = remember(uiState.moneyFromStr) {
        numpadState.hasOperator(uiState.moneyFromStr)
    }

    val evaluatedAmountStr = remember(uiState.moneyFromStr, uiState.currencyDecimals) {
        viewModel.getImmediateResult(uiState.moneyFromStr)
    }
    val amountValue = remember(evaluatedAmountStr) {
        evaluatedAmountStr.toDoubleOrNull()
    }
    val isWalletFromSelected = uiState.walletFromId.isNotBlank()
    val isWalletToSelected = uiState.walletToId.isNotBlank()
    val isAmountNonNegative = amountValue != null && amountValue > 0.0

    val isSaveEnabled = !uiState.isSaving && isWalletFromSelected && isWalletToSelected && isAmountNonNegative

    val actionBtnText = if (uiState.isNew) {
        "Add Recurrent Transfer"
    } else {
        "Save Changes"
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            AnimatedVisibility(
                visible = isSaveEnabled && !numpadState.isNumpadVisible,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                AppExtendedFab(
                    text = actionBtnText,
                    icon = if (uiState.isNew) Icons.Default.Add else Icons.Default.Check,
                    onClick = {
                        numpadState.dismiss()
                        viewModel.save(onSuccess = onNavigateBack)
                    },
                    containerColor = accentColor,
                    contentColor = Color.White
                )
            }
        },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }

                Text(
                    text = if (uiState.isNew) "New Recurrent Transfer" else "Edit Recurrent Transfer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.width(48.dp))
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(bottom = 88.dp)
                ) {
                        // 1. Amount Header Display
                        EditAmountHeader(
                            amountText = uiState.moneyFromStr,
                            currencySymbol = uiState.currencySymbol,
                            evaluatedResult = evaluatedAmountStr,
                            hasOperatorInAmount = hasOperatorInAmount,
                            accentColor = accentColor,
                            isNumpadVisible = numpadState.isNumpadVisible,
                            onHeaderClick = { numpadState.showNumpad() }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 2. Description Card
                        DescriptionEditForm(
                            icon = Icons.Default.Description,
                            value = uiState.description,
                            accentColor = accentColor,
                            onValueChange = viewModel::onDescriptionChanged,
                            focusRequester = numpadState.focusRequester,
                            label = "Description",
                            placeHolder = "Add Description"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 3. Transfer Accounts & Options Card
                        FormCardContainer {
                            // From Wallet Row
                            val fromWallet = uiState.availableWallets.find { it.id == uiState.walletFromId }
                            CleanListRow(
                                icon = {
                                    if (fromWallet != null) {
                                        CategoryIcon(
                                            iconString = fromWallet.icon,
                                            categoryName = fromWallet.name,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                },
                                label = "From Wallet",
                                value = fromWallet?.name ?: "Select Source Wallet",
                                onClick = {
                                    numpadState.dismiss()
                                    activeWalletPickerForTarget = false
                                    activePicker = FormPicker.Wallet
                                }
                            )

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            // To Wallet Row
                            val toWallet = uiState.availableWallets.find { it.id == uiState.walletToId }
                            CleanListRow(
                                icon = {
                                    if (toWallet != null) {
                                        CategoryIcon(
                                            iconString = toWallet.icon,
                                            categoryName = toWallet.name,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = IncomeColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                },
                                label = "To Wallet",
                                value = toWallet?.name ?: "Select Destination Wallet",
                                onClick = {
                                    numpadState.dismiss()
                                    activeWalletPickerForTarget = true
                                    activePicker = FormPicker.Wallet
                                }
                            )

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            // Recurrence Frequency Row
                            val setting = remember(uiState.startDate, uiState.rule) {
                                RecurrenceSetting.fromStringOrFallback(uiState.startDate, uiState.rule)
                            }
                            CleanListRow(
                                icon = {
                                    Icon(
                                        Icons.Default.Repeat,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = "Frequency",
                                value = setting.getUserReadableString(context),
                                onClick = {
                                    numpadState.dismiss()
                                    showRecurrencePicker = true
                                }
                            )

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            // Start Date Row
                            val formattedStartDate = remember(uiState.startDate, formattingSettings.dateFormat) {
                                DateUtils.formatDate(uiState.startDate, formattingSettings.dateFormat)
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
                                label = "Start Date",
                                value = formattedStartDate,
                                onClick = {
                                    numpadState.dismiss()
                                    activePicker = FormPicker.Date
                                }
                            )

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            // Place Row
                            val activePlace = uiState.availablePlaces.find { it.id == uiState.placeId }
                            CleanListRow(
                                icon = {
                                    Icon(
                                        Icons.Default.Place,
                                        contentDescription = null,
                                        tint = if (activePlace != null) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = "Place",
                                value = activePlace?.name ?: "None",
                                onClick = {
                                    numpadState.dismiss()
                                    activePicker = FormPicker.Place
                                }
                            )

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            // Event Row
                            val activeEvent = uiState.availableEvents.find { it.id == uiState.eventId }
                            CleanListRow(
                                icon = {
                                    Icon(
                                        Icons.Default.Event,
                                        contentDescription = null,
                                        tint = if (activeEvent != null) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = "Event",
                                value = activeEvent?.name ?: "None",
                                onClick = {
                                    numpadState.dismiss()
                                    activePicker = FormPicker.Event
                                }
                            )

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            // Note TextField Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Notes,
                                    contentDescription = null,
                                    tint = if (uiState.note.isNotBlank()) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                TextField(
                                    value = uiState.note,
                                    onValueChange = viewModel::onNoteChanged,
                                    placeholder = {
                                        Text(
                                            "Add a note...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            // Auto Confirmed Switch Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (uiState.confirmed) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        "Auto Confirmed",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Switch(
                                    checked = uiState.confirmed,
                                    onCheckedChange = {
                                        numpadState.dismiss()
                                        viewModel.onConfirmedChanged(it)
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentColor)
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.QueryStats,
                                        contentDescription = null,
                                        tint = if (uiState.countInTotal) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        "Count in Total",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Switch(
                                    checked = uiState.countInTotal,
                                    onCheckedChange = {
                                        numpadState.dismiss()
                                        viewModel.onCountInTotalChanged(it)
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentColor)
                                )
                            }
                    }
                }
            }
        }
    }

    if (numpadState.isNumpadVisible) {
        NumpadView(
            onKeyPress = viewModel::onNumpadKeyPress,
            onEvaluate = viewModel::evaluateMathExpression,
            onNext = { numpadState.onNext() },
            hasOperatorInAmount = hasOperatorInAmount,
            saveButtonText = actionBtnText,
            saveButtonColor = accentColor,
            onDismiss = { numpadState.dismiss() }
        )
    }

    // Modal Pickers & Dialogs
    when (activePicker) {
        FormPicker.Wallet -> {
            val title = if (activeWalletPickerForTarget) "Select Destination Wallet" else "Select Source Wallet"
            val selectedId = if (activeWalletPickerForTarget) uiState.walletToId else uiState.walletFromId
            WalletSelectionDialog(
                title = title,
                wallets = uiState.availableWallets,
                selectedWalletId = selectedId,
                onWalletSelected = { wallet ->
                    if (activeWalletPickerForTarget) {
                        viewModel.onWalletToChanged(wallet.id)
                    } else {
                        viewModel.onWalletFromChanged(wallet.id)
                    }
                    activePicker = null
                },
                onDismissRequest = { activePicker = null }
            )
        }
        FormPicker.Place -> {
            PlaceSelectionDialog(
                title = "Select Place",
                places = uiState.availablePlaces,
                selectedPlaceId = uiState.placeId,
                onPlaceSelected = { place ->
                    viewModel.onPlaceChanged(place?.id)
                    activePicker = null
                },
                onDismissRequest = { activePicker = null }
            )
        }
        FormPicker.Event -> {
            EventSelectionDialog(
                title = "Select Event",
                events = uiState.availableEvents,
                selectedEventId = uiState.eventId,
                onEventSelected = { event ->
                    viewModel.onEventChanged(event?.id)
                    activePicker = null
                },
                onDismissRequest = { activePicker = null }
            )
        }
        FormPicker.Date -> {
            FormDatePickerDialog(
                initialDateMillis = uiState.startDate.time,
                onDateSelected = { millis ->
                    viewModel.onRecurrenceRuleUpdated(java.util.Date(millis), uiState.rule)
                    activePicker = null
                },
                onDismissRequest = { activePicker = null }
            )
        }
        else -> Unit
    }

    if (showRecurrencePicker) {
        RecurrencePickerDialog(
            initialStartDate = uiState.startDate,
            initialRule = uiState.rule,
            onDismiss = { showRecurrencePicker = false },
            onConfirm = { startDate, rule ->
                viewModel.onRecurrenceRuleUpdated(startDate, rule)
                showRecurrencePicker = false
            }
        )
    }
}
