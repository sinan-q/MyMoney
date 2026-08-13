package com.sinxn.mymoney.feature.wallet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.entity.WalletEntity
import com.sinxn.mymoney.core.data.local.model.TransactionListItem
import com.sinxn.mymoney.core.data.local.model.WalletWithBalance
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.util.Constants
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WalletDetailsViewModel @Inject constructor(
    private val moneyDao: MoneyDao,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val walletId: String = checkNotNull(savedStateHandle["walletId"])

    init {
        // Save as current wallet accessible on launch
        viewModelScope.launch {
            try {
                if (walletId == Constants.TOTAL_WALLET_ID) {
                    settingsRepository.setCurrentWalletId(walletId)
                } else {
                    moneyDao.getWalletById(walletId)?.let { _ ->
                        settingsRepository.setCurrentWalletId(walletId)
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    val allWallets: StateFlow<List<WalletWithBalance>> = moneyDao.getWalletsWithBalance(DateUtils.getSQLDateTimeString(java.util.Date()))
        .map { list -> list.filter { !it.wallet.isArchived } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val wallet: StateFlow<WalletWithBalance?> = combine(
        settingsRepository.formattingSettings,
        moneyDao.getWalletsWithBalance(DateUtils.getSQLDateTimeString(java.util.Date()))
    ) { settings, allWallets ->
        if (walletId == Constants.TOTAL_WALLET_ID) {
            val walletsInTotal = allWallets.filter {
                it.wallet.countInTotal && (!settings.excludeArchivedFromTotal || !it.wallet.isArchived)
            }
            val totalBalance = walletsInTotal.sumOf { it.currentBalance }
            val globalCurrency = settings.globalCurrency
            val currency = try {
                java.util.Currency.getInstance(globalCurrency)
            } catch (e: Exception) {
                null
            }

            val distinctCurrencies = walletsInTotal.map { it.wallet.currency }.distinct()
            val isTotalValid = distinctCurrencies.size <= 1 && (distinctCurrencies.isEmpty() || distinctCurrencies.first() == globalCurrency)

            val breakdown = if (!isTotalValid && walletsInTotal.isNotEmpty()) {
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

            WalletWithBalance(
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
                currencySymbol = currency?.symbol ?: globalCurrency,
                isTotalValid = isTotalValid,
                balanceBreakdown = breakdown
            )
        } else {
            allWallets.find { it.wallet.id == walletId }
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val formattingSettings = settingsRepository.formattingSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FormattingSettings()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: StateFlow<List<TransactionListItem>> = combine(
        formattingSettings,
        wallet
    ) { settings, walletInfo ->
        val isTotalValid = walletInfo?.isTotalValid ?: true
        val maxDate = if (settings.includeFutureTransactions) "9999-12-31 23:59:59" else DateUtils.getSQLDateTimeString(java.util.Date())
        val transactionsFlow = if (walletId == Constants.TOTAL_WALLET_ID) {
            moneyDao.getAllTransactions(maxDate)
        } else {
            moneyDao.getTransactionsForWallet(walletId, maxDate)
        }
        
        transactionsFlow.map { list ->
            // Pre-parse dates to avoid repeated parsing during sort and group
            val validTransactions = list.map {
                it to DateUtils.parseDate(it.transaction.date)
            }

            val grouped = validTransactions
                .sortedByDescending { it.second }
                .groupBy { (_, date) ->
                    DateUtils.getStartOfBudgetMonth(date, settings.firstDayOfMonth)
                }

            val result = ArrayList<TransactionListItem>(list.size + grouped.size)

            grouped.forEach { (monthDate, transactionsInGroup) ->
                var total = 0L
                var income = 0L
                var expense = 0L
                
                transactionsInGroup.forEach { (t, _) ->
                     if (t.transaction.countInTotal && t.transaction.confirmed) {
                        if (t.transaction.direction == 1) {
                            total += t.transaction.money
                            income += t.transaction.money
                        } else {
                            total -= t.transaction.money
                            expense += t.transaction.money
                        }
                    }
                }

                val monthBreakdown = if (!isTotalValid && transactionsInGroup.isNotEmpty()) {
                    transactionsInGroup
                        .map { it.first }
                        .groupBy { it.currencyCode ?: "" }
                        .map { (currency, group) ->
                            var groupTotal = 0L
                            group.forEach { t ->
                                if (t.transaction.countInTotal && t.transaction.confirmed) {
                                    if (t.transaction.direction == 1) groupTotal += t.transaction.money
                                    else groupTotal -= t.transaction.money
                                }
                            }
                            val groupDecimals = group.firstOrNull()?.decimals ?: 2
                            MoneyFormatter.format(
                                amount = groupTotal,
                                currencyCode = currency,
                                decimals = groupDecimals
                            )
                        }.joinToString(", ")
                } else null

                result.add(TransactionListItem.Header(monthDate, total, income, expense, isTotalValid, monthBreakdown))
                
                transactionsInGroup.forEach { (t, _) ->
                    result.add(TransactionListItem.Transaction(t))
                }
            }
            result
        }
    }.flatMapLatest { it }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun toggleCountInTotal() {
        if (walletId == Constants.TOTAL_WALLET_ID) return // Cannot toggle for "Total"
        
        viewModelScope.launch {
            moneyDao.getWalletById(walletId)?.let { current ->
                moneyDao.updateWalletCountInTotal(
                    walletId = walletId,
                    countInTotal = !current.countInTotal,
                    lastEdit = System.currentTimeMillis()
                )
            }
        }
    }
    fun toggleArchived() {
        if (walletId == Constants.TOTAL_WALLET_ID) return
        
        viewModelScope.launch {
            moneyDao.getWalletById(walletId)?.let { current ->
                moneyDao.updateWalletArchived(
                    walletId = walletId,
                    isArchived = !current.isArchived,
                    lastEdit = System.currentTimeMillis()
                )
            }
        }
    }
}
