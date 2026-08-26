package com.sinxn.mymoney.core.data.importBackup

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.sinxn.mymoney.core.data.importBackup.model.BackupRoot
import com.sinxn.mymoney.core.data.importBackup.model.JsonAttachment
import com.sinxn.mymoney.core.data.importBackup.model.JsonBudget
import com.sinxn.mymoney.core.data.importBackup.model.JsonBudgetWallet
import com.sinxn.mymoney.core.data.importBackup.model.JsonCategory
import com.sinxn.mymoney.core.data.importBackup.model.JsonCurrency
import com.sinxn.mymoney.core.data.importBackup.model.JsonDebt
import com.sinxn.mymoney.core.data.importBackup.model.JsonDebtPerson
import com.sinxn.mymoney.core.data.importBackup.model.JsonEvent
import com.sinxn.mymoney.core.data.importBackup.model.JsonEventPerson
import com.sinxn.mymoney.core.data.importBackup.model.JsonHeader
import com.sinxn.mymoney.core.data.importBackup.model.JsonPerson
import com.sinxn.mymoney.core.data.importBackup.model.JsonPlace
import com.sinxn.mymoney.core.data.importBackup.model.JsonRecurrentTransaction
import com.sinxn.mymoney.core.data.importBackup.model.JsonRecurrentTransfer
import com.sinxn.mymoney.core.data.importBackup.model.JsonSaving
import com.sinxn.mymoney.core.data.importBackup.model.JsonTransaction
import com.sinxn.mymoney.core.data.importBackup.model.JsonTransactionAttachment
import com.sinxn.mymoney.core.data.importBackup.model.JsonTransactionModel
import com.sinxn.mymoney.core.data.importBackup.model.JsonTransactionPerson
import com.sinxn.mymoney.core.data.importBackup.model.JsonTransfer
import com.sinxn.mymoney.core.data.importBackup.model.JsonTransferAttachment
import com.sinxn.mymoney.core.data.importBackup.model.JsonTransferModel
import com.sinxn.mymoney.core.data.importBackup.model.JsonTransferPerson
import com.sinxn.mymoney.core.data.importBackup.model.JsonWallet
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupExporter @Inject constructor(
    private val moneyDao: MoneyDao,
    private val contentResolver: ContentResolver,
    private val gson: Gson,
    @ApplicationContext private val context: Context
) {

    suspend fun exportBackup(uri: Uri) = withContext(Dispatchers.IO) {
        val outputStream = contentResolver.openOutputStream(uri)
            ?: throw IllegalStateException("Could not open destination output stream")

        // 1. Gather all non-deleted entities from Room
        val currencies = moneyDao.getAllCurrenciesForExport().map {
            JsonCurrency(
                iso = it.iso,
                name = it.name,
                symbol = it.symbol,
                decimals = it.decimals,
                favourite = it.isFavourite,
                id = null, // Excluded in legacy JSON
                lastEdit = it.lastEdit,
                deleted = it.isDeleted
            )
        }

        val wallets = moneyDao.getAllWalletsForExport().map {
            JsonWallet(
                id = it.id,
                name = it.name,
                icon = it.icon,
                currency = it.currency,
                startMoney = it.startMoney,
                countInTotal = it.countInTotal,
                archived = it.isArchived,
                note = it.note,
                index = it.index,
                deleted = it.isDeleted,
                lastEdit = it.lastEdit,
                tag = it.tag
            )
        }

        val categories = moneyDao.getAllCategoriesForExport().map {
            JsonCategory(
                id = it.id,
                name = it.name,
                icon = it.icon,
                type = it.type,
                parentId = it.parentId,
                showReport = it.showReport,
                archived = null,
                index = it.index,
                deleted = it.isDeleted,
                lastEdit = it.lastEdit,
                tag = it.tag
            )
        }

        val places = moneyDao.getAllPlacesForExport().map {
            JsonPlace(
                id = it.id,
                name = it.name,
                icon = it.icon,
                address = it.address,
                latitude = it.latitude,
                longitude = it.longitude,
                archived = null,
                deleted = it.isDeleted,
                lastEdit = it.lastEdit,
                tag = it.tag
            )
        }

        val people = moneyDao.getAllPeopleForExport().map {
            JsonPerson(
                id = it.id,
                name = it.name,
                icon = it.icon,
                note = it.note,
                archived = null,
                deleted = it.isDeleted,
                lastEdit = it.lastEdit,
                tag = it.tag
            )
        }

        val events = moneyDao.getAllEventsForExport().map {
            JsonEvent(
                id = it.id,
                name = it.name,
                icon = it.icon,
                note = it.note,
                startDate = it.startDate,
                endDate = it.endDate,
                archived = null,
                deleted = it.isDeleted,
                lastEdit = it.lastEdit,
                tag = it.tag
            )
        }

        val eventPeople = moneyDao.getAllEventPeopleForExport().map {
            JsonEventPerson(
                id = it.id,
                eventId = it.eventId,
                personId = it.personId,
                deleted = it.isDeleted,
                lastEdit = it.lastEdit
            )
        }

        val debts = moneyDao.getAllDebtsForExport().map {
            JsonDebt(
                id = it.id,
                type = it.type,
                icon = it.icon,
                description = it.description,
                date = it.date,
                expirationDate = it.expirationDate,
                walletId = it.walletId,
                placeId = it.placeId,
                money = it.money,
                archived = it.isArchived,
                note = it.note,
                deleted = it.isDeleted,
                lastEdit = it.lastEdit,
                tag = it.tag
            )
        }

        val debtPeople = moneyDao.getAllDebtPeopleForExport().map {
            JsonDebtPerson(
                id = it.id,
                debtId = it.debtId,
                personId = it.personId,
                deleted = it.isDeleted,
                lastEdit = it.lastEdit
            )
        }

        val budgets = moneyDao.getAllBudgetsForExport().map {
            JsonBudget(
                id = it.id,
                type = it.type,
                categoryId = it.categoryId,
                startDate = it.startDate,
                endDate = it.endDate,
                money = it.money,
                currency = it.currency,
                deleted = it.isDeleted,
                lastEdit = it.lastEdit,
                tag = it.tag
            )
        }

        val budgetWallets = moneyDao.getAllBudgetWalletsForExport().map {
            JsonBudgetWallet(
                id = it.id,
                budgetId = it.budgetId,
                walletId = it.walletId,
                deleted = it.isDeleted,
                lastEdit = it.lastEdit
            )
        }

        val savings = moneyDao.getAllSavingsForExport().map {
            JsonSaving(
                id = it.id,
                description = it.description,
                icon = it.icon,
                startMoney = it.startMoney,
                endMoney = it.endMoney,
                walletId = it.walletId,
                endDate = it.endDate,
                complete = it.isComplete,
                note = it.note,
                deleted = it.isDeleted,
                lastEdit = it.lastEdit,
                tag = it.tag
            )
        }

        val recurrentTransactions = moneyDao.getAllRecurrentTransactionsForExport().map {
            JsonRecurrentTransaction(
                id = it.id,
                money = it.money,
                description = it.description,
                categoryId = it.categoryId,
                direction = it.direction,
                walletId = it.walletId,
                placeId = it.placeId,
                note = it.note,
                eventId = it.eventId,
                confirmed = it.confirmed,
                countInTotal = it.countInTotal,
                startDate = it.startDate,
                rule = it.rule,
                lastOccurrence = it.lastOccurrence,
                nextOccurrence = it.nextOccurrence,
                deleted = it.isDeleted,
                lastEdit = it.lastEdit,
                tag = it.tag
            )
        }

        val recurrentTransfers = moneyDao.getAllRecurrentTransfersForExport().map {
            JsonRecurrentTransfer(
                id = it.id,
                description = it.description,
                walletFromId = it.walletFromId,
                walletToId = it.walletToId,
                moneyFrom = it.moneyFrom,
                moneyTo = it.moneyTo,
                moneyTax = it.moneyTax,
                note = it.note,
                eventId = it.eventId,
                placeId = it.placeId,
                confirmed = it.confirmed,
                countInTotal = it.countInTotal,
                startDate = it.startDate,
                rule = it.rule,
                lastOccurrence = it.lastOccurrence,
                nextOccurrence = it.nextOccurrence,
                deleted = it.isDeleted,
                lastEdit = it.lastEdit,
                tag = it.tag
            )
        }

        val transactions = moneyDao.getAllTransactionsForExport().map {
            JsonTransaction(
                id = it.id,
                money = it.money,
                date = it.date,
                description = it.description,
                categoryId = it.categoryId,
                walletId = it.walletId,
                direction = it.direction,
                type = it.type,
                note = it.note,
                confirmed = it.confirmed,
                countInTotal = it.countInTotal,
                deleted = it.isDeleted,
                placeId = it.placeId,
                eventId = it.eventId,
                savingId = it.savingId,
                debtId = it.debtId,
                recurrenceId = it.recurrenceId,
                lastEdit = it.lastEdit,
                tag = it.tag
            )
        }

        val transactionPeople = moneyDao.getAllTransactionPeopleForExport().map {
            JsonTransactionPerson(
                id = it.id,
                transactionId = it.transactionId,
                personId = it.personId,
                deleted = it.isDeleted,
                lastEdit = it.lastEdit
            )
        }

        val transactionModels = moneyDao.getAllTransactionModelsForExport().map {
            JsonTransactionModel(
                id = it.id,
                money = it.money,
                description = it.description,
                categoryId = it.categoryId,
                direction = it.direction,
                walletId = it.walletId,
                placeId = it.placeId,
                note = it.note,
                eventId = it.eventId,
                confirmed = it.confirmed,
                countInTotal = it.countInTotal,
                tag = it.tag,
                lastEdit = it.lastEdit,
                deleted = it.isDeleted
            )
        }

        val transfers = moneyDao.getAllTransfersForExport().map {
            JsonTransfer(
                id = it.id,
                description = it.description,
                date = it.date,
                transactionFromId = it.transactionFromId,
                transactionToId = it.transactionToId,
                transactionTaxId = it.transactionTaxId,
                note = it.note,
                placeId = it.placeId,
                eventId = it.eventId,
                recurrenceId = it.recurrenceId,
                confirmed = it.confirmed,
                countInTotal = it.countInTotal,
                deleted = it.isDeleted,
                lastEdit = it.lastEdit,
                tag = it.tag
            )
        }

        val transferPeople = moneyDao.getAllTransferPeopleForExport().map {
            JsonTransferPerson(
                id = it.id,
                transferId = it.transferId,
                personId = it.personId,
                deleted = it.isDeleted,
                lastEdit = it.lastEdit
            )
        }

        val transferModels = moneyDao.getAllTransferModelsForExport().map {
            JsonTransferModel(
                id = it.id,
                description = it.description,
                walletFromId = it.walletFromId,
                walletToId = it.walletToId,
                moneyFrom = it.moneyFrom,
                moneyTo = it.moneyTo,
                moneyTax = it.moneyTax,
                note = it.note,
                eventId = it.eventId,
                placeId = it.placeId,
                confirmed = it.confirmed,
                countInTotal = it.countInTotal,
                tag = it.tag,
                lastEdit = it.lastEdit,
                deleted = it.isDeleted
            )
        }

        val attachments = moneyDao.getAllAttachmentsForExport().map {
            JsonAttachment(
                id = it.id,
                file = it.file,
                name = it.name,
                type = it.type,
                size = it.size,
                deleted = it.isDeleted,
                lastEdit = it.lastEdit,
                tag = it.tag
            )
        }

        val transactionAttachments = moneyDao.getAllTransactionAttachmentsForExport().map {
            JsonTransactionAttachment(
                id = it.id,
                transactionId = it.transactionId,
                attachmentId = it.attachmentId,
                deleted = it.isDeleted,
                lastEdit = it.lastEdit
            )
        }

        val transferAttachments = moneyDao.getAllTransferAttachmentsForExport().map {
            JsonTransferAttachment(
                id = it.id,
                transferId = it.transferId,
                attachmentId = it.attachmentId,
                deleted = it.isDeleted,
                lastEdit = it.lastEdit
            )
        }

        val root = BackupRoot(
            header = JsonHeader(versionCode = 2),
            currencies = currencies,
            wallets = wallets,
            categories = categories,
            events = events,
            places = places,
            people = people,
            eventPeople = eventPeople,
            debts = debts,
            debtPeople = debtPeople,
            budgets = budgets,
            budgetWallets = budgetWallets,
            savings = savings,
            recurrentTransactions = recurrentTransactions,
            recurrentTransfers = recurrentTransfers,
            transactions = transactions,
            transactionPeople = transactionPeople,
            transactionModels = transactionModels,
            transfers = transfers,
            transferPeople = transferPeople,
            transferModels = transferModels,
            attachments = attachments,
            transactionAttachments = transactionAttachments,
            transferAttachments = transferAttachments
        )

        // 2. Write to ZipOutputStream
        ZipOutputStream(BufferedOutputStream(outputStream)).use { zipStream ->
            // Entry: databases/database.json
            val dbEntry = ZipEntry("databases/database.json")
            zipStream.putNextEntry(dbEntry)
            val writer = OutputStreamWriter(zipStream, StandardCharsets.UTF_8)
            gson.toJson(root, writer)
            writer.flush()
            zipStream.closeEntry()

            // Entry: attachments/<file> (if any attachment files exist)
            val attachmentDirs = listOfNotNull(
                context.getExternalFilesDir(null)?.resolve("attachments"),
                File(context.filesDir, "attachments")
            )

            val exportedFiles = mutableSetOf<String>()
            for (att in attachments) {
                val fileName = att.file ?: continue
                if (exportedFiles.contains(fileName)) continue

                for (dir in attachmentDirs) {
                    val file = File(dir, fileName)
                    if (file.exists() && file.isFile) {
                        val attEntry = ZipEntry("attachments/$fileName")
                        zipStream.putNextEntry(attEntry)
                        BufferedInputStream(FileInputStream(file)).use { fileIn ->
                            fileIn.copyTo(zipStream)
                        }
                        zipStream.closeEntry()
                        exportedFiles.add(fileName)
                        break
                    }
                }
            }
        }
    }
}
