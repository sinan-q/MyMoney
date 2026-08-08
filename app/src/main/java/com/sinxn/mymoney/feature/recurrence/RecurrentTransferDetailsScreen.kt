package com.sinxn.mymoney.feature.recurrence

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sinxn.mymoney.core.util.RecurrenceSetting

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurrentTransferDetailsScreen(
    onNavigateBack: () -> Unit,
    viewModel: RecurrentTransferDetailsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showRecurrencePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (uiState.isNew) "New Recurrent Transfer" else "Edit Recurrent Transfer")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.save(onSuccess = onNavigateBack) }) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Description
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = viewModel::onDescriptionChanged,
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Source Wallet Dropdown
                var fromExpanded by remember { mutableStateOf(false) }
                val fromWalletName = uiState.availableWallets.find { it.id == uiState.walletFromId }?.name ?: ""

                ExposedDropdownMenuBox(
                    expanded = fromExpanded,
                    onExpandedChange = { fromExpanded = !fromExpanded }
                ) {
                    OutlinedTextField(
                        value = fromWalletName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Source Wallet") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = fromExpanded,
                        onDismissRequest = { fromExpanded = false }
                    ) {
                        uiState.availableWallets.forEach { w ->
                            DropdownMenuItem(
                                text = { Text(w.name) },
                                onClick = {
                                    viewModel.onWalletFromChanged(w.id)
                                    fromExpanded = false
                                }
                            )
                        }
                    }
                }

                // Destination Wallet Dropdown
                var toExpanded by remember { mutableStateOf(false) }
                val toWalletName = uiState.availableWallets.find { it.id == uiState.walletToId }?.name ?: ""

                ExposedDropdownMenuBox(
                    expanded = toExpanded,
                    onExpandedChange = { toExpanded = !toExpanded }
                ) {
                    OutlinedTextField(
                        value = toWalletName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Destination Wallet") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = toExpanded,
                        onDismissRequest = { toExpanded = false }
                    ) {
                        uiState.availableWallets.filter { it.id != uiState.walletFromId }.forEach { w ->
                            DropdownMenuItem(
                                text = { Text(w.name) },
                                onClick = {
                                    viewModel.onWalletToChanged(w.id)
                                    toExpanded = false
                                }
                            )
                        }
                    }
                }

                // Money From
                OutlinedTextField(
                    value = uiState.moneyFromStr,
                    onValueChange = viewModel::onMoneyFromChanged,
                    label = { Text("Amount From") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                // Money To
                OutlinedTextField(
                    value = uiState.moneyToStr,
                    onValueChange = viewModel::onMoneyToChanged,
                    label = { Text("Amount To") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                // Money Tax (Optional)
                OutlinedTextField(
                    value = uiState.moneyTaxStr,
                    onValueChange = viewModel::onMoneyTaxChanged,
                    label = { Text("Tax Amount (Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                // Recurrence Rule Picker Button Card
                val setting = remember(uiState.startDate, uiState.rule) {
                    RecurrenceSetting.fromStringOrFallback(uiState.startDate, uiState.rule)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showRecurrencePicker = true },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Repeat, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Recurrence Rule", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(setting.getUserReadableString(context), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Place Dropdown (Optional)
                if (uiState.availablePlaces.isNotEmpty()) {
                    var placeExpanded by remember { mutableStateOf(false) }
                    val selectedPlaceName = uiState.availablePlaces.find { it.id == uiState.placeId }?.name ?: "None"

                    ExposedDropdownMenuBox(
                        expanded = placeExpanded,
                        onExpandedChange = { placeExpanded = !placeExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedPlaceName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Place (Optional)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = placeExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = placeExpanded,
                            onDismissRequest = { placeExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("None") },
                                onClick = {
                                    viewModel.onPlaceChanged(null)
                                    placeExpanded = false
                                }
                            )
                            uiState.availablePlaces.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p.name) },
                                    onClick = {
                                        viewModel.onPlaceChanged(p.id)
                                        placeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Event Dropdown (Optional)
                if (uiState.availableEvents.isNotEmpty()) {
                    var eventExpanded by remember { mutableStateOf(false) }
                    val selectedEventName = uiState.availableEvents.find { it.id == uiState.eventId }?.name ?: "None"

                    ExposedDropdownMenuBox(
                        expanded = eventExpanded,
                        onExpandedChange = { eventExpanded = !eventExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedEventName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Event (Optional)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = eventExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = eventExpanded,
                            onDismissRequest = { eventExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("None") },
                                onClick = {
                                    viewModel.onEventChanged(null)
                                    eventExpanded = false
                                }
                            )
                            uiState.availableEvents.forEach { ev ->
                                DropdownMenuItem(
                                    text = { Text(ev.name) },
                                    onClick = {
                                        viewModel.onEventChanged(ev.id)
                                        eventExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Note
                OutlinedTextField(
                    value = uiState.note,
                    onValueChange = viewModel::onNoteChanged,
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Switches
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Confirmed")
                    Switch(checked = uiState.confirmed, onCheckedChange = viewModel::onConfirmedChanged)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Count in total")
                    Switch(checked = uiState.countInTotal, onCheckedChange = viewModel::onCountInTotalChanged)
                }
            }
        }
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
