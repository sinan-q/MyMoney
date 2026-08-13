package com.sinxn.mymoney.core.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.model.WalletWithBalance
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.util.Constants
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class WalletHeaderViewModel @Inject constructor(
    private val moneyDao: MoneyDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val allWallets: StateFlow<List<WalletWithBalance>> = moneyDao.getWalletsWithBalance(DateUtils.getSQLDateTimeString(Date()))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val currentWalletId: StateFlow<String> = settingsRepository.currentWalletId
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Constants.TOTAL_WALLET_ID
        )

    val currentWallet: StateFlow<WalletWithBalance?> = combine(allWallets, currentWalletId) { wallets, id ->
        wallets.find { it.wallet.id == id }
            ?: wallets.firstOrNull { it.wallet.id == Constants.TOTAL_WALLET_ID }
            ?: wallets.firstOrNull()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val formatterConfig: StateFlow<MoneyFormatter.Config> = settingsRepository.formattingSettings
        .map { settings ->
            MoneyFormatter.Config(
                showCurrency = settings.showCurrency,
                groupDigits = settings.groupDigits,
                roundDecimals = settings.roundDecimals,
                showPlusMinus = settings.showPlusMinus
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MoneyFormatter.Config()
        )
}
