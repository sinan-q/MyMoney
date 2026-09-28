package com.sinxn.mymoney.feature.review

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sinxn.mymoney.core.ui.components.TransactionItem
import com.sinxn.mymoney.core.util.MoneyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeedsReviewScreen(
    onNavigateBack: () -> Unit,
    onTransactionClick: (String) -> Unit,
    viewModel: NeedsReviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.formattingSettings.collectAsState()

    var showBulkActionDialog by remember { mutableStateOf(false) }

    val formatterConfig = MoneyFormatter.Config(
        showCurrency = settings.showCurrency,
        groupDigits = settings.groupDigits,
        roundDecimals = settings.roundDecimals,
        showPlusMinus = settings.showPlusMinus
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Needs Review") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.reviewItems.isNotEmpty()) {
                        IconButton(onClick = { showBulkActionDialog = true }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Bulk Actions")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.reviewItems.isEmpty()) {
                Text(
                    text = "No transactions need review.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = uiState.reviewItems,
                        key = { it.transaction.transaction.id }
                    ) { item ->
                        ReviewItemView(
                            item = item,
                            formatterConfig = formatterConfig,
                            dateFormat = settings.dateFormat,
                            onClick = { onTransactionClick(item.transaction.transaction.id) },
                            onAcceptSuggestion = { fieldId, value ->
                                viewModel.acceptSuggestion(item.transaction.transaction.id, fieldId, value)
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }

        if (showBulkActionDialog) {
            BulkActionDialog(
                reviewItems = uiState.reviewItems,
                onDismiss = { showBulkActionDialog = false },
                onApply = { fieldId, value, txIds ->
                    viewModel.applyBulkValue(fieldId, value, txIds)
                    showBulkActionDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReviewItemView(
    item: ReviewItemUiModel,
    formatterConfig: MoneyFormatter.Config,
    dateFormat: Int,
    onClick: () -> Unit,
    onAcceptSuggestion: (fieldId: String, value: String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TransactionItem(
            item = item.transaction,
            decimals = item.transaction.decimals ?: 2,
            currencyCode = item.transaction.currencySymbol ?: item.transaction.currencyCode ?: "USD",
            formatterConfig = formatterConfig,
            dateFormat = dateFormat,
            isLastItem = true, // Remove inner divider
            showDate = true,
            onClick = onClick
        )
        
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Missing Fields:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            item.missingFields.forEach { missingField ->
                var customValue by remember { mutableStateOf("") }
                
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = missingField.field.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (missingField.suggestions.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            missingField.suggestions.forEach { suggestion ->
                                SuggestionChip(
                                    onClick = { onAcceptSuggestion(missingField.field.id, suggestion) },
                                    label = { Text(suggestion) },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Accept",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = customValue,
                            onValueChange = { customValue = it },
                            placeholder = { Text("Enter ${missingField.field.label}") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { 
                                if (customValue.isNotBlank()) {
                                    onAcceptSuggestion(missingField.field.id, customValue)
                                }
                            },
                            enabled = customValue.isNotBlank()
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkActionDialog(
    reviewItems: List<ReviewItemUiModel>,
    onDismiss: () -> Unit,
    onApply: (fieldId: String, value: String, transactionIds: List<String>) -> Unit
) {
    // Collect all missing fields across all transactions
    val allMissingFields = reviewItems.flatMap { it.missingFields.map { mf -> mf.field } }.distinctBy { it.id }
    
    var selectedFieldId by remember { mutableStateOf(allMissingFields.firstOrNull()?.id) }
    var applyValue by remember { mutableStateOf("") }
    var fieldExpanded by remember { mutableStateOf(false) }
    
    val affectedTransactions = remember(selectedFieldId) {
        if (selectedFieldId == null) emptyList()
        else reviewItems.filter { it.missingFields.any { mf -> mf.field.id == selectedFieldId } }.map { it.transaction.transaction.id }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bulk Apply Value") },
        text = {
            if (allMissingFields.isEmpty()) {
                Text("No missing fields found.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Apply a value to all transactions missing a specific field.")
                    
                    ExposedDropdownMenuBox(
                        expanded = fieldExpanded,
                        onExpandedChange = { fieldExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = allMissingFields.find { it.id == selectedFieldId }?.label ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Target Field") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fieldExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = fieldExpanded,
                            onDismissRequest = { fieldExpanded = false }
                        ) {
                            allMissingFields.forEach { field ->
                                DropdownMenuItem(
                                    text = { Text(field.label) },
                                    onClick = {
                                        selectedFieldId = field.id
                                        fieldExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    OutlinedTextField(
                        value = applyValue,
                        onValueChange = { applyValue = it },
                        label = { Text("Value to apply") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Text(
                        text = "This will update ${affectedTransactions.size} transactions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    if (selectedFieldId != null && applyValue.isNotBlank()) {
                        onApply(selectedFieldId!!, applyValue, affectedTransactions)
                    }
                },
                enabled = selectedFieldId != null && applyValue.isNotBlank() && affectedTransactions.isNotEmpty()
            ) {
                Text("Apply to All")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
