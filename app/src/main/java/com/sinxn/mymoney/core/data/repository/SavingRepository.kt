package com.sinxn.mymoney.core.data.repository

import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.data.local.entity.SavingEntity
import com.sinxn.mymoney.core.data.local.entity.TransactionEntity
import com.sinxn.mymoney.core.data.local.model.SavingWithDetails
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import com.sinxn.mymoney.core.util.CategoryType
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
        val (name, icon) = when (tag) {
            TAG_SAVING_DEPOSIT -> Pair("Deposit", "ic_saving_deposit")
            TAG_SAVING_WITHDRAW -> Pair("Withdraw", "ic_saving_withdraw")
            else -> Pair("System", "ic_settings")
        }

        val newCategory = CategoryEntity(
            id = "system-category-$tag",
            name = name,
            icon = icon,
            type = CategoryType.SYSTEM,
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
        return moneyDao.getSavingsWithDetails(walletId = walletId, isComplete = isComplete)
    }

    /**
     * Get single saving goal details by ID.
     */
    fun getSavingDetails(savingId: String): Flow<SavingWithDetails?> {
        return moneyDao.getSavingWithDetailsById(savingId)
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
            if (startMoney > 0) {
                val systemCat = getOrCreateSystemCategory(TAG_SAVING_DEPOSIT)
                val depositTx = TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    money = startMoney,
                    date = DateUtils.getSQLDateTimeString(Date()),
                    description = "Initial deposit for ${description ?: "Saving Goal"}",
                    categoryId = systemCat.id,
                    walletId = walletId,
                    direction = 0, // EXPENSE (moves funds into savings)
                    type = 0,
                    note = note,
                    confirmed = true,
                    countInTotal = true,
                    isDeleted = false,
                    placeId = null,
                    eventId = null,
                    debtId = null,
                    savingId = savingId,
                    recurrenceId = null,
                    lastEdit = now,
                    tag = null
                )
                moneyDao.insertTransaction(depositTx)
            }
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
    suspend fun deleteSaving(savingId: String, deleteTransactions: Boolean = true) {
        val now = System.currentTimeMillis()
        if (deleteTransactions) {
            moneyDao.softDeleteTransactionsForSaving(savingId, now)
        } else {
            moneyDao.unlinkTransactionsForSaving(savingId, now)
        }
        moneyDao.softDeleteSaving(savingId, now)
    }
}
