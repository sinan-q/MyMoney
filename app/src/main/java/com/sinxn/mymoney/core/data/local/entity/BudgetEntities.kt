package com.sinxn.mymoney.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("categoryId")]
)
data class BudgetEntity(
    @PrimaryKey val id: String,
    val type: Int,
    val categoryId: String?,
    val startDate: String,
    val endDate: String,
    val money: Long,
    val currency: String,
    val tag: String?,
    val lastEdit: Long,
    val isDeleted: Boolean
)

@Entity(
    tableName = "budget_wallets",
    primaryKeys = ["budgetId", "walletId"],
    foreignKeys = [
        ForeignKey(entity = BudgetEntity::class, parentColumns = ["id"], childColumns = ["budgetId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = WalletEntity::class, parentColumns = ["id"], childColumns = ["walletId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("budgetId"), Index("walletId")]
)
data class BudgetWalletEntity(
    val budgetId: String,
    val walletId: String,
    val isDeleted: Boolean,
    val lastEdit: Long,
    val id: String // uuid
)
