package com.sinxn.mymoney.core.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun FormCardContainer(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
    containerAlpha: Float = 0.35f,
    content: @Composable () -> Unit
) {
    Surface (
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
        shape = RoundedCornerShape(12.dp),
        color =  MaterialTheme.colorScheme.surfaceVariant.copy(alpha = containerAlpha),
        content = content
    )
}
