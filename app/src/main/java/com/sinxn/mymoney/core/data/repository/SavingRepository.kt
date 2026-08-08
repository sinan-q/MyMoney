package com.sinxn.mymoney.core.data.repository

import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.data.local.entity.SavingEntity
import com.sinxn.mymoney.core.data.local.entity.TransactionEntity
import com.sinxn.mymoney.core.data.local.model.SavingWithDetails
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import com.sinxn.mymoney.core.util.DateUtils
import kotlinx.coroutines.flow.Flow
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavingRepository @Inject constructor(
    private val moneyDao: MoneyDao
) {
    companion object {
        const val TAG_SAVING_DEPOSIT = "system::deposit"
        const val TAG_SAVING_WITHDRAW = "system::withdraw"
    }

    /**
     * Get or create a system category for saving deposit / withdraw.
     */
    suspend fun getOrCreateSystemCategory(tag: String): CategoryEntity {
        val existing = moneyDao.getCategoryByTag(tag)
        if (existing != null) return existing

        val now = System.currentTimeMillis()
        val (name, icon, type) = when (tag) {
            TAG_SAVING_DEPOSIT -> Triple("Deposit", "ic_saving_deposit", 0) // Expense from wallet to saving
            TAG_SAVING_WITHDRAW -> Triple("Withdraw", "ic_saving_withdraw", 1) // Income to wallet from saving
            else -> Triple("System", "ic_settings", 2)
        }

        val newCategory = CategoryEntity(
            id = "system-category-$tag",
            name = name,
            icon = icon,
            type = type,
            parentId = null,
            showReport = false,
            index = 0,
            isDeleted = false,
            lastEdit = now,
            tag = tag
        )

        moneyDao.insertCategories(listOf(newCategory))
        return newCategory
    }

    /**
     * Get list of savings goals enriched with progress and wallet details.
     */
    fun getSavings(walletId: String? = null, isComplete: Boolean = false): Flow<List<SavingWithDetails>> {
        val maxDate = DateUtils.getSQLDateTimeString(Date())
        return moneyDao.getSavingsWithDetails(walletId = walletId, isComplete = isComplete, maxDate = maxDate)
    }

    /**
     * Get single saving goal details by ID.
     */
    fun getSavingDetails(savingId: String): Flow<SavingWithDetails?> {
        val maxDate = DateUtils.getSQLDateTimeString(Date())
        return moneyDao.getSavingWithDetailsById(savingId, maxDate)
    }

    /**
     * Get transactions linked to a saving goal.
     */
    fun getTransactionsForSaving(savingId: String): Flow<List<TransactionWithCategory>> {
        return moneyDao.getTransactionsForSaving(savingId)
    }

    /**
     * Create or Update a saving goal.
     */
    suspend fun saveSaving(
        id: String?,
        description: String?,
        icon: String,
        startMoney: Long,
        endMoney: Long,
        walletId: String,
        endDate: String?,
        note: String?,
        tag: String? = null
    ): String {
        val now = System.currentTimeMillis()
        val savingId = id ?: UUID.randomUUID().toString()

        val savingEntity = SavingEntity(
            id = savingId,
            description = description,
            icon = icon,
            startMoney = startMoney,
            endMoney = endMoney,
            walletId = walletId,
            endDate = endDate,
            isComplete = false,
            note = note,
            isDeleted = false,
            lastEdit = now,
            tag = tag
        )

        if (id == null) {
            moneyDao.insertSaving(savingEntity)
        } else {
            moneyDao.updateSaving(savingEntity)
        }

        return savingId
    }

    /**
     * Toggle completion / archive status of a saving goal.
     */
    suspend fun setSavingComplete(savingId: String, isComplete: Boolean) {
        val now = System.currentTimeMillis()
        moneyDao.updateSavingComplete(savingId, isComplete, now)
    }

    /**
     * Soft delete a saving goal.
     */
    suspend fun deleteSaving(savingId: String) {
        val now = System.currentTimeMillis()
        moneyDao.softDeleteSaving(savingId, now)
    }
}
