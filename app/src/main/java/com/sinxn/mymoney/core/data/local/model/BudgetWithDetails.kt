package com.sinxn.mymoney.core.data.local.model

import androidx.room.Embedded
import androidx.room.Ignore
import com.sinxn.mymoney.core.data.local.entity.BudgetEntity
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.data.local.entity.WalletEntity

data class BudgetWithDetails(
    @Embedded val budget: BudgetEntity,
    val categoryName: String? = null,
    val categoryIcon: String? = null,
    val categoryType: Int? = null,
    val categoryShowReport: Boolean? = null,
    val categoryTag: String? = null,
    val progress: Long = 0L,
    val walletIds: String? = null,
    val hasWalletInTotal: Boolean = true
) {
    @Ignore
    var wallets: List<WalletEntity> = emptyList()

    @Ignore
    var category: CategoryEntity? = null
}
