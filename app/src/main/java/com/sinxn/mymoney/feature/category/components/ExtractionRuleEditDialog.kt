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
import com.sinxn.mymoney.core.data.local.entity.CustomFieldDefinitionEntity
import com.sinxn.mymoney.core.data.local.entity.CustomFieldExtractionRuleEntity
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtractionRuleEditDialog(
    rules: List<CustomFieldExtractionRuleEntity>,
    availableFields: List<CustomFieldDefinitionEntity> = emptyList(),
    fieldId: String? = null,
    onDismissRequest: () -> Unit,
    onSaveRule: (CustomFieldExtractionRuleEntity) -> Unit,
    onDeleteRule: (CustomFieldExtractionRuleEntity) -> Unit
) {
    var showAddRule by remember { mutableStateOf(false) }
    val fieldsById = remember(availableFields) { availableFields.associateBy { it.id } }

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
                                        val fieldLabel = fieldsById[rule.fieldId]?.label
                                        Text(
                                            text = if (fieldLabel != null) "Field: $fieldLabel · Mode: ${rule.mode}" else "Mode: ${rule.mode}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
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
        var selectedFieldId by remember {
            mutableStateOf(fieldId ?: availableFields.firstOrNull()?.id ?: "")
        }
        var mode by remember { mutableStateOf("template") }
        var pattern by remember { mutableStateOf("") }
        var targetColumns by remember { mutableStateOf("description") }

        var fieldDropdownExpanded by remember { mutableStateOf(false) }
        var modeExpanded by remember { mutableStateOf(false) }

        val placeholderHint = remember(availableFields) {
            if (availableFields.isNotEmpty()) {
                "Placeholders: " + availableFields.joinToString(", ") { "{${it.key}}" }
            } else {
                "Use {field_key} placeholders, e.g. {food} @ {app} @ {restaurent}"
            }
        }

        AlertDialog(
            onDismissRequest = { showAddRule = false },
            title = { Text("New Rule") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (availableFields.size > 1 && fieldId == null) {
                        ExposedDropdownMenuBox(
                            expanded = fieldDropdownExpanded,
                            onExpandedChange = { fieldDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = fieldsById[selectedFieldId]?.label ?: "Select Field",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Primary Field") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fieldDropdownExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = fieldDropdownExpanded,
                                onDismissRequest = { fieldDropdownExpanded = false }
                            ) {
                                availableFields.forEach { f ->
                                    DropdownMenuItem(
                                        text = { Text("${f.label} (${f.type})") },
                                        onClick = {
                                            selectedFieldId = f.id
                                            fieldDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

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
                        supportingText = {
                            if (mode == "template") {
                                Text(placeholderHint)
                            } else {
                                Text("Standard regex pattern")
                            }
                        },
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
                            fieldId = selectedFieldId,
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
                    enabled = pattern.isNotBlank() && selectedFieldId.isNotBlank()
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
