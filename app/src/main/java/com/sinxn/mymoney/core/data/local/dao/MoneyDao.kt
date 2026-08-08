package com.sinxn.mymoney.core.data.local.dao

import androidx.room.*
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.data.local.entity.DebtEntity
import com.sinxn.mymoney.core.data.local.entity.DebtPeopleEntity
import com.sinxn.mymoney.core.data.local.entity.TransactionEntity
import com.sinxn.mymoney.core.data.local.entity.WalletEntity
import com.sinxn.mymoney.core.data.local.model.BudgetWithDetails
import com.sinxn.mymoney.core.data.local.model.DebtWithDetails
import com.sinxn.mymoney.core.data.local.model.SavingWithDetails
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
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

    @Query("SELECT * FROM wallets WHERE isDeleted = 0 ORDER BY `index` ASC")
    fun getWallets(): Flow<List<WalletEntity>>

    @Query("SELECT * FROM wallets WHERE isDeleted = 0 ORDER BY `index` ASC")
    suspend fun getWalletsList(): List<WalletEntity>

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

    @Query("SELECT * FROM categories WHERE isDeleted = 0 ORDER BY `index` ASC")
    fun getCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE isDeleted = 0 ORDER BY `index` ASC")
    suspend fun getCategoriesList(): List<CategoryEntity>

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

    @Query("SELECT * FROM places WHERE isDeleted = 0")
    suspend fun getPlacesList(): List<com.sinxn.mymoney.core.data.local.entity.PlaceEntity>

    @Query("SELECT * FROM places WHERE id = :id")
    suspend fun getPlaceById(id: String): com.sinxn.mymoney.core.data.local.entity.PlaceEntity?

    // Events
    @Query("SELECT * FROM events WHERE isDeleted = 0")
    fun getEvents(): Flow<List<com.sinxn.mymoney.core.data.local.entity.EventEntity>>

    @Query("SELECT * FROM events WHERE isDeleted = 0")
    suspend fun getEventsList(): List<com.sinxn.mymoney.core.data.local.entity.EventEntity>

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

    // Debts
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: DebtEntity)

    @Update
    suspend fun updateDebt(debt: DebtEntity)

    @Query("UPDATE debts SET isArchived = :isArchived, lastEdit = :lastEdit WHERE id = :debtId")
    suspend fun updateDebtArchived(debtId: String, isArchived: Boolean, lastEdit: Long)

    @Query("UPDATE debts SET isDeleted = 1, lastEdit = :lastEdit WHERE id = :debtId")
    suspend fun softDeleteDebt(debtId: String, lastEdit: Long)

    @Query("DELETE FROM debts WHERE id = :debtId")
    suspend fun hardDeleteDebt(debtId: String)

    @Query("SELECT * FROM debts WHERE id = :debtId AND isDeleted = 0")
    suspend fun getDebtById(debtId: String): DebtEntity?

    @Query("DELETE FROM debt_people WHERE debtId = :debtId")
    suspend fun deletePeopleForDebt(debtId: String)

    @Query("""
        SELECT p.* 
        FROM people p
        INNER JOIN debt_people dp ON p.id = dp.personId
        WHERE dp.debtId = :debtId AND dp.isDeleted = 0 AND p.isDeleted = 0
    """)
    fun getPeopleForDebt(debtId: String): Flow<List<com.sinxn.mymoney.core.data.local.entity.PersonEntity>>

    @Query("""
        SELECT p.* 
        FROM people p
        INNER JOIN debt_people dp ON p.id = dp.personId
        WHERE dp.debtId = :debtId AND dp.isDeleted = 0 AND p.isDeleted = 0
    """)
    suspend fun getPeopleListForDebt(debtId: String): List<com.sinxn.mymoney.core.data.local.entity.PersonEntity>

    @androidx.room.Transaction
    @Query("""
        SELECT d.*, 
               w.name as walletName, w.icon as walletIcon, w.currency as walletCurrency,
               COALESCE(curr.decimals, 2) as walletDecimals, w.isArchived as walletArchived,
               p.name as placeName, p.icon as placeIcon,
               COALESCE((
                   SELECT SUM(((t.direction * 2) - 1) * t.money)
                   FROM transactions t
                   LEFT JOIN categories c ON t.categoryId = c.id
                   WHERE t.debtId = d.id 
                     AND t.isDeleted = 0 
                     AND t.confirmed = 1 
                     AND (c.tag = 'system::paid_debt' OR c.tag = 'system::paid_credit')
                     AND t.date <= :maxDate
               ), 0) as progress
        FROM debts d
        INNER JOIN wallets w ON d.walletId = w.id
        LEFT JOIN currencies curr ON w.currency = curr.iso
        LEFT JOIN places p ON d.placeId = p.id
        WHERE d.isDeleted = 0 
          AND (:type IS NULL OR d.type = :type)
          AND (:includeArchived = 1 OR d.isArchived = 0)
          AND (
            (:walletId IS NULL OR :walletId = 'total' OR :walletId = '') AND w.countInTotal = 1
            OR (:walletId IS NOT NULL AND :walletId != 'total' AND :walletId != '' AND d.walletId = :walletId)
          )
        ORDER BY d.isArchived ASC, d.date DESC
    """)
    fun getDebtsWithDetails(
        type: Int?, 
        includeArchived: Boolean, 
        walletId: String? = null,
        maxDate: String
    ): Flow<List<DebtWithDetails>>

    @androidx.room.Transaction
    @Query("""
        SELECT d.*, 
               w.name as walletName, w.icon as walletIcon, w.currency as walletCurrency,
               COALESCE(curr.decimals, 2) as walletDecimals, w.isArchived as walletArchived,
               p.name as placeName, p.icon as placeIcon,
               COALESCE((
                   SELECT SUM(((t.direction * 2) - 1) * t.money)
                   FROM transactions t
                   LEFT JOIN categories c ON t.categoryId = c.id
                   WHERE t.debtId = d.id 
                     AND t.isDeleted = 0 
                     AND t.confirmed = 1 
                     AND (c.tag = 'system::paid_debt' OR c.tag = 'system::paid_credit')
                     AND t.date <= :maxDate
               ), 0) as progress
        FROM debts d
        INNER JOIN wallets w ON d.walletId = w.id
        LEFT JOIN currencies curr ON w.currency = curr.iso
        LEFT JOIN places p ON d.placeId = p.id
        WHERE d.id = :debtId AND d.isDeleted = 0
    """)
    fun getDebtWithDetailsById(debtId: String, maxDate: String): Flow<DebtWithDetails?>

    @androidx.room.Transaction
    @Query("""
        SELECT t.*, c.name as categoryName, c.icon as categoryIcon,
               COALESCE(curr.decimals, 2) as decimals, curr.symbol as currencySymbol,
               w.currency as currencyCode
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        INNER JOIN wallets w ON t.walletId = w.id
        LEFT JOIN currencies curr ON w.currency = curr.iso
        WHERE t.debtId = :debtId AND t.isDeleted = 0
        ORDER BY t.date DESC
    """)
    fun getTransactionsForDebt(debtId: String): Flow<List<TransactionWithCategory>>

    @Query("SELECT * FROM categories WHERE tag = :tag AND isDeleted = 0 LIMIT 1")
    suspend fun getCategoryByTag(tag: String): CategoryEntity?

    @Query("""
        SELECT t.* FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        WHERE t.debtId = :debtId AND t.type = 2 AND t.isDeleted = 0
          AND (c.tag = 'system::debt' OR c.tag = 'system::credit')
        LIMIT 1
    """)
    suspend fun getMasterTransactionForDebt(debtId: String): TransactionEntity?

    @Query("UPDATE transactions SET isDeleted = 1, lastEdit = :lastEdit WHERE debtId = :debtId")
    suspend fun softDeleteTransactionsForDebt(debtId: String, lastEdit: Long)

    @Query("UPDATE debt_people SET isDeleted = 1, lastEdit = :lastEdit WHERE debtId = :debtId")
    suspend fun softDeletePeopleForDebt(debtId: String, lastEdit: Long)

    // --- Budget Queries & Operations ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: com.sinxn.mymoney.core.data.local.entity.BudgetEntity)

    @Update
    suspend fun updateBudget(budget: com.sinxn.mymoney.core.data.local.entity.BudgetEntity)

    @Query("UPDATE budgets SET isDeleted = 1, lastEdit = :lastEdit WHERE id = :budgetId")
    suspend fun softDeleteBudget(budgetId: String, lastEdit: Long)

    @Query("DELETE FROM budget_wallets WHERE budgetId = :budgetId")
    suspend fun deleteBudgetWallets(budgetId: String)

    @Query("SELECT walletId FROM budget_wallets WHERE budgetId = :budgetId AND isDeleted = 0")
    suspend fun getWalletIdsForBudget(budgetId: String): List<String>

    @Query("SELECT * FROM wallets WHERE id IN (SELECT walletId FROM budget_wallets WHERE budgetId = :budgetId AND isDeleted = 0) AND isDeleted = 0")
    suspend fun getWalletsForBudget(budgetId: String): List<WalletEntity>

    @androidx.room.Transaction
    @Query("""
        SELECT b.*,
               c.name as categoryName, c.icon as categoryIcon, c.type as categoryType,
               c.showReport as categoryShowReport, c.tag as categoryTag,
               GROUP_CONCAT('<' || bw.walletId || '>') as walletIds,
               MAX(w.countInTotal) as hasWalletInTotal,
               COALESCE((
                   CASE b.type
                       WHEN 0 THEN (
                           SELECT SUM(t.money)
                           FROM transactions t
                           INNER JOIN budget_wallets bw2 ON t.walletId = bw2.walletId
                           WHERE bw2.budgetId = b.id AND bw2.isDeleted = 0 AND t.isDeleted = 0
                             AND t.direction = 0 AND t.date <= :maxDate
                             AND DATE(t.date) >= DATE(b.startDate) AND DATE(t.date) <= DATE(b.endDate)
                             AND t.id NOT IN (
                                 SELECT tf.transactionFromId
                                 FROM transfers tf
                                 INNER JOIN transactions t2 ON tf.transactionToId = t2.id
                                 INNER JOIN budget_wallets bw3 ON t2.walletId = bw3.walletId
                                 WHERE bw3.budgetId = b.id AND bw3.walletId != t.walletId
                             )
                       )
                       WHEN 1 THEN (
                           SELECT SUM(t.money)
                           FROM transactions t
                           INNER JOIN budget_wallets bw2 ON t.walletId = bw2.walletId
                           WHERE bw2.budgetId = b.id AND bw2.isDeleted = 0 AND t.isDeleted = 0
                             AND t.direction = 1 AND t.date <= :maxDate
                             AND DATE(t.date) >= DATE(b.startDate) AND DATE(t.date) <= DATE(b.endDate)
                             AND t.id NOT IN (
                                 SELECT tf.transactionToId
                                 FROM transfers tf
                                 INNER JOIN transactions t2 ON tf.transactionFromId = t2.id
                                 INNER JOIN budget_wallets bw3 ON t2.walletId = bw3.walletId
                                 WHERE bw3.budgetId = b.id AND bw3.walletId != t.walletId
                             )
                       )
                       WHEN 2 THEN (
                           SELECT SUM(((t.direction * 2) - 1) * t.money)
                           FROM transactions t
                           INNER JOIN budget_wallets bw2 ON t.walletId = bw2.walletId
                           LEFT JOIN categories tc ON t.categoryId = tc.id
                           WHERE bw2.budgetId = b.id AND bw2.isDeleted = 0 AND t.isDeleted = 0 AND tc.isDeleted = 0
                             AND t.date <= :maxDate
                             AND DATE(t.date) >= DATE(b.startDate) AND DATE(t.date) <= DATE(b.endDate)
                             AND (b.categoryId = t.categoryId OR b.categoryId = tc.parentId)
                       )
                       ELSE 0
                   END
               ), 0) as progress
        FROM budgets b
        INNER JOIN budget_wallets bw ON b.id = bw.budgetId AND bw.isDeleted = 0
        INNER JOIN wallets w ON bw.walletId = w.id AND w.isDeleted = 0
        LEFT JOIN categories c ON b.categoryId = c.id AND c.isDeleted = 0
        WHERE b.isDeleted = 0
          AND (
            (:walletId IS NULL OR :walletId = 'total' OR :walletId = '') AND w.countInTotal = 1
            OR (:walletId IS NOT NULL AND :walletId != 'total' AND :walletId != '' AND bw.walletId = :walletId)
          )
        GROUP BY b.id
        ORDER BY b.startDate DESC
    """)
    fun getBudgetsWithDetails(walletId: String? = null, maxDate: String): Flow<List<BudgetWithDetails>>

    @androidx.room.Transaction
    @Query("""
        SELECT b.*,
               c.name as categoryName, c.icon as categoryIcon, c.type as categoryType,
               c.showReport as categoryShowReport, c.tag as categoryTag,
               GROUP_CONCAT('<' || bw.walletId || '>') as walletIds,
               MAX(w.countInTotal) as hasWalletInTotal,
               COALESCE((
                   CASE b.type
                       WHEN 0 THEN (
                           SELECT SUM(t.money)
                           FROM transactions t
                           INNER JOIN budget_wallets bw2 ON t.walletId = bw2.walletId
                           WHERE bw2.budgetId = b.id AND bw2.isDeleted = 0 AND t.isDeleted = 0
                             AND t.direction = 0 AND t.date <= :maxDate
                             AND DATE(t.date) >= DATE(b.startDate) AND DATE(t.date) <= DATE(b.endDate)
                       )
                       WHEN 1 THEN (
                           SELECT SUM(t.money)
                           FROM transactions t
                           INNER JOIN budget_wallets bw2 ON t.walletId = bw2.walletId
                           WHERE bw2.budgetId = b.id AND bw2.isDeleted = 0 AND t.isDeleted = 0
                             AND t.direction = 1 AND t.date <= :maxDate
                             AND DATE(t.date) >= DATE(b.startDate) AND DATE(t.date) <= DATE(b.endDate)
                       )
                       WHEN 2 THEN (
                           SELECT SUM(((t.direction * 2) - 1) * t.money)
                           FROM transactions t
                           INNER JOIN budget_wallets bw2 ON t.walletId = bw2.walletId
                           LEFT JOIN categories tc ON t.categoryId = tc.id
                           WHERE bw2.budgetId = b.id AND bw2.isDeleted = 0 AND t.isDeleted = 0 AND tc.isDeleted = 0
                             AND t.date <= :maxDate
                             AND DATE(t.date) >= DATE(b.startDate) AND DATE(t.date) <= DATE(b.endDate)
                             AND (b.categoryId = t.categoryId OR b.categoryId = tc.parentId)
                       )
                       ELSE 0
                   END
               ), 0) as progress
        FROM budgets b
        INNER JOIN budget_wallets bw ON b.id = bw.budgetId AND bw.isDeleted = 0
        INNER JOIN wallets w ON bw.walletId = w.id AND w.isDeleted = 0
        LEFT JOIN categories c ON b.categoryId = c.id AND c.isDeleted = 0
        WHERE b.id = :budgetId AND b.isDeleted = 0
        GROUP BY b.id
    """)
    fun getBudgetWithDetailsById(budgetId: String, maxDate: String): Flow<BudgetWithDetails?>

    // --- Savings Queries & Operations ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaving(saving: com.sinxn.mymoney.core.data.local.entity.SavingEntity)

    @Update
    suspend fun updateSaving(saving: com.sinxn.mymoney.core.data.local.entity.SavingEntity)

    @Query("UPDATE savings SET isDeleted = 1, lastEdit = :lastEdit WHERE id = :savingId")
    suspend fun softDeleteSaving(savingId: String, lastEdit: Long)

    @Query("UPDATE transactions SET isDeleted = 1, lastEdit = :lastEdit WHERE savingId = :savingId")
    suspend fun softDeleteTransactionsForSaving(savingId: String, lastEdit: Long)

    @Query("UPDATE savings SET isComplete = :isComplete, lastEdit = :lastEdit WHERE id = :savingId")
    suspend fun updateSavingComplete(savingId: String, isComplete: Boolean, lastEdit: Long)

    @androidx.room.Transaction
    @Query("""
        SELECT s.*,
               w.name as walletName, w.icon as walletIcon, w.currency as walletCurrency,
               w.countInTotal as walletCountInTotal, w.isArchived as walletArchived, w.tag as walletTag,
               COALESCE((
                   SELECT SUM(((t.direction * -2) + 1) * t.money)
                   FROM transactions t
                   LEFT JOIN categories c ON t.categoryId = c.id
                   WHERE t.savingId = s.id AND t.isDeleted = 0 AND t.confirmed = 1
                     AND (c.tag = 'system::deposit' OR c.tag = 'system::withdraw')
                     AND t.date <= :maxDate
               ), 0) as progress
        FROM savings s
        INNER JOIN wallets w ON s.walletId = w.id AND w.isDeleted = 0
        WHERE s.isDeleted = 0
          AND s.isComplete = :isComplete
          AND (
            (:walletId IS NULL OR :walletId = 'total' OR :walletId = '') AND w.countInTotal = 1
            OR (:walletId IS NOT NULL AND :walletId != 'total' AND :walletId != '' AND s.walletId = :walletId)
          )
        ORDER BY s.id DESC
    """)
    fun getSavingsWithDetails(walletId: String? = null, isComplete: Boolean = false, maxDate: String): Flow<List<SavingWithDetails>>

    @androidx.room.Transaction
    @Query("""
        SELECT s.*,
               w.name as walletName, w.icon as walletIcon, w.currency as walletCurrency,
               w.countInTotal as walletCountInTotal, w.isArchived as walletArchived, w.tag as walletTag,
               COALESCE((
                   SELECT SUM(((t.direction * -2) + 1) * t.money)
                   FROM transactions t
                   LEFT JOIN categories c ON t.categoryId = c.id
                   WHERE t.savingId = s.id AND t.isDeleted = 0 AND t.confirmed = 1
                     AND (c.tag = 'system::deposit' OR c.tag = 'system::withdraw')
                     AND t.date <= :maxDate
               ), 0) as progress
        FROM savings s
        INNER JOIN wallets w ON s.walletId = w.id AND w.isDeleted = 0
        WHERE s.id = :savingId AND s.isDeleted = 0
    """)
    fun getSavingWithDetailsById(savingId: String, maxDate: String): Flow<SavingWithDetails?>

    @androidx.room.Transaction
    @Query("""
        SELECT t.*, c.name as categoryName, c.icon as categoryIcon,
               COALESCE(curr.decimals, 2) as decimals, curr.symbol as currencySymbol,
               w.currency as currencyCode
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        INNER JOIN wallets w ON t.walletId = w.id
        LEFT JOIN currencies curr ON w.currency = curr.iso
        WHERE t.savingId = :savingId AND t.isDeleted = 0
        ORDER BY t.date DESC
    """)
    fun getTransactionsForSaving(savingId: String): Flow<List<TransactionWithCategory>>

    // Recurrent Transactions & Transfers
    @androidx.room.Transaction
    @Query("SELECT * FROM recurrent_transactions WHERE isDeleted = 0 ORDER BY startDate DESC")
    fun getRecurrentTransactionsWithDetails(): Flow<List<com.sinxn.mymoney.core.data.local.model.RecurrentTransactionWithDetails>>

    @androidx.room.Transaction
    @Query("SELECT * FROM recurrent_transfers WHERE isDeleted = 0 ORDER BY startDate DESC")
    fun getRecurrentTransfersWithDetails(): Flow<List<com.sinxn.mymoney.core.data.local.model.RecurrentTransferWithDetails>>

    @Query("SELECT * FROM recurrent_transactions WHERE id = :id AND isDeleted = 0")
    suspend fun getRecurrentTransactionById(id: String): com.sinxn.mymoney.core.data.local.entity.RecurrentTransactionEntity?

    @Query("SELECT * FROM recurrent_transfers WHERE id = :id AND isDeleted = 0")
    suspend fun getRecurrentTransferById(id: String): com.sinxn.mymoney.core.data.local.entity.RecurrentTransferEntity?

    @androidx.room.Transaction
    @Query("SELECT * FROM recurrent_transactions WHERE id = :id AND isDeleted = 0")
    fun getRecurrentTransactionWithDetailsById(id: String): Flow<com.sinxn.mymoney.core.data.local.model.RecurrentTransactionWithDetails?>

    @androidx.room.Transaction
    @Query("SELECT * FROM recurrent_transfers WHERE id = :id AND isDeleted = 0")
    fun getRecurrentTransferWithDetailsById(id: String): Flow<com.sinxn.mymoney.core.data.local.model.RecurrentTransferWithDetails?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurrentTransaction(entity: com.sinxn.mymoney.core.data.local.entity.RecurrentTransactionEntity)

    @Update
    suspend fun updateRecurrentTransaction(entity: com.sinxn.mymoney.core.data.local.entity.RecurrentTransactionEntity)

    @Query("UPDATE recurrent_transactions SET isDeleted = 1, lastEdit = :lastEdit WHERE id = :id")
    suspend fun deleteRecurrentTransaction(id: String, lastEdit: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurrentTransfer(entity: com.sinxn.mymoney.core.data.local.entity.RecurrentTransferEntity)

    @Update
    suspend fun updateRecurrentTransfer(entity: com.sinxn.mymoney.core.data.local.entity.RecurrentTransferEntity)

    @Query("UPDATE recurrent_transfers SET isDeleted = 1, lastEdit = :lastEdit WHERE id = :id")
    suspend fun deleteRecurrentTransfer(id: String, lastEdit: Long = System.currentTimeMillis())

    @Query("UPDATE transactions SET recurrenceId = NULL, lastEdit = :lastEdit WHERE recurrenceId = :recurrenceId")
    suspend fun unlinkTransactionsForRecurrence(recurrenceId: String, lastEdit: Long = System.currentTimeMillis())

    @Query("UPDATE transfers SET recurrenceId = NULL, lastEdit = :lastEdit WHERE recurrenceId = :recurrenceId")
    suspend fun unlinkTransfersForRecurrence(recurrenceId: String, lastEdit: Long = System.currentTimeMillis())

    @Query("SELECT * FROM recurrent_transactions WHERE isDeleted = 0 AND nextOccurrence IS NOT NULL AND DATE(nextOccurrence) <= DATE(:today)")
    suspend fun getPendingRecurrentTransactions(today: String): List<com.sinxn.mymoney.core.data.local.entity.RecurrentTransactionEntity>

    @Query("SELECT * FROM recurrent_transfers WHERE isDeleted = 0 AND nextOccurrence IS NOT NULL AND DATE(nextOccurrence) <= DATE(:today)")
    suspend fun getPendingRecurrentTransfers(today: String): List<com.sinxn.mymoney.core.data.local.entity.RecurrentTransferEntity>
}


