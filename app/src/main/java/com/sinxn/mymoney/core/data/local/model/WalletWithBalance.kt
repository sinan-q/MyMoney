package com.sinxn.mymoney.core.data.local.model

import androidx.room.Embedded
import com.sinxn.mymoney.core.data.local.entity.WalletEntity

data class WalletWithBalance(
    @Embedded val wallet: WalletEntity,
    val currentBalance: Long,
    val decimals: Int = 2,
    val currencySymbol: String? = null,
    val isTotalValid: Boolean = true
)
