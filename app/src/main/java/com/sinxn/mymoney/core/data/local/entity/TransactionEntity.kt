package com.sinxn.mymoney.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["walletId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("walletId"),
        Index("categoryId")
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: Long,
    val money: Long,
    val date: String, // Stored as string in legacy, typically ISO or timestamp
    val description: String?,
    val categoryId: Long?,
    val walletId: Long,
    val direction: Int, // -1: Expense, 1: Income
    val type: Int,
    val note: String?,
    val confirmed: Boolean
)
