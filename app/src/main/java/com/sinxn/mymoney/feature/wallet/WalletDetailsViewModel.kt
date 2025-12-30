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
                settingsRepository.setCurrentWalletId(walletId)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
    
    // ... rest of the code

    val wallet: Flow<WalletWithBalance?> = if (walletId == Constants.TOTAL_WALLET_ID) {
        moneyDao.getTotalBalance().map { balance ->
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
                currentBalance = balance,
                decimals = 2,
                currencySymbol = "$" // Default
            )
        }
    } else {
        moneyDao.getWalletWithBalance(walletId)
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
            val transactionsFlow = if (walletId == Constants.TOTAL_WALLET_ID) {
                moneyDao.getAllTransactions()
            } else {
                moneyDao.getTransactionsForWallet(walletId)
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
                    // Calculate total efficiently
                    var total = 0L
                    var income = 0L
                    var expense = 0L
                    val transactionItems = ArrayList<TransactionListItem.Transaction>(transactionsInGroup.size)

                    for ((t, _) in transactionsInGroup) {
                        if (t.transaction.countInTotal && t.transaction.confirmed) {
                            if (t.transaction.direction == 1) {
                                total += t.transaction.money
                                income += t.transaction.money
                            } else {
                                total -= t.transaction.money
                                expense += t.transaction.money
                            }
                        }
                        transactionItems.add(TransactionListItem.Transaction(t))
                    }

                    result.add(TransactionListItem.Header(monthDate, total, income, expense))
                    result.addAll(transactionItems)
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
}
