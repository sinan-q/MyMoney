package com.sinxn.mymoney.feature.category.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sinxn.mymoney.core.data.local.entity.CustomFieldDefinitionEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomFieldEditDialog(
    field: CustomFieldDefinitionEntity?,
    availableFields: List<CustomFieldDefinitionEntity>,
    onDismissRequest: () -> Unit,
    onSave: (label: String, type: String, isRequired: Boolean, visibilityDependsOnFieldId: String?, visibilityDependsOnValue: String?) -> Unit,
    onDelete: ((String) -> Unit)? = null,
    onManageExtractionRules: ((String) -> Unit)? = null
) {
    var label by remember { mutableStateOf(field?.label ?: "") }
    var type by remember { mutableStateOf(field?.type ?: "text") }
    var isRequired by remember { mutableStateOf(field?.isRequired ?: false) }
    var visibilityDependsOnFieldId by remember { mutableStateOf(field?.visibilityDependsOnFieldId) }
    var visibilityDependsOnValue by remember { mutableStateOf(field?.visibilityDependsOnValue ?: "") }

    val types = listOf("text" to "Text", "number" to "Number", "boolean" to "Yes/No (Switch)")
    var typeExpanded by remember { mutableStateOf(false) }
    var dependencyExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(if (field == null) "New Custom Field" else "Edit Custom Field") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Field Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = types.find { it.first == type }?.second ?: type,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Field Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        types.forEach { (typeKey, typeLabel) ->
                            DropdownMenuItem(
                                text = { Text(typeLabel) },
                                onClick = {
                                    type = typeKey
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = isRequired, onCheckedChange = { isRequired = it })
                    Text("Required Field")
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Visibility Condition (Optional)", style = MaterialTheme.typography.labelMedium)

                ExposedDropdownMenuBox(
                    expanded = dependencyExpanded,
                    onExpandedChange = { dependencyExpanded = it }
                ) {
                    val depLabel = availableFields.find { it.id == visibilityDependsOnFieldId }?.label ?: "None"
                    OutlinedTextField(
                        value = depLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Depends On") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dependencyExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = dependencyExpanded,
                        onDismissRequest = { dependencyExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None") },
                            onClick = {
                                visibilityDependsOnFieldId = null
                                dependencyExpanded = false
                            }
                        )
                        availableFields.forEach { availableField ->
                            if (availableField.id != field?.id) { // Cannot depend on itself
                                DropdownMenuItem(
                                    text = { Text(availableField.label) },
                                    onClick = {
                                        visibilityDependsOnFieldId = availableField.id
                                        dependencyExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (visibilityDependsOnFieldId != null) {
                    val targetField = availableFields.find { it.id == visibilityDependsOnFieldId }
                    if (targetField?.type == "boolean") {
                        val isTrue = visibilityDependsOnValue == "true"
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text("Show when value is:")
                            Spacer(Modifier.width(8.dp))
                            Switch(
                                checked = isTrue,
                                onCheckedChange = { visibilityDependsOnValue = it.toString() }
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = visibilityDependsOnValue,
                            onValueChange = { visibilityDependsOnValue = it },
                            label = { Text("Show when value equals") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                if (field != null && onManageExtractionRules != null) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    TextButton(
                        onClick = { onManageExtractionRules(field.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Manage Extraction Rules")
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (field != null && onDelete != null) {
                    TextButton(
                        onClick = { onDelete(field.id) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }
                
                Row {
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = { onSave(label, type, isRequired, visibilityDependsOnFieldId, visibilityDependsOnValue) },
                        enabled = label.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            }
        },
        dismissButton = {} // Handled in Row
    )
}
