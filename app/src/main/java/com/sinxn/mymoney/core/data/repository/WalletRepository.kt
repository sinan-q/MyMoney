package com.sinxn.mymoney.core.data.repository

import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.entity.CurrencyEntity
import com.sinxn.mymoney.core.data.local.entity.WalletEntity
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import com.sinxn.mymoney.core.data.local.model.WalletWithBalance
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.util.Constants
import com.sinxn.mymoney.core.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

class WalletInUseInTransferException(message: String = "The wallet cannot be deleted because it is in use in a transfer") : Exception(message)

@Singleton
class WalletRepository @Inject constructor(
    private val moneyDao: MoneyDao,
    private val settingsRepository: SettingsRepository
) {
    fun getWallets(): Flow<List<WalletEntity>> {
        return moneyDao.getWallets()
    }

    suspend fun getWalletsList(): List<WalletEntity> {
        return moneyDao.getWalletsList()
    }

    fun getAllWalletsIncludingArchived(): Flow<List<WalletEntity>> {
        return moneyDao.getAllWalletsIncludingArchived()
    }

    fun getWalletsWithBalance(maxDate: String = DateUtils.getSQLDateTimeString(Date())): Flow<List<WalletWithBalance>> {
        return moneyDao.getWalletsWithBalance(maxDate)
    }

    fun getWalletWithBalance(walletId: String, maxDate: String = DateUtils.getSQLDateTimeString(Date())): Flow<WalletWithBalance?> {
        return moneyDao.getWalletWithBalance(walletId, maxDate)
    }

    suspend fun getWalletById(walletId: String): WalletEntity? {
        return moneyDao.getWalletById(walletId)
    }

    fun getCurrencies(): Flow<List<CurrencyEntity>> {
        return moneyDao.getCurrencies()
    }

    suspend fun getCurrenciesList(): List<CurrencyEntity> {
        return moneyDao.getCurrenciesList()
    }

    fun getTransactionsForWallet(walletId: String, maxDate: String = "9999-12-31 23:59:59"): Flow<List<TransactionWithCategory>> {
        return moneyDao.getTransactionsForWallet(walletId, maxDate)
    }

    suspend fun saveWallet(
        id: String?,
        name: String,
        icon: String,
        currency: String,
        startMoney: Long,
        countInTotal: Boolean = true,
        note: String? = null,
        isArchived: Boolean = false,
        tag: String? = null
    ): String {
        val now = System.currentTimeMillis()
        val walletId = id ?: UUID.randomUUID().toString()

        val currentIndex = if (id == null) {
            (moneyDao.getMaxWalletIndex() ?: 0) + 1
        } else {
            moneyDao.getWalletById(id)?.index ?: 0
        }

        val wallet = if (id == null) {
            WalletEntity(
                id = walletId,
                name = name.trim(),
                icon = icon,
                currency = currency,
                startMoney = startMoney,
                isArchived = isArchived,
                note = note?.trim()?.ifEmpty { null },
                countInTotal = countInTotal,
                index = currentIndex,
                isDeleted = false,
                lastEdit = now,
                tag = tag
            )
        } else {
            val existing = moneyDao.getWalletById(id)
            if (existing != null) {
                existing.copy(
                    name = name.trim(),
                    icon = icon,
                    currency = currency,
                    startMoney = startMoney,
                    isArchived = isArchived,
                    note = note?.trim()?.ifEmpty { null },
                    countInTotal = countInTotal,
                    lastEdit = now,
                    tag = tag ?: existing.tag
                )
            } else {
                WalletEntity(
                    id = walletId,
                    name = name.trim(),
                    icon = icon,
                    currency = currency,
                    startMoney = startMoney,
                    isArchived = isArchived,
                    note = note?.trim()?.ifEmpty { null },
                    countInTotal = countInTotal,
                    index = currentIndex,
                    isDeleted = false,
                    lastEdit = now,
                    tag = tag
                )
            }
        }

        if (id == null || moneyDao.getWalletById(id) == null) {
            moneyDao.insertWallet(wallet)
        } else {
            moneyDao.updateWallet(wallet)
        }
        return walletId
    }

    suspend fun updateWalletArchived(walletId: String, isArchived: Boolean) {
        val now = System.currentTimeMillis()
        moneyDao.updateWalletArchived(walletId, isArchived, now)
    }

    suspend fun updateWalletCountInTotal(walletId: String, countInTotal: Boolean) {
        val now = System.currentTimeMillis()
        moneyDao.updateWalletCountInTotal(walletId, countInTotal, now)
    }

    suspend fun reorderWallets(walletIds: List<String>) {
        val now = System.currentTimeMillis()
        walletIds.forEachIndexed { index, walletId ->
            moneyDao.updateWalletIndex(walletId, index + 1, now)
        }
    }

    suspend fun canDeleteWallet(walletId: String): Boolean {
        val transferCount = moneyDao.getTransferCountForWallet(walletId)
        return transferCount == 0
    }

    suspend fun deleteWallet(walletId: String): Result<Unit> {
        if (!canDeleteWallet(walletId)) {
            return Result.failure(WalletInUseInTransferException())
        }

        val now = System.currentTimeMillis()

        // 1. Soft-delete transactions for this wallet
        moneyDao.softDeleteTransactionsForWallet(walletId, now)

        // 2. Soft-delete transaction models for this wallet
        moneyDao.softDeleteTransactionModelsForWallet(walletId, now)

        // 3. Soft-delete transfer models
        moneyDao.softDeleteTransferModelsForWallet(walletId, now)

        // 4. Soft-delete recurrent transactions
        moneyDao.softDeleteRecurrentTransactionsForWallet(walletId, now)

        // 5. Soft-delete recurrent transfers
        moneyDao.softDeleteRecurrentTransfersForWallet(walletId, now)

        // 6. Soft-delete savings
        moneyDao.softDeleteSavingsForWallet(walletId, now)

        // 7. Soft-delete debts
        moneyDao.softDeleteDebtsForWallet(walletId, now)

        // 8. Soft-delete budget wallets
        moneyDao.softDeleteBudgetWalletsForWallet(walletId, now)

        // 9. Soft-delete the wallet entity itself
        moneyDao.softDeleteWallet(walletId, now)

        // 10. Check if this wallet was selected in SettingsRepository
        try {
            val currentWallet = settingsRepository.currentWalletId.first()
            if (currentWallet == walletId) {
                settingsRepository.setCurrentWalletId(Constants.TOTAL_WALLET_ID)
            }
        } catch (_: Exception) {
        }

        return Result.success(Unit)
    }
}
