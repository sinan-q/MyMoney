package com.sinxn.mymoney.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.data.local.entity.TransactionEntity
import com.sinxn.mymoney.core.data.local.entity.WalletEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MoneyDao {
    // Wallets
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallets(wallets: List<WalletEntity>)

    @Query("SELECT * FROM wallets ORDER BY `index` ASC")
    fun getWallets(): Flow<List<WalletEntity>>

    // Categories
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("SELECT * FROM categories")
    fun getCategories(): Flow<List<CategoryEntity>>

    // Transactions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)
    
    @Query("SELECT * FROM transactions WHERE walletId = :walletId ORDER BY date DESC")
    fun getTransactionsForWallet(walletId: Long): Flow<List<TransactionEntity>>
    
    @Query("DELETE FROM wallets")
    suspend fun clearWallets()
    
    @Query("DELETE FROM categories")
    suspend fun clearCategories()
    
    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    @Transaction
    suspend fun clearAll() {
        clearTransactions()
        clearCategories()
        clearWallets()
    }
}
