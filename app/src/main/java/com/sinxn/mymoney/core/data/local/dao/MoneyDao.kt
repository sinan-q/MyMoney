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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaces(places: List<com.sinxn.mymoney.core.data.local.entity.PlaceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeople(people: List<com.sinxn.mymoney.core.data.local.entity.PersonEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<com.sinxn.mymoney.core.data.local.entity.EventEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebts(debts: List<com.sinxn.mymoney.core.data.local.entity.DebtEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavings(savings: List<com.sinxn.mymoney.core.data.local.entity.SavingEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurrentTransactions(recurrentTransactions: List<com.sinxn.mymoney.core.data.local.entity.RecurrentTransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfers(transfers: List<com.sinxn.mymoney.core.data.local.entity.TransferEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurrentTransfers(transfers: List<com.sinxn.mymoney.core.data.local.entity.RecurrentTransferEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<com.sinxn.mymoney.core.data.local.entity.BudgetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetWallets(budgetWallets: List<com.sinxn.mymoney.core.data.local.entity.BudgetWalletEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransferPeople(items: List<com.sinxn.mymoney.core.data.local.entity.TransferPeopleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachments(items: List<com.sinxn.mymoney.core.data.local.entity.AttachmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionAttachments(items: List<com.sinxn.mymoney.core.data.local.entity.TransactionAttachmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransferAttachments(items: List<com.sinxn.mymoney.core.data.local.entity.TransferAttachmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrencies(items: List<com.sinxn.mymoney.core.data.local.entity.CurrencyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionModels(items: List<com.sinxn.mymoney.core.data.local.entity.TransactionModelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransferModels(items: List<com.sinxn.mymoney.core.data.local.entity.TransferModelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEventPeople(items: List<com.sinxn.mymoney.core.data.local.entity.EventPeopleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebtPeople(items: List<com.sinxn.mymoney.core.data.local.entity.DebtPeopleEntity>)

    @Query("SELECT * FROM wallets WHERE isDeleted = 0 AND isArchived = 0 ORDER BY `index` ASC")
    fun getWallets(): Flow<List<WalletEntity>>

    @androidx.room.Transaction
    @Query("""
        SELECT 
            w.*, 
            (w.startMoney + COALESCE(SUM(
                CASE 
                    WHEN t.direction = 1 THEN t.money 
                    WHEN t.direction = 0 THEN -t.money 
                    ELSE 0 
                END
            ), 0)) AS currentBalance,
            COALESCE(c.decimals, 2) as decimals,
            c.symbol as currencySymbol
        FROM wallets w 
        LEFT JOIN transactions t ON w.id = t.walletId 
            AND t.confirmed = 1 
            AND t.countInTotal = 1
            AND t.isDeleted = 0
        LEFT JOIN currencies c ON w.currency = c.iso
        WHERE w.isDeleted = 0
        GROUP BY w.id 
        ORDER BY w.`index` ASC
    """)
    fun getWalletsWithBalance(): Flow<List<com.sinxn.mymoney.core.data.local.model.WalletWithBalance>>

    @androidx.room.Transaction
    @Query("""
        SELECT 
            w.*, 
            (w.startMoney + COALESCE(SUM(
                CASE 
                    WHEN t.direction = 1 THEN t.money 
                    WHEN t.direction = 0 THEN -t.money 
                    ELSE 0 
                END
            ), 0)) AS currentBalance,
            COALESCE(c.decimals, 2) as decimals,
            c.symbol as currencySymbol
        FROM wallets w 
        LEFT JOIN transactions t ON w.id = t.walletId 
            AND t.confirmed = 1 
            AND t.countInTotal = 1
            AND t.isDeleted = 0
        LEFT JOIN currencies c ON w.currency = c.iso
        WHERE w.isDeleted = 0 AND w.id = :walletId
        GROUP BY w.id
    """)
    fun getWalletWithBalance(walletId: String): Flow<com.sinxn.mymoney.core.data.local.model.WalletWithBalance?>

    // Categories
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("SELECT * FROM categories WHERE isDeleted = 0")
    fun getCategories(): Flow<List<CategoryEntity>>

    // Transactions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)
    
    @androidx.room.Transaction
    @Query("""
        SELECT t.*, c.name as categoryName, c.icon as categoryIcon
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        WHERE t.walletId = :walletId 
        ORDER BY t.date DESC
    """)
    fun getTransactionsForWallet(walletId: String): Flow<List<com.sinxn.mymoney.core.data.local.model.TransactionWithCategory>>

    @androidx.room.Transaction
    @Query("""
        SELECT t.*, c.name as categoryName, c.icon as categoryIcon
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        WHERE t.isDeleted = 0 
        ORDER BY t.date DESC
    """)
    fun getAllTransactions(): Flow<List<com.sinxn.mymoney.core.data.local.model.TransactionWithCategory>>

    @androidx.room.Transaction
    @Query("""
        SELECT t.*, c.name as categoryName, c.icon as categoryIcon
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        WHERE t.id = :transactionId
    """)
    fun getTransactionWithCategory(transactionId: String): Flow<com.sinxn.mymoney.core.data.local.model.TransactionWithCategory?>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Query("""
        SELECT COALESCE(SUM(
            CASE 
                WHEN t.direction = 1 THEN t.money 
                WHEN t.direction = 0 THEN -t.money 
                ELSE 0 
            END
        ), 0)
        FROM transactions t
        INNER JOIN wallets w ON t.walletId = w.id
        WHERE t.isDeleted = 0 
          AND t.confirmed = 1 
          AND t.countInTotal = 1
          AND w.countInTotal = 1
          AND w.isDeleted = 0
    """)
    fun getTotalBalance(): Flow<Long>
    
    @Query("SELECT * FROM wallets WHERE id = :id")
    suspend fun getWalletById(id: String): WalletEntity?
    
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

    // Places
    @Query("SELECT * FROM places WHERE isDeleted = 0")
    fun getPlaces(): Flow<List<com.sinxn.mymoney.core.data.local.entity.PlaceEntity>>

    @Query("SELECT * FROM places WHERE id = :id")
    suspend fun getPlaceById(id: String): com.sinxn.mymoney.core.data.local.entity.PlaceEntity?

    // Events
    @Query("SELECT * FROM events WHERE isDeleted = 0")
    fun getEvents(): Flow<List<com.sinxn.mymoney.core.data.local.entity.EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getEventById(id: String): com.sinxn.mymoney.core.data.local.entity.EventEntity?

    // People
    @Query("SELECT * FROM people WHERE isDeleted = 0")
    fun getPeople(): Flow<List<com.sinxn.mymoney.core.data.local.entity.PersonEntity>>

    @Query("""
        SELECT p.* 
        FROM people p
        INNER JOIN transaction_people tp ON p.id = tp.personId
        WHERE tp.transactionId = :transactionId AND p.isDeleted = 0
    """)
    fun getPeopleForTransaction(transactionId: String): Flow<List<com.sinxn.mymoney.core.data.local.entity.PersonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionPeople(links: List<com.sinxn.mymoney.core.data.local.entity.TransactionPeopleEntity>)

    @Query("DELETE FROM transaction_people WHERE transactionId = :transactionId")
    suspend fun deletePeopleForTransaction(transactionId: String)

    // Attachments
    @Query("""
        SELECT a.* 
        FROM attachments a
        INNER JOIN transaction_attachment ta ON a.id = ta.attachmentId
        WHERE ta.transactionId = :transactionId
    """)
    fun getAttachmentsForTransaction(transactionId: String): Flow<List<com.sinxn.mymoney.core.data.local.entity.AttachmentEntity>>
}
