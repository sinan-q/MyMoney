package com.sinxn.mymoney.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun <T> SelectionDialog(
    title: String,
    options: List<T>,
    selectedOptions: Set<T> = emptySet(),
    onOptionSelected: (T) -> Unit,
    onDismissRequest: () -> Unit,
    labelProvider: (T) -> String,
    multiSelect: Boolean = false
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = title, style = MaterialTheme.typography.titleLarge)
                    if (multiSelect) {
                        IconButton(onClick = onDismissRequest) {
                            Icon(Icons.Default.Check, contentDescription = "Done")
                        }
                    } else {
                        IconButton(onClick = onDismissRequest) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                HorizontalDivider()

                // List
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(options) { option ->
                        val isSelected = selectedOptions.contains(option)
                        ListItem(
                            headlineContent = { Text(labelProvider(option)) },
                            trailingContent = {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = "Selected")
                                }
                            },
                            modifier = Modifier.clickable {
                                onOptionSelected(option)
                                if (!multiSelect) {
                                    onDismissRequest()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
