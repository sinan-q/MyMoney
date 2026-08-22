package com.sinxn.mymoney.feature.budget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sinxn.mymoney.core.data.repository.BudgetPeriod
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.CategorySelectionDialog
import com.sinxn.mymoney.core.ui.components.CleanListRow
import com.sinxn.mymoney.core.ui.components.EditAmountHeader
import com.sinxn.mymoney.core.ui.components.FormCardContainer
import com.sinxn.mymoney.core.ui.components.FormDatePickerDialog
import com.sinxn.mymoney.core.ui.components.NumpadView
import com.sinxn.mymoney.core.ui.components.TabPill
import com.sinxn.mymoney.core.ui.components.rememberNumpadFormState
import com.sinxn.mymoney.core.util.BudgetType

private val ExpenseColor = Color(0xFFE53935)
private val IncomeColor = Color(0xFF43A047)
private val CategoryColor = Color(0xFF1E88E5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetAddEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: BudgetAddEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val numpadState = rememberNumpadFormState(initialNumpadVisible = uiState.isNewBudget)
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showPeriodDropdown by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val accentColor = when (uiState.editType) {
        BudgetType.EXPENSES -> ExpenseColor
        BudgetType.INCOMES -> IncomeColor
        else -> CategoryColor
    }

    val hasOperatorInAmount = remember(uiState.editAmount) {
        numpadState.hasOperator(uiState.editAmount)
    }
    val evaluatedAmountStr = remember(uiState.editAmount) {
        numpadState.getImmediateResult(uiState.editAmount, uiState.currencyDecimals)
    }
    val amountValue = remember(evaluatedAmountStr) {
        evaluatedAmountStr.toDoubleOrNull()
    }

    val isSaveEnabled = (amountValue != null && amountValue > 0.0) &&
            (uiState.editType != BudgetType.CATEGORY || !uiState.editCategoryId.isNullOrBlank()) &&
            uiState.selectedWalletIds.isNotEmpty() &&
            uiState.walletWarning == null &&
            !uiState.isSaving

    val actionBtnText = if (uiState.isNewBudget) "Create Budget" else "Save Changes"
    val titleText = if (uiState.isNewBudget) "New Budget" else "Edit Budget"

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Budget") },
            text = { Text("Are you sure you want to delete this budget?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteBudget(onSuccess = onNavigateBack)
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCategoryDialog) {
        CategorySelectionDialog(
            showIncome = uiState.editType == BudgetType.INCOMES,
            incomeCategories = uiState.incomeCategories,
            expenseCategories = uiState.expenseCategories,
            selectedCategoryId = uiState.editCategoryId,
            onCategorySelected = { cat ->
                viewModel.updateCategory(cat)
                showCategoryDialog = false
            },
            onDismissRequest = { showCategoryDialog = false }
        )
    }

    if (showStartDatePicker) {
        FormDatePickerDialog(
            initialDateString = uiState.editStartDate,
            onDateStringSelected = { dateStr ->
                viewModel.updateStartDate(dateStr)
                showStartDatePicker = false
            },
            onDismissRequest = { showStartDatePicker = false }
        )
    }

    if (showEndDatePicker) {
        FormDatePickerDialog(
            initialDateString = uiState.editEndDate,
            onDateStringSelected = { dateStr ->
                viewModel.updateEndDate(dateStr)
                showEndDatePicker = false
            },
            onDismissRequest = { showEndDatePicker = false }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                    if (!uiState.isNewBudget) {
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
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 16.dp)
                    ) {
                        // 1. Type Selector Tabs
                        TabPill(
                            tabs = listOf(
                                "Expenses" to ExpenseColor,
                                "Incomes" to IncomeColor,
                                "Category" to CategoryColor
                            ),
                            activeTab = uiState.editType,
                            onTabChange = { viewModel.updateType(it) }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // 2. Edit Amount Header (Hero Amount Input)
                        EditAmountHeader(
                            amountText = uiState.editAmount,
                            currencySymbol = uiState.currencySymbol,
                            evaluatedResult = evaluatedAmountStr,
                            hasOperatorInAmount = hasOperatorInAmount,
                            accentColor = accentColor,
                            isNumpadVisible = numpadState.isNumpadVisible,
                            onHeaderClick = { numpadState.showNumpad() }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Error or Warning banners
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

                        uiState.walletWarning?.let { warning ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFFF3E0),
                                border = BorderStroke(1.dp, Color(0xFFFFB74D))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFE65100),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = warning,
                                        color = Color(0xFFE65100),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // 3. Category Card (if Category Type)
                        if (uiState.editType == BudgetType.CATEGORY) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                FormCardContainer(horizontalPadding = 0.dp) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                numpadState.dismiss()
                                                showCategoryDialog = true
                                            }
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val category = uiState.selectedCategory
                                        if (category != null) {
                                            CategoryIcon(
                                                iconString = category.icon,
                                                categoryName = category.name,
                                                modifier = Modifier.size(36.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Category",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = category.name,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(accentColor.copy(alpha = 0.12f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Category,
                                                    contentDescription = null,
                                                    tint = accentColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = "Select Category",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        Icon(
                                            Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }

                        // 4. Renewal Period & Dates Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            FormCardContainer {
                                Column {
                                    val periodNames = listOf(
                                        BudgetPeriod.CUSTOM to "Custom (Fixed Dates)",
                                        BudgetPeriod.WEEKLY to "Weekly (Auto-renew)",
                                        BudgetPeriod.MONTHLY to "Monthly (Auto-renew)",
                                        BudgetPeriod.ANNUAL to "Annual (Auto-renew)"
                                    )
                                    val currentPeriodLabel = periodNames.firstOrNull { it.first == uiState.editPeriod }?.second ?: "Monthly (Auto-renew)"

                                    // Renewal Period Row
                                    Box {
                                        CleanListRow(
                                            icon = {
                                                Icon(
                                                    Icons.Default.Repeat,
                                                    contentDescription = null,
                                                    tint = accentColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            },
                                            label = "Renewal Period",
                                            value = currentPeriodLabel,
                                            onClick = {
                                                numpadState.dismiss()
                                                showPeriodDropdown = true
                                            }
                                        )

                                        DropdownMenu(
                                            expanded = showPeriodDropdown,
                                            onDismissRequest = { showPeriodDropdown = false }
                                        ) {
                                            periodNames.forEach { (periodVal, label) ->
                                                DropdownMenuItem(
                                                    text = { Text(label) },
                                                    leadingIcon = if (uiState.editPeriod == periodVal) {
                                                        { Icon(Icons.Default.Check, contentDescription = null, tint = accentColor) }
                                                    } else null,
                                                    onClick = {
                                                        viewModel.updatePeriod(periodVal)
                                                        showPeriodDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )

                                    // Start Date Row
                                    CleanListRow(
                                        icon = {
                                            Icon(
                                                Icons.Default.Event,
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        label = "Start Date",
                                        value = uiState.editStartDate.take(10).ifBlank { "Select Start Date" },
                                        onClick = {
                                            numpadState.dismiss()
                                            showStartDatePicker = true
                                        }
                                    )

                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )

                                    // End Date Row
                                    CleanListRow(
                                        icon = {
                                            Icon(
                                                Icons.Default.DateRange,
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        label = "End Date",
                                        value = uiState.editEndDate.take(10).ifBlank { "Select End Date" },
                                        onClick = {
                                            numpadState.dismiss()
                                            showEndDatePicker = true
                                        }
                                    )
                                }
                            }
                        }

                        // 5. Linked Wallets Card
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
                                            tint = accentColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Linked Wallets",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = "${uiState.selectedWalletIds.size} selected",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        uiState.availableWallets.forEach { wallet ->
                                            val isSelected = uiState.selectedWalletIds.contains(wallet.id)
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    numpadState.dismiss()
                                                    viewModel.toggleWalletSelection(wallet.id)
                                                },
                                                label = { Text("${wallet.name} (${wallet.currency})") },
                                                leadingIcon = if (isSelected) {
                                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                                } else null,
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = accentColor.copy(alpha = 0.15f),
                                                    selectedLabelColor = accentColor,
                                                    selectedLeadingIconColor = accentColor
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 6. Optional Tag / Group Card
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
                                            tint = accentColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = accentColor,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    )
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
                                        viewModel.saveBudget { onNavigateBack() }
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
                                    if (uiState.isSaving) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
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
}
