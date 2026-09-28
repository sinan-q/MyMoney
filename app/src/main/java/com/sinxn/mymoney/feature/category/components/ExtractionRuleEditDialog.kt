package com.sinxn.mymoney.feature.category.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sinxn.mymoney.core.data.local.entity.CustomFieldExtractionRuleEntity
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtractionRuleEditDialog(
    fieldId: String,
    rules: List<CustomFieldExtractionRuleEntity>,
    onDismissRequest: () -> Unit,
    onSaveRule: (CustomFieldExtractionRuleEntity) -> Unit,
    onDeleteRule: (CustomFieldExtractionRuleEntity) -> Unit
) {
    var showAddRule by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Extraction Rules") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (rules.isEmpty()) {
                    Text(
                        "No extraction rules defined. Add one to automatically extract values from description or note.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(rules) { rule ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Mode: ${rule.mode}",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Text(
                                            text = rule.pattern,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    IconButton(onClick = { onDeleteRule(rule) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Rule")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { showAddRule = true }) {
                Text("Add Rule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Close")
            }
        }
    )

    if (showAddRule) {
        var mode by remember { mutableStateOf("template") }
        var pattern by remember { mutableStateOf("") }
        var targetColumns by remember { mutableStateOf("description") }

        var modeExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddRule = false },
            title = { Text("New Rule") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = modeExpanded,
                        onExpandedChange = { modeExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = if (mode == "template") "Template" else "Regex",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Mode") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modeExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = modeExpanded,
                            onDismissRequest = { modeExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Template (e.g. Uber {amount})") },
                                onClick = {
                                    mode = "template"
                                    modeExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Regex") },
                                onClick = {
                                    mode = "regex"
                                    modeExpanded = false
                                }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = pattern,
                        onValueChange = { pattern = it },
                        label = { Text(if (mode == "template") "Pattern Template" else "Regex Pattern") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = targetColumns,
                        onValueChange = { targetColumns = it },
                        label = { Text("Target Columns (comma-separated)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newRule = CustomFieldExtractionRuleEntity(
                            id = UUID.randomUUID().toString(),
                            fieldId = fieldId,
                            ruleOrder = rules.size,
                            mode = mode,
                            pattern = pattern,
                            targetColumns = targetColumns,
                            fieldMappings = "{}",
                            lastEdit = System.currentTimeMillis()
                        )
                        onSaveRule(newRule)
                        showAddRule = false
                    },
                    enabled = pattern.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRule = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
