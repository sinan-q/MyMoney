package com.sinxn.mymoney.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val icon: String,
    val type: Int, // 0: Expense, 1: Income (guessing from context, or checking moneywallet source)
    val parentId: Long?,
    val showReport: Boolean,
    val index: Int
)
