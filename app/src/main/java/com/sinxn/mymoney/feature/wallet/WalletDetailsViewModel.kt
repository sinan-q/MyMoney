package com.sinxn.mymoney.feature.wallet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.entity.WalletEntity
import com.sinxn.mymoney.core.data.local.model.WalletWithBalance
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.util.Constants
import com.sinxn.mymoney.core.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
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
                    moneyDao.getWalletById(walletId)?.let { wallet ->
                        if (!wallet.isArchived) {
                            settingsRepository.setCurrentWalletId(walletId)
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
    
    // ... rest of the code

    @OptIn(ExperimentalCoroutinesApi::class)
    val wallet: StateFlow<WalletWithBalance?> = settingsRepository.formattingSettings
        .flatMapLatest { settings ->
            if (walletId == Constants.TOTAL_WALLET_ID) {
                moneyDao.getTotalBalance(
                    DateUtils.getSQLDateTimeString(java.util.Date()),
                    settings.excludeArchivedFromTotal
                ).map { balance ->
                    WalletWithBalance(
                        wallet = WalletEntity(
                            id = Constants.TOTAL_WALLET_ID,
                            name = "Total",
                            icon = "sigma",
                            currency = "USD", // Better default?
                            startMoney = 0,
                            isArchived = false,
                            note = null,
                            countInTotal = false,
                            index = -1,
                            isDeleted = false,
                            lastEdit = 0,
                            tag = null
                        ),
                        currentBalance = balance ?: 0L,
                        decimals = 2,
                        currencySymbol = "$" // Default
                    )
                }
            } else {
                moneyDao.getWalletWithBalance(walletId, DateUtils.getSQLDateTimeString(java.util.Date()))
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
    val transactions: StateFlow<List<TransactionListItem>> = formattingSettings
        .flatMapLatest { settings ->
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
                        // Use firstDayOfMonth from settings
                        DateUtils.getStartOfBudgetMonth(date, settings.firstDayOfMonth)
                    }

                val result = ArrayList<TransactionListItem>(list.size + grouped.size) // Pre-allocate

                grouped.forEach { (monthDate, transactionsInGroup) ->
                    // Calculate Month Totals
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

                    result.add(TransactionListItem.Header(monthDate, total, income, expense))
                    
                    // Group by Day within the month
                     val dayGrouped = transactionsInGroup.groupBy { (_, date) ->
                        val cal = Calendar.getInstance()
                        cal.time = date
                        cal.set(Calendar.HOUR_OF_DAY, 0)
                        cal.set(Calendar.MINUTE, 0)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        cal.time
                    }
                    
                    dayGrouped.forEach { (dayDate, transactionsInDay) ->
                        // Calculate Daily Total
                         var dailyTotal = 0L
                         transactionsInDay.forEach { (t, _) ->
                             val amount = if(t.transaction.direction == 1) t.transaction.money else -t.transaction.money
                             dailyTotal += amount
                         }

                        result.add(TransactionListItem.DateHeader(dayDate, dailyTotal))
                        
                        transactionsInDay.forEach { (t, _) ->
                             result.add(TransactionListItem.Transaction(t))
                        }
                    }
                }
                result
            }
        }
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
