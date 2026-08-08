package com.sinxn.mymoney.core.data.repository

import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.entity.TransactionEntity
import com.sinxn.mymoney.core.data.local.entity.TransactionModelEntity
import com.sinxn.mymoney.core.data.local.entity.TransferEntity
import com.sinxn.mymoney.core.data.local.entity.TransferModelEntity
import com.sinxn.mymoney.core.data.local.model.TransactionModelWithDetails
import com.sinxn.mymoney.core.data.local.model.TransferModelWithDetails
import com.sinxn.mymoney.core.util.DateUtils
import kotlinx.coroutines.flow.Flow
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemplateRepository @Inject constructor(
    private val moneyDao: MoneyDao
) {
    fun getTransactionModels(): Flow<List<TransactionModelWithDetails>> {
        return moneyDao.getTransactionModelsWithDetails()
    }

    fun getTransferModels(): Flow<List<TransferModelWithDetails>> {
        return moneyDao.getTransferModelsWithDetails()
    }

    suspend fun saveTransactionModel(model: TransactionModelEntity) {
        if (moneyDao.getTransactionModelById(model.id) == null) {
            moneyDao.insertTransactionModel(model)
        } else {
            moneyDao.updateTransactionModel(model)
        }
    }

    suspend fun saveTransferModel(model: TransferModelEntity) {
        if (moneyDao.getTransferModelById(model.id) == null) {
            moneyDao.insertTransferModel(model)
        } else {
            moneyDao.updateTransferModel(model)
        }
    }

    suspend fun deleteTransactionModel(id: String) {
        val now = System.currentTimeMillis()
        moneyDao.softDeleteTransactionModel(id, now)
    }

    suspend fun deleteTransferModel(id: String) {
        val now = System.currentTimeMillis()
        moneyDao.softDeleteTransferModel(id, now)
    }

    suspend fun applyTransactionModel(modelId: String, date: String = DateUtils.getSQLDateTimeString(Date())): String {
        val template = moneyDao.getTransactionModelById(modelId) ?: throw IllegalArgumentException("Transaction template not found")
        val now = System.currentTimeMillis()
        val txId = UUID.randomUUID().toString()

        val transaction = TransactionEntity(
            id = txId,
            money = template.money,
            date = date,
            description = template.description,
            categoryId = template.categoryId,
            direction = template.direction,
            type = 0, // STANDARD
            walletId = template.walletId,
            placeId = template.placeId,
            note = template.note,
            savingId = null,
            debtId = null,
            eventId = template.eventId,
            recurrenceId = null,
            confirmed = template.confirmed,
            countInTotal = template.countInTotal,
            tag = template.tag,
            isDeleted = false,
            lastEdit = now
        )

        moneyDao.insertTransaction(transaction)
        return txId
    }

    suspend fun applyTransferModel(modelId: String, date: String = DateUtils.getSQLDateTimeString(Date())): String {
        val template = moneyDao.getTransferModelById(modelId) ?: throw IllegalArgumentException("Transfer template not found")
        val now = System.currentTimeMillis()

        val categories = moneyDao.getCategoriesList()
        val transferCategory = categories.find {
            it.tag == "system::transfer" || it.tag == "transfer" || it.name.equals("Transfer", ignoreCase = true)
        }?.id ?: (categories.firstOrNull()?.id ?: "")

        val transferTaxCategory = categories.find {
            it.tag == "system::transfer_tax" || it.tag == "transfer_tax" || it.name.contains("Tax", ignoreCase = true)
        }?.id ?: transferCategory

        val fromTxId = UUID.randomUUID().toString()
        val toTxId = UUID.randomUUID().toString()
        val taxTxId = template.moneyTax?.takeIf { it > 0 }?.let { UUID.randomUUID().toString() }

        val fromTx = TransactionEntity(
            id = fromTxId,
            money = template.moneyFrom,
            date = date,
            description = template.description,
            categoryId = transferCategory,
            direction = 0, // EXPENSE
            type = 1, // TRANSFER
            walletId = template.walletFromId,
            placeId = template.placeId,
            note = template.note,
            savingId = null,
            debtId = null,
            eventId = template.eventId,
            recurrenceId = null,
            confirmed = template.confirmed,
            countInTotal = template.countInTotal,
            tag = template.tag,
            isDeleted = false,
            lastEdit = now
        )
        moneyDao.insertTransaction(fromTx)

        val toTx = TransactionEntity(
            id = toTxId,
            money = template.moneyTo,
            date = date,
            description = template.description,
            categoryId = transferCategory,
            direction = 1, // INCOME
            type = 1, // TRANSFER
            walletId = template.walletToId,
            placeId = template.placeId,
            note = template.note,
            savingId = null,
            debtId = null,
            eventId = template.eventId,
            recurrenceId = null,
            confirmed = template.confirmed,
            countInTotal = template.countInTotal,
            tag = template.tag,
            isDeleted = false,
            lastEdit = now
        )
        moneyDao.insertTransaction(toTx)

        if (taxTxId != null && template.moneyTax != null) {
            val taxTx = TransactionEntity(
                id = taxTxId,
                money = template.moneyTax,
                date = date,
                description = template.description,
                categoryId = transferTaxCategory,
                direction = 0, // EXPENSE
                type = 1, // TRANSFER
                walletId = template.walletFromId,
                placeId = template.placeId,
                note = template.note,
                savingId = null,
                debtId = null,
                eventId = template.eventId,
                recurrenceId = null,
                confirmed = template.confirmed,
                countInTotal = template.countInTotal,
                tag = template.tag,
                isDeleted = false,
                lastEdit = now
            )
            moneyDao.insertTransaction(taxTx)
        }

        val transferEntity = TransferEntity(
            id = UUID.randomUUID().toString(),
            description = template.description,
            date = date,
            transactionFromId = fromTxId,
            transactionToId = toTxId,
            transactionTaxId = taxTxId,
            note = template.note,
            placeId = template.placeId,
            eventId = template.eventId,
            recurrenceId = null,
            confirmed = template.confirmed,
            countInTotal = template.countInTotal,
            isDeleted = false,
            lastEdit = now,
            tag = template.tag
        )
        moneyDao.insertTransfer(transferEntity)

        return transferEntity.id
    }
}
