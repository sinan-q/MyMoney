package com.sinxn.mymoney.feature.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.entity.WalletEntity
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import com.sinxn.mymoney.core.data.local.model.WalletWithBalance
import com.sinxn.mymoney.core.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MoneyFormatter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

data class HomeUiState(
    val activeWallets: List<WalletWithBalance> = emptyList(),
    val archivedWallets: List<WalletWithBalance> = emptyList(),
    val isTotalValid: Boolean = true,
    val balanceBreakdown: String? = null,
    val pendingTransactions: List<TransactionWithCategory> = emptyList() // REG-03: Unconfirmed recurrence inbox
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val moneyDao: MoneyDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState = combine(
        moneyDao.getWalletsWithBalance(DateUtils.getSQLDateTimeString(java.util.Date())),
        settingsRepository.formattingSettings,
        moneyDao.getPendingUnconfirmedTransactions() // REG-03
    ) { list, settings, pendingTx ->
        Log.d("HomeViewModel", "Wallets emitted: ${list.size}, Pending: ${pendingTx.size}")
        
        // Calculate Total
        val walletsInTotal = list.filter { 
            it.wallet.countInTotal && (!settings.excludeArchivedFromTotal || !it.wallet.isArchived)
        }
        val totalBalance = walletsInTotal.sumOf { it.currentBalance }
        
        val globalCurrency = settings.globalCurrency
        val currency = try {
            java.util.Currency.getInstance(globalCurrency)
        } catch (e: Exception) {
            null
        }

        val totalWallet = WalletWithBalance(
            wallet = WalletEntity(
                id = Constants.TOTAL_WALLET_ID,
                name = "Total",
                icon = "sigma", 
                currency = globalCurrency, 
                startMoney = 0,
                isArchived = false,
                note = null,
                countInTotal = false, 
                index = -1,
                isDeleted = false,
                lastEdit = 0,
                tag = null
            ),
            currentBalance = totalBalance,
            decimals = currency?.defaultFractionDigits ?: 2,
            currencySymbol = currency?.symbol ?: globalCurrency
        )

        val currencies = walletsInTotal.map { it.wallet.currency }.distinct()
        val isTotalValid = currencies.size == 1 && currencies.first() == globalCurrency || (walletsInTotal.isEmpty())

        val balanceBreakdown = if (!isTotalValid && walletsInTotal.isNotEmpty()) {
            walletsInTotal
                .groupBy { it.wallet.currency }
                .map { (currency, group) ->
                    val sum = group.sumOf { it.currentBalance }
                    val groupDecimals = group.firstOrNull()?.decimals ?: 2
                    MoneyFormatter.format(
                        amount = sum,
                        currencyCode = currency,
                        decimals = groupDecimals
                    )
                }.joinToString(", ")
        } else null

        HomeUiState(
            activeWallets = listOf(totalWallet) + list.filter { !it.wallet.isArchived },
            archivedWallets = list.filter { it.wallet.isArchived },
            isTotalValid = isTotalValid,
            balanceBreakdown = balanceBreakdown,
            pendingTransactions = pendingTx
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState()
        )

    /**
     * REG-03: Confirm a pending (unconfirmed) transaction.
     * Once confirmed, it will affect wallet balances.
     */
    fun confirmTransaction(transactionId: String) {
        viewModelScope.launch {
            moneyDao.confirmTransaction(transactionId, System.currentTimeMillis())
        }
    }

    /**
     * REG-03: Dismiss (soft-delete) a pending transaction.
     */
    fun dismissTransaction(transactionId: String) {
        viewModelScope.launch {
            moneyDao.softDeleteTransaction(transactionId, System.currentTimeMillis())
        }
    }
}

