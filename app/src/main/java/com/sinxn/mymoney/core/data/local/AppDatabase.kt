package com.sinxn.mymoney.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.data.local.entity.TransactionEntity
import com.sinxn.mymoney.core.data.local.entity.WalletEntity

@Database(
    entities = [
        WalletEntity::class,
        CategoryEntity::class,
        TransactionEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun moneyDao(): MoneyDao
}
