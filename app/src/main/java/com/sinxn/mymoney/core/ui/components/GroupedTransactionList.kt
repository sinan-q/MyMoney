package com.sinxn.mymoney.core.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sinxn.mymoney.core.data.local.model.TransactionListItem
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MoneyFormatter

/**
 * Group raw List<TransactionWithCategory> into month headers and transaction items.
 */
fun groupTransactionsByMonth(
    transactions: List<TransactionWithCategory>,
    firstDayOfMonth: Int = 1
): List<TransactionListItem> {
    if (transactions.isEmpty()) return emptyList()

    val validTransactions = transactions.map {
        it to DateUtils.parseDate(it.transaction.date)
    }

    val grouped = validTransactions
        .sortedByDescending { it.second }
        .groupBy { (_, date) ->
            DateUtils.getStartOfBudgetMonth(date, firstDayOfMonth)
        }

    val result = ArrayList<TransactionListItem>(transactions.size + grouped.size)

    grouped.forEach { (monthDate, transactionsInGroup) ->
        var total = 0L
        var income = 0L
        var expense = 0L

        transactionsInGroup.forEach { (t, _) ->
            if (t.transaction.direction == 1 || t.transaction.type == 1) {
                total += t.transaction.money
                income += t.transaction.money
            } else {
                total -= t.transaction.money
                expense += t.transaction.money
            }
        }

        result.add(
            TransactionListItem.Header(
                date = monthDate,
                totalAmount = total,
                income = income,
                expense = expense,
                isTotalValid = true,
                balanceBreakdown = null
            )
        )

        transactionsInGroup.forEach { (t, _) ->
            result.add(TransactionListItem.Transaction(t))
        }
    }

    return result
}

/**
 * LazyListScope extension for rendering month-grouped transaction sticky headers and transaction items.
 * Can be plugged directly inside any parent LazyColumn (CategoryDetails, BudgetOverview, WalletDetails, etc.).
 */
@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.monthGroupedTransactionItems(
    items: List<TransactionListItem>,
    collapsedGroups: Set<String>,
    onToggleGroup: (String) -> Unit,
    onTransactionClick: (String) -> Unit,
    decimals: Int = 2,
    currencyCode: String = "USD",
    formatterConfig: MoneyFormatter.Config = MoneyFormatter.Config(),
    dateFormat: Int = 0,
    emptyMessage: String = "No transactions found"
) {
    if (items.isEmpty()) {
        item(key = "empty_transactions_state") {
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

    var currentHeader: TransactionListItem.Header? = null
    val customGrouped = mutableListOf<Pair<TransactionListItem.Header, MutableList<TransactionListItem>>>()

    items.forEach { item ->
        when (item) {
            is TransactionListItem.Header -> {
                currentHeader = item
                customGrouped.add(item to mutableListOf())
            }
            is TransactionListItem.Transaction -> {
                currentHeader?.let {
                    customGrouped.lastOrNull()?.second?.add(item)
                }
            }
        }
    }

    customGrouped.forEach { (header, groupItems) ->
        val headerKey = DateUtils.formatMonthHeader(header.date)
        val isCollapsed = collapsedGroups.contains(headerKey)

        val groupTransactions = groupItems.mapNotNull { (it as? TransactionListItem.Transaction)?.transaction }
        val groupCurrencies = groupTransactions.mapNotNull { it.currencySymbol ?: it.currencyCode }.distinct()
        val groupCurrency = if (groupCurrencies.size == 1) groupCurrencies.first() else currencyCode
        val groupDecimals = groupTransactions.firstOrNull()?.decimals ?: decimals

        stickyHeader(key = "header_$headerKey") {
            TransactionHeader(
                header = header.copy(isTotalValid = header.isTotalValid && groupCurrencies.size <= 1),
                decimals = groupDecimals,
                currencyCode = groupCurrency,
                formatterConfig = formatterConfig,
                isCollapsed = isCollapsed,
                onToggle = { onToggleGroup(headerKey) }
            )
        }

        if (!isCollapsed) {
            itemsIndexed(
                items = groupItems,
                key = { _, item ->
                    when (item) {
                        is TransactionListItem.Transaction -> item.transaction.transaction.id
                        else -> "unknown_${System.currentTimeMillis()}"
                    }
                }
            ) { index, item ->
                val isLastItem = index == groupItems.lastIndex

                when (item) {
                    is TransactionListItem.Transaction -> {
                        val trans = item.transaction
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
                    else -> {}
                }
            }
        }
    }
}
