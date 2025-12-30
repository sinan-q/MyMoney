package com.sinxn.mymoney.feature.wallet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.entity.TransactionEntity
import com.sinxn.mymoney.core.data.local.model.WalletWithBalance
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
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

    val transactions: Flow<List<TransactionEntity>> = moneyDao.getTransactionsForWallet(walletId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
