package com.sinxn.mymoney.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "debts",
    foreignKeys = [
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["walletId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PlaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["placeId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("walletId"), Index("placeId")]
)
data class DebtEntity(
    @PrimaryKey val id: String,
    val type: Int, // 0: Borrowed, 1: Lent (Check Schema)
    val icon: String,
    val description: String,
    val date: String,
    val expirationDate: String?,
    val walletId: String,
    val placeId: String?,
    val money: Long,
    val isArchived: Boolean,
    val note: String?,
    val isDeleted: Boolean,
    val lastEdit: Long,
    val tag: String?
)

@Entity(
    tableName = "savings",
    foreignKeys = [
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["walletId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("walletId")]
)
data class SavingEntity(
    @PrimaryKey val id: String,
    val description: String?,
    val icon: String,
    val startMoney: Long,
    val endMoney: Long, // Target amount
    val walletId: String,
    val endDate: String?,
    val isComplete: Boolean,
    val note: String?,
    val isDeleted: Boolean,
    val lastEdit: Long,
    val tag: String?
)

@Entity(
    tableName = "recurrent_transactions",
    foreignKeys = [
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = WalletEntity::class, parentColumns = ["id"], childColumns = ["walletId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PlaceEntity::class, parentColumns = ["id"], childColumns = ["placeId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = EventEntity::class, parentColumns = ["id"], childColumns = ["eventId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("categoryId"), Index("walletId"), Index("placeId"), Index("eventId")]
)
data class RecurrentTransactionEntity(
    @PrimaryKey val id: String,
    val money: Long,
    val description: String?,
    val categoryId: String,
    val direction: Int,
    val walletId: String,
    val placeId: String?,
    val note: String?,
    val eventId: String?,
    val confirmed: Boolean,
    val countInTotal: Boolean,
    val startDate: String,
    val rule: String, // RRule string
    val lastOccurrence: String,
    val nextOccurrence: String?,
    val isDeleted: Boolean,
    val lastEdit: Long,
    val tag: String?
)

@Entity(
    tableName = "transfers",
    foreignKeys = [
        ForeignKey(entity = TransactionEntity::class, parentColumns = ["id"], childColumns = ["transactionFromId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TransactionEntity::class, parentColumns = ["id"], childColumns = ["transactionToId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TransactionEntity::class, parentColumns = ["id"], childColumns = ["transactionTaxId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = PlaceEntity::class, parentColumns = ["id"], childColumns = ["placeId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = EventEntity::class, parentColumns = ["id"], childColumns = ["eventId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = RecurrentTransferEntity::class, parentColumns = ["id"], childColumns = ["recurrenceId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [
        Index("transactionFromId"), 
        Index("transactionToId"), 
        Index("transactionTaxId"),
        Index("placeId"), 
        Index("eventId"),
        Index("recurrenceId")
    ]
)
data class TransferEntity(
    @PrimaryKey val id: String,
    val description: String?,
    val date: String,
    val transactionFromId: String,
    val transactionToId: String,
    val transactionTaxId: String?,
    val note: String?,
    val placeId: String?,
    val eventId: String?,
    val recurrenceId: String?,
    val confirmed: Boolean,
    val countInTotal: Boolean,
    val isDeleted: Boolean,
    val lastEdit: Long,
    val tag: String?
)

@Entity(
    tableName = "recurrent_transfers",
    foreignKeys = [
        ForeignKey(entity = WalletEntity::class, parentColumns = ["id"], childColumns = ["walletFromId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = WalletEntity::class, parentColumns = ["id"], childColumns = ["walletToId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = EventEntity::class, parentColumns = ["id"], childColumns = ["eventId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = PlaceEntity::class, parentColumns = ["id"], childColumns = ["placeId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("walletFromId"), Index("walletToId"), Index("eventId"), Index("placeId")]
)
data class RecurrentTransferEntity(
    @PrimaryKey val id: String,
    val description: String?,
    val walletFromId: String,
    val walletToId: String,
    val moneyFrom: Long,
    val moneyTo: Long,
    val moneyTax: Long?,
    val note: String?,
    val eventId: String?,
    val placeId: String?,
    val confirmed: Boolean,
    val countInTotal: Boolean,
    val startDate: String,
    val lastOccurrence: String,
    val nextOccurrence: String?,
    val rule: String,
    val isDeleted: Boolean,
    val lastEdit: Long,
    val tag: String?
)
