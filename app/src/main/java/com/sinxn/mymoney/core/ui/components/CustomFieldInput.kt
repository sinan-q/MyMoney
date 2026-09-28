package com.sinxn.mymoney.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sinxn.mymoney.core.data.local.entity.CustomFieldDefinitionEntity

@Composable
fun CustomFieldInput(
    field: CustomFieldDefinitionEntity,
    value: String?,
    onValueChange: (String) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
        modifier = Modifier.padding(horizontal = 16.dp)
    )
    
    when (field.type.lowercase()) {
        "number" -> { // Number
            DescriptionEditForm(
                modifier = modifier,
                icon = Icons.Default.Numbers,
                accentColor = accentColor,
                value = value ?: "",
                onValueChange = onValueChange,
                singleLine = true,
                label = field.label + if (field.isRequired) " *" else "",
                placeHolder = "0",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
        "text" -> { // Text
            DescriptionEditForm(
                modifier = modifier,
                icon = Icons.Default.TextFields,
                accentColor = accentColor,
                value = value ?: "",
                onValueChange = onValueChange,
                singleLine = true,
                label = field.label + if (field.isRequired) " *" else "",
                placeHolder = "Enter text..."
            )
        }
        "boolean" -> { // Boolean
            val isChecked = value == "true"
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ToggleOn,
                        contentDescription = null,
                        tint = if (isChecked) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = field.label + if (field.isRequired) " *" else "",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                }
                Switch(
                    checked = isChecked,
                    onCheckedChange = { onValueChange(it.toString()) },
                    colors = SwitchDefaults.colors(checkedTrackColor = accentColor)
                )
            }
        }
    }
}
