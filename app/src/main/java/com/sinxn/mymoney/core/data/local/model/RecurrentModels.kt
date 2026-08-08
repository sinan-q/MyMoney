package com.sinxn.mymoney.core.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.data.local.entity.EventEntity
import com.sinxn.mymoney.core.data.local.entity.PlaceEntity
import com.sinxn.mymoney.core.data.local.entity.RecurrentTransactionEntity
import com.sinxn.mymoney.core.data.local.entity.RecurrentTransferEntity
import com.sinxn.mymoney.core.data.local.entity.WalletEntity

data class RecurrentTransactionWithDetails(
    @Embedded val recurrentTransaction: RecurrentTransactionEntity,
    @Relation(parentColumn = "categoryId", entityColumn = "id") val category: CategoryEntity,
    @Relation(parentColumn = "walletId", entityColumn = "id") val wallet: WalletEntity,
    @Relation(parentColumn = "placeId", entityColumn = "id") val place: PlaceEntity?,
    @Relation(parentColumn = "eventId", entityColumn = "id") val event: EventEntity?
)

data class RecurrentTransferWithDetails(
    @Embedded val recurrentTransfer: RecurrentTransferEntity,
    @Relation(parentColumn = "walletFromId", entityColumn = "id") val walletFrom: WalletEntity,
    @Relation(parentColumn = "walletToId", entityColumn = "id") val walletTo: WalletEntity,
    @Relation(parentColumn = "placeId", entityColumn = "id") val place: PlaceEntity?,
    @Relation(parentColumn = "eventId", entityColumn = "id") val event: EventEntity?
)
