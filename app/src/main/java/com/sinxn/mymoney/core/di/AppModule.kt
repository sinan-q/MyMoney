package com.sinxn.mymoney.core.di

import android.content.ContentResolver
import android.content.Context
import com.google.gson.Gson
import com.sinxn.mymoney.core.data.repository.BackupRepository
import com.sinxn.mymoney.core.data.repository.BackupRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver {
        return context.contentResolver
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindBackupRepository(
        backupRepositoryImpl: BackupRepositoryImpl
    ): BackupRepository
}
