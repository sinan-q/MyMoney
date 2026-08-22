package com.sinxn.mymoney.feature.recurrence

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.sinxn.mymoney.core.util.Direction
import com.sinxn.mymoney.core.util.RecurrenceSetting

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurrentTransactionAddEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: RecurrentTransactionAddEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val formattingSettings by viewModel.formattingSettings.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val numpadState = rememberNumpadFormState(initialNumpadVisible = uiState.isNew)

    var activePicker by remember { mutableStateOf<FormPicker?>(null) }
    var showRecurrencePicker by remember { mutableStateOf(false) }

    val accentColor = remember(uiState.direction) {
        if (uiState.direction == Direction.INCOME) Color(0xFF10B981) else Color(0xFFE11D48)
    }

    val hasOperatorInAmount = remember(uiState.moneyStr) {
        numpadState.hasOperator(uiState.moneyStr)
    }

    val evaluatedAmountStr = remember(uiState.moneyStr, uiState.currencyDecimals) {
        viewModel.getImmediateResult(uiState.moneyStr)
    }
    val amountValue = remember(evaluatedAmountStr) {
        evaluatedAmountStr.toDoubleOrNull()
    }
    val isCategorySelected = uiState.categoryId.isNotBlank()
    val isWalletSelected = uiState.walletId.isNotBlank()
    val isAmountNonNegative = amountValue != null && amountValue > 0.0

    val isSaveEnabled = !uiState.isSaving && isCategorySelected && isWalletSelected && isAmountNonNegative

    val actionBtnText = if (uiState.isNew) {
        val dirName = if (uiState.direction == Direction.INCOME) "Income" else "Expense"
        "Add Recurrent $dirName"
    } else {
        "Save Changes"
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                    text = if (uiState.isNew) "New Recurrence" else "Edit Recurrence",
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
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState)
                            .padding(bottom = 16.dp)
                    ) {
                        // Direction Selector Tab Pill (if new recurrence)
                        if (uiState.isNew) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                TabPill(
                                    tabs = listOf(
                                        "Expense" to Color(0xFFE11D48),
                                        "Income" to Color(0xFF10B981)
                                    ),
                                    activeTab = if (uiState.direction == Direction.EXPENSE) 0 else 1,
                                    onTabChange = { index: Int ->
                                        viewModel.onDirectionChanged(if (index == 0) Direction.EXPENSE else Direction.INCOME)
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // 1. Amount Header Display
                        EditAmountHeader(
                            amountText = uiState.moneyStr,
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
                            onFocusField = { numpadState.onFocusField() },
                            label = "Description",
                            placeHolder = "Add Description"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 3. Form Options Container
                        FormCardContainer {
                            // Category Row
                            val activeCategory = uiState.availableCategories.find { it.id == uiState.categoryId }
                            CleanListRow(
                                icon = {
                                    if (activeCategory != null) {
                                        CategoryIcon(
                                            iconString = activeCategory.icon,
                                            categoryName = activeCategory.name,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Category,
                                            contentDescription = null,
                                            tint = accentColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                },
                                label = "Category",
                                value = activeCategory?.name?.replace("  ↳ ", "") ?: "Select Category",
                                onClick = {
                                    numpadState.dismiss()
                                    activePicker = FormPicker.Category
                                }
                            )

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            // Wallet Row
                            val activeWallet = uiState.availableWallets.find { it.id == uiState.walletId }
                            CleanListRow(
                                icon = {
                                    if (activeWallet != null) {
                                        CategoryIcon(
                                            iconString = activeWallet.icon,
                                            categoryName = activeWallet.name,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = accentColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                },
                                label = "Wallet",
                                value = activeWallet?.name ?: "Select Wallet",
                                onClick = {
                                    numpadState.dismiss()
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
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged {
                                            if (it.isFocused) numpadState.onFocusField()
                                        }
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
                                    onNext = { numpadState.onNext() },
                                    hasOperatorInAmount = hasOperatorInAmount,
                                    saveButtonText = actionBtnText,
                                    saveButtonColor = accentColor,
                                    onDismiss = { numpadState.dismiss() }
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
                                            viewModel.save(onSuccess = onNavigateBack)
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
            }
        }
    }

    // Modal Pickers & Dialogs
    when (activePicker) {
        FormPicker.Category -> {
            CategorySelectionDialog(
                showIncome = uiState.direction == Direction.INCOME,
                incomeCategories = uiState.availableIncomeCategories,
                expenseCategories = uiState.availableExpenseCategories,
                selectedCategoryId = uiState.categoryId,
                onCategorySelected = { category ->
                    viewModel.onCategoryChanged(category.id)
                    activePicker = null
                },
                onDismissRequest = { activePicker = null }
            )
        }
        FormPicker.Wallet -> {
            WalletSelectionDialog(
                title = "Select Wallet",
                wallets = uiState.availableWallets,
                selectedWalletId = uiState.walletId,
                onWalletSelected = { wallet ->
                    viewModel.onWalletChanged(wallet.id)
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
