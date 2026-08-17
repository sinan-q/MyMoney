package com.sinxn.mymoney.core.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.entity.RecurrentTransactionEntity
import com.sinxn.mymoney.core.data.local.entity.RecurrentTransferEntity
import com.sinxn.mymoney.core.data.local.entity.TransactionEntity
import com.sinxn.mymoney.core.data.local.entity.TransferEntity
import com.sinxn.mymoney.core.data.local.model.RecurrentTransactionWithDetails
import com.sinxn.mymoney.core.data.local.model.RecurrentTransferWithDetails
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.RecurrenceSetting
import com.sinxn.mymoney.core.worker.RecurrenceWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.dmfs.rfc5545.DateTime
import org.dmfs.rfc5545.recur.InvalidRecurrenceRuleException
import org.dmfs.rfc5545.recur.RecurrenceRule
import org.dmfs.rfc5545.recur.RecurrenceRuleIterator
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurrenceRepository @Inject constructor(
    private val moneyDao: MoneyDao,
    @ApplicationContext private val context: Context
) {

    fun getRecurrentTransactions(walletId: String? = null): Flow<List<RecurrentTransactionWithDetails>> =
        moneyDao.getRecurrentTransactionsWithDetails(walletId)

    fun getRecurrentTransfers(walletId: String? = null): Flow<List<RecurrentTransferWithDetails>> =
        moneyDao.getRecurrentTransfersWithDetails(walletId)

    val recurrentTransactions: Flow<List<RecurrentTransactionWithDetails>> =
        getRecurrentTransactions()

    val recurrentTransfers: Flow<List<RecurrentTransferWithDetails>> =
        getRecurrentTransfers()

    fun getRecurrentTransactionWithDetails(id: String): Flow<RecurrentTransactionWithDetails?> {
        return moneyDao.getRecurrentTransactionWithDetailsById(id)
    }

    fun getRecurrentTransferWithDetails(id: String): Flow<RecurrentTransferWithDetails?> {
        return moneyDao.getRecurrentTransferWithDetailsById(id)
    }

    suspend fun saveRecurrentTransaction(item: RecurrentTransactionEntity) {
        withContext(Dispatchers.IO) {
            moneyDao.insertRecurrentTransaction(item)
            processPendingRecurrences()
        }
    }

    suspend fun saveRecurrentTransfer(item: RecurrentTransferEntity) {
        withContext(Dispatchers.IO) {
            moneyDao.insertRecurrentTransfer(item)
            processPendingRecurrences()
        }
    }

    suspend fun deleteRecurrentTransaction(id: String) {
        withContext(Dispatchers.IO) {
            moneyDao.unlinkTransactionsForRecurrence(id)
            moneyDao.deleteRecurrentTransaction(id)
        }
    }

    suspend fun deleteRecurrentTransfer(id: String) {
        withContext(Dispatchers.IO) {
            moneyDao.unlinkTransfersForRecurrence(id)
            moneyDao.deleteRecurrentTransfer(id)
        }
    }

    suspend fun processPendingRecurrences() {
        withContext(Dispatchers.IO) {
            val now = Date()
            val todaySqlDate = DateUtils.getSQLDateTimeString(now)
            val currentDateTime = RecurrenceSetting.getFixedDateTime(now)

            // 1. Process Recurrent Transactions
            val pendingTransactions = moneyDao.getPendingRecurrentTransactions(todaySqlDate)
            for (rt in pendingTransactions) {
                val nextOccurrenceStr = rt.nextOccurrence ?: continue
                val firstOccurrenceDate = DateUtils.parseDate(nextOccurrenceStr)
                val startDateTime = RecurrenceSetting.getFixedDateTime(firstOccurrenceDate)
                var lastOccurrenceDateTime: DateTime = startDateTime
                var nextOccurrenceDateTime: DateTime? = null

                try {
                    val rule = RecurrenceRule(rt.rule)
                    val iterator: RecurrenceRuleIterator = rule.iterator(startDateTime)
                    while (iterator.hasNext()) {
                        val nextInstance = iterator.nextDateTime()
                        if (!nextInstance.after(currentDateTime)) {
                            val occurrenceDate = RecurrenceSetting.getFixedDate(nextInstance)
                            val txEntity = TransactionEntity(
                                id = UUID.randomUUID().toString(),
                                money = rt.money,
                                date = DateUtils.getSQLDateTimeString(occurrenceDate),
                                description = rt.description,
                                categoryId = rt.categoryId,
                                direction = rt.direction,
                                type = 0, // STANDARD
                                walletId = rt.walletId,
                                placeId = rt.placeId,
                                note = rt.note,
                                savingId = null,
                                debtId = null,
                                eventId = rt.eventId,
                                recurrenceId = rt.id,
                                confirmed = rt.confirmed,
                                countInTotal = rt.countInTotal,
                                tag = rt.tag,
                                isDeleted = false,
                                lastEdit = System.currentTimeMillis()
                            )
                            moneyDao.insertTransaction(txEntity)
                            lastOccurrenceDateTime = nextInstance
                        } else {
                            nextOccurrenceDateTime = nextInstance
                            break
                        }
                    }

                    val updatedLast = DateUtils.getSQLDateTimeString(RecurrenceSetting.getFixedDate(lastOccurrenceDateTime))
                    val updatedNext = nextOccurrenceDateTime?.let {
                        DateUtils.getSQLDateTimeString(RecurrenceSetting.getFixedDate(it))
                    }

                    moneyDao.updateRecurrentTransaction(
                        rt.copy(
                            lastOccurrence = updatedLast,
                            nextOccurrence = updatedNext,
                            lastEdit = System.currentTimeMillis()
                        )
                    )
                } catch (e: InvalidRecurrenceRuleException) {
                    // Invalid rule, mark nextOccurrence null
                    moneyDao.updateRecurrentTransaction(rt.copy(nextOccurrence = null))
                }
            }

            // 2. Process Recurrent Transfers
            val pendingTransfers = moneyDao.getPendingRecurrentTransfers(todaySqlDate)
            val categories = moneyDao.getCategoriesList()
            val transferCategory = categories.find {
                it.tag == "system::transfer" || it.tag == "transfer" || it.name.equals("Transfer", ignoreCase = true)
            }?.id ?: (categories.firstOrNull()?.id ?: "")

            val transferTaxCategory = categories.find {
                it.tag == "system::transfer_tax" || it.tag == "transfer_tax" || it.name.contains("Tax", ignoreCase = true)
            }?.id ?: transferCategory

            for (rtf in pendingTransfers) {
                val nextOccurrenceStr = rtf.nextOccurrence ?: continue
                val firstOccurrenceDate = DateUtils.parseDate(nextOccurrenceStr)
                val startDateTime = RecurrenceSetting.getFixedDateTime(firstOccurrenceDate)
                var lastOccurrenceDateTime: DateTime = startDateTime
                var nextOccurrenceDateTime: DateTime? = null

                try {
                    val rule = RecurrenceRule(rtf.rule)
                    val iterator: RecurrenceRuleIterator = rule.iterator(startDateTime)
                    while (iterator.hasNext()) {
                        val nextInstance = iterator.nextDateTime()
                        if (!nextInstance.after(currentDateTime)) {
                            val occurrenceDate = RecurrenceSetting.getFixedDate(nextInstance)
                            val occurrenceDateStr = DateUtils.getSQLDateTimeString(occurrenceDate)

                            val fromTxId = UUID.randomUUID().toString()
                            val toTxId = UUID.randomUUID().toString()
                            val taxTxId = rtf.moneyTax?.takeIf { it > 0 }?.let { UUID.randomUUID().toString() }

                            // From Tx
                            val fromTx = TransactionEntity(
                                id = fromTxId,
                                money = rtf.moneyFrom,
                                date = occurrenceDateStr,
                                description = rtf.description,
                                categoryId = transferCategory,
                                direction = 0, // EXPENSE
                                type = 1, // TRANSFER
                                walletId = rtf.walletFromId,
                                placeId = rtf.placeId,
                                note = rtf.note,
                                savingId = null,
                                debtId = null,
                                eventId = rtf.eventId,
                                recurrenceId = rtf.id,
                                confirmed = rtf.confirmed,
                                countInTotal = rtf.countInTotal,
                                tag = rtf.tag,
                                isDeleted = false,
                                lastEdit = System.currentTimeMillis()
                            )
                            moneyDao.insertTransaction(fromTx)

                            // To Tx
                            val toTx = TransactionEntity(
                                id = toTxId,
                                money = rtf.moneyTo,
                                date = occurrenceDateStr,
                                description = rtf.description,
                                categoryId = transferCategory,
                                direction = 1, // INCOME
                                type = 1, // TRANSFER
                                walletId = rtf.walletToId,
                                placeId = rtf.placeId,
                                note = rtf.note,
                                savingId = null,
                                debtId = null,
                                eventId = rtf.eventId,
                                recurrenceId = rtf.id,
                                confirmed = rtf.confirmed,
                                countInTotal = rtf.countInTotal,
                                tag = rtf.tag,
                                isDeleted = false,
                                lastEdit = System.currentTimeMillis()
                            )
                            moneyDao.insertTransaction(toTx)

                            // Tax Tx (if any)
                            if (taxTxId != null && rtf.moneyTax != null) {
                                val taxTx = TransactionEntity(
                                    id = taxTxId,
                                    money = rtf.moneyTax,
                                    date = occurrenceDateStr,
                                    description = rtf.description,
                                    categoryId = transferTaxCategory,
                                    direction = 0, // EXPENSE
                                    type = 1, // TRANSFER
                                    walletId = rtf.walletFromId,
                                    placeId = rtf.placeId,
                                    note = rtf.note,
                                    savingId = null,
                                    debtId = null,
                                    eventId = rtf.eventId,
                                    recurrenceId = rtf.id,
                                    confirmed = rtf.confirmed,
                                    countInTotal = rtf.countInTotal,
                                    tag = rtf.tag,
                                    isDeleted = false,
                                    lastEdit = System.currentTimeMillis()
                                )
                                moneyDao.insertTransaction(taxTx)
                            }

                            // Transfer Entity
                            val transferEntity = TransferEntity(
                                id = UUID.randomUUID().toString(),
                                description = rtf.description,
                                date = occurrenceDateStr,
                                transactionFromId = fromTxId,
                                transactionToId = toTxId,
                                transactionTaxId = taxTxId,
                                note = rtf.note,
                                placeId = rtf.placeId,
                                eventId = rtf.eventId,
                                recurrenceId = rtf.id,
                                confirmed = rtf.confirmed,
                                countInTotal = rtf.countInTotal,
                                isDeleted = false,
                                lastEdit = System.currentTimeMillis(),
                                tag = rtf.tag
                            )
                            moneyDao.insertTransfer(transferEntity)

                            lastOccurrenceDateTime = nextInstance
                        } else {
                            nextOccurrenceDateTime = nextInstance
                            break
                        }
                    }

                    val updatedLast = DateUtils.getSQLDateTimeString(RecurrenceSetting.getFixedDate(lastOccurrenceDateTime))
                    val updatedNext = nextOccurrenceDateTime?.let {
                        DateUtils.getSQLDateTimeString(RecurrenceSetting.getFixedDate(it))
                    }

                    moneyDao.updateRecurrentTransfer(
                        rtf.copy(
                            lastOccurrence = updatedLast,
                            nextOccurrence = updatedNext,
                            lastEdit = System.currentTimeMillis()
                        )
                    )
                } catch (e: InvalidRecurrenceRuleException) {
                    moneyDao.updateRecurrentTransfer(rtf.copy(nextOccurrence = null))
                }
            }

            scheduleWorkManager()
        }
    }

    private fun scheduleWorkManager() {
        val constraints = Constraints.Builder().build()
        val periodicRequest = PeriodicWorkRequestBuilder<RecurrenceWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "RecurrenceWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
    }
}
