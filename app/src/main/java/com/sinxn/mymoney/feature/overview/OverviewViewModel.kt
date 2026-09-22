package com.sinxn.mymoney.feature.overview

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
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

@Immutable
data class OverviewPeriodUiModel(
    val id: String,
    val index: Int,
    val dateRangeText: String,
    val startDateTimeSql: String,
    val endDateTimeSql: String,
    val formattedIncome: String?,
    val formattedExpense: String?,
    val formattedNetAmount: String,
    val netAmountColorType: Int // 1: positive, -1: negative, 0: neutral
)

@Immutable
data class OverviewUiState(
    val overviewData: OverviewData? = null,
    val periodsUi: List<OverviewPeriodUiModel> = emptyList(),
    val settings: OverviewSettings? = null,
    val currentWalletId: String = Constants.TOTAL_WALLET_ID,
    val walletName: String = "Total",
    val walletIcon: String? = "sigma",
    val currencyCode: String = "USD",
    val currencySymbol: String = "$",
    val currencyDecimals: Int = 2,
    val formattingSettings: FormattingSettings = FormattingSettings(),
    val selectedCategoryName: String? = null,
    val isLoading: Boolean = true,
    val showSettingsSheet: Boolean = false,
    val showWalletPickerSheet: Boolean = false,
    val isDivergingChart: Boolean = false
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

    val allCategories: StateFlow<List<CategoryEntity>> = moneyDao
        .getCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var currentWalletId: String = Constants.TOTAL_WALLET_ID

    init {
        // Observe current wallet, formatting settings, and wallets, reload on changes
        viewModelScope.launch {
            combine(
                settingsRepository.currentWalletId,
                settingsRepository.formattingSettings,
                moneyDao.getWallets()
            ) { walletId, formatting, wallets -> Triple(walletId, formatting, wallets) }
                .collect { (walletId, formatting, wallets) ->
                    val effectiveWalletId = walletId.ifEmpty { Constants.TOTAL_WALLET_ID }
                    currentWalletId = effectiveWalletId

                    // Resolve wallet info
                    val walletInfo = resolveWalletInfo(effectiveWalletId, formatting, wallets)

                    _uiState.update {
                        it.copy(
                            currentWalletId = effectiveWalletId,
                            walletName = walletInfo.name,
                            walletIcon = walletInfo.icon,
                            currencyCode = walletInfo.currencyCode,
                            currencySymbol = walletInfo.currencySymbol,
                            currencyDecimals = walletInfo.decimals,
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



    fun toggleSettingsSheet() {
        _uiState.update { it.copy(showSettingsSheet = !it.showSettingsSheet) }
    }

    fun dismissSettingsSheet() {
        _uiState.update { it.copy(showSettingsSheet = false) }
    }

    fun setDivergingChart(isDiverging: Boolean) {
        _uiState.update { it.copy(isDivergingChart = isDiverging) }
    }

    fun toggleWalletPickerSheet() {
        _uiState.update { it.copy(showWalletPickerSheet = !it.showWalletPickerSheet) }
    }

    fun dismissWalletPickerSheet() {
        _uiState.update { it.copy(showWalletPickerSheet = false) }
    }

    fun selectWallet(walletId: String) {
        dismissWalletPickerSheet()
        viewModelScope.launch {
            settingsRepository.setCurrentWalletId(walletId)
        }
    }

    fun applyOverviewConfig(newSettings: OverviewSettings, newWalletId: String) {
        _uiState.update { it.copy(settings = newSettings, showSettingsSheet = false) }
        viewModelScope.launch {
            if (newWalletId != currentWalletId) {
                settingsRepository.setCurrentWalletId(newWalletId)
            } else {
                loadData(currentWalletId, newSettings)
            }
        }
    }

    private suspend fun loadData(walletId: String, settings: OverviewSettings) {
        _uiState.update { it.copy(isLoading = true) }
        try {
            val data = overviewRepository.loadOverviewData(walletId, settings)
            val currentState = _uiState.value

            val effectiveCurrencyCode = if (walletId == Constants.TOTAL_WALLET_ID) {
                val dataCurrencies = data.totalNetIncomes.getCurrencies()
                if (dataCurrencies.size == 1) {
                    dataCurrencies.first()
                } else if (!dataCurrencies.contains(currentState.currencyCode) && dataCurrencies.isNotEmpty()) {
                    dataCurrencies.first()
                } else {
                    currentState.currencyCode
                }
            } else {
                currentState.currencyCode
            }

            val decimals = try {
                java.util.Currency.getInstance(effectiveCurrencyCode).defaultFractionDigits
            } catch (e: Exception) { currentState.currencyDecimals }
            val symbol = MoneyFormatter.getCurrencySymbol(effectiveCurrencyCode)

            val periodsUi = buildPeriodUiModels(
                periods = data.periods,
                currencyCode = effectiveCurrencyCode,
                decimals = decimals,
                formattingSettings = currentState.formattingSettings
            )
            val categoryName = if (settings.overviewType == OverviewType.CATEGORY && !settings.categoryId.isNullOrEmpty()) {
                moneyDao.getCategoryById(settings.categoryId)?.name ?: "Category"
            } else {
                null
            }
            _uiState.update {
                it.copy(
                    overviewData = data,
                    periodsUi = periodsUi,
                    settings = settings,
                    selectedCategoryName = categoryName,
                    currencyCode = effectiveCurrencyCode,
                    currencySymbol = symbol,
                    currencyDecimals = decimals,
                    isLoading = false
                )
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun buildPeriodUiModels(
        periods: List<PeriodMoney>,
        currencyCode: String,
        decimals: Int,
        formattingSettings: FormattingSettings
    ): List<OverviewPeriodUiModel> {
        val config = MoneyFormatter.Config(
            showCurrency = formattingSettings.showCurrency,
            groupDigits = formattingSettings.groupDigits,
            roundDecimals = formattingSettings.roundDecimals,
            showPlusMinus = true
        )
        val noSignConfig = config.copy(showPlusMinus = false)

        return periods.mapIndexed { index, period ->
            val startStr = DateUtils.formatMonthDay(period.startDate)
            val endStr = DateUtils.formatMonthDay(period.endDate)
            val dateRangeText = "$startStr — $endStr"
            val startDateTimeSql = DateUtils.getSQLDateTimeString(period.startDate)
            val endDateTimeSql = DateUtils.getSQLDateTimeString(period.endDate)

            val incomeAmt = period.incomes.getMoney(currencyCode)
            val expenseAmt = period.expenses.getMoney(currencyCode)

            val formattedIncome = if (incomeAmt > 0) {
                "↑ " + MoneyFormatter.format(incomeAmt, currencyCode, decimals, noSignConfig)
            } else null

            val formattedExpense = if (expenseAmt > 0) {
                "↓ " + MoneyFormatter.format(expenseAmt, currencyCode, decimals, noSignConfig)
            } else null

            val netAmount = period.netIncomes.getMoney(currencyCode)
            val formattedNetAmount = MoneyFormatter.format(netAmount, currencyCode, decimals, config)
            val netColorType = when {
                netAmount > 0 -> 1
                netAmount < 0 -> -1
                else -> 0
            }

            OverviewPeriodUiModel(
                id = "period_${index}_${period.startDate.time}",
                index = index + 1,
                dateRangeText = dateRangeText,
                startDateTimeSql = startDateTimeSql,
                endDateTimeSql = endDateTimeSql,
                formattedIncome = formattedIncome,
                formattedExpense = formattedExpense,
                formattedNetAmount = formattedNetAmount,
                netAmountColorType = netColorType
            )
        }.reversed()
    }

    private fun resolveWalletInfo(
        walletId: String,
        formatting: FormattingSettings,
        wallets: List<com.sinxn.mymoney.core.data.local.entity.WalletEntity>
    ): WalletInfo {
        return if (walletId == Constants.TOTAL_WALLET_ID) {
            val walletsInTotal = wallets.filter {
                it.countInTotal && (!formatting.excludeArchivedFromTotal || !it.isArchived)
            }
            val distinctCurrencies = walletsInTotal
                .map { it.currency.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
            val currCode = if (distinctCurrencies.size == 1) {
                distinctCurrencies.first()
            } else {
                formatting.globalCurrency.ifEmpty { "USD" }
            }
            val currency = try {
                java.util.Currency.getInstance(currCode)
            } catch (e: Exception) { null }
            val symbol = MoneyFormatter.getCurrencySymbol(currCode)
            val decimals = currency?.defaultFractionDigits ?: 2
            WalletInfo(
                name = "Total",
                icon = "sigma",
                currencyCode = currCode,
                currencySymbol = symbol,
                decimals = decimals
            )
        } else {
            val wallet = wallets.find { it.id == walletId }
            if (wallet != null) {
                val currSymbol = MoneyFormatter.getCurrencySymbol(wallet.currency)
                val decimals = try {
                    java.util.Currency.getInstance(wallet.currency).defaultFractionDigits
                } catch (e: Exception) { 2 }
                WalletInfo(
                    name = wallet.name,
                    icon = wallet.icon,
                    currencyCode = wallet.currency,
                    currencySymbol = currSymbol,
                    decimals = decimals
                )
            } else {
                WalletInfo("Total", "sigma", formatting.globalCurrency, formatting.globalCurrency, 2)
            }
        }
    }

    private data class WalletInfo(
        val name: String,
        val icon: String?,
        val currencyCode: String,
        val currencySymbol: String,
        val decimals: Int
    )
}
