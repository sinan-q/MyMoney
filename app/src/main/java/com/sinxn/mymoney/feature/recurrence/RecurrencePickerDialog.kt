package com.sinxn.mymoney.feature.recurrence

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sinxn.mymoney.R
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.RecurrenceSetting
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurrencePickerDialog(
    initialStartDate: Date,
    initialRule: String?,
    onDismiss: () -> Unit,
    onConfirm: (startDate: Date, rule: String) -> Unit
) {
    val context = LocalContext.current
    var startDate by remember { mutableStateOf(initialStartDate) }
    var selectedType by remember { mutableIntStateOf(RecurrenceSetting.TYPE_DAILY) }
    var intervalStr by remember { mutableStateOf("1") }
    var weekDays by remember { mutableStateOf(BooleanArray(7) { false }) }
    var endType by remember { mutableIntStateOf(RecurrenceSetting.END_FOREVER) }
    var occurrencesStr by remember { mutableStateOf("1") }
    var endDate by remember { mutableStateOf(initialStartDate) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // Initialize from initial rule if present
    LaunchedEffect(initialRule) {
        if (!initialRule.isNullOrBlank()) {
            val setting = RecurrenceSetting.fromStringOrFallback(initialStartDate, initialRule)
            selectedType = setting.type
            intervalStr = setting.offsetValue.toString()
            weekDays = setting.weekDays
            endType = setting.endType
            occurrencesStr = setting.occurrenceValue.toString()
            endDate = setting.endDate
        }
    }

    val currentSetting = remember(startDate, selectedType, intervalStr, weekDays, endType, occurrencesStr, endDate) {
        val interval = intervalStr.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val builder = RecurrenceSetting.Builder(startDate, selectedType)
        builder.setOffset(interval)

        if (selectedType == RecurrenceSetting.TYPE_WEEKLY) {
            builder.setRepeatWeekDay(weekDays)
        } else if (selectedType == RecurrenceSetting.TYPE_MONTHLY) {
            builder.setRepeatSameMonthDay()
        }

        when (endType) {
            RecurrenceSetting.END_FOREVER -> {}
            RecurrenceSetting.END_UNTIL -> builder.setEndUntil(endDate)
            RecurrenceSetting.END_FOR -> {
                val occurrences = occurrencesStr.toIntOrNull()?.coerceAtLeast(1) ?: 1
                builder.setEndFor(occurrences)
            }
        }
        builder.build()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.dialog_recurrence_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Live Description Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = currentSetting.getUserReadableString(context),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                // Start Date Picker Field
                OutlinedTextField(
                    value = DateUtils.formatDate(startDate, 2),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.recurrence_hint_from)) },
                    trailingIcon = {
                        IconButton(onClick = { showStartDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Select Date")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Frequency Dropdown
                var freqExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = freqExpanded,
                    onExpandedChange = { freqExpanded = !freqExpanded }
                ) {
                    OutlinedTextField(
                        value = when (selectedType) {
                            RecurrenceSetting.TYPE_DAILY -> stringResource(R.string.recurrence_type_daily)
                            RecurrenceSetting.TYPE_WEEKLY -> stringResource(R.string.recurrence_type_weekly)
                            RecurrenceSetting.TYPE_MONTHLY -> stringResource(R.string.recurrence_type_monthly)
                            RecurrenceSetting.TYPE_YEARLY -> stringResource(R.string.recurrence_type_yearly)
                            else -> stringResource(R.string.recurrence_type_daily)
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Frequency") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = freqExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = freqExpanded,
                        onDismissRequest = { freqExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.recurrence_type_daily)) },
                            onClick = { selectedType = RecurrenceSetting.TYPE_DAILY; freqExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.recurrence_type_weekly)) },
                            onClick = { selectedType = RecurrenceSetting.TYPE_WEEKLY; freqExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.recurrence_type_monthly)) },
                            onClick = { selectedType = RecurrenceSetting.TYPE_MONTHLY; freqExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.recurrence_type_yearly)) },
                            onClick = { selectedType = RecurrenceSetting.TYPE_YEARLY; freqExpanded = false }
                        )
                    }
                }

                // Interval Field ("Every N ...")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = intervalStr,
                        onValueChange = { intervalStr = it.filter { char -> char.isDigit() } },
                        label = { Text(stringResource(R.string.recurrence_hint_every)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = when (selectedType) {
                            RecurrenceSetting.TYPE_DAILY -> stringResource(R.string.recurrence_hint_days)
                            RecurrenceSetting.TYPE_WEEKLY -> stringResource(R.string.recurrence_hint_weeks)
                            RecurrenceSetting.TYPE_MONTHLY -> stringResource(R.string.recurrence_hint_months)
                            RecurrenceSetting.TYPE_YEARLY -> stringResource(R.string.recurrence_hint_years)
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                // Weekday selection if Weekly
                if (selectedType == RecurrenceSetting.TYPE_WEEKLY) {
                    Text(text = "Repeat on:", style = MaterialTheme.typography.labelLarge)
                    val daysLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                    Column {
                        daysLabels.chunked(4).forEachIndexed { rowIndex, chunk ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                chunk.forEachIndexed { colIndex, label ->
                                    val index = rowIndex * 4 + colIndex
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable {
                                            val copy = weekDays.copyOf()
                                            copy[index] = !copy[index]
                                            weekDays = copy
                                        }
                                    ) {
                                        Checkbox(
                                            checked = weekDays[index],
                                            onCheckedChange = { checked ->
                                                val copy = weekDays.copyOf()
                                                copy[index] = checked
                                                weekDays = copy
                                            }
                                        )
                                        Text(text = label, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }

                // End Condition
                var endExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = endExpanded,
                    onExpandedChange = { endExpanded = !endExpanded }
                ) {
                    OutlinedTextField(
                        value = when (endType) {
                            RecurrenceSetting.END_FOREVER -> stringResource(R.string.recurrence_end_type_forever)
                            RecurrenceSetting.END_UNTIL -> stringResource(R.string.recurrence_end_type_until)
                            RecurrenceSetting.END_FOR -> stringResource(R.string.recurrence_end_type_for)
                            else -> stringResource(R.string.recurrence_end_type_forever)
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Ends") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = endExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = endExpanded,
                        onDismissRequest = { endExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.recurrence_end_type_forever)) },
                            onClick = { endType = RecurrenceSetting.END_FOREVER; endExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.recurrence_end_type_until)) },
                            onClick = { endType = RecurrenceSetting.END_UNTIL; endExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.recurrence_end_type_for)) },
                            onClick = { endType = RecurrenceSetting.END_FOR; endExpanded = false }
                        )
                    }
                }

                if (endType == RecurrenceSetting.END_FOR) {
                    OutlinedTextField(
                        value = occurrencesStr,
                        onValueChange = { occurrencesStr = it.filter { char -> char.isDigit() } },
                        label = { Text(stringResource(R.string.recurrence_hint_times)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (endType == RecurrenceSetting.END_UNTIL) {
                    OutlinedTextField(
                        value = DateUtils.formatDate(endDate, 2),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.recurrence_end_type_until)) },
                        trailingIcon = {
                            IconButton(onClick = { showEndDatePicker = true }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Select End Date")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(startDate, currentSetting.rule)
            }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )

    // Date Pickers
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate.time
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { startDate = Date(it) }
                    showStartDatePicker = false
                }) {
                    Text("OK")
                }
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
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { endDate = Date(it) }
                    showEndDatePicker = false
                }) {
                    Text("OK")
                }
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
