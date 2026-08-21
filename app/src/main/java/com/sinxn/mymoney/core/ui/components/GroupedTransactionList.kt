package com.sinxn.mymoney.core.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sinxn.mymoney.core.data.local.model.TransactionListItem
import com.sinxn.mymoney.core.data.local.model.TransactionMonthGroup
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MoneyFormatter

/**
 * Group raw List<TransactionWithCategory> directly into structured TransactionMonthGroup instances.
 */
fun groupTransactionsIntoMonthGroups(
    transactions: List<TransactionWithCategory>,
    firstDayOfMonth: Int = 1,
    decimals: Int = 2,
    currencyCode: String = "USD",
    formatterConfig: MoneyFormatter.Config = MoneyFormatter.Config(),
    dateFormat: Int = 0
): List<TransactionMonthGroup> {
    if (transactions.isEmpty()) return emptyList()

    val validTransactions = transactions.map {
        it to DateUtils.parseDate(it.transaction.date)
    }

    val grouped = validTransactions
        .sortedByDescending { it.second }
        .groupBy { (_, date) ->
            DateUtils.getStartOfBudgetMonth(date, firstDayOfMonth)
        }

    val result = ArrayList<TransactionMonthGroup>(grouped.size)

    grouped.forEach { (monthDate, transactionsInGroup) ->
        var total = 0L
        var income = 0L
        var expense = 0L

        transactionsInGroup.forEach { (t, _) ->
            if (t.transaction.direction == 1) {
                total += t.transaction.money
                income += t.transaction.money
            } else {
                total -= t.transaction.money
                expense += t.transaction.money
            }
        }

        val formattedTotal = MoneyFormatter.format(
            amount = total,
            currencyCode = currencyCode,
            decimals = decimals,
            config = formatterConfig
        )
        val formattedIncome = MoneyFormatter.format(
            amount = income,
            currencyCode = currencyCode,
            decimals = decimals,
            config = formatterConfig
        )
        val formattedExpense = MoneyFormatter.format(
            amount = expense,
            currencyCode = currencyCode,
            decimals = decimals,
            config = formatterConfig
        )
        val formattedMonthHeader = DateUtils.formatMonthHeader(monthDate)

        val header = TransactionListItem.Header(
            date = monthDate,
            totalAmount = total,
            income = income,
            expense = expense,
            isTotalValid = true,
            balanceBreakdown = null,
            transactionCount = transactionsInGroup.size,
            formattedDate = formattedMonthHeader,
            formattedTotal = formattedTotal,
            formattedIncome = formattedIncome,
            formattedExpense = formattedExpense
        )

        val items = transactionsInGroup.map { (t, _) ->
            val uiModel = t.toUiModel(
                decimals = t.decimals,
                currencyCode = t.currencySymbol ?: t.currencyCode ?: currencyCode,
                formatterConfig = formatterConfig,
                dateFormat = dateFormat
            )
            TransactionListItem.Transaction(t, uiModel)
        }

        result.add(TransactionMonthGroup(header = header, items = items))
    }

    return result
}

/**
 * Group raw List<TransactionWithCategory> into month headers and transaction items.
 */
fun groupTransactionsByMonth(
    transactions: List<TransactionWithCategory>,
    firstDayOfMonth: Int = 1,
    decimals: Int = 2,
    currencyCode: String = "USD",
    formatterConfig: MoneyFormatter.Config = MoneyFormatter.Config(),
    dateFormat: Int = 0
): List<TransactionListItem> {
    val monthGroups = groupTransactionsIntoMonthGroups(
        transactions = transactions,
        firstDayOfMonth = firstDayOfMonth,
        decimals = decimals,
        currencyCode = currencyCode,
        formatterConfig = formatterConfig,
        dateFormat = dateFormat
    )

    val result = ArrayList<TransactionListItem>(transactions.size + monthGroups.size)
    monthGroups.forEach { group ->
        result.add(group.header)
        result.addAll(group.items)
    }
    return result
}

/**
 * Primary LazyListScope extension: renders structured List<TransactionMonthGroup> directly
 * with ZERO in-composable grouping or memory allocations.
 */
@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.monthGroupedTransactionItems(
    monthGroups: List<TransactionMonthGroup>,
    collapsedGroups: Set<String>,
    onToggleGroup: (String) -> Unit,
    onTransactionClick: (String) -> Unit,
    decimals: Int = 2,
    currencyCode: String = "USD",
    formatterConfig: MoneyFormatter.Config = MoneyFormatter.Config(),
    dateFormat: Int = 0,
    emptyMessage: String = "No transactions found"
) {
    if (monthGroups.isEmpty()) {
        item(key = "empty_transactions_state", contentType = "empty_state") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    monthGroups.forEach { group ->
        val header = group.header
        val headerKey = header.formattedDate ?: DateUtils.formatMonthHeader(header.date)
        val isCollapsed = collapsedGroups.contains(headerKey)

        stickyHeader(key = "header_$headerKey", contentType = "header") {
            TransactionHeader(
                header = header,
                decimals = decimals,
                currencyCode = currencyCode,
                formatterConfig = formatterConfig,
                isCollapsed = isCollapsed,
                onToggle = { onToggleGroup(headerKey) }
            )
        }

        if (!isCollapsed) {
            itemsIndexed(
                items = group.items,
                key = { _, item -> item.transaction.transaction.id },
                contentType = { _, _ -> "transaction" }
            ) { index, item ->
                val isLastItem = index == group.items.lastIndex
                val trans = item.transaction
                val uiModel = item.uiModel

                if (uiModel != null) {
                    TransactionItem(
                        uiModel = uiModel,
                        isLastItem = isLastItem,
                        showDate = true,
                        onClick = { onTransactionClick(uiModel.id) }
                    )
                } else {
                    TransactionItem(
                        item = trans,
                        decimals = trans.decimals,
                        currencyCode = trans.currencySymbol ?: trans.currencyCode ?: currencyCode,
                        formatterConfig = formatterConfig,
                        dateFormat = dateFormat,
                        isLastItem = isLastItem,
                        showDate = true,
                        onClick = { onTransactionClick(trans.transaction.id) }
                    )
                }
            }
        }
    }
}
