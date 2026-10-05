package com.sinxn.mymoney.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import com.sinxn.mymoney.core.ui.LocalFormatterConfig
import com.sinxn.mymoney.core.ui.LocalFormattingSettings
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MoneyFormatter
import com.sinxn.mymoney.ui.theme.ExpenseColor
import com.sinxn.mymoney.ui.theme.IncomeColor
import com.sinxn.mymoney.ui.theme.TransferColor

@Immutable
data class TransactionUiModel(
    val id: String,
    val primaryTitle: String,
    val subtitleText: String?,
    val formattedMoney: String,
    val amountColor: Color,
    val formattedDate: String,
    val isIncome: Boolean,
    val isTransferItem: Boolean,
    val isIncomeTransfer: Boolean,
    val iconData: IconData
)

fun TransactionWithCategory.toUiModel(
    decimals: Int = this.decimals,
    currencyCode: String = this.currencySymbol ?: this.currencyCode ?: "$",
    formatterConfig: MoneyFormatter.Config = MoneyFormatter.Config(),
    dateFormat: Int = 0
): TransactionUiModel {
    val isTransferItem = transaction.type == 1 || categoryName.equals("Transfer", ignoreCase = true)
    val categoryDisplayName = when {
        isTransferItem -> "Transfer"
        !categoryName.isNullOrBlank() -> categoryName
        else -> "Uncategorized"
    }

    val hasDescription = !transaction.description.isNullOrBlank()
    val primaryTitle = if (hasDescription) transaction.description!! else categoryDisplayName
    val subtitleText = if (hasDescription) categoryDisplayName else null

    val dateObj = DateUtils.parseDate(transaction.date)
    val formattedDate = DateUtils.formatDate(dateObj, dateFormat)

    val isIncome = transaction.direction == 1
    val isIncomeTransfer = isIncome
    val amountColor = when {
        isTransferItem -> TransferColor
        isIncome -> IncomeColor
        else -> ExpenseColor
    }

    val amount = if (isIncome || isTransferItem) transaction.money else -transaction.money
    val rawMoney = MoneyFormatter.format(
        amount = amount,
        currencyCode = currencyCode,
        decimals = decimals,
        config = formatterConfig
    )
    val formattedMoney = if (isIncome && !rawMoney.startsWith("+")) "+$rawMoney" else rawMoney
    val iconData = parseIconData(categoryIcon, categoryDisplayName)

    return TransactionUiModel(
        id = transaction.id,
        primaryTitle = primaryTitle,
        subtitleText = subtitleText,
        formattedMoney = formattedMoney,
        amountColor = amountColor,
        formattedDate = formattedDate,
        isIncome = isIncome,
        isTransferItem = isTransferItem,
        isIncomeTransfer = isIncomeTransfer,
        iconData = iconData
    )
}

@Composable
fun TransactionItem(
    uiModel: TransactionUiModel,
    showDate: Boolean = true,
    onClick: () -> Unit = {}
) {
    FinanceListItem(
        icon = {
            if (uiModel.isTransferItem) {
                CategoryIconExtended(
                    color = if (uiModel.isIncomeTransfer) IncomeColor else ExpenseColor,
                    icon = if (uiModel.isIncomeTransfer) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward)
            } else {
                CategoryIcon(iconData = uiModel.iconData)
            }
        },
        title = uiModel.primaryTitle,
        subtitle = uiModel.subtitleText,
        amountText = uiModel.formattedMoney,
        amountColor = uiModel.amountColor,
        subAmountText = if (showDate) uiModel.formattedDate else null,
        onClick = onClick
    )
}

@Composable
fun TransactionItem(
    item: TransactionWithCategory,
    decimals: Int = item.decimals,
    currencyCode: String = item.currencySymbol ?: item.currencyCode ?: "$",
    formatterConfig: MoneyFormatter.Config = LocalFormatterConfig.current,
    dateFormat: Int = LocalFormattingSettings.current.dateFormat,
    isLastItem: Boolean = false,
    showDate: Boolean = true,
    onClick: () -> Unit = {}
) {
    val uiModel = remember(item, decimals, currencyCode, formatterConfig, dateFormat) {
        item.toUiModel(decimals, currencyCode, formatterConfig, dateFormat)
    }
    TransactionItem(
        uiModel = uiModel,
        showDate = showDate,
        onClick = onClick
    )
}
