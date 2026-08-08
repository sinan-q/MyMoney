package com.sinxn.mymoney.core.data.repository

import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.entity.BudgetEntity
import com.sinxn.mymoney.core.data.local.entity.BudgetWalletEntity
import com.sinxn.mymoney.core.data.local.entity.WalletEntity
import com.sinxn.mymoney.core.data.local.model.BudgetWithDetails
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import com.sinxn.mymoney.core.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor(
    private val moneyDao: MoneyDao
) {
    /**
     * Get list of budgets enriched with linked wallets and category.
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
        walletIds: List<String>
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

        val budgetEntity = BudgetEntity(
            id = budgetId,
            type = type,
            categoryId = if (type == 2) categoryId else null,
            startDate = startDate,
            endDate = endDate,
            money = money,
            currency = currency,
            tag = tag,
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
