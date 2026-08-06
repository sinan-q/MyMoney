package com.sinxn.mymoney.core.data.local.dao

import androidx.room.*
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.data.local.entity.TransactionEntity
import com.sinxn.mymoney.core.data.local.entity.WalletEntity
import com.sinxn.mymoney.core.data.local.model.WalletWithBalance
import kotlinx.coroutines.flow.Flow

@Dao
interface MoneyDao {
    // Wallets
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWallets(wallets: List<WalletEntity>)

    @Update
    suspend fun updateWallet(wallet: WalletEntity)

    @Update
    suspend fun updateWallets(wallets: List<WalletEntity>)

    @Query("UPDATE wallets SET countInTotal = :countInTotal, lastEdit = :lastEdit WHERE id = :walletId")
    suspend fun updateWalletCountInTotal(walletId: String, countInTotal: Boolean, lastEdit: Long)

    @Query("UPDATE wallets SET isArchived = :isArchived, lastEdit = :lastEdit WHERE id = :walletId")
    suspend fun updateWalletArchived(walletId: String, isArchived: Boolean, lastEdit: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaces(places: List<com.sinxn.mymoney.core.data.local.entity.PlaceEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPeople(people: List<com.sinxn.mymoney.core.data.local.entity.PersonEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEvents(events: List<com.sinxn.mymoney.core.data.local.entity.EventEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDebts(debts: List<com.sinxn.mymoney.core.data.local.entity.DebtEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSavings(savings: List<com.sinxn.mymoney.core.data.local.entity.SavingEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRecurrentTransactions(recurrentTransactions: List<com.sinxn.mymoney.core.data.local.entity.RecurrentTransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransfers(transfers: List<com.sinxn.mymoney.core.data.local.entity.TransferEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfer(transfer: com.sinxn.mymoney.core.data.local.entity.TransferEntity)

    @Update
    suspend fun updateTransfer(transfer: com.sinxn.mymoney.core.data.local.entity.TransferEntity)

    @Query("SELECT * FROM transfers WHERE (transactionFromId = :transactionId OR transactionToId = :transactionId) AND isDeleted = 0 LIMIT 1")
    suspend fun getTransferByTransactionId(transactionId: String): com.sinxn.mymoney.core.data.local.entity.TransferEntity?

    @Query("SELECT * FROM transactions WHERE type = 2 AND money = :money AND date = :date AND id != :transactionId AND isDeleted = 0 LIMIT 1")
    suspend fun findSiblingTransferTransaction(money: Long, date: String, transactionId: String): TransactionEntity?

    @Query("SELECT * FROM transfers WHERE id = :id AND isDeleted = 0")
    suspend fun getTransferById(id: String): com.sinxn.mymoney.core.data.local.entity.TransferEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRecurrentTransfers(transfers: List<com.sinxn.mymoney.core.data.local.entity.RecurrentTransferEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBudgets(budgets: List<com.sinxn.mymoney.core.data.local.entity.BudgetEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBudgetWallets(budgetWallets: List<com.sinxn.mymoney.core.data.local.entity.BudgetWalletEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransferPeople(items: List<com.sinxn.mymoney.core.data.local.entity.TransferPeopleEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAttachments(items: List<com.sinxn.mymoney.core.data.local.entity.AttachmentEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactionAttachments(items: List<com.sinxn.mymoney.core.data.local.entity.TransactionAttachmentEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransferAttachments(items: List<com.sinxn.mymoney.core.data.local.entity.TransferAttachmentEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCurrencies(items: List<com.sinxn.mymoney.core.data.local.entity.CurrencyEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactionModels(items: List<com.sinxn.mymoney.core.data.local.entity.TransactionModelEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransferModels(items: List<com.sinxn.mymoney.core.data.local.entity.TransferModelEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEventPeople(items: List<com.sinxn.mymoney.core.data.local.entity.EventPeopleEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDebtPeople(items: List<com.sinxn.mymoney.core.data.local.entity.DebtPeopleEntity>)

    @Query("SELECT * FROM wallets WHERE isDeleted = 0 AND isArchived = 0 ORDER BY `index` ASC")
    fun getWallets(): Flow<List<WalletEntity>>

    @Query("SELECT * FROM wallets WHERE isDeleted = 0 ORDER BY `index` ASC")
    fun getAllWalletsIncludingArchived(): Flow<List<WalletEntity>>

    @Transaction
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
            c.symbol as currencySymbol,
            1 AS isTotalValid
        FROM wallets w 
        LEFT JOIN transactions t ON w.id = t.walletId 
            AND t.confirmed = 1 
            AND t.countInTotal = 1
            AND t.isDeleted = 0
            AND t.date <= :maxDate
        LEFT JOIN currencies c ON w.currency = c.iso
        WHERE w.isDeleted = 0
        GROUP BY w.id 
        ORDER BY w.`index` ASC
    """)
    fun getWalletsWithBalance(maxDate: String): Flow<List<WalletWithBalance>>

    @Transaction
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
            c.symbol as currencySymbol,
            1 AS isTotalValid
        FROM wallets w 
        LEFT JOIN transactions t ON w.id = t.walletId 
            AND t.confirmed = 1 
            AND t.countInTotal = 1
            AND t.isDeleted = 0
            AND t.date <= :maxDate
        LEFT JOIN currencies c ON w.currency = c.iso
        WHERE w.isDeleted = 0 AND w.id = :walletId
        GROUP BY w.id
    """)
    fun getWalletWithBalance(walletId: String, maxDate: String): Flow<WalletWithBalance?>

    // Categories
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Update
    suspend fun updateCategories(categories: List<CategoryEntity>)

    @Query("UPDATE categories SET showReport = :showReport, lastEdit = :lastEdit WHERE id = :categoryId")
    suspend fun updateCategoryShowReport(categoryId: String, showReport: Boolean, lastEdit: Long)

    @Query("SELECT * FROM categories WHERE isDeleted = 0")
    fun getCategories(): Flow<List<CategoryEntity>>

    // Transactions
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransactions(transactions: List<TransactionEntity>)
    
    @androidx.room.Transaction
    @Query("""
        SELECT t.*, c.name as categoryName, c.icon as categoryIcon,
               COALESCE(curr.decimals, 2) as decimals, curr.symbol as currencySymbol,
               w.currency as currencyCode
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        INNER JOIN wallets w ON t.walletId = w.id
        LEFT JOIN currencies curr ON w.currency = curr.iso
        WHERE t.walletId = :walletId 
          AND t.date <= :maxDate
        ORDER BY t.date DESC
    """)
    fun getTransactionsForWallet(walletId: String, maxDate: String): Flow<List<com.sinxn.mymoney.core.data.local.model.TransactionWithCategory>>

    @androidx.room.Transaction
    @Query("""
        SELECT t.*, c.name as categoryName, c.icon as categoryIcon,
               COALESCE(curr.decimals, 2) as decimals, curr.symbol as currencySymbol,
               w.currency as currencyCode
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        INNER JOIN wallets w ON t.walletId = w.id
        LEFT JOIN currencies curr ON w.currency = curr.iso
        WHERE t.isDeleted = 0 AND w.isDeleted = 0 AND w.countInTotal = 1
          AND t.date <= :maxDate
        ORDER BY t.date DESC
    """)
    fun getAllTransactions(maxDate: String): Flow<List<com.sinxn.mymoney.core.data.local.model.TransactionWithCategory>>

    @androidx.room.Transaction
    @Query("""
        SELECT t.*, c.name as categoryName, c.icon as categoryIcon,
               COALESCE(curr.decimals, 2) as decimals, curr.symbol as currencySymbol,
               w.currency as currencyCode
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        INNER JOIN wallets w ON t.walletId = w.id
        LEFT JOIN currencies curr ON w.currency = curr.iso
        WHERE t.id = :transactionId
    """)
    fun getTransactionWithCategory(transactionId: String): Flow<com.sinxn.mymoney.core.data.local.model.TransactionWithCategory?>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Query("""
        SELECT SUM(wallet_balance) FROM (
            SELECT 
                w.startMoney + COALESCE(SUM(
                    CASE 
                        WHEN t.direction = 1 THEN t.money 
                        WHEN t.direction = 0 THEN -t.money 
                        ELSE 0 
                    END
                ), 0) as wallet_balance
            FROM wallets w
            LEFT JOIN transactions t ON w.id = t.walletId 
                AND t.isDeleted = 0 
                AND t.confirmed = 1 
                AND t.countInTotal = 1
                AND t.date <= :maxDate
            WHERE w.isDeleted = 0 
              AND w.countInTotal = 1
              AND (:excludeArchived = 1 AND w.isArchived = 0 OR :excludeArchived = 0)
            GROUP BY w.id
        )
    """)
    fun getTotalBalance(maxDate: String, excludeArchived: Boolean): Flow<Long?>
    
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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
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
