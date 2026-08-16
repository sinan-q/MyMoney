package com.sinxn.mymoney.core.data.local.model

import java.util.Date

sealed class TransactionListItem {
    data class Header(
        val date: Date,
        val totalAmount: Long,
        val income: Long,
        val expense: Long,
        val isTotalValid: Boolean = true,
        val balanceBreakdown: String? = null,
        val transactionCount: Int = 0
    ) : TransactionListItem()

    data class Transaction(
        val transaction: TransactionWithCategory
    ) : TransactionListItem()
}
