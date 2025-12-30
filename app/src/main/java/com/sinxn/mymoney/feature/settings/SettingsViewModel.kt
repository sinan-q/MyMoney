package com.sinxn.mymoney.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val formattingSettings: StateFlow<FormattingSettings> = settingsRepository.formattingSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FormattingSettings()
        )

    fun updateShowCurrency(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowCurrency(enabled)
        }
    }

    fun updateGroupDigits(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setGroupDigits(enabled)
        }
    }

    fun updateRoundDecimals(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setRoundDecimals(enabled)
        }
    }

    fun updateShowPlusMinus(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowPlusMinus(enabled)
        }
    }

    fun updateDateFormat(format: Int) {
        viewModelScope.launch {
            settingsRepository.setDateFormat(format)
        }
    }
}
