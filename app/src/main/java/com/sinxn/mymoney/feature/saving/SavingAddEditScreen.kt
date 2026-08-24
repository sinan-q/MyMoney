package com.sinxn.mymoney.feature.saving

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sinxn.mymoney.core.ui.components.AppExtendedFab
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.CleanListRow
import com.sinxn.mymoney.core.ui.components.EditAmountHeader
import com.sinxn.mymoney.core.ui.components.FormCardContainer
import com.sinxn.mymoney.core.ui.components.FormDatePickerDialog
import com.sinxn.mymoney.core.ui.components.NumpadView
import com.sinxn.mymoney.core.ui.components.rememberNumpadFormState
import com.sinxn.mymoney.ui.theme.IncomeColor

private val SavingAccentColor = IncomeColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingAddEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: SavingAddEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val numpadState = rememberNumpadFormState(initialNumpadVisible = uiState.isNewSaving)
    var showTargetDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val hasOperatorInAmount = remember(uiState.editAmount) {
        numpadState.hasOperator(uiState.editAmount)
    }
    val evaluatedAmountStr = remember(uiState.editAmount) {
        numpadState.getImmediateResult(uiState.editAmount, uiState.currencyDecimals)
    }
    val targetAmountValue = remember(evaluatedAmountStr) {
        evaluatedAmountStr.toDoubleOrNull()
    }

    val isSaveEnabled = (targetAmountValue != null && targetAmountValue > 0.0) &&
            uiState.editDescription.isNotBlank() &&
            uiState.editWalletId.isNotBlank() &&
            !uiState.isSaving

    val actionBtnText = if (uiState.isNewSaving) "Create Saving Goal" else "Save Changes"
    val titleText = if (uiState.isNewSaving) "New Saving Goal" else "Edit Saving Goal"

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Saving Goal") },
            text = { Text("Do you want to delete all associated transactions or keep them in history?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteSaving(deleteTransactions = true, onSuccess = onNavigateBack)
                }) {
                    Text("Delete All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        viewModel.deleteSaving(deleteTransactions = false, onSuccess = onNavigateBack)
                    }) {
                        Text("Keep Transactions")
                    }
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    if (showTargetDatePicker) {
        FormDatePickerDialog(
            initialDateString = uiState.editEndDate ?: "",
            onDateStringSelected = { dateStr ->
                viewModel.updateEndDate(dateStr)
                showTargetDatePicker = false
            },
            onDismissRequest = { showTargetDatePicker = false }
        )
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
                    icon = if (uiState.isNewSaving) Icons.Default.Add else Icons.Default.Check,
                    onClick = {
                        numpadState.dismiss()
                        viewModel.saveSaving { onNavigateBack() }
                    },
                    containerColor = SavingAccentColor,
                    contentColor = Color.White
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!uiState.isNewSaving) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 88.dp)
                ) {
                        // 1. Target Goal Amount Header (Hero Amount Input)
                        EditAmountHeader(
                            amountText = uiState.editAmount,
                            currencySymbol = uiState.currencySymbol,
                            evaluatedResult = evaluatedAmountStr,
                            hasOperatorInAmount = hasOperatorInAmount,
                            accentColor = SavingAccentColor,
                            isNumpadVisible = numpadState.isNumpadVisible,
                            onHeaderClick = { numpadState.showNumpad() }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Error Banner
                        uiState.errorMessage?.let { err ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    text = err,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        // 2. Goal Name / Description Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            FormCardContainer(horizontalPadding = 0.dp) {
                                OutlinedTextField(
                                    value = uiState.editDescription,
                                    onValueChange = viewModel::updateDescription,
                                    label = { Text("Goal Name") },
                                    placeholder = { Text("e.g. Emergency Fund, New Car, Vacation") },
                                    leadingIcon = {
                                        CategoryIcon(
                                            iconString = uiState.editIcon.ifBlank { "ic_saving" },
                                            categoryName = uiState.editDescription.ifBlank { "Goal" },
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = SavingAccentColor,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    )
                                )
                            }
                        }

                        // 3. Initial Starting Money Card (for new goals)
                        if (uiState.isNewSaving) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                FormCardContainer(horizontalPadding = 0.dp) {
                                    OutlinedTextField(
                                        value = uiState.editStartMoney,
                                        onValueChange = viewModel::updateStartMoney,
                                        label = { Text("Initial Saved Amount (Optional)") },
                                        placeholder = { Text("0.00") },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Savings,
                                                contentDescription = null,
                                                tint = SavingAccentColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        prefix = { Text("${uiState.currencySymbol} ") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = SavingAccentColor,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                        )
                                    )
                                }
                            }
                        }

                        // 4. Linked Wallet Selection Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            FormCardContainer(horizontalPadding = 0.dp) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Wallet,
                                            contentDescription = null,
                                            tint = SavingAccentColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Linked Wallet",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        uiState.availableWallets.forEach { wallet ->
                                            val isSelected = uiState.editWalletId == wallet.id
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    numpadState.dismiss()
                                                    viewModel.updateWalletId(wallet.id)
                                                },
                                                label = { Text("${wallet.name} (${wallet.currency})") },
                                                leadingIcon = if (isSelected) {
                                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                                } else null,
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = SavingAccentColor.copy(alpha = 0.15f),
                                                    selectedLabelColor = SavingAccentColor,
                                                    selectedLeadingIconColor = SavingAccentColor
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 5. Target Completion Date Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            FormCardContainer(horizontalPadding = 0.dp) {
                                CleanListRow(
                                    icon = {
                                        Icon(
                                            Icons.Default.Event,
                                            contentDescription = null,
                                            tint = SavingAccentColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    label = "Target Completion Date",
                                    value = uiState.editEndDate?.take(10) ?: "None (No Deadline)",
                                    onClick = {
                                        numpadState.dismiss()
                                        showTargetDatePicker = true
                                    },
                                    trailingIcon = if (uiState.editEndDate != null) {
                                        {
                                            IconButton(
                                                onClick = { viewModel.updateEndDate(null) }
                                            ) {
                                                Icon(
                                                    Icons.Default.Clear,
                                                    contentDescription = "Clear date",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    } else null
                                )
                            }
                        }

                        // 6. Note Card (Optional)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            FormCardContainer(horizontalPadding = 0.dp) {
                                OutlinedTextField(
                                    value = uiState.editNote,
                                    onValueChange = viewModel::updateNote,
                                    label = { Text("Note (Optional)") },
                                    placeholder = { Text("Add any extra notes or plans...") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Notes,
                                            contentDescription = null,
                                            tint = SavingAccentColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    minLines = 2,
                                    maxLines = 4,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = SavingAccentColor,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    )
                                )
                            }
                        }

                        // 7. Tag Card (Optional)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            FormCardContainer(horizontalPadding = 0.dp) {
                                OutlinedTextField(
                                    value = uiState.editTag,
                                    onValueChange = viewModel::updateTag,
                                    label = { Text("Tag / Group (Optional)") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Label,
                                            contentDescription = null,
                                            tint = SavingAccentColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = SavingAccentColor,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    )
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
            saveButtonColor = SavingAccentColor,
            onDismiss = { numpadState.dismiss() }
        )
    }
}
