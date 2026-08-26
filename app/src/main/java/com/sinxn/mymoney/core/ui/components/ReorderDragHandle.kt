package com.sinxn.mymoney.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standard drag handle indicator icon used in reorderable lists.
 */
@Composable
fun ReorderDragHandle(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconSize: Dp = 20.dp,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
    contentDescription: String? = "Reorder"
) {
    if (!enabled) return

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.DragIndicator,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}
