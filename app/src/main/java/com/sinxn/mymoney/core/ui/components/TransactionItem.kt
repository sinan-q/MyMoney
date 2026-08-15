package com.sinxn.mymoney.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MoneyFormatter

private const val TRANSFER_ICON_DATA = "{\"type\":\"color\",\"color\":\"#0284C7\",\"name\":\"⇄\"}"

@Composable
fun TransactionItem(
    item: TransactionWithCategory,
    decimals: Int = item.decimals,
    currencyCode: String = item.currencySymbol ?: item.currencyCode ?: "$",
    formatterConfig: MoneyFormatter.Config = MoneyFormatter.Config(),
    dateFormat: Int = 0,
    isLastItem: Boolean = false,
    showDate: Boolean = true,
    onClick: () -> Unit = {}
) {
    val transaction = item.transaction
    val isTransferItem = transaction.direction == 2 || transaction.type == 1 || transaction.type == 2 || item.categoryName.equals("Transfer", ignoreCase = true)
    val categoryDisplayName = when {
        isTransferItem -> "Transfer"
        !item.categoryName.isNullOrBlank() -> item.categoryName
        else -> "Uncategorized"
    }
    val categoryIconData = if (isTransferItem) TRANSFER_ICON_DATA else item.categoryIcon
    
    val hasDescription = !transaction.description.isNullOrBlank()
    val primaryTitle = if (hasDescription) transaction.description else categoryDisplayName
    val subtitleText = if (hasDescription) categoryDisplayName else null

    val formattedDate = androidx.compose.runtime.remember(transaction.date, dateFormat) {
        val dateObj = DateUtils.parseDate(transaction.date)
        DateUtils.formatDate(dateObj, dateFormat)
    }

    val isIncome = transaction.direction == 1
    val amountColor = when {
        isTransferItem -> Color(0xFF0284C7)
        isIncome -> Color(0xFF2E7D32)
        else -> Color(0xFFC62828)
    }

    val amount = if (isIncome || isTransferItem) transaction.money else -transaction.money
    val formattedMoney = androidx.compose.runtime.remember(amount, currencyCode, decimals, formatterConfig) {
        MoneyFormatter.format(
            amount = amount,
            currencyCode = currencyCode,
            decimals = decimals,
            config = formatterConfig
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .clickable(onClick = onClick)
    ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    CategoryIcon(
                        iconString = categoryIconData,
                        categoryName = categoryDisplayName,
                        modifier = Modifier.size(44.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = primaryTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (!subtitleText.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = (if (isIncome && !formattedMoney.startsWith("+")) "+" else "") + formattedMoney,
                        style = MaterialTheme.typography.titleMedium,
                        color = amountColor,
                        fontWeight = FontWeight.Bold
                    )
                    if (showDate) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

    }
}
