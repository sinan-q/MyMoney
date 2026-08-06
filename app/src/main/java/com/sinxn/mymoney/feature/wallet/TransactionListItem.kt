package com.sinxn.mymoney.feature.wallet

import com.sinxn.mymoney.core.data.local.entity.TransactionEntity
import java.util.Date

sealed class TransactionListItem {
    data class Header(
        val date: Date,
        val totalAmount: Long,
        val income: Long,
        val expense: Long,
        val isTotalValid: Boolean = true,
        val balanceBreakdown: String? = null
    ) : TransactionListItem()

    data class Transaction(
        val transaction: com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
    ) : TransactionListItem()
}
