package com.sinxn.mymoney.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transaction_models",
    foreignKeys = [
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = WalletEntity::class, parentColumns = ["id"], childColumns = ["walletId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PlaceEntity::class, parentColumns = ["id"], childColumns = ["placeId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = EventEntity::class, parentColumns = ["id"], childColumns = ["eventId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("categoryId"), Index("walletId"), Index("placeId"), Index("eventId")]
)
data class TransactionModelEntity(
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
    val isDeleted: Boolean,
    val lastEdit: Long,
    val tag: String?
)

@Entity(
    tableName = "transfer_models",
    foreignKeys = [
        ForeignKey(entity = WalletEntity::class, parentColumns = ["id"], childColumns = ["walletFromId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = WalletEntity::class, parentColumns = ["id"], childColumns = ["walletToId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = EventEntity::class, parentColumns = ["id"], childColumns = ["eventId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = PlaceEntity::class, parentColumns = ["id"], childColumns = ["placeId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("walletFromId"), Index("walletToId"), Index("eventId"), Index("placeId")]
)
data class TransferModelEntity(
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
    val isDeleted: Boolean,
    val lastEdit: Long,
    val tag: String?
)
