package com.sinxn.mymoney.feature.transaction.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TransactionTransferSegmentedControl(
    isTransfer: Boolean,
    onModeChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onModeChange(false) },
            color = if (!isTransfer) MaterialTheme.colorScheme.surface else Color.Transparent,
            tonalElevation = if (!isTransfer) 2.dp else 0.dp
        ) {
            Box(
                modifier = Modifier.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Transaction",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (!isTransfer) FontWeight.Bold else FontWeight.Medium,
                    color = if (!isTransfer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        Surface(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onModeChange(true) },
            color = if (isTransfer) MaterialTheme.colorScheme.surface else Color.Transparent,
            tonalElevation = if (isTransfer) 2.dp else 0.dp
        ) {
            Box(
                modifier = Modifier.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "⇄ ",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isTransfer) Color(0xFF0284C7) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Transfer",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isTransfer) FontWeight.Bold else FontWeight.Medium,
                        color = if (isTransfer) Color(0xFF0284C7) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}