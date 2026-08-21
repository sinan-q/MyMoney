package com.sinxn.mymoney.feature.transaction.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sinxn.mymoney.core.ui.components.CleanListRow
import com.sinxn.mymoney.core.ui.components.FormCardContainer

@Composable
fun TransactionDescriptionCard(
    description: String,
    accentColor: Color,
    isEditable: Boolean = false,
    onDescriptionChange: ((String) -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    onFocusField: (() -> Unit)? = null
) {
    if (!isEditable && description.isEmpty()) return

    FormCardContainer {
        if (isEditable) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { onDescriptionChange?.invoke(it) },
                    label = { Text("Description") },
                    placeholder = { Text("Add description...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                onFocusField?.invoke()
                            }
                        },
                    shape = RoundedCornerShape(14.dp)
                )
            }
        } else {
            CleanListRow(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = accentColor
                    )
                },
                label = "Description",
                value = description,
            )
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.Start,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Icon(
//                    modifier = Modifier
//                        .padding(vertical = 16.dp, horizontal = 12.dp),
//                    imageVector = Icons.Default.Description,
//                    contentDescription = null,
//                    tint = accentColor
//                )
//                Column() {
//                    Text(
//                        text = "Description",
//                        style = MaterialTheme.typography.labelSmall,
//                        fontWeight = FontWeight.Medium,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
//                    )
//                    Text(
//                        text = description,
//                        style = MaterialTheme.typography.bodyMedium,
//                        fontWeight = FontWeight.SemiBold,
//                        color = MaterialTheme.colorScheme.onSurface,
//                        lineHeight = 22.sp
//                    )
//                }
//            }
        }
    }
}
