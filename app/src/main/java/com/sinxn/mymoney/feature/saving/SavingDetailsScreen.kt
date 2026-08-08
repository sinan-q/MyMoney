package com.sinxn.mymoney.feature.saving

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingDetailsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SavingDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                SavingDetailsEvent.Saved, SavingDetailsEvent.Deleted -> onNavigateBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditing) "Edit Saving Goal" else "New Saving Goal", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.isEditing) {
                        IconButton(onClick = viewModel::deleteSaving) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Goal", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    IconButton(onClick = viewModel::saveSaving) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                }
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    uiState.errorMessage?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Description
                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = viewModel::setDescription,
                        label = { Text("Description") },
                        placeholder = { Text("e.g. New Car, Vacation") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Target Amount & Initial Saved Money
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.targetMoneyInput,
                            onValueChange = viewModel::setTargetMoneyInput,
                            label = { Text("Target Goal") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = uiState.startMoneyInput,
                            onValueChange = viewModel::setStartMoneyInput,
                            label = { Text("Initial Saved") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    // Wallet Selection
                    var walletExpanded by remember { mutableStateOf(false) }
                    val selectedWallet = uiState.availableWallets.firstOrNull { it.id == uiState.walletId }

                    ExposedDropdownMenuBox(
                        expanded = walletExpanded,
                        onExpandedChange = { walletExpanded = !walletExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedWallet?.let { "${it.name} (${it.currency})" } ?: "Select Wallet",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Wallet") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = walletExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = walletExpanded,
                            onDismissRequest = { walletExpanded = false }
                        ) {
                            uiState.availableWallets.forEach { wallet ->
                                DropdownMenuItem(
                                    text = { Text("${wallet.name} (${wallet.currency})") },
                                    onClick = {
                                        viewModel.setWalletId(wallet.id)
                                        walletExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Note
                    OutlinedTextField(
                        value = uiState.note,
                        onValueChange = viewModel::setNote,
                        label = { Text("Note (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    Button(
                        onClick = viewModel::saveSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (uiState.isEditing) "Save Changes" else "Create Saving Goal", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
