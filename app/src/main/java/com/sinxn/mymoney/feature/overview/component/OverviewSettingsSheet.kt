package com.sinxn.mymoney.feature.overview.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.feature.overview.CashFlowFilter
import com.sinxn.mymoney.feature.overview.GroupType
import com.sinxn.mymoney.feature.overview.OverviewSettings
import com.sinxn.mymoney.feature.overview.OverviewType
import java.util.Calendar
import java.util.Date

/**
 * Bottom sheet for configuring overview settings.
 * Matches legacy OverviewSettingDialog functionality.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewSettingsSheet(
    settings: OverviewSettings,
    onDismiss: () -> Unit,
    onApply: (OverviewSettings) -> Unit
) {
    var groupType by remember(settings) { mutableStateOf(settings.groupType) }
    var overviewType by remember(settings) { mutableStateOf(settings.overviewType) }
    var cashFlowFilter by remember(settings) { mutableStateOf(settings.cashFlowFilter) }
    var startDate by remember(settings) { mutableStateOf(settings.startDate) }
    var endDate by remember(settings) { mutableStateOf(settings.endDate) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Overview Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Date Range Section
            Text(
                text = "Date Range",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DateChip(
                    label = "From",
                    date = DateUtils.formatMonthDayYear(startDate),
                    onClick = { showStartDatePicker = true },
                    modifier = Modifier.weight(1f)
                )
                DateChip(
                    label = "To",
                    date = DateUtils.formatMonthDayYear(endDate),
                    onClick = { showEndDatePicker = true },
                    modifier = Modifier.weight(1f)
                )
            }

            // Group Type — matching legacy: Daily, Weekly, Monthly, Yearly
            Text(
                text = "Group By",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                GroupType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = groupType == type,
                        onClick = { groupType = type },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = GroupType.entries.size
                        )
                    ) {
                        Text(
                            text = type.name.lowercase()
                                .replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            // Overview Type — Cash Flow / Category
            Text(
                text = "View Type",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = overviewType == OverviewType.CASH_FLOW,
                    onClick = { overviewType = OverviewType.CASH_FLOW },
                    label = { Text("Cash Flow") },
                    leadingIcon = {
                        Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = overviewType == OverviewType.CATEGORY,
                    onClick = { overviewType = OverviewType.CATEGORY },
                    label = { Text("Category") },
                    leadingIcon = {
                        Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Cash Flow Sub-filter — matching legacy: Incomes, Expenses, Net Incomes
            if (overviewType == OverviewType.CASH_FLOW) {
                Text(
                    text = "Cash Flow Filter",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val filters = CashFlowFilter.entries
                    filters.forEachIndexed { index, filter ->
                        SegmentedButton(
                            selected = cashFlowFilter == filter,
                            onClick = { cashFlowFilter = filter },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = filters.size
                            )
                        ) {
                            Text(
                                text = when (filter) {
                                    CashFlowFilter.INCOMES -> "Incomes"
                                    CashFlowFilter.EXPENSES -> "Expenses"
                                    CashFlowFilter.NET_INCOMES -> "Net"
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            // Apply button
            Button(
                onClick = {
                    onApply(
                        OverviewSettings(
                            startDate = startDate,
                            endDate = endDate,
                            groupType = groupType,
                            overviewType = overviewType,
                            cashFlowFilter = cashFlowFilter,
                            categoryId = settings.categoryId
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Apply", fontWeight = FontWeight.Bold)
            }
        }
    }

    // Date Pickers
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate.time
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            startDate = Date(it)
                        }
                        showStartDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = endDate.time
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            endDate = Date(it)
                        }
                        showEndDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun DateChip(
    label: String,
    date: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
