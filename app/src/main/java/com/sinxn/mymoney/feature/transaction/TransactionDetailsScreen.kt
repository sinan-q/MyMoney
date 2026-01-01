package com.sinxn.mymoney.feature.transaction

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.CategorySelectionDialog
import com.sinxn.mymoney.core.ui.components.SelectionDialog
import com.sinxn.mymoney.core.util.MoneyFormatter
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailsScreen(
    onNavigateBack: () -> Unit,
    viewModel: TransactionDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.formattingSettings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "Edit Transaction" else "Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (uiState.isEditMode) viewModel.saveChanges() else viewModel.toggleEditMode() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = {
                    Crossfade(targetState = uiState.isEditMode, label = "fabIcon") { isEdit ->
                        Icon(
                            imageVector = if (isEdit) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = if (isEdit) "Save" else "Edit"
                        )
                    }
                },
                text = {
                    AnimatedContent(targetState = uiState.isEditMode, label = "fabText") { isEdit ->
                        Text(if (isEdit) "Save" else "Edit")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                TransactionContent(
                    uiState = uiState,
                    settings = settings,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun TransactionContent(
    uiState: TransactionDetailsUiState,
    settings: com.sinxn.mymoney.core.data.preferences.FormattingSettings,
    viewModel: TransactionDetailsViewModel
) {
    if (uiState.transaction == null && !uiState.isNewTransaction) return
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Hero Header
        val rawAmount = if (uiState.isEditMode) {
             val multiplier = 10.0.pow(uiState.currencyDecimals.toDouble())
             (uiState.editAmount.replace(",", ".").toDoubleOrNull()?.let { it * multiplier } ?: 0.0).toLong()
        } else {
             uiState.transaction?.transaction?.money ?: 0L
        }
        
        val displayDirection = if (uiState.isEditMode) uiState.editDirection else uiState.transaction?.transaction?.direction ?: 0
        val displayCategoryName = if (uiState.isEditMode) {
            uiState.availableCategories.find { it.id == uiState.editCategoryId }?.name ?: "No Category"
        } else {
            uiState.transaction?.categoryName ?: "No Category"
        }
        val displayCategoryIcon = if (uiState.isEditMode) {
            uiState.availableCategories.find { it.id == uiState.editCategoryId }?.icon
        } else {
            uiState.transaction?.categoryIcon
        }

        TransactionHeroHeader(
            amount = if (displayDirection == 1) rawAmount else -rawAmount,
            categoryIcon = displayCategoryIcon,
            categoryName = displayCategoryName,
            currencySymbol = uiState.currencySymbol, 
            currencyDecimals = uiState.currencyDecimals, 
            settings = settings,
            categoryColor = uiState.categoryColor
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Details List wrapped in a premium card
        Card(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            border = BorderStroke(
                1.dp, 
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (uiState.isEditMode) {
                    EditForm(uiState, viewModel)
                } else {
                    ViewDetails(uiState, settings)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(100.dp)) // FAB spacing
    }
}

@Composable
fun TransactionHeroHeader(
    amount: Long,
    categoryIcon: String?,
    categoryName: String,
    currencySymbol: String,
    currencyDecimals: Int,
    settings: com.sinxn.mymoney.core.data.preferences.FormattingSettings,
    categoryColor: Color
) {
    val secondaryColor = remember(categoryColor) { 
        Color(android.graphics.Color.HSVToColor(FloatArray(3).apply {
            android.graphics.Color.colorToHSV(categoryColor.toArgb(), this)
            this[2] *= 0.7f // Darken
            this[0] = (this[0] + 30) % 360 // Shift hue
        }))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(categoryColor, secondaryColor)
                )
            )
    ) {
        // Subtle decorative background circles for "Premium" look
        Canvas(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 60.dp)
        ) {
            drawCircle(
                color = Color.White.copy(alpha = 0.15f),
                radius = size.minDimension
            )
        }

        Column(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Category Icon with Glassmorphism
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CategoryIcon(
                        iconString = categoryIcon,
                        categoryName = categoryName,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val formattedMoney = MoneyFormatter.format(
                amount = amount,
                currencyCode = currencySymbol,
                decimals = currencyDecimals,
                config = MoneyFormatter.Config(
                    showCurrency = settings.showCurrency,
                    groupDigits = settings.groupDigits,
                    roundDecimals = settings.roundDecimals,
                    showPlusMinus = settings.showPlusMinus
                )
            )
            
            Text(
                text = formattedMoney,
                style = MaterialTheme.typography.displayMedium.copy(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.15f),
                        offset = Offset(2f, 4f),
                        blurRadius = 8f
                    )
                ),
                color = Color.White,
                fontWeight = FontWeight.Black
            )
            
            Text(
                text = categoryName,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ViewDetails(
    uiState: TransactionDetailsUiState,
    settings: com.sinxn.mymoney.core.data.preferences.FormattingSettings
) {
    val transaction = uiState.transaction?.transaction ?: return
    
    // Date & Time Merge
    val date = com.sinxn.mymoney.core.util.DateUtils.parseDate(transaction.date)
    val formattedDate = com.sinxn.mymoney.core.util.DateUtils.formatDate(date, settings.dateFormat)
    val formattedTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(date)
    
    DetailItem(
        icon = Icons.Default.CalendarToday,
        label = "Date & Time", 
        value = "$formattedDate, $formattedTime"
    )
    
    // Wallet (Missing field added)
    DetailItem(
        icon = Icons.Default.AccountBalanceWallet,
        label = "Wallet",
        value = uiState.walletName
    )
    
    if (!transaction.note.isNullOrEmpty()) {
        DetailItem(
            icon = Icons.Default.Notes,
            label = "Note", 
            value = transaction.note
        )
    }
    
    if (!transaction.description.isNullOrEmpty()) {
        DetailItem(
            icon = Icons.Default.Description,
            label = "Description", 
            value = transaction.description
        )
    }

    // Extended Fields
    uiState.place?.let { 
        DetailItem(
            icon = Icons.Default.LocationOn,
            label = "Place", 
            value = it.name
        ) 
    }
    uiState.event?.let { 
        DetailItem(
            icon = Icons.Default.Event,
            label = "Event", 
            value = it.name
        ) 
    }
    if (uiState.people.isNotEmpty()) {
        DetailItem(
            icon = Icons.Default.People,
            label = "People", 
            value = uiState.people.joinToString { it.name }
        )
    }
    if (uiState.attachments.isNotEmpty()) {
        DetailItem(
            icon = Icons.Default.AttachFile,
            label = "Attachments", 
            value = "${uiState.attachments.size} files"
        )
    }

    DetailItem(
        icon = Icons.Default.Info,
        label = "Status", 
        value = if (transaction.confirmed) "Confirmed" else "Pending"
    )
    DetailItem(
        icon = Icons.Default.Info,
        label = "Impact", 
        value = if (transaction.countInTotal) "Included in Total" else "Excluded"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditForm(
    uiState: TransactionDetailsUiState,
    viewModel: TransactionDetailsViewModel
) {
    // Dialog States
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showWalletPicker by remember { mutableStateOf(false) }
    var showPlacePicker by remember { mutableStateOf(false) }
    var showEventPicker by remember { mutableStateOf(false) }
    var showPeoplePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Logic to hide Category for Debt/Saving
    // Ideally this logic relies on Checking TransactionType, which we should expose in UiState if needed.
    // Legacy app hides Category for DEBT (2) and SAVING (3)
    val transactionType = uiState.transaction?.transaction?.type ?: 0
    val showCategory = transactionType != 2 && transactionType != 3

    // Amount
    OutlinedTextField(
        value = uiState.editAmount,
        onValueChange = viewModel::onAmountChange,
        label = { Text("Amount") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        leadingIcon = { Icon(Icons.Default.Inventory, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
        ),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedBorderColor = MaterialTheme.colorScheme.primary
        )
    )

    // Date & Time Selectors
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val dateTimeParts = uiState.editDate.split(" ")
        val datePart = dateTimeParts.firstOrNull() ?: ""
        val timePart = dateTimeParts.getOrNull(1)?.take(5) ?: "" // Take HH:mm
        
        OutlinedTextField(
            value = datePart,
            onValueChange = {},
            label = { Text("Date") },
            modifier = Modifier.weight(1f).clickable { showDatePicker = true },
            enabled = false,
            shape = RoundedCornerShape(16.dp),
            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                disabledLeadingIconColor = MaterialTheme.colorScheme.primary
            )
        )
        OutlinedTextField(
            value = timePart,
            onValueChange = {},
            label = { Text("Time") },
            modifier = Modifier.weight(1f).clickable { showTimePicker = true },
            enabled = false,
            shape = RoundedCornerShape(16.dp),
            leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                disabledLeadingIconColor = MaterialTheme.colorScheme.primary
            )
        )
    }
    
    // Pickers
    if (showCategory) {
        PickerField(
            icon = Icons.Default.Inventory,
            label = "Category",
            value = uiState.availableCategories.find { it.id == uiState.editCategoryId }?.name ?: "Select Category",
            onClick = { showCategoryPicker = true }
        )
    }
    
    PickerField(
        icon = Icons.Default.AccountBalanceWallet,
        label = "Wallet",
        value = uiState.availableWallets.find { it.id == uiState.editWalletId }?.name ?: "Select Wallet",
        onClick = { showWalletPicker = true }
    )

    // Note & Description
    OutlinedTextField(
        value = uiState.editNote,
        onValueChange = viewModel::onNoteChange,
        label = { Text("Note") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedBorderColor = MaterialTheme.colorScheme.primary
        )
    )

    OutlinedTextField(
        value = uiState.editDescription,
        onValueChange = viewModel::onDescriptionChange,
        label = { Text("Description") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedBorderColor = MaterialTheme.colorScheme.primary
        )
    )

    // Advanced Fields
    PickerField(
        icon = Icons.Default.LocationOn,
        label = "Place",
        value = uiState.availablePlaces.find { it.id == uiState.editPlaceId }?.name ?: "No Place",
        onClick = { showPlacePicker = true }
    )
    
    PickerField(
        icon = Icons.Default.Event,
        label = "Event",
        value = uiState.availableEvents.find { it.id == uiState.editEventId }?.name ?: "No Event",
        onClick = { showEventPicker = true }
    )

    val selectedPeopleNames = uiState.availablePeople
        .filter { it.id in uiState.editPeopleIds }
        .joinToString { it.name }
        .ifEmpty { "No People" }
    
    PickerField(
        icon = Icons.Default.People,
        label = "People",
        value = selectedPeopleNames,
        onClick = { showPeoplePicker = true }
    )

    // Toggles with better styling
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Confirmed", style = MaterialTheme.typography.bodyLarge)
                }
                Switch(checked = uiState.editConfirmed, onCheckedChange = viewModel::onConfirmedChange)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Count in Total", style = MaterialTheme.typography.bodyLarge)
                }
                Switch(checked = uiState.editCountInTotal, onCheckedChange = viewModel::onCountInTotalChange)
            }
        }
    }

    // Dialogs
    if (showCategoryPicker) {
        CategorySelectionDialog(
            showIncome = uiState.editDirection == 1,
            incomeCategories = uiState.availableIncomeCategories,
            expenseCategories = uiState.availableExpenseCategories,
            selectedCategoryId = uiState.editCategoryId,
            onCategorySelected = { category ->
                viewModel.onCategoryIdChange(category.id)
            },
            onDismissRequest = { showCategoryPicker = false }
        )
    }
    
    if (showWalletPicker) {
        com.sinxn.mymoney.core.ui.components.SelectionDialog(
            title = "Select Wallet",
            options = uiState.availableWallets,
            onOptionSelected = { viewModel.onWalletIdChange(it.id); showWalletPicker = false },
            onDismissRequest = { showWalletPicker = false },
            labelProvider = { it.name }
        )
    }
    
    if (showPlacePicker) {
        com.sinxn.mymoney.core.ui.components.SelectionDialog(
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
         com.sinxn.mymoney.core.ui.components.SelectionDialog(
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
        com.sinxn.mymoney.core.ui.components.SelectionDialog(
            title = "Select People",
            options = uiState.availablePeople,
            selectedOptions = uiState.availablePeople.filter { it.id in uiState.editPeopleIds }.toSet(),
            onOptionSelected = { viewModel.onPeopleToggle(it.id) },
            onDismissRequest = { showPeoplePicker = false },
            labelProvider = { it.name },
            multiSelect = true
        )
    }
    
    // Date/Time Dialogs
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
    
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState()
        // Custom Dialog for Time Picker since Material3 TimePickerDialog is experimental/limited in some versions
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                  TextButton(onClick = {
                      viewModel.onTimeChange(timePickerState.hour, timePickerState.minute)
                      showTimePicker = false
                }) { Text("OK") }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}

@Composable
fun PickerField(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        enabled = false,
        shape = RoundedCornerShape(16.dp),
        leadingIcon = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledLeadingIconColor = MaterialTheme.colorScheme.primary
        ),
        trailingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
    )
}

@Composable
fun DetailItem(
    icon: ImageVector,
    label: String, 
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label, 
                style = MaterialTheme.typography.labelMedium, 
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value, 
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
