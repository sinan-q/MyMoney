package com.sinxn.mymoney.feature.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.entity.WalletEntity
import com.sinxn.mymoney.core.data.local.model.WalletWithBalance
import com.sinxn.mymoney.core.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val activeWallets: List<WalletWithBalance> = emptyList(),
    val archivedWallets: List<WalletWithBalance> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    moneyDao: MoneyDao
) : ViewModel() {

    val uiState = moneyDao.getWalletsWithBalance()
        .onEach { Log.d("HomeViewModel", "Wallets emitted: ${it.size}") }
        .map { list ->
            // Calculate Total
            val totalBalance = list.filter { it.wallet.countInTotal && !it.wallet.isArchived }
                .sumOf { it.currentBalance }
            
            val totalWallet = com.sinxn.mymoney.core.data.local.model.WalletWithBalance(
                wallet = WalletEntity(
                    id = Constants.TOTAL_WALLET_ID,
                    name = "Total",
                    icon = "sigma", // Icon name?
                    currency = list.firstOrNull()?.wallet?.currency ?: "USD", // Use first wallet's currency or default
                    startMoney = 0,
                    isArchived = false,
                    note = null,
                    countInTotal = false, // Doesn't matter for this fake wallet
                    index = -1,
                    isDeleted = false,
                    lastEdit = 0,
                    tag = null
                ),
                currentBalance = totalBalance,
                decimals = 2,
                currencySymbol = list.firstOrNull()?.currencySymbol ?: "$"
            )

            HomeUiState(
                activeWallets = listOf(totalWallet) + list.filter { !it.wallet.isArchived },
                archivedWallets = list.filter { it.wallet.isArchived }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState()
        )
}
