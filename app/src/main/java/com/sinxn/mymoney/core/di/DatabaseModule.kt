package com.sinxn.mymoney.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sinxn.mymoney.core.data.local.AppDatabase
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "mymoney.db"
        )
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                super.onCreate(db)
                val now = System.currentTimeMillis()
                
                // 1. Seed System Categories
                val systemCategories = listOf(
                    arrayOf("system-category-system::transfer", "Transfer", "ic_transfer", "2", "0", "0", "0", now.toString(), "system::transfer"),
                    arrayOf("system-category-system::transfer_tax", "Transfer fee", "ic_transfer_tax", "2", "0", "0", "0", now.toString(), "system::transfer_tax"),
                    arrayOf("system-category-system::debt", "Debt", "ic_debt", "2", "0", "0", "0", now.toString(), "system::debt"),
                    arrayOf("system-category-system::credit", "Credit", "ic_credit", "2", "0", "0", "0", now.toString(), "system::credit"),
                    arrayOf("system-category-system::paid_debt", "Paid debt", "ic_debt_paid", "2", "0", "0", "0", now.toString(), "system::paid_debt"),
                    arrayOf("system-category-system::paid_credit", "Paid credit", "ic_credit_paid", "2", "0", "0", "0", now.toString(), "system::paid_credit"),
                    arrayOf("system-category-system::tax", "Tax", "ic_tax", "2", "0", "0", "0", now.toString(), "system::tax"),
                    arrayOf("system-category-system::deposit", "Deposit", "ic_saving_deposit", "2", "0", "0", "0", now.toString(), "system::deposit"),
                    arrayOf("system-category-system::withdraw", "Withdraw", "ic_saving_withdraw", "2", "0", "0", "0", now.toString(), "system::withdraw")
                )

                for (cat in systemCategories) {
                    db.execSQL(
                        "INSERT OR IGNORE INTO categories (id, name, icon, type, parentId, showReport, isArchived, `index`, isDeleted, lastEdit, tag) VALUES (?, ?, ?, ?, NULL, ?, 0, ?, 0, ?, ?)",
                        arrayOf<Any>(cat[0], cat[1], cat[2], cat[3].toInt(), cat[4].toInt(), cat[5].toInt(), cat[7].toLong(), cat[8])
                    )
                }

                // 2. Seed Default Currencies
                val defaultCurrencies = listOf(
                    arrayOf("USD", "US Dollar", "$", "2", "1", "uuid-currency-usd"),
                    arrayOf("EUR", "Euro", "€", "2", "1", "uuid-currency-eur"),
                    arrayOf("GBP", "British Pound", "£", "2", "1", "uuid-currency-gbp"),
                    arrayOf("INR", "Indian Rupee", "₹", "2", "1", "uuid-currency-inr"),
                    arrayOf("JPY", "Japanese Yen", "¥", "0", "0", "uuid-currency-jpy"),
                    arrayOf("CAD", "Canadian Dollar", "$", "2", "0", "uuid-currency-cad"),
                    arrayOf("AUD", "Australian Dollar", "$", "2", "0", "uuid-currency-aud"),
                    arrayOf("CHF", "Swiss Franc", "CHF", "2", "0", "uuid-currency-chf"),
                    arrayOf("CNY", "Chinese Yuan", "¥", "2", "0", "uuid-currency-cny")
                )

                for (cur in defaultCurrencies) {
                    db.execSQL(
                        "INSERT OR IGNORE INTO currencies (iso, name, symbol, decimals, isFavourite, id, lastEdit, isDeleted) VALUES (?, ?, ?, ?, ?, ?, ?, 0)",
                        arrayOf<Any>(cur[0], cur[1], cur[2], cur[3].toInt(), cur[4].toInt(), cur[5], now)
                    )
                }
            }
        })
        .build()
    }

    @Provides
    @Singleton
    fun provideMoneyDao(database: AppDatabase): MoneyDao {
        return database.moneyDao()
    }
}
