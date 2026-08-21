package com.sinxn.mymoney.core.data.local.model

import androidx.compose.runtime.Immutable
import com.sinxn.mymoney.core.ui.components.TransactionUiModel
import java.util.Date

@Immutable
sealed class TransactionListItem {
    @Immutable
    data class Header(
        val date: Date,
        val totalAmount: Long,
        val income: Long,
        val expense: Long,
        val isTotalValid: Boolean = true,
        val balanceBreakdown: String? = null,
        val transactionCount: Int = 0,
        val formattedDate: String? = null,
        val formattedTotal: String? = null,
        val formattedIncome: String? = null,
        val formattedExpense: String? = null
    ) : TransactionListItem()

    @Immutable
    data class Transaction(
        val transaction: TransactionWithCategory,
        val uiModel: TransactionUiModel? = null
    ) : TransactionListItem()
}

@Immutable
data class TransactionMonthGroup(
    val header: TransactionListItem.Header,
    val items: List<TransactionListItem.Transaction>
)
