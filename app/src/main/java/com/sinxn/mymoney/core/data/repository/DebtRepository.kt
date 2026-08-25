package com.sinxn.mymoney.core.data.repository

import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.data.local.entity.DebtEntity
import com.sinxn.mymoney.core.data.local.entity.DebtPeopleEntity
import com.sinxn.mymoney.core.data.local.entity.TransactionEntity
import com.sinxn.mymoney.core.data.local.entity.TransactionPeopleEntity
import com.sinxn.mymoney.core.data.local.model.DebtWithDetails
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import com.sinxn.mymoney.core.util.CategoryType
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
class DebtRepository @Inject constructor(
    private val moneyDao: MoneyDao
) {
    companion object {
        const val TAG_DEBT = "system::debt"
        const val TAG_CREDIT = "system::credit"
        const val TAG_PAID_DEBT = "system::paid_debt"
        const val TAG_PAID_CREDIT = "system::paid_credit"
    }

    /**
     * Get or create a system category by tag.
     */
    suspend fun getOrCreateSystemCategory(tag: String): CategoryEntity {
        val existing = moneyDao.getCategoryByTag(tag)
        if (existing != null) return existing

        val now = System.currentTimeMillis()
        val (name, icon) = when (tag) {
            TAG_DEBT -> Pair("Debt", "ic_debt")
            TAG_CREDIT -> Pair("Credit", "ic_credit")
            TAG_PAID_DEBT -> Pair("Paid debt", "ic_debt_paid")
            TAG_PAID_CREDIT -> Pair("Paid credit", "ic_credit_paid")
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
     * Observe list of debts enriched with people.
     */
    fun getDebts(
        type: Int? = null, 
        includeArchived: Boolean = false, 
        walletId: String? = null
    ): Flow<List<DebtWithDetails>> {
        return moneyDao.getDebtsWithDetails(
            type = type, 
            includeArchived = includeArchived, 
            walletId = walletId
        ).flatMapLatest { debtList ->
            if (debtList.isEmpty()) {
                flowOf(emptyList())
            } else {
                // Enrich each debt with its linked people
                moneyDao.getPeople().map { allPeople ->
                    debtList.map { debtWithDetails ->
                        val peopleList = moneyDao.getPeopleListForDebt(debtWithDetails.debt.id)
                        debtWithDetails.apply { people = peopleList }
                    }
                }
            }
        }
    }

    /**
     * Observe single debt by ID enriched with people.
     */
    fun getDebtDetails(debtId: String): Flow<DebtWithDetails?> {
        return moneyDao.getDebtWithDetailsById(debtId).flatMapLatest { debtWithDetails ->
            if (debtWithDetails == null) {
                flowOf(null)
            } else {
                moneyDao.getPeopleForDebt(debtId).map { peopleList ->
                    debtWithDetails.apply { people = peopleList }
                }
            }
        }
    }

    /**
     * Get transactions linked to debt.
     */
    fun getTransactionsForDebt(debtId: String): Flow<List<TransactionWithCategory>> {
        return moneyDao.getTransactionsForDebt(debtId)
    }

    /**
     * Create a new debt item and optionally insert master transaction.
     */
    suspend fun createDebt(
        type: Int, // 0: DEBT, 1: CREDIT
        icon: String,
        description: String,
        date: String,
        expirationDate: String?,
        walletId: String,
        placeId: String?,
        money: Long,
        note: String?,
        peopleIds: Set<String>,
        insertMasterTransaction: Boolean = true
    ): String {
        val debtId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val debt = DebtEntity(
            id = debtId,
            type = type,
            icon = icon,
            description = description,
            date = date,
            expirationDate = expirationDate,
            walletId = walletId,
            placeId = placeId,
            money = money,
            isArchived = false,
            note = note,
            isDeleted = false,
            lastEdit = now,
            tag = null
        )

        moneyDao.insertDebt(debt)

        if (peopleIds.isNotEmpty()) {
            val peopleLinks = peopleIds.map { personId ->
                DebtPeopleEntity(
                    debtId = debtId,
                    personId = personId,
                    isDeleted = false,
                    lastEdit = now,
                    id = UUID.randomUUID().toString()
                )
            }
            moneyDao.insertDebtPeople(peopleLinks)
            moneyDao.updatePeopleLastUsed(peopleIds.toList(), now)
        }

        if (insertMasterTransaction) {
            val systemCatTag = if (type == 1) TAG_CREDIT else TAG_DEBT
            val systemCat = getOrCreateSystemCategory(systemCatTag)
            val direction = if (type == 1) 0 else 1 // Expense for CREDIT, Income for DEBT

            val masterTx = TransactionEntity(
                id = UUID.randomUUID().toString(),
                money = money,
                date = date,
                description = description,
                categoryId = systemCat.id,
                walletId = walletId,
                direction = direction,
                type = 2, // TransactionType.DEBT
                note = note,
                confirmed = true,
                countInTotal = true,
                isDeleted = false,
                placeId = placeId,
                eventId = null,
                debtId = debtId,
                savingId = null,
                recurrenceId = null,
                lastEdit = now,
                tag = null
            )

            moneyDao.insertTransaction(masterTx)

            if (peopleIds.isNotEmpty()) {
                val txPeople = peopleIds.map { personId ->
                    TransactionPeopleEntity(
                        transactionId = masterTx.id,
                        personId = personId,
                        isDeleted = false,
                        lastEdit = now,
                        id = UUID.randomUUID().toString()
                    )
                }
                moneyDao.insertTransactionPeople(txPeople)
            }
        }

        return debtId
    }

    /**
     * Update an existing debt item and its associated master transaction if present.
     */
    suspend fun updateDebt(
        debtId: String,
        type: Int,
        icon: String,
        description: String,
        date: String,
        expirationDate: String?,
        walletId: String,
        placeId: String?,
        money: Long,
        note: String?,
        peopleIds: Set<String>
    ) {
        val existing = moneyDao.getDebtById(debtId) ?: return
        val now = System.currentTimeMillis()

        val updatedDebt = existing.copy(
            type = type,
            icon = icon,
            description = description,
            date = date,
            expirationDate = expirationDate,
            walletId = walletId,
            placeId = placeId,
            money = money,
            note = note,
            lastEdit = now
        )

        moneyDao.updateDebt(updatedDebt)

        // Sync debt people links
        moneyDao.deletePeopleForDebt(debtId)
        if (peopleIds.isNotEmpty()) {
            val peopleLinks = peopleIds.map { personId ->
                DebtPeopleEntity(
                    debtId = debtId,
                    personId = personId,
                    isDeleted = false,
                    lastEdit = now,
                    id = UUID.randomUUID().toString()
                )
            }
            moneyDao.insertDebtPeople(peopleLinks)
            moneyDao.updatePeopleLastUsed(peopleIds.toList(), now)
        }

        // Sync master transaction if present
        val masterTx = moneyDao.getMasterTransactionForDebt(debtId)
        if (masterTx != null) {
            val systemCatTag = if (type == 1) TAG_CREDIT else TAG_DEBT
            val systemCat = getOrCreateSystemCategory(systemCatTag)
            val direction = if (type == 1) 0 else 1

            val updatedTx = masterTx.copy(
                money = money,
                date = date,
                description = description,
                categoryId = systemCat.id,
                walletId = walletId,
                direction = direction,
                placeId = placeId,
                note = note,
                lastEdit = now
            )

            moneyDao.updateTransaction(updatedTx)

            moneyDao.deletePeopleForTransaction(masterTx.id)
            if (peopleIds.isNotEmpty()) {
                val txPeople = peopleIds.map { personId ->
                    TransactionPeopleEntity(
                        transactionId = masterTx.id,
                        personId = personId,
                        isDeleted = false,
                        lastEdit = now,
                        id = UUID.randomUUID().toString()
                    )
                }
                moneyDao.insertTransactionPeople(txPeople)
            }
        }
    }

    /**
     * Soft delete debt and all related transactions & people links.
     */
    suspend fun deleteDebt(debtId: String, deleteTransactions: Boolean = true) {
        val now = System.currentTimeMillis()
        if (deleteTransactions) {
            moneyDao.softDeleteTransactionsForDebt(debtId, now)
        } else {
            moneyDao.unlinkTransactionsForDebt(debtId, now)
        }
        moneyDao.softDeletePeopleForDebt(debtId, now)
        moneyDao.softDeleteDebt(debtId, now)
    }

    /**
     * Toggle archived state of a debt.
     */
    suspend fun setDebtArchived(debtId: String, isArchived: Boolean) {
        val now = System.currentTimeMillis()
        moneyDao.updateDebtArchived(debtId, isArchived, now)
    }

    /**
     * Add a payment transaction towards a debt.
     */
    suspend fun addDebtPayment(
        debtId: String,
        amount: Long,
        walletId: String,
        date: String = DateUtils.getSQLDateTimeString(Date()),
        description: String? = null,
        note: String? = null,
        placeId: String? = null
    ): String {
        val debt = moneyDao.getDebtById(debtId) ?: throw IllegalArgumentException("Debt not found")
        val isDebtType = debt.type == 0
        val tag = if (isDebtType) TAG_PAID_DEBT else TAG_PAID_CREDIT
        val systemCat = getOrCreateSystemCategory(tag)
        val direction = if (isDebtType) 0 else 1 // Expense to pay debt, Income to collect credit
        val now = System.currentTimeMillis()

        val paymentTx = TransactionEntity(
            id = UUID.randomUUID().toString(),
            money = amount,
            date = date,
            description = description ?: if (isDebtType) "Paid Debt" else "Collected Credit",
            categoryId = systemCat.id,
            walletId = walletId,
            direction = direction,
            type = 2, // TransactionType.DEBT
            note = note,
            confirmed = true,
            countInTotal = true,
            isDeleted = false,
            placeId = placeId,
            eventId = null,
            debtId = debtId,
            savingId = null,
            recurrenceId = null,
            lastEdit = now,
            tag = null
        )

        moneyDao.insertTransaction(paymentTx)
        return paymentTx.id
    }
}
