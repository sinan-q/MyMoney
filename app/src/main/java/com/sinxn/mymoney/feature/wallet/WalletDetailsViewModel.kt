package com.sinxn.mymoney.feature.wallet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.model.WalletWithBalance
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class WalletDetailsViewModel @Inject constructor(
    private val moneyDao: MoneyDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val walletId: String = checkNotNull(savedStateHandle["walletId"])

    val wallet: Flow<WalletWithBalance?> = moneyDao.getWalletWithBalance(walletId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: StateFlow<List<TransactionListItem>> = flowOf(walletId)
        .flatMapLatest { id ->
            moneyDao.getTransactionsForWallet(id) // Assuming getTransactionsForWallet is the correct method, not getWalletTransactions
                .map { list ->
                    // Pre-parse dates to avoid repeated parsing during sort and group
                    val validTransactions = list.map {
                        it to com.sinxn.mymoney.core.util.DateUtils.parseDate(it.date)
                    }

                    val grouped = validTransactions
                        .sortedByDescending { it.second }
                        .groupBy { (_, date) ->
                            val cal = java.util.Calendar.getInstance()
                            cal.time = date
                            cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                            cal.set(java.util.Calendar.MINUTE, 0)
                            cal.set(java.util.Calendar.SECOND, 0)
                            cal.set(java.util.Calendar.MILLISECOND, 0)
                            cal.time
                        }

                    val result = ArrayList<TransactionListItem>(list.size + grouped.size) // Pre-allocate

                    grouped.forEach { (monthDate, transactionsInGroup) ->
                        // Calculate total efficiently
                        var total = 0L
                        val transactionItems = ArrayList<TransactionListItem.Transaction>(transactionsInGroup.size)

                        for ((t, _) in transactionsInGroup) {
                            if (t.countInTotal && t.confirmed) {
                                if (t.direction == 1) total += t.money else total -= t.money
                            }
                            transactionItems.add(TransactionListItem.Transaction(t))
                        }

                        result.add(TransactionListItem.Header(monthDate, total))
                        result.addAll(transactionItems)
                    }
                    result
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
