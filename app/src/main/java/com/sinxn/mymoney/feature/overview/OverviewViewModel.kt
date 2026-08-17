package com.sinxn.mymoney.feature.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.model.WalletWithBalance
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.util.Constants
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class OverviewUiState(
    val overviewData: OverviewData? = null,
    val settings: OverviewSettings? = null,
    val walletName: String = "Total",
    val currencyCode: String = "USD",
    val currencySymbol: String = "$",
    val currencyDecimals: Int = 2,
    val formattingSettings: FormattingSettings = FormattingSettings(),
    val isLoading: Boolean = true,
    val showSettingsSheet: Boolean = false
)

@HiltViewModel
class OverviewViewModel @Inject constructor(
    private val overviewRepository: OverviewRepository,
    private val settingsRepository: SettingsRepository,
    private val moneyDao: MoneyDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(OverviewUiState())
    val uiState: StateFlow<OverviewUiState> = _uiState.asStateFlow()

    val allWallets: StateFlow<List<WalletWithBalance>> = moneyDao
        .getWalletsWithBalance(DateUtils.getSQLDateTimeString(Date()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var currentWalletId: String = Constants.TOTAL_WALLET_ID

    init {
        // Observe current wallet and formatting settings, reload on changes
        viewModelScope.launch {
            combine(
                settingsRepository.currentWalletId,
                settingsRepository.formattingSettings
            ) { walletId, formatting -> Pair(walletId, formatting) }
                .collect { (walletId, formatting) ->
                    val effectiveWalletId = walletId.ifEmpty { Constants.TOTAL_WALLET_ID }
                    currentWalletId = effectiveWalletId

                    // Resolve wallet info
                    val (walletName, currCode, currSymbol, decimals) = resolveWalletInfo(
                        effectiveWalletId, formatting
                    )

                    _uiState.update {
                        it.copy(
                            walletName = walletName,
                            currencyCode = currCode,
                            currencySymbol = currSymbol,
                            currencyDecimals = decimals,
                            formattingSettings = formatting
                        )
                    }

                    // Load data with current or default settings
                    val settings = _uiState.value.settings
                        ?: overviewRepository.getDefaultSettings()
                    loadData(effectiveWalletId, settings)
                }
        }
    }

    fun updateSettings(newSettings: OverviewSettings) {
        _uiState.update { it.copy(settings = newSettings, showSettingsSheet = false) }
        viewModelScope.launch {
            loadData(currentWalletId, newSettings)
        }
    }

    fun setGroupType(groupType: GroupType) {
        viewModelScope.launch {
            val newSettings = overviewRepository.getDefaultSettings(groupType)
            updateSettings(newSettings)
        }
    }

    fun setCashFlowFilter(filter: CashFlowFilter) {
        val current = _uiState.value.settings ?: return
        updateSettings(current.copy(cashFlowFilter = filter))
    }

    fun setOverviewType(type: OverviewType) {
        val current = _uiState.value.settings ?: return
        updateSettings(current.copy(overviewType = type))
    }

    fun setCategoryFilter(categoryId: String?) {
        val current = _uiState.value.settings ?: return
        updateSettings(current.copy(overviewType = OverviewType.CATEGORY, categoryId = categoryId))
    }

    fun setDateRange(startDate: Date, endDate: Date) {
        val current = _uiState.value.settings ?: return
        updateSettings(current.copy(startDate = startDate, endDate = endDate))
    }

    fun toggleSettingsSheet() {
        _uiState.update { it.copy(showSettingsSheet = !it.showSettingsSheet) }
    }

    fun dismissSettingsSheet() {
        _uiState.update { it.copy(showSettingsSheet = false) }
    }

    private suspend fun loadData(walletId: String, settings: OverviewSettings) {
        _uiState.update { it.copy(isLoading = true) }
        try {
            val data = overviewRepository.loadOverviewData(walletId, settings)
            _uiState.update {
                it.copy(
                    overviewData = data,
                    settings = settings,
                    isLoading = false
                )
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun resolveWalletInfo(
        walletId: String,
        formatting: FormattingSettings
    ): WalletInfo {
        return if (walletId == Constants.TOTAL_WALLET_ID) {
            val currCode = formatting.globalCurrency
            val currency = try {
                java.util.Currency.getInstance(currCode)
            } catch (e: Exception) { null }
            WalletInfo(
                name = "Total",
                currencyCode = currCode,
                currencySymbol = currency?.symbol ?: currCode,
                decimals = currency?.defaultFractionDigits ?: 2
            )
        } else {
            val wallet = moneyDao.getWalletById(walletId)
            if (wallet != null) {
                val currSymbol = MoneyFormatter.getCurrencySymbol(wallet.currency)
                val decimals = try {
                    java.util.Currency.getInstance(wallet.currency).defaultFractionDigits
                } catch (e: Exception) { 2 }
                WalletInfo(
                    name = wallet.name,
                    currencyCode = wallet.currency,
                    currencySymbol = currSymbol,
                    decimals = decimals
                )
            } else {
                WalletInfo("Total", formatting.globalCurrency, formatting.globalCurrency, 2)
            }
        }
    }

    private data class WalletInfo(
        val name: String,
        val currencyCode: String,
        val currencySymbol: String,
        val decimals: Int
    )
}
