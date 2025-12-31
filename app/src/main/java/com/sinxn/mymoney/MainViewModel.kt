package com.sinxn.mymoney

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
                "home"
            } else if (id == com.sinxn.mymoney.core.util.Constants.TOTAL_WALLET_ID) {
                "wallet_details/$id"
            } else {
                // Check if wallet exists
                val wallet = moneyDao.getWalletById(id)
                if (wallet != null && !wallet.isDeleted) {
                    "wallet_details/$id"
                } else {
                    // Invalid or deleted wallet, reset preference and go home
                    // Note: Optimally we should clear the preference here, but doing side effects in map is debatable.
                    // Ideally, we launch a coroutine to clear it, but for now defaulting to home is safe.
                    "home"
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "loading"
        )
}
