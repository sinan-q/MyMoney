package com.sinxn.mymoney.core.data.importBackup

import android.content.ContentResolver
import android.net.Uri
import com.google.gson.Gson
import com.sinxn.mymoney.core.data.importBackup.model.BackupRoot
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.data.local.entity.TransactionEntity
import com.sinxn.mymoney.core.data.local.entity.WalletEntity
import com.sinxn.mymoney.core.data.local.entity.PlaceEntity
import com.sinxn.mymoney.core.data.local.entity.PersonEntity
import com.sinxn.mymoney.core.data.local.entity.EventEntity
import com.sinxn.mymoney.core.data.local.entity.DebtEntity
import com.sinxn.mymoney.core.data.local.entity.SavingEntity
import com.sinxn.mymoney.core.data.local.entity.RecurrentTransactionEntity
import com.sinxn.mymoney.core.data.local.entity.TransferEntity
import com.sinxn.mymoney.core.data.local.entity.RecurrentTransferEntity
import com.sinxn.mymoney.core.data.local.entity.BudgetEntity
import com.sinxn.mymoney.core.data.local.entity.BudgetWalletEntity
import com.sinxn.mymoney.core.data.local.entity.TransactionPeopleEntity
import com.sinxn.mymoney.core.data.local.entity.TransferPeopleEntity
import com.sinxn.mymoney.core.data.local.entity.AttachmentEntity
import com.sinxn.mymoney.core.data.local.entity.TransactionAttachmentEntity
import com.sinxn.mymoney.core.data.local.entity.TransferAttachmentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipInputStream
import javax.inject.Inject

class BackupImporter @Inject constructor(
    private val moneyDao: MoneyDao,
    private val contentResolver: ContentResolver,
    private val gson: Gson
) {

    suspend fun importBackup(uri: Uri) = withContext(Dispatchers.IO) {
        val inputStream = contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Could not open file")

        ZipInputStream(inputStream).use { zipStream ->
            var entry = zipStream.nextEntry
            while (entry != null) {
                if (entry.name.endsWith("database.json")) {
                    android.util.Log.d("BackupImporter", "Found database.json")
                    val reader = BufferedReader(InputStreamReader(zipStream))
                    
                    try {
                        val root = gson.fromJson(reader, BackupRoot::class.java)
                        if (root == null) {
                            throw IllegalStateException("Parsed root is null")
                        }
                        android.util.Log.d("BackupImporter", "Parsed Root: wallets=${root.wallets?.size}, categories=${root.categories?.size}")
                        insertData(root)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        throw Exception("Failed to parse backup: ${e.message}", e)
                    }
                    return@withContext
                }
                entry = zipStream.nextEntry
            }
        }
        throw Exception("database.json not found in backup file")
    }

    suspend fun analyzeBackup(uri: Uri): com.sinxn.mymoney.core.data.importBackup.model.BackupAnalysisReport = withContext(Dispatchers.IO) {
        val inputStream = contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Could not open file")

        ZipInputStream(inputStream).use { zipStream ->
            var entry = zipStream.nextEntry
            while (entry != null) {
                if (entry.name.endsWith("database.json")) {
                    val reader = BufferedReader(InputStreamReader(zipStream))
                    val root = gson.fromJson(reader, BackupRoot::class.java)
                        ?: throw IllegalStateException("Parsed root is null")
                    
                    return@withContext generateAnalysisReport(root)
                }
                entry = zipStream.nextEntry
            }
        }
        throw Exception("database.json not found in backup file")
    }

    private fun generateAnalysisReport(root: BackupRoot): com.sinxn.mymoney.core.data.importBackup.model.BackupAnalysisReport {
        // ID Sets for Validation
        val validWalletIds = root.wallets?.map { it.id }?.toSet() ?: emptySet()
        val validCategoryIds = root.categories?.map { it.id }?.toSet() ?: emptySet()
        val validPlaceIds = root.places?.map { it.id }?.toSet() ?: emptySet()
        val validPersonIds = root.people?.map { it.id }?.toSet() ?: emptySet()
        val validEventIds = root.events?.map { it.id }?.toSet() ?: emptySet()
        val validTransactionIds = root.transactions?.map { it.id }?.toSet() ?: emptySet()
        val validAttachmentIds = root.attachments?.map { it.id }?.toSet() ?: emptySet()
        val validDebtIds = root.debts?.map { it.id }?.toSet() ?: emptySet()

        // Helper to calc stats
        fun <T> calcStats(
            list: List<T>?, 
            isDeleted: (T) -> Boolean, 
            isZombie: (T) -> Boolean = { false },
            isInvalid: (T) -> Boolean = { false }
        ): com.sinxn.mymoney.core.data.importBackup.model.EntityStats {
            if (list == null) return com.sinxn.mymoney.core.data.importBackup.model.EntityStats(0, 0, 0, 0)
            val total = list.size
            val soft = list.count { isDeleted(it) }
            val zombies = list.count { isZombie(it) }
            val invalid = list.count { isInvalid(it) }
            return com.sinxn.mymoney.core.data.importBackup.model.EntityStats(total, soft, zombies, invalid)
        }

        val wallets = calcStats(root.wallets, { it.deleted == true })
        val categories = calcStats(root.categories, { it.deleted == true }) // Parent check could be zombie check? Skipping for simplicity
        val places = calcStats(root.places, { it.deleted == true })
        val people = calcStats(root.people, { it.deleted == true })
        val events = calcStats(root.events, { it.deleted == true })
        val budgets = calcStats(root.budgets, { it.deleted == true }, { 
             it.categoryId != null && !validCategoryIds.contains(it.categoryId)
        })
        val attachments = calcStats(root.attachments, { it.deleted == true })

        val debts = calcStats(root.debts, { it.deleted == true }, { 
            !validWalletIds.contains(it.walletId)
        })
        val savings = calcStats(root.savings, { it.deleted == true }, { 
            !validWalletIds.contains(it.walletId)
        })
        val recurrentTransactions = calcStats(root.recurrentTransactions, { it.deleted == true }, { 
            !validCategoryIds.contains(it.categoryId) || !validWalletIds.contains(it.walletId)
        })
        val recurrentTransfers = calcStats(root.recurrentTransfers, { it.deleted == true }, { 
            !validWalletIds.contains(it.walletFromId) || !validWalletIds.contains(it.walletToId)
        })

        val transactions = calcStats(root.transactions, { it.deleted == true }, {
             !validWalletIds.contains(it.walletId) || !validCategoryIds.contains(it.categoryId)
        })

        val transfers = calcStats(root.transfers, { it.deleted == true }, {
             !validTransactionIds.contains(it.transactionFromId) || !validTransactionIds.contains(it.transactionToId)
        })

        // New Entities Stats
        val currencies = calcStats(root.currencies, { it.deleted == true }, { false }, { it.iso == null })
        
        val transactionModels = calcStats(root.transactionModels, { it.deleted == true }, {
            val wid = it.walletId
            val cid = it.categoryId
            (wid != null && !validWalletIds.contains(wid)) || (cid != null && !validCategoryIds.contains(cid))
        }, { it.walletId == null || it.categoryId == null })

        val transferModels = calcStats(root.transferModels, { it.deleted == true }, {
            val f = it.walletFromId
            val t = it.walletToId
            (f != null && !validWalletIds.contains(f)) || (t != null && !validWalletIds.contains(t))
        }, { it.walletFromId == null || it.walletToId == null })
        
        val eventPeople = calcStats(root.eventPeople, { it.deleted == true }, {
             !validEventIds.contains(it.eventId) || !validPersonIds.contains(it.personId)
        })
        
        val debtPeople = calcStats(root.debtPeople, { it.deleted == true }, {
             !validDebtIds.contains(it.debtId) || !validPersonIds.contains(it.personId)
        })

        val debugInfo = StringBuilder()
        debugInfo.append("Transactions Found: ${validTransactionIds.size}\n")
        debugInfo.append("Sample Trans ID: ${validTransactionIds.firstOrNull() ?: "None"}\n")
        debugInfo.append("Transfers Found: ${root.transfers?.size ?: 0}\n")
        root.transfers?.firstOrNull()?.let { t ->
            debugInfo.append("Sample Transfer From: ${t.transactionFromId}\n")
            debugInfo.append("Sample Transfer To: ${t.transactionToId}\n")
        }

        return com.sinxn.mymoney.core.data.importBackup.model.BackupAnalysisReport(
            wallets = wallets,
            categories = categories,
            transactions = transactions,
            transfers = transfers,
            debts = debts,
            savings = savings,
            recurrentTransactions = recurrentTransactions,
            recurrentTransfers = recurrentTransfers,
            places = places,
            people = people,
            events = events,
            budgets = budgets,
            attachments = attachments,
            currencies = currencies,
            transactionModels = transactionModels,
            transferModels = transferModels,
            eventPeople = eventPeople,
            debtPeople = debtPeople,
            debugInfo = debugInfo.toString()
        )
    }

    private suspend fun insertData(root: BackupRoot) {
        moneyDao.clearAll()

        val validWalletIds = mutableSetOf<String>()
        val validCategoryIds = mutableSetOf<String>()
        val validPlaceIds = mutableSetOf<String>()
        val validPersonIds = mutableSetOf<String>()
        val validEventIds = mutableSetOf<String>()
        val validDebtIds = mutableSetOf<String>()
        val validSavingIds = mutableSetOf<String>()
        val validRecurrentTransactionIds = mutableSetOf<String>()
        val validRecurrentTransferIds = mutableSetOf<String>()
        val validTransactionIds = mutableSetOf<String>()
        val validTransferIds = mutableSetOf<String>()
        val validAttachmentIds = mutableSetOf<String>()

        // 1. Wallets
        root.wallets?.let { wallets ->
            val entities = wallets.map { json ->
                validWalletIds.add(json.id)
                WalletEntity(
                    id = json.id,
                    name = json.name ?: "Unknown",
                    icon = json.icon ?: "",
                    currency = json.currency ?: "USD",
                    startMoney = json.startMoney ?: 0L,
                    isArchived = json.archived ?: false,
                    note = json.note,
                    countInTotal = json.countInTotal ?: true,
                    index = json.index ?: 0,
                    isDeleted = json.deleted ?: false,
                    lastEdit = json.lastEdit ?: 0L,
                    tag = json.tag
                )
            }
            moneyDao.insertWallets(entities)
        }

        // 2. Categories (Sorted: Parents first)
        root.categories?.let { categories ->
            // Sort to ensure parents exist before children
            val sortedCategories = categories.sortedWith(Comparator { c1, c2 ->
                if (c1.parentId == null && c2.parentId != null) -1
                else if (c1.parentId != null && c2.parentId == null) 1
                else 0
                // Note: deeply nested categories might still fail if not strictly topological, 
                // but this handles the common root-first case. 
                // For full robustness we might need topological sort or multiple passes.
                // Assuming 1-level depth or order in JSON is mostly correct.
            })
            
            val entities = sortedCategories.map { json ->
                validCategoryIds.add(json.id)
                CategoryEntity(
                    id = json.id,
                    name = json.name ?: "Unknown",
                    icon = json.icon ?: "",
                    type = json.type ?: 0,
                    // If parent doesn't exist in our VALID list (even if self-referencing in same batch), 
                    // Room might fail if we inserted strictly. 
                    // However, for now we assume parentId refers to valid categories.
                    // We can stricter check: if (validCategoryIds.contains(json.parentId)) ... 
                    // But we are building validCategoryIds as we map. This map is effectively just one batch.
                    // To include checking against *already processed* items we'd need sequential inserts or correct order.
                    // For now, map parentId only if it *exists in the input list* to avoid referring to deleted/missing cats.
                    parentId = if (categories.any { it.id == json.parentId }) json.parentId else null,
                    showReport = json.showReport ?: true,
                    index = json.index ?: 0,
                    isDeleted = json.deleted ?: false,
                    lastEdit = json.lastEdit ?: 0L,
                    tag = json.tag
                )
            }
            moneyDao.insertCategories(entities)
        }

        // 3. Metadata
        root.places?.let { places ->
            val entities = places.map { json ->
                validPlaceIds.add(json.id)
                PlaceEntity(
                    id = json.id,
                    name = json.name ?: "",
                    icon = json.icon ?: "",
                    address = json.address,
                    latitude = json.latitude,
                    longitude = json.longitude,
                    isDeleted = json.deleted ?: false,
                    lastEdit = json.lastEdit ?: 0L,
                    tag = json.tag
                )
            }
            moneyDao.insertPlaces(entities)
        }

        root.people?.let { people ->
            val entities = people.map { json ->
                validPersonIds.add(json.id)
                PersonEntity(
                    id = json.id,
                    name = json.name ?: "",
                    icon = json.icon ?: "",
                    note = json.note,
                    isDeleted = json.deleted ?: false,
                    lastEdit = json.lastEdit ?: 0L,
                    tag = json.tag
                )
            }
            moneyDao.insertPeople(entities)
        }

        root.events?.let { events ->
            val entities = events.map { json ->
                validEventIds.add(json.id)
                EventEntity(
                    id = json.id,
                    name = json.name ?: "",
                    icon = json.icon ?: "",
                    note = json.note,
                    startDate = json.startDate ?: "",
                    endDate = json.endDate ?: "",
                    isDeleted = json.deleted ?: false,
                    lastEdit = json.lastEdit ?: 0L,
                    tag = json.tag
                )
            }
            moneyDao.insertEvents(entities)
        }

        // 4. Budgets
        root.budgets?.let { budgets ->
            val entities = budgets.mapNotNull { json ->
                // Budget needs category? Check schema.
                // If categoryId is missing/invalid, do we drop? or nullify?
                // BudgetEntity: val categoryId: String? (Nullable)
                val catId = if (validCategoryIds.contains(json.categoryId)) json.categoryId else null
                
                BudgetEntity(
                    id = json.id,
                    type = json.type ?: 0,
                    categoryId = catId,
                    startDate = json.startDate ?: "",
                    endDate = json.endDate ?: "",
                    money = json.money ?: 0L,
                    currency = json.currency ?: "USD",
                    tag = json.tag,
                    lastEdit = json.lastEdit ?: 0L,
                    isDeleted = json.deleted ?: false
                )
            }
            moneyDao.insertBudgets(entities)
        }

        root.budgetWallets?.let { budgetWallets ->
            val entities = budgetWallets.mapNotNull { json ->
                if (!validWalletIds.contains(json.walletId)) return@mapNotNull null
                // BudgetId check skipped (assumed sorted or valid)
                BudgetWalletEntity(
                    budgetId = json.budgetId,
                    walletId = json.walletId,
                    isDeleted = json.deleted ?: false,
                    lastEdit = json.lastEdit ?: 0L,
                    id = json.id
                )
            }
            moneyDao.insertBudgetWallets(entities)
        }

        // 5. Functional
        root.debts?.let { debts ->
            val entities = debts.mapNotNull { json ->
                if (!validWalletIds.contains(json.walletId)) return@mapNotNull null
                validDebtIds.add(json.id)
                
                DebtEntity(
                    id = json.id,
                    type = json.type ?: 0,
                    icon = json.icon ?: "",
                    description = json.description ?: "",
                    date = json.date ?: "",
                    expirationDate = json.expirationDate,
                    walletId = json.walletId!!,
                    placeId = if (validPlaceIds.contains(json.placeId)) json.placeId else null,
                    money = json.money ?: 0L,
                    isArchived = json.archived ?: false,
                    note = json.note,
                    isDeleted = json.deleted ?: false,
                    lastEdit = json.lastEdit ?: 0L,
                    tag = json.tag
                )
            }
            moneyDao.insertDebts(entities)
        }

        root.savings?.let { savings ->
            val entities = savings.mapNotNull { json ->
                if (!validWalletIds.contains(json.walletId)) return@mapNotNull null
                validSavingIds.add(json.id)

                SavingEntity(
                    id = json.id,
                    description = json.description,
                    icon = json.icon ?: "",
                    startMoney = json.startMoney ?: 0L,
                    endMoney = json.endMoney ?: 0L,
                    walletId = json.walletId!!,
                    endDate = json.endDate,
                    isComplete = json.complete ?: false,
                    note = json.note,
                    isDeleted = json.deleted ?: false,
                    lastEdit = json.lastEdit ?: 0L,
                    tag = json.tag
                )
            }
            moneyDao.insertSavings(entities)
        }

        root.recurrentTransactions?.let { recurrent ->
            val entities = recurrent.mapNotNull { json ->
                // Required: Category, Wallet
                if (!validCategoryIds.contains(json.categoryId)) return@mapNotNull null
                if (!validWalletIds.contains(json.walletId)) return@mapNotNull null
                
                validRecurrentTransactionIds.add(json.id)

                RecurrentTransactionEntity(
                    id = json.id,
                    money = json.money ?: 0L,
                    description = json.description,
                    categoryId = json.categoryId!!,
                    direction = json.direction ?: 0,
                    walletId = json.walletId!!,
                    placeId = if (validPlaceIds.contains(json.placeId)) json.placeId else null,
                    note = json.note,
                    eventId = if (validEventIds.contains(json.eventId)) json.eventId else null,
                    confirmed = json.confirmed ?: true,
                    countInTotal = json.countInTotal ?: true,
                    startDate = json.startDate ?: "",
                    rule = json.rule ?: "",
                    lastOccurrence = json.lastOccurrence ?: "",
                    nextOccurrence = json.nextOccurrence,
                    isDeleted = json.deleted ?: false,
                    lastEdit = json.lastEdit ?: 0L,
                    tag = json.tag
                )
            }
            moneyDao.insertRecurrentTransactions(entities)
        }
        
        root.recurrentTransfers?.let { recurrent ->
            val entities = recurrent.mapNotNull { json ->
                // Required: WalletFrom, WalletTo
                if (!validWalletIds.contains(json.walletFromId)) return@mapNotNull null
                if (!validWalletIds.contains(json.walletToId)) return@mapNotNull null
                
                validRecurrentTransferIds.add(json.id)
                
                RecurrentTransferEntity(
                    id = json.id,
                    description = json.description,
                    walletFromId = json.walletFromId!!,
                    walletToId = json.walletToId!!,
                    moneyFrom = json.moneyFrom ?: 0L,
                    moneyTo = json.moneyTo ?: 0L,
                    moneyTax = json.moneyTax,
                    note = json.note,
                    eventId = if (validEventIds.contains(json.eventId)) json.eventId else null,
                    placeId = if (validPlaceIds.contains(json.placeId)) json.placeId else null,
                    confirmed = json.confirmed ?: true,
                    countInTotal = json.countInTotal ?: true,
                    startDate = json.startDate ?: "",
                    lastOccurrence = json.lastOccurrence ?: "",
                    nextOccurrence = json.nextOccurrence,
                    rule = json.rule ?: "",
                    isDeleted = json.deleted ?: false,
                    lastEdit = json.lastEdit ?: 0L,
                    tag = json.tag
                )
            }
            moneyDao.insertRecurrentTransfers(entities)
        }

        // 6. Transactions
        root.transactions?.let { transactions ->
            val entities = transactions.mapNotNull { json ->
                // Required Check
                if (!validWalletIds.contains(json.walletId)) return@mapNotNull null
                if (!validCategoryIds.contains(json.categoryId)) return@mapNotNull null 
                // Note: categoryId IS required in Entity.
                
                // Optional FKs check
                val place = if (validPlaceIds.contains(json.placeId)) json.placeId else null
                val event = if (validEventIds.contains(json.eventId)) json.eventId else null
                val debt = if (validDebtIds.contains(json.debtId)) json.debtId else null
                val saving = if (validSavingIds.contains(json.savingId)) json.savingId else null
                val recurrence = if (validRecurrentTransactionIds.contains(json.recurrenceId)) json.recurrenceId else null
                
                validTransactionIds.add(json.id)
                
                TransactionEntity(
                    id = json.id,
                    money = json.money ?: 0L,
                    date = json.date ?: "",
                    description = json.description,
                    categoryId = json.categoryId!!,
                    walletId = json.walletId!!,
                    direction = json.direction ?: -1,
                    type = json.type ?: 0,
                    note = json.note,
                    confirmed = json.confirmed ?: true,
                    countInTotal = json.countInTotal ?: true,
                    isDeleted = json.deleted ?: false,
                    placeId = place,
                    eventId = event,
                    debtId = debt,
                    savingId = saving,
                    recurrenceId = recurrence,
                    lastEdit = json.lastEdit ?: 0L,
                    tag = json.tag
                )
            }
            moneyDao.insertTransactions(entities)
        }

        // 7. Transfers
        root.transfers?.let { transfers ->
            val entities = transfers.mapNotNull { json ->
                // Required: TransactionFrom, TransactionTo
                if (!validTransactionIds.contains(json.transactionFromId)) return@mapNotNull null
                if (!validTransactionIds.contains(json.transactionToId)) return@mapNotNull null
                
                validTransferIds.add(json.id)

                TransferEntity(
                    id = json.id,
                    description = json.description,
                    date = json.date ?: "",
                    transactionFromId = json.transactionFromId!!,
                    transactionToId = json.transactionToId!!,
                    transactionTaxId = if (validTransactionIds.contains(json.transactionTaxId)) json.transactionTaxId else null,
                    note = json.note,
                    placeId = if (validPlaceIds.contains(json.placeId)) json.placeId else null,
                    eventId = if (validEventIds.contains(json.eventId)) json.eventId else null,
                    recurrenceId = json.recurrenceId, // No FK constraint
                    confirmed = json.confirmed ?: true,
                    countInTotal = json.countInTotal ?: true,
                    isDeleted = json.deleted ?: false,
                    lastEdit = json.lastEdit ?: 0L,
                    tag = json.tag
                )
            }
            moneyDao.insertTransfers(entities)
        }
        
        // 8. People Joins
        root.transactionPeople?.let { items ->
            val entities = items.mapNotNull { json ->
                if (!validTransactionIds.contains(json.transactionId)) return@mapNotNull null
                if (!validPersonIds.contains(json.personId)) return@mapNotNull null
                
                TransactionPeopleEntity(
                    transactionId = json.transactionId,
                    personId = json.personId,
                    isDeleted = json.deleted ?: false,
                    lastEdit = json.lastEdit ?: 0L,
                    id = json.id
                )
            }
            moneyDao.insertTransactionPeople(entities)
        }

        root.transferPeople?.let { items ->
             val entities = items.mapNotNull { json ->
                if (!validTransferIds.contains(json.transferId)) return@mapNotNull null
                if (!validPersonIds.contains(json.personId)) return@mapNotNull null

                TransferPeopleEntity(
                    transferId = json.transferId,
                    personId = json.personId,
                    isDeleted = json.deleted ?: false,
                    lastEdit = json.lastEdit ?: 0L,
                    id = json.id
                )
            }
            moneyDao.insertTransferPeople(entities)
        }

        // 9. Attachments
        root.attachments?.let { items ->
             val entities = items.map { json ->
                validAttachmentIds.add(json.id)
                AttachmentEntity(
                    id = json.id,
                    file = json.file ?: "",
                    name = json.name ?: "",
                    type = json.type,
                    size = json.size ?: 0L,
                    tag = json.tag,
                    lastEdit = json.lastEdit ?: 0L,
                    isDeleted = json.deleted ?: false
                )
            }
            moneyDao.insertAttachments(entities)
        }

        root.transactionAttachments?.let { items ->
             val entities = items.mapNotNull { json ->
                if (!validTransactionIds.contains(json.transactionId)) return@mapNotNull null
                if (!validAttachmentIds.contains(json.attachmentId)) return@mapNotNull null

                TransactionAttachmentEntity(
                    transactionId = json.transactionId,
                    attachmentId = json.attachmentId,
                    isDeleted = json.deleted ?: false,
                    lastEdit = json.lastEdit ?: 0L,
                    id = json.id
                )
            }
            moneyDao.insertTransactionAttachments(entities)
        }

        root.transferAttachments?.let { items ->
             val entities = items.mapNotNull { json ->
                if (!validTransferIds.contains(json.transferId)) return@mapNotNull null
                if (!validAttachmentIds.contains(json.attachmentId)) return@mapNotNull null

                TransferAttachmentEntity(
                    transferId = json.transferId,
                    attachmentId = json.attachmentId,
                    isDeleted = json.deleted ?: false,
                    lastEdit = json.lastEdit ?: 0L,
                    id = json.id
                )
            }
            moneyDao.insertTransferAttachments(entities)
        }

        // 10. Currencies
        root.currencies?.let { items ->
            val entities = items.mapNotNull { json ->
                val iso = json.iso ?: return@mapNotNull null
                val id = json.id ?: java.util.UUID.randomUUID().toString()
                
                com.sinxn.mymoney.core.data.local.entity.CurrencyEntity(
                    iso = iso,
                    name = json.name ?: iso,
                    symbol = json.symbol,
                    decimals = json.decimals ?: 2,
                    isFavourite = json.favourite ?: false,
                    id = id,
                    lastEdit = json.lastEdit ?: 0L,
                    isDeleted = json.deleted ?: false
                )
            }
            moneyDao.insertCurrencies(entities)
        }

        // 11. Transaction Models
        root.transactionModels?.let { items ->
            val entities = items.mapNotNull { json ->
                 // Required checks: Wallet, Category
                 val walletId = json.walletId ?: return@mapNotNull null
                 val categoryId = json.categoryId ?: return@mapNotNull null
                 
                 if (!validWalletIds.contains(walletId)) return@mapNotNull null
                 if (!validCategoryIds.contains(categoryId)) return@mapNotNull null

                 com.sinxn.mymoney.core.data.local.entity.TransactionModelEntity(
                     id = json.id,
                     money = json.money ?: 0L,
                     description = json.description,
                     categoryId = categoryId,
                     direction = json.direction ?: 0,
                     walletId = walletId,
                     placeId = if (validPlaceIds.contains(json.placeId)) json.placeId else null,
                     note = json.note,
                     eventId = if (validEventIds.contains(json.eventId)) json.eventId else null,
                     confirmed = json.confirmed ?: true,
                     countInTotal = json.countInTotal ?: true,
                     isDeleted = json.deleted ?: false,
                     lastEdit = json.lastEdit ?: 0L,
                     tag = json.tag
                 )
            }
            moneyDao.insertTransactionModels(entities)
        }

        // 12. Transfer Models
        root.transferModels?.let { items ->
             val entities = items.mapNotNull { json ->
                 // Required checks: WalletFrom, WalletTo
                 val walletFromId = json.walletFromId ?: return@mapNotNull null
                 val walletToId = json.walletToId ?: return@mapNotNull null
                 
                 if (!validWalletIds.contains(walletFromId)) return@mapNotNull null
                 if (!validWalletIds.contains(walletToId)) return@mapNotNull null

                 com.sinxn.mymoney.core.data.local.entity.TransferModelEntity(
                     id = json.id,
                     description = json.description,
                     walletFromId = walletFromId,
                     walletToId = walletToId,
                     moneyFrom = json.moneyFrom ?: 0L,
                     moneyTo = json.moneyTo ?: 0L,
                     moneyTax = json.moneyTax,
                     note = json.note,
                     eventId = if (validEventIds.contains(json.eventId)) json.eventId else null,
                     placeId = if (validPlaceIds.contains(json.placeId)) json.placeId else null,
                     confirmed = json.confirmed ?: true,
                     countInTotal = json.countInTotal ?: true,
                     isDeleted = json.deleted ?: false,
                     lastEdit = json.lastEdit ?: 0L,
                     tag = json.tag
                 )
             }
             moneyDao.insertTransferModels(entities)
        }

        // 13. Event People
        root.eventPeople?.let { items ->
             val entities = items.mapNotNull { json ->
                 if (!validEventIds.contains(json.eventId)) return@mapNotNull null
                 if (!validPersonIds.contains(json.personId)) return@mapNotNull null
                 
                 com.sinxn.mymoney.core.data.local.entity.EventPeopleEntity(
                     eventId = json.eventId,
                     personId = json.personId,
                     isDeleted = json.deleted ?: false,
                     lastEdit = json.lastEdit ?: 0L,
                     id = json.id
                 )
             }
             moneyDao.insertEventPeople(entities)
        }

        // 14. Debt People
        root.debtPeople?.let { items ->
             val entities = items.mapNotNull { json ->
                 if (!validDebtIds.contains(json.debtId)) return@mapNotNull null
                 if (!validPersonIds.contains(json.personId)) return@mapNotNull null
                 
                 com.sinxn.mymoney.core.data.local.entity.DebtPeopleEntity(
                     debtId = json.debtId,
                     personId = json.personId,
                     isDeleted = json.deleted ?: false,
                     lastEdit = json.lastEdit ?: 0L,
                     id = json.id
                 )
             }
             moneyDao.insertDebtPeople(entities)
        }
    }
}
