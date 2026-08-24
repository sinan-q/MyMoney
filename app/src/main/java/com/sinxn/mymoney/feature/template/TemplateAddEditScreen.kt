package com.sinxn.mymoney.feature.template

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import com.sinxn.mymoney.core.ui.components.AppExtendedFab
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.CategorySelectionDialog
import com.sinxn.mymoney.core.ui.components.CleanListRow
import com.sinxn.mymoney.core.ui.components.EventSelectionDialog
import com.sinxn.mymoney.core.ui.components.FormCardContainer
import com.sinxn.mymoney.core.ui.components.FormPicker
import com.sinxn.mymoney.core.ui.components.PlaceSelectionDialog
import com.sinxn.mymoney.core.ui.components.WalletSelectionDialog
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateAddEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: TemplateAddEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    var activePicker by remember { mutableStateOf<FormPicker?>(null) }

    val evaluatedAmount = remember(uiState.amountStr, uiState.currencyDecimals) {
        viewModel.getImmediateResult(uiState.amountStr)
    }
    val amountNum = evaluatedAmount.toDoubleOrNull() ?: 0.0
    val isSaveEnabled = amountNum > 0.0 && uiState.walletId.isNotBlank() &&
            (uiState.type == TemplateType.TRANSFER || uiState.categoryId.isNotBlank()) &&
            (uiState.type != TemplateType.TRANSFER || uiState.targetWalletId.isNotBlank())

    val selectedWallet = uiState.availableWallets.find { it.id == uiState.walletId }
    val selectedTargetWallet = uiState.availableWallets.find { it.id == uiState.targetWalletId }
    val selectedCategory = uiState.availableCategories.find { it.id == uiState.categoryId }
    val selectedPlace = uiState.availablePlaces.find { it.id == uiState.placeId }
    val selectedEvent = uiState.availableEvents.find { it.id == uiState.eventId }

    val accentColor = when (uiState.type) {
        TemplateType.EXPENSE -> Color(0xFFE53935)
        TemplateType.INCOME -> Color(0xFF43A047)
        TemplateType.TRANSFER -> Color(0xFF1E88E5)
    }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                TemplateAddEditEvent.Saved -> onNavigateBack()
                TemplateAddEditEvent.Deleted -> onNavigateBack()
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Template") },
            text = { Text("Are you sure you want to delete this template?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        viewModel.deleteTemplate()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Pickers
    when (activePicker) {
        FormPicker.Category -> {
            CategorySelectionDialog(
                categories = uiState.availableCategories,
                showIncome = uiState.type == TemplateType.INCOME,
                selectedCategoryId = uiState.categoryId,
                onCategorySelected = { category ->
                    category?.let { viewModel.onCategoryChange(it.id) }
                    activePicker = null
                },
                onDismissRequest = { activePicker = null }
            )
        }
        FormPicker.Wallet -> {
            WalletSelectionDialog(
                wallets = uiState.availableWallets,
                selectedWalletId = uiState.walletId,
                onWalletSelected = {
                    viewModel.onWalletChange(it.id)
                    activePicker = null
                },
                onDismissRequest = { activePicker = null }
            )
        }
        FormPicker.TargetWallet -> {
            WalletSelectionDialog(
                wallets = uiState.availableWallets,
                selectedWalletId = uiState.targetWalletId,
                onWalletSelected = {
                    viewModel.onTargetWalletChange(it.id)
                    activePicker = null
                },
                onDismissRequest = { activePicker = null }
            )
        }
        FormPicker.Place -> {
            PlaceSelectionDialog(
                places = uiState.availablePlaces,
                selectedPlaceId = uiState.placeId,
                onPlaceSelected = {
                    viewModel.onPlaceChange(it?.id)
                    activePicker = null
                },
                onDismissRequest = { activePicker = null }
            )
        }
        FormPicker.Event -> {
            EventSelectionDialog(
                events = uiState.availableEvents,
                selectedEventId = uiState.eventId,
                onEventSelected = {
                    viewModel.onEventChange(it?.id)
                    activePicker = null
                },
                onDismissRequest = { activePicker = null }
            )
        }
        else -> {}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditMode) "Edit Template" else "New Template",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.isEditMode) {
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Template",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            val text = if (uiState.isEditMode) "Save Changes" else "Save Template"
            val icon = if (uiState.isEditMode) Icons.Default.Check else Icons.Default.Add
            AppExtendedFab(
                text = text,
                icon = icon,
                onClick = {
                    if (isSaveEnabled && !uiState.isSaving) {
                        viewModel.saveTemplate()
                    }
                },
                containerColor = if (isSaveEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isSaveEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Type Selector Tabs (Only active in New mode)
                    if (!uiState.isEditMode) {
                        item {
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                SegmentedButton(
                                    selected = uiState.type == TemplateType.EXPENSE,
                                    onClick = { viewModel.onTypeChange(TemplateType.EXPENSE) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                                ) {
                                    Text("Expense")
                                }
                                SegmentedButton(
                                    selected = uiState.type == TemplateType.INCOME,
                                    onClick = { viewModel.onTypeChange(TemplateType.INCOME) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                                ) {
                                    Text("Income")
                                }
                                SegmentedButton(
                                    selected = uiState.type == TemplateType.TRANSFER,
                                    onClick = { viewModel.onTypeChange(TemplateType.TRANSFER) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                                ) {
                                    Text("Transfer")
                                }
                            }
                        }
                    }

                    // Amount Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = if (uiState.type == TemplateType.TRANSFER) "Transfer Amount" else "Template Amount",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                val placeholderFrom = if (uiState.currencyDecimals == 0) "0" else "0." + "0".repeat(uiState.currencyDecimals)
                                OutlinedTextField(
                                    value = uiState.amountStr,
                                    onValueChange = viewModel::onAmountChange,
                                    label = { Text(if (uiState.type == TemplateType.TRANSFER) "From Amount (${uiState.currencySymbol})" else "Amount") },
                                    placeholder = { Text(placeholderFrom) },
                                    prefix = { Text(uiState.currencySymbol + " ", fontWeight = FontWeight.Bold) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    )
                                )

                                if (uiState.type == TemplateType.TRANSFER) {
                                    val placeholderTo = if (uiState.targetCurrencyDecimals == 0) "0" else "0." + "0".repeat(uiState.targetCurrencyDecimals)
                                    OutlinedTextField(
                                        value = uiState.amountToStr,
                                        onValueChange = viewModel::onAmountToChange,
                                        label = { Text("To Amount (${uiState.targetCurrencySymbol}) (Optional)") },
                                        placeholder = { Text(if (uiState.amountToStr.isBlank() && uiState.amountStr.isNotBlank()) evaluatedAmount else placeholderTo) },
                                        prefix = { Text(uiState.targetCurrencySymbol + " ", fontWeight = FontWeight.Bold) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                        )
                                    )

                                    OutlinedTextField(
                                        value = uiState.taxAmountStr,
                                        onValueChange = viewModel::onTaxAmountChange,
                                        label = { Text("Transfer Tax (Optional)") },
                                        placeholder = { Text(placeholderFrom) },
                                        prefix = { Text(uiState.currencySymbol + " ", fontWeight = FontWeight.Bold) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Core Entities Card (Category & Wallets)
                    item {
                        FormCardContainer(horizontalPadding = 0.dp) {
                            Column {
                                if (uiState.type != TemplateType.TRANSFER) {
                                    CleanListRow(
                                        icon = {
                                            if (selectedCategory != null) {
                                                CategoryIcon(
                                                    iconString = selectedCategory.icon,
                                                    categoryName = selectedCategory.name,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            } else {
                                                Icon(Icons.Default.Category, contentDescription = null, tint = accentColor)
                                            }
                                        },
                                        label = "Category",
                                        value = selectedCategory?.name ?: "Select Category",
                                        onClick = { activePicker = FormPicker.Category }
                                    )

                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }

                                CleanListRow(
                                    icon = {
                                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    },
                                    label = if (uiState.type == TemplateType.TRANSFER) "From Wallet" else "Wallet",
                                    value = selectedWallet?.name ?: "Select Wallet",
                                    onClick = { activePicker = FormPicker.Wallet }
                                )

                                if (uiState.type == TemplateType.TRANSFER) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )

                                    CleanListRow(
                                        icon = {
                                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                        },
                                        label = "To Wallet",
                                        value = selectedTargetWallet?.name ?: "Select Target Wallet",
                                        onClick = { activePicker = FormPicker.TargetWallet }
                                    )
                                }
                            }
                        }
                    }

                    // Details Card: Description, Place, Event, Note, Tag
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "Additional Details",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedTextField(
                                    value = uiState.description,
                                    onValueChange = viewModel::onDescriptionChange,
                                    label = { Text("Description (Optional)") },
                                    placeholder = { Text("e.g. Morning Coffee, Rent...") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    )
                                )

                                FormCardContainer(horizontalPadding = 0.dp) {
                                    Column {
                                        CleanListRow(
                                            icon = { Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                            label = "Place",
                                            value = selectedPlace?.name ?: "None",
                                            onClick = { activePicker = FormPicker.Place }
                                        )

                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )

                                        CleanListRow(
                                            icon = { Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                            label = "Event",
                                            value = selectedEvent?.name ?: "None",
                                            onClick = { activePicker = FormPicker.Event }
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = uiState.note,
                                    onValueChange = viewModel::onNoteChange,
                                    label = { Text("Note (Optional)") },
                                    minLines = 2,
                                    maxLines = 4,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    )
                                )

                                OutlinedTextField(
                                    value = uiState.tag,
                                    onValueChange = viewModel::onTagChange,
                                    label = { Text("Tag / Group (Optional)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    )
                                )
                            }
                        }
                    }

                    // Options Card: Confirmed & Count in total
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Confirmed",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "Mark created transactions as confirmed",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = uiState.confirmed,
                                        onCheckedChange = viewModel::onConfirmedChange
                                    )
                                }

                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Count in Total",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "Include in wallet balance totals",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = uiState.countInTotal,
                                        onCheckedChange = viewModel::onCountInTotalChange
                                    )
                                }
                            }
                        }
                    }

                    // Spacer for FAB
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }
}
