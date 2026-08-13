package com.sinxn.mymoney

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import com.sinxn.mymoney.core.util.DateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val moneyDao: com.sinxn.mymoney.core.data.local.dao.MoneyDao
) : ViewModel() {

    val startDestination: StateFlow<String> = settingsRepository.currentWalletId
        .map { id ->
            if (id.isEmpty()) {
                "wallet_details/${com.sinxn.mymoney.core.util.Constants.TOTAL_WALLET_ID}"
            } else if (id == com.sinxn.mymoney.core.util.Constants.TOTAL_WALLET_ID) {
                "wallet_details/$id"
            } else {
                // Check if wallet exists
                val wallet = moneyDao.getWalletById(id)
                if (wallet != null && !wallet.isDeleted) {
                    "wallet_details/$id"
                } else {
                    "wallet_details/${com.sinxn.mymoney.core.util.Constants.TOTAL_WALLET_ID}"
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "loading"
        )
        
    val allWallets: StateFlow<List<com.sinxn.mymoney.core.data.local.model.WalletWithBalance>> = moneyDao.getWalletsWithBalance(DateUtils.getSQLDateTimeString(java.util.Date()))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val formattingSettings: StateFlow<com.sinxn.mymoney.core.data.preferences.FormattingSettings> = settingsRepository.formattingSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = com.sinxn.mymoney.core.data.preferences.FormattingSettings()
        )
}
