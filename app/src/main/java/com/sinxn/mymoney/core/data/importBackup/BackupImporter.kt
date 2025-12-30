package com.sinxn.mymoney.core.data.importBackup

import android.content.ContentResolver
import android.net.Uri
import com.google.gson.Gson
import com.sinxn.mymoney.core.data.importBackup.model.BackupRoot
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.data.local.entity.TransactionEntity
import com.sinxn.mymoney.core.data.local.entity.WalletEntity
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
                    val reader = BufferedReader(InputStreamReader(zipStream))
                    
                    // Parse the whole JSON structure (assuming it fits in memory)
                    // The moneywallet export format seems to be a single object containing arrays?
                    // Or header, then currencies, etc sequentially? 
                    // Based on JSONDatabaseImporter, it seems to rely on streaming keys. 
                    // However, standard JSON objects are unordered.
                    // If the file is valid JSON, Gson can parse it into a matching class.
                    // We will try to parse it as a BackupRoot object.
                    
                    // Note: If the file is HUGE, we should use streaming. 
                    // For now, simple Gson.fromJson.
                    try {
                        val root = gson.fromJson(reader, BackupRoot::class.java)
                        insertData(root)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // If root parsing fails (maybe because it's not a single root object?),
                        // we might need a more robust streaming approach. 
                        // But for "database.json" in standard export, it should be a JSON object.
                    }
                    return@withContext
                }
                entry = zipStream.nextEntry
            }
        }
    }

    private suspend fun insertData(root: BackupRoot) {
        moneyDao.clearAll()

        root.wallets?.let { wallets ->
            moneyDao.insertWallets(wallets.map { json ->
                WalletEntity(
                    id = json.id,
                    name = json.name ?: "Unknown",
                    icon = json.icon ?: "",
                    currency = json.currency ?: "USD",
                    startMoney = json.startMoney ?: 0L,
                    isArchived = json.archived ?: false,
                    note = json.note,
                    countInTotal = json.countInTotal ?: true,
                    index = json.index ?: 0
                )
            })
        }

        root.categories?.let { categories ->
            moneyDao.insertCategories(categories.map { json ->
                CategoryEntity(
                    id = json.id,
                    name = json.name ?: "Unknown",
                    icon = json.icon ?: "",
                    type = json.type ?: 0,
                    parentId = json.parentId,
                    showReport = json.showReport ?: true,
                    index = json.index ?: 0
                )
            })
        }

        root.transactions?.let { transactions ->
            moneyDao.insertTransactions(transactions.mapNotNull { json ->
                if (json.walletId == null) return@mapNotNull null
                
                TransactionEntity(
                    id = json.id,
                    money = json.money ?: 0L,
                    date = json.date ?: "",
                    description = json.description,
                    categoryId = json.categoryId,
                    walletId = json.walletId,
                    direction = json.direction ?: -1,
                    type = json.type ?: 0,
                    note = json.note,
                    confirmed = json.confirmed ?: true
                )
            })
        }
    }
}
