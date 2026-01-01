package com.sinxn.mymoney.core.data.local.model

import androidx.room.Embedded
import com.sinxn.mymoney.core.data.local.entity.TransactionEntity

data class TransactionWithCategory(
    @Embedded val transaction: TransactionEntity,
    val categoryName: String?, // Nullable because left join or basic system categories
    val categoryIcon: String?,
    val decimals: Int = 2,
    val currencyCode: String? = null,
    val currencySymbol: String? = null
)
