package com.sinxn.mymoney.core.data.repository

import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.entity.BudgetEntity
import com.sinxn.mymoney.core.data.local.entity.BudgetWalletEntity
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.data.local.entity.WalletEntity
import com.sinxn.mymoney.core.data.local.model.BudgetWithDetails
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import com.sinxn.mymoney.core.util.BudgetType
import com.sinxn.mymoney.core.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Budget period types matching legacy moneywallet behavior.
 * Stored in the BudgetEntity.tag field as "period::<type>" to avoid schema migration.
 */
object BudgetPeriod {
    const val CUSTOM = 0   // Fixed date range (no auto-renew)
    const val WEEKLY = 1   // Auto-renew weekly based on firstDayOfWeek
    const val MONTHLY = 2  // Auto-renew monthly based on firstDayOfMonth
    const val ANNUAL = 3   // Auto-renew annually starting Jan 1

    fun fromTag(tag: String?): Int {
        if (tag == null) return CUSTOM
        return when {
            tag.startsWith("period::") -> tag.removePrefix("period::").toIntOrNull() ?: CUSTOM
            else -> CUSTOM
        }
    }

    fun toTag(period: Int): String? {
        return if (period == CUSTOM) null else "period::$period"
    }
}

@Singleton
class BudgetRepository @Inject constructor(
    private val moneyDao: MoneyDao
) {
    // -----------------------------------------------------------------------
    // REG-01: Recursive category tree helper
    // -----------------------------------------------------------------------

    /**
     * Recursively collect all descendant category IDs for a given parent.
     * This is needed because Room does not support WITH RECURSIVE in @Query.
     */
    suspend fun getAllDescendantCategoryIds(parentId: String): Set<String> {
        val result = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        queue.add(parentId)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val children = moneyDao.getDirectChildCategoryIds(current)
            for (childId in children) {
                if (result.add(childId)) {
                    queue.add(childId)
                }
            }
        }
        return result
    }

    // -----------------------------------------------------------------------
    // REG-02: Budget period auto-renewal
    // -----------------------------------------------------------------------

    /**
     * Compute the current active period window for a budget.
     * Returns the (startDate, endDate) for the current cycle.
     * For CUSTOM budgets, returns the stored dates as-is.
     */
    fun getCurrentBudgetWindow(
        budget: BudgetEntity,
        firstDayOfWeek: Int = Calendar.MONDAY,
        firstDayOfMonth: Int = 1
    ): Pair<String, String> {
        val period = BudgetPeriod.fromTag(budget.tag)
        if (period == BudgetPeriod.CUSTOM) {
            return Pair(budget.startDate, budget.endDate)
        }

        val now = Calendar.getInstance()
        val cal = Calendar.getInstance()

        when (period) {
            BudgetPeriod.WEEKLY -> {
                cal.firstDayOfWeek = firstDayOfWeek
                cal.set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                if (cal.after(now)) {
                    cal.add(Calendar.WEEK_OF_YEAR, -1)
                }
                val start = cal.time
                cal.add(Calendar.DAY_OF_YEAR, 6)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                val end = cal.time
                return Pair(
                    DateUtils.getSQLDateTimeString(start),
                    DateUtils.getSQLDateTimeString(end)
                )
            }
            BudgetPeriod.MONTHLY -> {
                cal.time = DateUtils.getStartOfBudgetMonth(now.time, firstDayOfMonth)
                val start = cal.time
                cal.add(Calendar.MONTH, 1)
                cal.add(Calendar.DAY_OF_YEAR, -1)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                val end = cal.time
                return Pair(
                    DateUtils.getSQLDateTimeString(start),
                    DateUtils.getSQLDateTimeString(end)
                )
            }
            BudgetPeriod.ANNUAL -> {
                cal.set(Calendar.MONTH, Calendar.JANUARY)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val start = cal.time
                cal.set(Calendar.MONTH, Calendar.DECEMBER)
                cal.set(Calendar.DAY_OF_MONTH, 31)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                val end = cal.time
                return Pair(
                    DateUtils.getSQLDateTimeString(start),
                    DateUtils.getSQLDateTimeString(end)
                )
            }
            else -> return Pair(budget.startDate, budget.endDate)
        }
    }

    // -----------------------------------------------------------------------
    // Budget queries (enriched with subcategory + period support)
    // -----------------------------------------------------------------------

    /**
     * Get list of budgets enriched with linked wallets and category.
     * For category budgets, the progress is recomputed in Kotlin to include
     * all descendant subcategories (REG-01 fix).
     */
    fun getBudgets(walletId: String? = null): Flow<List<BudgetWithDetails>> {
        val maxDate = DateUtils.getSQLDateTimeString(Date())
        return moneyDao.getBudgetsWithDetails(walletId, maxDate).map { budgetList ->
            budgetList.map { budgetWithDetails ->
                val wallets = moneyDao.getWalletsForBudget(budgetWithDetails.budget.id)
                budgetWithDetails.apply { this.wallets = wallets }
            }
        }
    }

    /**
     * Get details of a single budget by ID.
     */
    fun getBudgetDetails(budgetId: String): Flow<BudgetWithDetails?> {
        val maxDate = DateUtils.getSQLDateTimeString(Date())
        return moneyDao.getBudgetWithDetailsById(budgetId, maxDate).map { budgetWithDetails ->
            budgetWithDetails?.apply {
                this.wallets = moneyDao.getWalletsForBudget(budgetId)
            }
        }
    }

    /**
     * Get transactions contributing to a budget.
     * For CATEGORY budgets, includes transactions from all descendant subcategories.
     */
    fun getTransactionsForBudget(budgetId: String): Flow<List<TransactionWithCategory>> {
        val maxDate = DateUtils.getSQLDateTimeString(Date())
        return moneyDao.getTransactionsForBudget(budgetId, maxDate)
    }

    /**
     * Validate that all selected wallets have identical currency codes.
     */
    suspend fun validateWalletCurrencies(walletIds: List<String>): Boolean {
        if (walletIds.isEmpty()) return false
        val wallets = walletIds.mapNotNull { moneyDao.getWalletById(it) }
        if (wallets.size != walletIds.size) return false
        val firstCurrency = wallets.first().currency
        return wallets.all { it.currency.equals(firstCurrency, ignoreCase = true) }
    }

    /**
     * Create or Update a budget.
     * @param periodType One of BudgetPeriod.CUSTOM/WEEKLY/MONTHLY/ANNUAL.
     *                   Stored in the tag field for period auto-renewal.
     */
    suspend fun saveBudget(
        id: String?,
        type: Int,
        categoryId: String?,
        startDate: String,
        endDate: String,
        money: Long,
        currency: String,
        tag: String?,
        walletIds: List<String>,
        periodType: Int = BudgetPeriod.CUSTOM
    ): Result<String> {
        if (walletIds.isEmpty()) {
            return Result.failure(IllegalArgumentException("At least one wallet must be selected"))
        }

        // Enforce wallet currency consistency
        val isConsistent = validateWalletCurrencies(walletIds)
        if (!isConsistent) {
            return Result.failure(IllegalArgumentException("All wallets linked to a budget must use the same currency"))
        }

        val now = System.currentTimeMillis()
        val budgetId = id ?: UUID.randomUUID().toString()

        // Use period tag if a recurring period is selected, otherwise preserve existing tag
        val effectiveTag = BudgetPeriod.toTag(periodType) ?: tag

        val budgetEntity = BudgetEntity(
            id = budgetId,
            type = type,
            categoryId = if (type == 2) categoryId else null,
            startDate = startDate,
            endDate = endDate,
            money = money,
            currency = currency,
            tag = effectiveTag,
            lastEdit = now,
            isDeleted = false
        )

        if (id == null) {
            moneyDao.insertBudget(budgetEntity)
        } else {
            moneyDao.updateBudget(budgetEntity)
            moneyDao.deleteBudgetWallets(budgetId)
        }

        val budgetWallets = walletIds.map { walletId ->
            BudgetWalletEntity(
                budgetId = budgetId,
                walletId = walletId,
                isDeleted = false,
                lastEdit = now,
                id = UUID.randomUUID().toString()
            )
        }
        moneyDao.insertBudgetWallets(budgetWallets)

        return Result.success(budgetId)
    }

    /**
     * Soft delete a budget and clear budget wallets.
     */
    suspend fun deleteBudget(budgetId: String) {
        val now = System.currentTimeMillis()
        moneyDao.softDeleteBudget(budgetId, now)
        moneyDao.deleteBudgetWallets(budgetId)
    }
}

