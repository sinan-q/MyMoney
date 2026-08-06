package com.sinxn.mymoney.core.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class FormattingSettings(
    val showCurrency: Boolean = true,
    val groupDigits: Boolean = true,
    val roundDecimals: Boolean = false,
    val showPlusMinus: Boolean = false,
    val hideTime: Boolean = false,
    val dateFormat: Int = 2,
    val firstDayOfWeek: Int = java.util.Calendar.getInstance().firstDayOfWeek,
    val firstDayOfMonth: Int = 1,
    val includeFutureTransactions: Boolean = false,
    val excludeArchivedFromTotal: Boolean = false,
    val hideStatusAndImpact: Boolean = false,
    val globalCurrency: String = "USD"
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val CURRENT_WALLET_ID = stringPreferencesKey("current_wallet_id")
        
        val SHOW_CURRENCY = booleanPreferencesKey("show_currency")
        val GROUP_DIGITS = booleanPreferencesKey("group_digits")
        val ROUND_DECIMALS = booleanPreferencesKey("round_decimals")
        val SHOW_PLUS_MINUS = booleanPreferencesKey("show_plus_minus_symbol")
        val HIDE_TIME = booleanPreferencesKey("hide_time")
        val DATE_FORMAT = intPreferencesKey("date_format")
        val FIRST_DAY_OF_WEEK = intPreferencesKey("first_day_of_week")
        val FIRST_DAY_OF_MONTH = intPreferencesKey("first_day_of_month")
        val INCLUDE_FUTURE_TRANSACTIONS = booleanPreferencesKey("include_future_transactions")
        val EXCLUDE_ARCHIVED_FROM_TOTAL = booleanPreferencesKey("exclude_archived_from_total")
        val HIDE_STATUS_AND_IMPACT = booleanPreferencesKey("hide_status_and_impact")
        val GLOBAL_CURRENCY = stringPreferencesKey("global_currency")
    }

    val currentWalletId: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[CURRENT_WALLET_ID] ?: ""
        }

    val formattingSettings: Flow<FormattingSettings> = dataStore.data
        .map { preferences ->
            FormattingSettings(
                showCurrency = preferences[SHOW_CURRENCY] ?: true,
                groupDigits = preferences[GROUP_DIGITS] ?: true,
                roundDecimals = preferences[ROUND_DECIMALS] ?: false,
                showPlusMinus = preferences[SHOW_PLUS_MINUS] ?: false,
                hideTime = preferences[HIDE_TIME] ?: false,
                dateFormat = preferences[DATE_FORMAT] ?: 2,
                firstDayOfWeek = preferences[FIRST_DAY_OF_WEEK] ?: java.util.Calendar.getInstance().firstDayOfWeek,
                firstDayOfMonth = preferences[FIRST_DAY_OF_MONTH] ?: 1,
                includeFutureTransactions = preferences[INCLUDE_FUTURE_TRANSACTIONS] ?: false,
                excludeArchivedFromTotal = preferences[EXCLUDE_ARCHIVED_FROM_TOTAL] ?: false,
                hideStatusAndImpact = preferences[HIDE_STATUS_AND_IMPACT] ?: false,
                globalCurrency = preferences[GLOBAL_CURRENCY] ?: "USD"
            )
        }

    suspend fun setCurrentWalletId(id: String) {
        dataStore.edit { preferences ->
            preferences[CURRENT_WALLET_ID] = id
        }
    }

    suspend fun setShowCurrency(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SHOW_CURRENCY] = enabled
        }
    }

    suspend fun setGroupDigits(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[GROUP_DIGITS] = enabled
        }
    }

    suspend fun setRoundDecimals(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[ROUND_DECIMALS] = enabled
        }
    }

    suspend fun setShowPlusMinus(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SHOW_PLUS_MINUS] = enabled
        }
    }

    suspend fun setHideTime(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[HIDE_TIME] = enabled
        }
    }

    suspend fun setDateFormat(format: Int) {
        dataStore.edit { preferences ->
            preferences[DATE_FORMAT] = format
        }
    }

    suspend fun setFirstDayOfWeek(day: Int) {
        dataStore.edit { preferences ->
            preferences[FIRST_DAY_OF_WEEK] = day
        }
    }

    suspend fun setFirstDayOfMonth(day: Int) {
        dataStore.edit { preferences ->
            preferences[FIRST_DAY_OF_MONTH] = day
        }
    }

    suspend fun setIncludeFutureTransactions(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[INCLUDE_FUTURE_TRANSACTIONS] = enabled
        }
    }

    suspend fun setExcludeArchivedFromTotal(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[EXCLUDE_ARCHIVED_FROM_TOTAL] = enabled
        }
    }

    suspend fun setHideStatusAndImpact(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[HIDE_STATUS_AND_IMPACT] = enabled
        }
    }

    suspend fun setGlobalCurrency(currency: String) {
        dataStore.edit { preferences ->
            preferences[GLOBAL_CURRENCY] = currency
        }
    }
}
