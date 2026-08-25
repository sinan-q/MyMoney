package com.sinxn.mymoney.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "places")
data class PlaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    val isArchived: Boolean = false,
    val isDeleted: Boolean,
    val lastEdit: Long,
    val tag: String?
)

@Entity(tableName = "people")
data class PersonEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val note: String?,
    val isArchived: Boolean = false,
    val isDeleted: Boolean,
    val lastEdit: Long,
    val lastUsed: Long = lastEdit,
    val tag: String?
)

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val note: String?,
    val startDate: String,
    val endDate: String,
    val isArchived: Boolean = false,
    val isDeleted: Boolean,
    val lastEdit: Long,
    val tag: String?
)

@Entity(
    tableName = "currencies",
    indices = [androidx.room.Index(value = ["id"], unique = true)]
)
data class CurrencyEntity(
    @PrimaryKey val iso: String,
    val name: String,
    val symbol: String?,
    val decimals: Int,
    val isFavourite: Boolean,
    val id: String, // uuid
    val lastEdit: Long,
    val isDeleted: Boolean
)
