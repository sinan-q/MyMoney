package com.sinxn.mymoney.feature.debt.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sinxn.mymoney.core.ui.components.CleanListRow
import com.sinxn.mymoney.core.ui.components.FormCardContainer
import com.sinxn.mymoney.core.ui.components.FormDatePickerDialog
import com.sinxn.mymoney.core.ui.components.FormPicker
import com.sinxn.mymoney.core.ui.components.PeopleSelectionDialog
import com.sinxn.mymoney.core.ui.components.PlaceSelectionDialog
import com.sinxn.mymoney.core.ui.components.WalletSelectionDialog
import com.sinxn.mymoney.feature.debt.DebtAddEditUiState
import com.sinxn.mymoney.feature.debt.DebtAddEditViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtFormContent(
    accentColor: Color,
    focusRequester: FocusRequester,
    uiState: DebtAddEditUiState,
    viewModel: DebtAddEditViewModel,
    onFocusField: () -> Unit,
    onDismissKeyboardAndNumpad: () -> Unit
) {
    var activePicker by remember { mutableStateOf<FormPicker?>(null) }

    val selectedWallet = uiState.availableWallets.find { it.id == uiState.editWalletId }
    val selectedPlace = uiState.availablePlaces.find { it.id == uiState.editPlaceId }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Description Input Card
        OutlinedTextField(
            value = uiState.editDescription,
            onValueChange = viewModel::updateDescription,
            label = { Text("Description") },
            placeholder = { Text("e.g. Lunch with team, Loan for car...") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        onFocusField()
                    }
                },
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        // Form Fields (Wallet, Dates, People, Place)
        FormCardContainer(horizontalPadding = 0.dp) {
            Column {
                // Wallet Selector Row
                CleanListRow(
                    icon = { Icon(Icons.Default.Wallet, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp)) },
                    label = "Associated Wallet",
                    value = selectedWallet?.name ?: "Select Wallet",
                    onClick = {
                        onDismissKeyboardAndNumpad()
                        activePicker = FormPicker.Wallet
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))

                // Creation Date Row
                CleanListRow(
                    icon = { Icon(Icons.Default.Event, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp)) },
                    label = "Date",
                    value = uiState.editDate.take(10),
                    onClick = {
                        onDismissKeyboardAndNumpad()
                        activePicker = FormPicker.Date
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))

                // Expiration / Due Date Row
                CleanListRow(
                    icon = { Icon(Icons.Default.Event, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp)) },
                    label = "Due Date (Optional)",
                    value = uiState.editExpirationDate?.take(10) ?: "Not Set",
                    onClick = {
                        onDismissKeyboardAndNumpad()
                        activePicker = FormPicker.DueDate
                    },
                    trailingBadge = if (!uiState.editExpirationDate.isNullOrBlank()) {
                        {
                            IconButton(
                                onClick = { viewModel.updateExpirationDate(null) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Due Date", modifier = Modifier.size(16.dp))
                            }
                        }
                    } else null
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))

                // Linked People Row
                val linkedPeopleNames = uiState.availablePeople
                    .filter { uiState.editPeopleIds.contains(it.id) }
                    .joinToString(", ") { it.name }

                CleanListRow(
                    icon = { Icon(Icons.Default.Person, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp)) },
                    label = "Linked People",
                    value = linkedPeopleNames.ifBlank { "None Selected" },
                    onClick = {
                        onDismissKeyboardAndNumpad()
                        activePicker = FormPicker.People
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))

                // Linked Place Row
                CleanListRow(
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp)) },
                    label = "Place",
                    value = selectedPlace?.name ?: "None",
                    onClick = {
                        onDismissKeyboardAndNumpad()
                        activePicker = FormPicker.Place
                    }
                )
            }
        }

        // Note Input Card
        OutlinedTextField(
            value = uiState.editNote,
            onValueChange = viewModel::updateNote,
            label = { Text("Note (Optional)") },
            placeholder = { Text("Additional notes...") },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        onFocusField()
                    }
                },
            shape = RoundedCornerShape(16.dp),
            minLines = 3
        )

        // Initial Master Transaction Toggle (Only for new debt)
        if (uiState.isNewDebt) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDismissKeyboardAndNumpad()
                        viewModel.updateInsertMasterTransaction(!uiState.editInsertMasterTransaction)
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = uiState.editInsertMasterTransaction,
                        onCheckedChange = {
                            onDismissKeyboardAndNumpad()
                            viewModel.updateInsertMasterTransaction(it)
                        }
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Create Initial Wallet Transaction",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (uiState.editType == 0) "Adds income transaction when borrowing money"
                            else "Adds expense transaction when lending money",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Dialog Pickers
    when (activePicker) {
        FormPicker.Wallet -> {
            WalletSelectionDialog(
                title = "Select Associated Wallet",
                wallets = uiState.availableWallets,
                selectedWalletId = uiState.editWalletId,
                onWalletSelected = { wallet ->
                    viewModel.updateWalletId(wallet.id)
                    activePicker = null
                },
                onDismissRequest = { activePicker = null }
            )
        }
        FormPicker.Place -> {
            PlaceSelectionDialog(
                title = "Select Place",
                places = uiState.availablePlaces,
                selectedPlaceId = uiState.editPlaceId,
                onPlaceSelected = { place ->
                    viewModel.updatePlaceId(place?.id)
                    activePicker = null
                },
                onDismissRequest = { activePicker = null }
            )
        }
        FormPicker.People -> {
            PeopleSelectionDialog(
                title = "Select People",
                people = uiState.availablePeople,
                selectedPeopleIds = uiState.editPeopleIds,
                onPersonToggle = { person -> viewModel.togglePersonSelection(person.id) },
                onDismissRequest = { activePicker = null }
            )
        }
        FormPicker.Date -> {
            FormDatePickerDialog(
                initialDateString = uiState.editDate,
                onDateStringSelected = { dateStr ->
                    viewModel.updateDate(dateStr)
                    activePicker = null
                },
                onDismissRequest = { activePicker = null }
            )
        }
        FormPicker.DueDate -> {
            FormDatePickerDialog(
                initialDateString = uiState.editExpirationDate,
                onDateStringSelected = { expDateStr ->
                    viewModel.updateExpirationDate(expDateStr)
                    activePicker = null
                },
                onDismissRequest = { activePicker = null }
            )
        }
        else -> Unit
    }
}