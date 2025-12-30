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
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PlaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["placeId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = DebtEntity::class,
            parentColumns = ["id"],
            childColumns = ["debtId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = SavingEntity::class,
            parentColumns = ["id"],
            childColumns = ["savingId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = RecurrentTransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["recurrenceId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("walletId"),
        Index("categoryId"),
        Index("placeId"),
        Index("eventId"),
        Index("debtId"),
        Index("savingId"),
        Index("recurrenceId"),
        // Optimization Indices
        Index(value = ["walletId", "confirmed", "countInTotal", "isDeleted", "direction", "money"], name = "index_wallet_balance"),
        Index(value = ["isDeleted", "date"], name = "index_all_transactions_sorted")
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val money: Long,
    val date: String, // Stored as string in legacy, typically ISO or timestamp
    val description: String?,
    val categoryId: String?,
    val walletId: String,
    val direction: Int, // 0: Expense, 1: Income
    val type: Int,
    val note: String?,
    val confirmed: Boolean,
    val countInTotal: Boolean,
    val isDeleted: Boolean,
    val placeId: String?,
    val eventId: String?,
    val debtId: String?,
    val savingId: String?,
    val recurrenceId: String?,
    val lastEdit: Long,
    val tag: String?
)
