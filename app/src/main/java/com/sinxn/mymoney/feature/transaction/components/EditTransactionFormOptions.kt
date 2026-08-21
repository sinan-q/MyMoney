package com.sinxn.mymoney.feature.transaction.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.CleanListRow
import com.sinxn.mymoney.core.ui.components.FormCardContainer
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.feature.transaction.TransactionAddEditUiState

@Composable
fun EditTransactionFormOptions(
    uiState: TransactionAddEditUiState,
    settings: FormattingSettings,
    accentColor: Color,
    onCategoryClick: () -> Unit,
    onWalletClick: () -> Unit,
    onDateClick: () -> Unit,
    onPeopleClick: () -> Unit,
    onPlaceClick: () -> Unit,
    onEventClick: () -> Unit,
    onNoteChange: (String) -> Unit,
    onConfirmedChange: (Boolean) -> Unit,
    onCountInTotalChange: (Boolean) -> Unit,
    onFocusField: () -> Unit
) {
    FormCardContainer {
        Column {
            // Category Row
            val activeCategory = uiState.availableCategories.find { it.id == uiState.editCategoryId }
            CleanListRow(
                icon = {
                    Icon(
                        Icons.Default.Category,
                        contentDescription = null,
                        tint = accentColor,
                    )
                },
                label = "Category",
                value = activeCategory?.name?.replace("  ↳ ", "") ?: "Select Category",
                active = !activeCategory?.name.isNullOrEmpty(),
                onClick = onCategoryClick,
                trailingIcon =  { if (activeCategory != null)
                    CategoryIcon(
                        iconString = activeCategory.icon,
                        categoryName = activeCategory.name,
                        modifier = Modifier.size(32.dp)
                    )
                }
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Wallet Row
            val activeWallet = uiState.availableWallets.find { it.id == uiState.editWalletId }
            CleanListRow(
                icon = {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = accentColor,
                    )
                },
                label = "Wallet",
                value = activeWallet?.name ?: "Select Wallet",
                trailingIcon =  { if (activeWallet != null)
                    CategoryIcon(
                        iconString = activeWallet.icon,
                        categoryName = activeWallet.name,
                        modifier = Modifier.size(32.dp)
                    )
                },
                onClick = onWalletClick
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Date Row
            val parsedDate = DateUtils.parseDate(uiState.editDate)
            val dateText = when {
                DateUtils.isToday(parsedDate) -> "Today"
                DateUtils.isYesterday(parsedDate) -> "Yesterday"
                else -> DateUtils.formatDate(parsedDate, settings.dateFormat)
            }

            CleanListRow(
                icon = {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = accentColor,
                    )
                },
                label = "Date",
                value = dateText,
                onClick = onDateClick
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // People Row
            val selectedPeople = uiState.availablePeople.filter { it.id in uiState.editPeopleIds }
            val selectedPeopleNames = selectedPeople.joinToString { it.name }.ifEmpty { "None" }

            CleanListRow(

                icon = {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        tint = if (selectedPeople.isNotEmpty()) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                },
                label = "People",
                value = selectedPeopleNames,
                active = selectedPeople.isNotEmpty(),
                onClick = onPeopleClick
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Place Row
            val activePlace = uiState.availablePlaces.find { it.id == uiState.editPlaceId }
            CleanListRow(
                icon = {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = if (activePlace != null) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                },
                label = "Place",
                value = activePlace?.name ?: "None",
                active = activePlace != null,
                trailingIcon = {if ( activePlace != null && activePlace.icon.isNotBlank())
                CategoryIcon(iconString = activePlace.icon,
                    categoryName = activePlace.name,
                    modifier = Modifier.size(32.dp)

                )
                },
                onClick = onPlaceClick
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Event Row
            val activeEvent = uiState.availableEvents.find { it.id == uiState.editEventId }
            CleanListRow(
                icon = {
                    Icon(
                        Icons.Default.Event,
                        contentDescription = null,
                        tint = if (activeEvent != null) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                },
                label = "Event",
                value = activeEvent?.name?: "None",
                active = !activeEvent?.name.isNullOrEmpty(),
                trailingIcon =  { if (activeEvent != null && activeEvent.icon.isNotBlank())
                    CategoryIcon(
                        iconString = activeEvent.icon,
                        categoryName = activeEvent.name,
                        modifier = Modifier.size(32.dp)
                    )
                },
                onClick = onEventClick
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Note Field
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = uiState.editNote,
                    onValueChange = onNoteChange,
                    label = { Text("Note") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                onFocusField()
                            }
                        },
                    shape = RoundedCornerShape(14.dp),
                    leadingIcon = {
                        Icon(
                            Icons.Default.Notes,
                            contentDescription = null,
                            tint = accentColor
                        )
                    }
                )
            }

            if (!settings.hideStatusAndImpact) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // Confirmed Switch Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Confirmed",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = uiState.editConfirmed,
                        onCheckedChange = onConfirmedChange
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
                    Text(
                        "Count in Total",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = uiState.editCountInTotal,
                        onCheckedChange = onCountInTotalChange
                    )
                }
            }
        }
    }
}
