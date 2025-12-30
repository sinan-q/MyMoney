package com.sinxn.mymoney.core.di

import android.content.Context
import androidx.room.Room
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
        ).fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideMoneyDao(database: AppDatabase): MoneyDao {
        return database.moneyDao()
    }
}
