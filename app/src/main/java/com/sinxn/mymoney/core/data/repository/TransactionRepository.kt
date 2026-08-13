package com.sinxn.mymoney.core.data.repository

import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.entity.AttachmentEntity
import com.sinxn.mymoney.core.data.local.entity.PersonEntity
import com.sinxn.mymoney.core.data.local.entity.TransactionEntity
import com.sinxn.mymoney.core.data.local.entity.TransactionPeopleEntity
import com.sinxn.mymoney.core.data.local.entity.TransferEntity
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val moneyDao: MoneyDao,
    private val savingRepository: SavingRepository
) {

    fun getTransactionWithCategory(id: String): Flow<TransactionWithCategory?> {
        return moneyDao.getTransactionWithCategory(id)
    }

    fun getPeopleForTransaction(id: String): Flow<List<PersonEntity>> {
        return moneyDao.getPeopleForTransaction(id)
    }

    fun getAttachmentsForTransaction(id: String): Flow<List<AttachmentEntity>> {
        return moneyDao.getAttachmentsForTransaction(id)
    }

    suspend fun getTransactionById(id: String): TransactionEntity? {
        return moneyDao.getTransactionById(id)
    }

    suspend fun getTransferByTransactionId(id: String): TransferEntity? {
        return moneyDao.getTransferByTransactionId(id)
    }

    suspend fun findSiblingTransferTransaction(money: Long, date: String, excludeId: String): TransactionEntity? {
        return moneyDao.findSiblingTransferTransaction(money, date, excludeId)
    }

    suspend fun saveTransactionPeople(transactionId: String, peopleIds: Set<String>) {
        moneyDao.deletePeopleForTransaction(transactionId)
        val now = System.currentTimeMillis()
        val newPeople = peopleIds.map { personId ->
            TransactionPeopleEntity(
                id = UUID.randomUUID().toString(),
                transactionId = transactionId,
                personId = personId,
                isDeleted = false,
                lastEdit = now
            )
        }
        if (newPeople.isNotEmpty()) {
            moneyDao.insertTransactionPeople(newPeople)
        }
    }

    suspend fun softDeleteTransferAndSiblings(transfer: TransferEntity) {
        val now = System.currentTimeMillis()
        moneyDao.updateTransfer(transfer.copy(isDeleted = true, lastEdit = now))

        val siblingTxId = transfer.transactionToId
        val siblingTx = moneyDao.getTransactionById(siblingTxId)
        if (siblingTx != null) {
            moneyDao.updateTransaction(siblingTx.copy(isDeleted = true, lastEdit = now))
        }

        val taxTxId = transfer.transactionTaxId
        if (taxTxId != null) {
            val taxTx = moneyDao.getTransactionById(taxTxId)
            if (taxTx != null) {
                moneyDao.updateTransaction(taxTx.copy(isDeleted = true, lastEdit = now))
            }
        }
    }

    suspend fun deleteTransaction(transaction: TransactionEntity, transfer: TransferEntity?) {
        val now = System.currentTimeMillis()
        moneyDao.updateTransaction(transaction.copy(isDeleted = true, lastEdit = now))
        if (transfer != null) {
            softDeleteTransferAndSiblings(transfer)
        }
    }

    suspend fun saveSingleTransaction(
        transaction: TransactionEntity,
        isNewTransaction: Boolean,
        savingId: String?,
        isSavingCompleted: Boolean,
        peopleIds: Set<String>,
        previousTransfer: TransferEntity?
    ) {
        if (isNewTransaction) {
            moneyDao.insertTransaction(transaction)
            if (savingId != null && isSavingCompleted) {
                savingRepository.setSavingComplete(savingId, true)
            }
        } else {
            moneyDao.updateTransaction(transaction)
            if (previousTransfer != null) {
                softDeleteTransferAndSiblings(previousTransfer)
            }
        }
        saveTransactionPeople(transaction.id, peopleIds)
    }

    suspend fun saveTransferTransaction(
        fromTx: TransactionEntity,
        toTx: TransactionEntity,
        transfer: TransferEntity,
        taxTx: TransactionEntity?,
        existingTaxIdToRemove: String?,
        isNewTransfer: Boolean,
        peopleIds: Set<String>
    ) {
        if (taxTx != null) {
            moneyDao.insertTransaction(taxTx)
        } else if (existingTaxIdToRemove != null) {
            val oldTax = moneyDao.getTransactionById(existingTaxIdToRemove)
            if (oldTax != null) {
                moneyDao.updateTransaction(oldTax.copy(isDeleted = true, lastEdit = System.currentTimeMillis()))
            }
        }

        if (isNewTransfer) {
            moneyDao.insertTransaction(fromTx)
            moneyDao.insertTransaction(toTx)
            moneyDao.insertTransfer(transfer)
        } else {
            moneyDao.updateTransaction(fromTx)
            moneyDao.updateTransaction(toTx)
            moneyDao.updateTransfer(transfer)
        }

        saveTransactionPeople(fromTx.id, peopleIds)
    }
}
