package com.sinxn.mymoney.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val icon: String,
    val currency: String,
    val startMoney: Long,
    val isArchived: Boolean,
    val note: String?,
    val countInTotal: Boolean,
    val index: Int
)
