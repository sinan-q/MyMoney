package com.sinxn.mymoney.core.data.local.model

import androidx.room.Embedded
import com.sinxn.mymoney.core.data.local.entity.SavingEntity

data class SavingWithDetails(
    @Embedded val saving: SavingEntity,
    val walletName: String = "",
    val walletIcon: String? = null,
    val walletCurrency: String = "USD",
    val walletCountInTotal: Boolean = true,
    val walletArchived: Boolean = false,
    val walletTag: String? = null,
    val progress: Long = 0L
) {
    val currentMoney: Long
        get() = saving.startMoney + progress

    val neededMoney: Long
        get() = (saving.endMoney - currentMoney).coerceAtLeast(0L)

    val isGoalReached: Boolean
        get() = currentMoney >= saving.endMoney
}
