package com.sinxn.mymoney.feature.transaction

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.CategorySelectionDialog
import com.sinxn.mymoney.core.ui.components.NumpadView
import com.sinxn.mymoney.core.ui.components.SelectionDialog
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MoneyFormatter
import java.text.SimpleDateFormat
import java.util.Locale

private fun resolveCategoryHierarchy(
    categoryId: String?,
    allCategories: List<CategoryEntity>
): Pair<String, String?> {
    val category = allCategories.find { it.id == categoryId }
    if (category == null) return "No Category" to null

    val cleanName = category.name.replace("  ↳ ", "")
    val parent = category.parentId?.let { parentId ->
        allCategories.find { it.id == parentId }
    }

    return if (parent != null) {
        cleanName to parent.name.replace("  ↳ ", "")
    } else {
        cleanName to null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailsScreen(
    onNavigateBack: () -> Unit,
    viewModel: TransactionDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.formattingSettings.collectAsState()
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val isEditing = uiState.isEditMode || uiState.isNewTransaction

    val handleBack: () -> Unit = {
        if (!uiState.isNewTransaction && uiState.isEditMode) {
            viewModel.toggleEditMode()
        } else {
            onNavigateBack()
        }
    }

    BackHandler(enabled = !uiState.isNewTransaction && uiState.isEditMode) {
        viewModel.toggleEditMode()
    }

    Scaffold(
        topBar = {
            MinimalTopBar(
                isNewTransaction = uiState.isNewTransaction,
                isEditMode = uiState.isEditMode,
                onBackClick = handleBack,
                onDeleteClick = { showDeleteConfirmation = true }
            )
        },
        floatingActionButton = {
            if (!isEditing) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.toggleEditMode() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(18.dp),
                    icon = { Icon(Icons.Default.Edit, contentDescription = "Edit") },
                    text = { Text("Edit Transaction", fontWeight = FontWeight.Bold) }
                )
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
                Crossfade(targetState = isEditing, label = "TransactionScreenMode") { editing ->
                    if (editing) {
                        UltraCleanTransactionContent(
                            uiState = uiState,
                            settings = settings,
                            viewModel = viewModel,
                            onNavigateBack = handleBack
                        )
                    } else {
                        ViewTransactionContent(
                            uiState = uiState,
                            settings = settings,
                            onEditClick = { viewModel.toggleEditMode() }
                        )
                    }
                }
            }
        }

        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Delete Transaction") },
                text = { Text("Are you sure you want to delete this transaction?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmation = false
                            viewModel.deleteTransaction { onNavigateBack() }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun MinimalTopBar(
    isNewTransaction: Boolean,
    isEditMode: Boolean,
    onBackClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back"
            )
        }

        Text(
            text = when {
                isNewTransaction -> "New Transaction"
                isEditMode -> "Edit Transaction"
                else -> "Transaction Details"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!isNewTransaction && !isEditMode) {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
        }
    }
}

// ==========================================
// VIEW MODE CONTENT (Read-Only Summary Page)
// ==========================================

@Composable
fun ViewTransactionContent(
    uiState: TransactionDetailsUiState,
    settings: com.sinxn.mymoney.core.data.preferences.FormattingSettings,
    onEditClick: () -> Unit
) {
    val transaction = uiState.transaction?.transaction ?: return
    val scrollState = rememberScrollState()

    val (categoryName, parentCategoryName) = resolveCategoryHierarchy(
        transaction.categoryId,
        uiState.availableCategories
    )
    val categoryIcon = uiState.transaction?.categoryIcon

    val directionColor = when (transaction.direction) {
        1 -> Color(0xFF10B981) // Income Mint
        2 -> Color(0xFF0284C7) // Transfer Blue
        else -> Color(0xFFE11D48) // Expense Rose
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 100.dp)
    ) {
        // 1. Centered Hero Amount Display (Matching Edit Screen Style)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Clean Large Amount Text
            val formattedAmountValue = MoneyFormatter.format(
                amount = transaction.money,
                currencyCode = "",
                decimals = uiState.currencyDecimals,
                config = MoneyFormatter.Config(
                    showCurrency = false,
                    groupDigits = settings.groupDigits,
                    roundDecimals = settings.roundDecimals,
                    showPlusMinus = false
                )
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = uiState.currencySymbol,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    color = directionColor
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formattedAmountValue,
                    fontSize = 58.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-1.5).sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Category Icon & Name Chip (Below Amount)
            Surface(
                shape = CircleShape,
                color = directionColor.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, directionColor.copy(alpha = 0.25f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    CategoryIcon(
                        iconString = categoryIcon,
                        categoryName = categoryName,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (parentCategoryName != null) {
                        Text(
                            text = "$parentCategoryName • ",
                            style = MaterialTheme.typography.labelMedium,
                            color = directionColor.copy(alpha = 0.8f)
                        )
                    }
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = directionColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Description Card (Separate Card if description exists)
        if (!transaction.description.isNullOrEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // 3. Primary Details Card (Wallet, Date & Time)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column {
                // Wallet Row
                ViewDetailRow(
                    icon = {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = directionColor, modifier = Modifier.size(22.dp))
                    },
                    label = "Wallet",
                    value = uiState.walletName.ifEmpty { "Default Wallet" }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))

                // Date & Time Row
                val parsedDate = DateUtils.parseDate(transaction.date)
                val formattedDate = DateUtils.formatDate(parsedDate, settings.dateFormat)
                val formattedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(parsedDate)

                ViewDetailRow(
                    icon = {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = directionColor, modifier = Modifier.size(20.dp))
                    },
                    label = if (settings.hideTime) "Date" else "Date & Time",
                    value = if (settings.hideTime) formattedDate else "$formattedDate at $formattedTime"
                )

                if (uiState.people.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))
                    ViewDetailRow(
                        icon = { Icon(Icons.Default.People, contentDescription = null, tint = directionColor, modifier = Modifier.size(20.dp)) },
                        label = "People",
                        value = uiState.people.joinToString { it.name }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Secondary Details Card (Matching Edit Screen Card 2)
        val hasSecondaryContent = uiState.place != null ||
                uiState.event != null ||
                !transaction.note.isNullOrEmpty() ||
                uiState.attachments.isNotEmpty() ||
                !settings.hideStatusAndImpact

        if (hasSecondaryContent) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column {
                    var hasPreviousRow = false

                    uiState.place?.let { place ->
                        ViewDetailRow(
                            icon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = directionColor, modifier = Modifier.size(20.dp)) },
                            label = "Place",
                            value = place.name
                        )
                        hasPreviousRow = true
                    }

                    uiState.event?.let { event ->
                        if (hasPreviousRow) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))
                        }
                        ViewDetailRow(
                            icon = { Icon(Icons.Default.Event, contentDescription = null, tint = directionColor, modifier = Modifier.size(20.dp)) },
                            label = "Event",
                            value = event.name
                        )
                        hasPreviousRow = true
                    }

                    if (!transaction.note.isNullOrEmpty()) {
                        if (hasPreviousRow) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))
                        }
                        ViewStackedDetailBlock(
                            icon = Icons.Default.Notes,
                            iconTint = directionColor,
                            label = "Note",
                            value = transaction.note
                        )
                        hasPreviousRow = true
                    }

                    if (uiState.attachments.isNotEmpty()) {
                        if (hasPreviousRow) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))
                        }
                        ViewDetailRow(
                            icon = { Icon(Icons.Default.AttachFile, contentDescription = null, tint = directionColor, modifier = Modifier.size(20.dp)) },
                            label = "Attachments",
                            value = "${uiState.attachments.size} files"
                        )
                        hasPreviousRow = true
                    }

                    if (!settings.hideStatusAndImpact) {
                        if (hasPreviousRow) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))
                        }

                        // Status Row with Badge
                        ViewDetailRow(
                            icon = { Icon(Icons.Default.Info, contentDescription = null, tint = directionColor, modifier = Modifier.size(20.dp)) },
                            label = "Status",
                            value = if (transaction.confirmed) "Confirmed" else "Pending",
                            trailingBadge = {
                                Surface(
                                    shape = CircleShape,
                                    color = if (transaction.confirmed) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, if (transaction.confirmed) Color(0xFF10B981).copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = if (transaction.confirmed) "Confirmed" else "Pending",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (transaction.confirmed) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))

                        // Impact Row with Badge
                        ViewDetailRow(
                            icon = { Icon(Icons.Default.Tune, contentDescription = null, tint = directionColor, modifier = Modifier.size(20.dp)) },
                            label = "Impact",
                            value = if (transaction.countInTotal) "Included in Total" else "Excluded from Total",
                            trailingBadge = {
                                Surface(
                                    shape = CircleShape,
                                    color = directionColor.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, directionColor.copy(alpha = 0.25f))
                                ) {
                                    Text(
                                        text = if (transaction.countInTotal) "Included in Total" else "Excluded from Total",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = directionColor,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewStackedDetailBlock(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun ViewDetailRow(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    trailingBadge: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            icon()
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        if (trailingBadge != null) {
            trailingBadge()
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ==========================================
// EDIT / ADD MODE CONTENT (Numpad + Form)
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UltraCleanTransactionContent(
    uiState: TransactionDetailsUiState,
    settings: com.sinxn.mymoney.core.data.preferences.FormattingSettings,
    viewModel: TransactionDetailsViewModel,
    onNavigateBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var showCategoryPicker by remember { mutableStateOf(false) }
    var showWalletPicker by remember { mutableStateOf(false) }
    var showPlacePicker by remember { mutableStateOf(false) }
    var showEventPicker by remember { mutableStateOf(false) }
    var showPeoplePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isNumpadVisible by remember { mutableStateOf(true) }

    // Minimal Direction Accent
    val accentColor = when (uiState.editDirection) {
        1 -> Color(0xFF10B981) // Income Mint
        2 -> Color(0xFF0284C7) // Transfer Blue
        else -> Color(0xFFE11D48) // Expense Rose
    }

    val hasOperatorInAmount = run {
        val amountStr = uiState.editAmount.trim()
        val rest = if (amountStr.startsWith("-")) amountStr.substring(1) else amountStr
        rest.contains("+") || rest.contains("-") || rest.contains("×") || rest.contains("÷")
    }

    val evaluatedAmountStr = viewModel.getImmediateResult(uiState.editAmount)
    val amountValue = evaluatedAmountStr.toDoubleOrNull()
    val isCategorySelected = !uiState.editCategoryId.isNullOrBlank()
    val isWalletSelected = uiState.editWalletId.isNotBlank()
    val isAmountNonNegative = amountValue != null && amountValue >= 0.0

    val isSaveEnabled = !uiState.isSaving && isCategorySelected && isWalletSelected && isAmountNonNegative

    val actionBtnText = if (uiState.isNewTransaction) {
        val dirName = if (uiState.editDirection == 1) "Income" else if (uiState.editDirection == 2) "Transfer" else "Expense"
        "Add $dirName"
    } else {
        "Save Changes"
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Scrollable Form Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(bottom = 16.dp)
        ) {
            // 1. Centered Large Amount Display (Click to open Numpad)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        isNumpadVisible = true
                    }
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val displayAmount = if (hasOperatorInAmount) {
                    viewModel.getImmediateResult(uiState.editAmount)
                } else {
                    if (uiState.editAmount.isEmpty()) "0" else uiState.editAmount
                }

                if (hasOperatorInAmount) {
                    Text(
                        text = uiState.editAmount,
                        style = MaterialTheme.typography.titleMedium,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = uiState.currencySymbol,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = displayAmount,
                        fontSize = 60.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-1.5).sp
                    )
                }

                if (!isNumpadVisible) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Amount",
                            tint = accentColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Tap to edit amount",
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Description Card (Top of inputs)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.editDescription,
                        onValueChange = viewModel::onDescriptionChange,
                        placeholder = { Text("Add description...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    isNumpadVisible = false
                                }
                            },
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Single Unified Options Card (Category to Count in Total)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column {
                    // Category Row
                    val activeCategory = uiState.availableCategories.find { it.id == uiState.editCategoryId }
                    CleanListRow(
                        icon = {
                            if (activeCategory != null) {
                                CategoryIcon(
                                    iconString = activeCategory.icon,
                                    categoryName = activeCategory.name,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Icon(Icons.Default.Category, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
                            }
                        },
                        label = "Category",
                        value = activeCategory?.name?.replace("  ↳ ", "") ?: "Select Category",
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            isNumpadVisible = false
                            showCategoryPicker = true
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Wallet Row
                    val activeWallet = uiState.availableWallets.find { it.id == uiState.editWalletId }
                    CleanListRow(
                        icon = {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
                        },
                        label = "Wallet",
                        value = activeWallet?.name ?: "Select Wallet",
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            isNumpadVisible = false
                            showWalletPicker = true
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Date Row
                    val parsedDate = DateUtils.parseDate(uiState.editDate)
                    val dateText = when {
                        DateUtils.isToday(parsedDate) -> "Today"
                        DateUtils.isYesterday(parsedDate) -> "Yesterday"
                        else -> DateUtils.formatDate(parsedDate, settings.dateFormat)
                    }

                    CleanListRow(
                        icon = {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                        },
                        label = "Date",
                        value = dateText,
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            isNumpadVisible = false
                            showDatePicker = true
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))

                    // People Row
                    val selectedPeopleNames = uiState.availablePeople
                        .filter { it.id in uiState.editPeopleIds }
                        .joinToString { it.name }
                        .ifEmpty { "None" }

                    CleanListRow(
                        icon = { Icon(Icons.Default.People, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp)) },
                        label = "People",
                        value = selectedPeopleNames,
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            isNumpadVisible = false
                            showPeoplePicker = true
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Place Row
                    val activePlace = uiState.availablePlaces.find { it.id == uiState.editPlaceId }
                    CleanListRow(
                        icon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp)) },
                        label = "Place",
                        value = activePlace?.name ?: "None",
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            isNumpadVisible = false
                            showPlacePicker = true
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Event Row
                    val activeEvent = uiState.availableEvents.find { it.id == uiState.editEventId }
                    CleanListRow(
                        icon = { Icon(Icons.Default.Event, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp)) },
                        label = "Event",
                        value = activeEvent?.name ?: "None",
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            isNumpadVisible = false
                            showEventPicker = true
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Note Field
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        OutlinedTextField(
                            value = uiState.editNote,
                            onValueChange = viewModel::onNoteChange,
                            label = { Text("Note") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        isNumpadVisible = false
                                    }
                                },
                            shape = RoundedCornerShape(14.dp),
                            leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null, tint = accentColor) }
                        )
                    }

                    if (!settings.hideStatusAndImpact) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))

                        // Confirmed Switch Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Confirmed", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Switch(
                                checked = uiState.editConfirmed,
                                onCheckedChange = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    viewModel.onConfirmedChange(it)
                                }
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))

                        // Count in Total Switch Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Count in Total", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Switch(
                                checked = uiState.editCountInTotal,
                                onCheckedChange = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    viewModel.onCountInTotalChange(it)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Docked Bottom Bar (Numpad or Save Button like IME sheet)
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
                        onSave = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            viewModel.saveChanges()
                            onNavigateBack()
                        },
                        onNext = {
                            isNumpadVisible = false
                            focusRequester.requestFocus()
                            keyboardController?.show()
                        },
                        hasOperatorInAmount = hasOperatorInAmount,
                        saveButtonText = actionBtnText,
                        saveButtonColor = accentColor,
                        isSaving = uiState.isSaving,
                        isSaveEnabled = isSaveEnabled
                    )
                }

                if (!isNumpadVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                viewModel.saveChanges()
                                onNavigateBack()
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

    // Modal Dialog Pickers
    if (showCategoryPicker) {
        CategorySelectionDialog(
            showIncome = uiState.editDirection == 1,
            incomeCategories = uiState.availableIncomeCategories,
            expenseCategories = uiState.availableExpenseCategories,
            selectedCategoryId = uiState.editCategoryId,
            onCategorySelected = { category -> viewModel.onCategoryIdChange(category.id) },
            onDismissRequest = { showCategoryPicker = false }
        )
    }

    if (showWalletPicker) {
        SelectionDialog(
            title = "Select Wallet",
            options = uiState.availableWallets,
            onOptionSelected = {
                viewModel.onWalletIdChange(it.id)
                showWalletPicker = false
            },
            onDismissRequest = { showWalletPicker = false },
            labelProvider = { it.name }
        )
    }

    if (showPlacePicker) {
        SelectionDialog(
            title = "Select Place",
            options = uiState.availablePlaces + com.sinxn.mymoney.core.data.local.entity.PlaceEntity("null", "None", "", null, null, null, false, 0, null),
            onOptionSelected = {
                viewModel.onPlaceIdChange(if (it.id == "null") null else it.id)
                showPlacePicker = false
            },
            onDismissRequest = { showPlacePicker = false },
            labelProvider = { it.name }
        )
    }

    if (showEventPicker) {
        SelectionDialog(
            title = "Select Event",
            options = uiState.availableEvents + com.sinxn.mymoney.core.data.local.entity.EventEntity("null", "None", "", null, "", "", false, 0, null),
            onOptionSelected = {
                viewModel.onEventIdChange(if (it.id == "null") null else it.id)
                showEventPicker = false
            },
            onDismissRequest = { showEventPicker = false },
            labelProvider = { it.name }
        )
    }

    if (showPeoplePicker) {
        SelectionDialog(
            title = "Select People",
            options = uiState.availablePeople,
            selectedOptions = uiState.availablePeople.filter { it.id in uiState.editPeopleIds }.toSet(),
            onOptionSelected = { viewModel.onPeopleToggle(it.id) },
            onDismissRequest = { showPeoplePicker = false },
            labelProvider = { it.name },
            multiSelect = true
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        viewModel.onDateChange(it)
                        showDatePicker = false
                    }
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun CleanListRow(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            icon()
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
