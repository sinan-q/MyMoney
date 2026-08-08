package com.sinxn.mymoney.core.data.local.model

import androidx.room.Embedded
import com.sinxn.mymoney.core.data.local.entity.TransactionModelEntity
import com.sinxn.mymoney.core.data.local.entity.TransferModelEntity

data class TransactionModelWithDetails(
    @Embedded val model: TransactionModelEntity,
    val categoryName: String? = null,
    val categoryIcon: String? = null,
    val walletName: String = "",
    val walletCurrency: String = "USD",
    val walletDecimals: Int = 2,
    val currencySymbol: String = "$"
)

data class TransferModelWithDetails(
    @Embedded val model: TransferModelEntity,
    val walletFromName: String = "",
    val walletFromCurrency: String = "USD",
    val walletToName: String = "",
    val walletToCurrency: String = "USD",
    val currencySymbol: String = "$"
)
