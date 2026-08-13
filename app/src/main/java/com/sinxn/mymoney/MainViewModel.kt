package com.sinxn.mymoney

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.navigation.Screen
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    private val moneyDao: MoneyDao
) : ViewModel() {

    val startDestination: StateFlow<String> = settingsRepository.currentWalletId
        .map { id ->
            val targetId = if (id.isEmpty() || id == Constants.TOTAL_WALLET_ID) Constants.TOTAL_WALLET_ID
                else {
                    val wallet = moneyDao.getWalletById(id)
                    if (wallet != null && !wallet.isDeleted) id else Constants.TOTAL_WALLET_ID
                }
            Screen.Transactions.createRoute(targetId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "loading"
        )

    val currentWalletId: StateFlow<String> = settingsRepository.currentWalletId
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Constants.TOTAL_WALLET_ID
        )

    val formattingSettings: StateFlow<FormattingSettings> = settingsRepository.formattingSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FormattingSettings()
        )
}
