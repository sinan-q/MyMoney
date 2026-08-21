package com.sinxn.mymoney.core.ui.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.sinxn.mymoney.core.util.DateUtils
import java.util.Date

/**
 * Reusable date picker dialog that accepts milliseconds.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormDatePickerDialog(
    initialDateMillis: Long? = null,
    onDateSelected: (Long) -> Unit,
    onDismissRequest: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(millis)
                    }
                    onDismissRequest()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

/**
 * Overload of [FormDatePickerDialog] that accepts and returns formatted date strings (SQL format).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormDatePickerDialog(
    initialDateString: String?,
    onDateStringSelected: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val initialMillis = remember(initialDateString) {
        if (initialDateString.isNullOrBlank()) null
        else {
            try {
                DateUtils.parseDate(initialDateString).time
            } catch (e: Exception) {
                null
            }
        }
    }

    FormDatePickerDialog(
        initialDateMillis = initialMillis,
        onDateSelected = { millis ->
            onDateStringSelected(DateUtils.getSQLDateTimeString(Date(millis)))
        },
        onDismissRequest = onDismissRequest
    )
}
