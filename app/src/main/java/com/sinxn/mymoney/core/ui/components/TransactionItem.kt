package com.sinxn.mymoney.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MoneyFormatter

private val DebtRoseColor = Color(0xFFE11D48)
private val CreditEmeraldColor = Color(0xFF10B981)

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
    val isTransferItem = transaction.type == 1 || item.categoryName.equals("Transfer", ignoreCase = true)
    val categoryDisplayName = when {
        isTransferItem -> "Transfer"
        !item.categoryName.isNullOrBlank() -> item.categoryName
        else -> "Uncategorized"
    }
    val categoryIconData = item.categoryIcon
    
    val hasDescription = !transaction.description.isNullOrBlank()
    val primaryTitle = if (hasDescription) transaction.description else categoryDisplayName
    val subtitleText = if (hasDescription) categoryDisplayName else null

    val formattedDate = remember(transaction.date, dateFormat) {
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
    val formattedMoney = remember(amount, currencyCode, decimals, formatterConfig) {
        MoneyFormatter.format(
            amount = amount,
            currencyCode = currencyCode,
            decimals = decimals,
            config = formatterConfig
        )
    }

    FinanceListItem(
        icon = {
            if (isTransferItem) {
                val isIncomeTransfer = transaction.direction == 1
                val transferAccentColor = if (isIncomeTransfer) CreditEmeraldColor else DebtRoseColor
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(transferAccentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isIncomeTransfer) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = transferAccentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            } else {
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
            }
        },
        title = primaryTitle,
        subtitle = subtitleText,
        amountText = (if (isIncome && !formattedMoney.startsWith("+")) "+" else "") + formattedMoney,
        amountColor = amountColor,
        subAmountText = if (showDate) formattedDate else null,
        onClick = onClick
    )
}
