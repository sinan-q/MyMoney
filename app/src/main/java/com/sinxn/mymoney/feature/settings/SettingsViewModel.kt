package com.sinxn.mymoney.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.worker.DailyReminderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context
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

    fun updateHideTime(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHideTime(enabled)
        }
    }

    fun updateDateFormat(format: Int) {
        viewModelScope.launch {
            settingsRepository.setDateFormat(format)
        }
    }

    fun updateIncludeFutureTransactions(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setIncludeFutureTransactions(enabled)
        }
    }

    fun updateExcludeArchivedFromTotal(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setExcludeArchivedFromTotal(enabled)
        }
    }

    fun updateHideStatusAndImpact(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHideStatusAndImpact(enabled)
        }
    }

    fun updateGlobalCurrency(currency: String) {
        viewModelScope.launch {
            settingsRepository.setGlobalCurrency(currency)
        }
    }

    /**
     * REG-04: Schedule or cancel the daily reminder notification.
     * @param hour Hour of day (0-23) to show the reminder, or -1 to disable.
     * Matches legacy DailyBroadcastReceiver.scheduleDailyNotification() behavior.
     */
    fun updateDailyReminderHour(hour: Int) {
        viewModelScope.launch {
            settingsRepository.setDailyReminderHour(hour)
            if (hour == -1) {
                // Cancel the daily reminder
                WorkManager.getInstance(context)
                    .cancelUniqueWork(DailyReminderWorker.WORK_NAME)
            } else {
                // Calculate initial delay to the target hour
                val now = Calendar.getInstance()
                val target = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (before(now)) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
                val initialDelay = target.timeInMillis - now.timeInMillis

                val workRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(
                    24, TimeUnit.HOURS
                )
                    .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                    .build()

                WorkManager.getInstance(context)
                    .enqueueUniquePeriodicWork(
                        DailyReminderWorker.WORK_NAME,
                        ExistingPeriodicWorkPolicy.UPDATE,
                        workRequest
                    )
            }
        }
    }
}

