package com.sinxn.mymoney.core.data.local.model

import androidx.room.Embedded
import androidx.room.Ignore
import com.sinxn.mymoney.core.data.local.entity.DebtEntity
import com.sinxn.mymoney.core.data.local.entity.PersonEntity
import kotlin.math.abs

data class DebtWithDetails(
    @Embedded val debt: DebtEntity,
    val walletName: String = "",
    val walletIcon: String? = null,
    val walletCurrency: String = "USD",
    val walletArchived: Boolean = false,
    val placeName: String? = null,
    val placeIcon: String? = null,
    val progress: Long = 0L
) {
    @Ignore
    var people: List<PersonEntity> = emptyList()

    val remainingMoney: Long
        get() = (abs(debt.money) - abs(progress)).coerceAtLeast(0L)
}
