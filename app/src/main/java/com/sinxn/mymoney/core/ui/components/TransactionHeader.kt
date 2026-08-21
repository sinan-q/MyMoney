package com.sinxn.mymoney.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sinxn.mymoney.core.data.local.model.TransactionListItem
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MoneyFormatter

@Composable
fun TransactionHeader(
    header: TransactionListItem.Header,
    decimals: Int = 2,
    currencyCode: String = "USD",
    formatterConfig: MoneyFormatter.Config = MoneyFormatter.Config(),
    isCollapsed: Boolean = false,
    onToggle: () -> Unit = {}
) {
    val formattedDate = header.formattedDate ?: DateUtils.formatMonthHeader(header.date)
    val formattedTotal = header.formattedTotal ?: MoneyFormatter.format(
        amount = header.totalAmount,
        currencyCode = currencyCode,
        decimals = decimals,
        config = formatterConfig
    )
    val formattedIncome = header.formattedIncome ?: MoneyFormatter.format(
        amount = header.income,
        currencyCode = currencyCode,
        decimals = decimals,
        config = formatterConfig
    )
    val formattedExpense = header.formattedExpense ?: MoneyFormatter.format(
        amount = header.expense,
        currencyCode = currencyCode,
        decimals = decimals,
        config = formatterConfig
    )
    
    val rotation by animateFloatAsState(
        targetValue = if (isCollapsed) 180f else 0f,
        label = "ArrowRotation"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .background(color = MaterialTheme.colorScheme.background)
            .border(border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)), shape = RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = if (isCollapsed) "Expand" else "Collapse",
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(rotation),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
            if (header.transactionCount > 1) {
                VerticalDivider(thickness = 2.dp)
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (header.isTotalValid) formattedTotal else "Multi-Currency",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (!header.isTotalValid) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else if (header.totalAmount >= 0) {
                            Color(0xFF2E7D32)
                        } else {
                            Color(0xFFC62828)
                        }
                    )

                    if (header.isTotalValid && header.income > 0 && header.expense > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF43A047).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "+$formattedIncome",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFE53935).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "-$formattedExpense",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFC62828),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else if (!header.isTotalValid) {
                        if (!header.balanceBreakdown.isNullOrEmpty()) {
                            Text(
                                text = header.balanceBreakdown,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.End
                            )
                        }
                        Text(
                            text = "Mixed currencies",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
