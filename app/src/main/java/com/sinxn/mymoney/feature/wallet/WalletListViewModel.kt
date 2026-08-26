package com.sinxn.mymoney.feature.wallet

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.model.WalletWithBalance
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.repository.WalletRepository
import com.sinxn.mymoney.core.ui.components.IconData
import com.sinxn.mymoney.core.ui.components.SortOption
import com.sinxn.mymoney.core.ui.components.parseIconData
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

enum class WalletSortOption(override val title: String) : SortOption {
    CUSTOM("Custom"),
    LAST_EDIT("Recently Edited"),
    ALPHABETICAL("Alphabetical (A-Z)")
}

@Immutable
data class WalletUiModel(
    val id: String,
    val name: String,
    val currency: String,
    val currentBalance: Long,
    val formattedBalance: String,
    val formattedStartMoney: String?,
    val isNegativeBalance: Boolean,
    val iconData: IconData,
    val isExcludedFromTotal: Boolean,
    val note: String?,
    val rawItem: WalletWithBalance
)

data class WalletListUiState(
    val activeWallets: List<WalletUiModel> = emptyList(),
    val archivedWallets: List<WalletUiModel> = emptyList(),
    val totalBalance: Long = 0L,
    val totalBreakdown: String? = null,
    val isTotalValid: Boolean = true,
    val globalCurrency: String = "USD",
    val globalCurrencySymbol: String = "$",
    val globalCurrencyDecimals: Int = 2,
    val sortOption: WalletSortOption = WalletSortOption.CUSTOM,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val formattingSettings: FormattingSettings = FormattingSettings()
)

@HiltViewModel
class WalletListViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<WalletListUiState> = combine(
        walletRepository.getWalletsWithBalance(DateUtils.getSQLDateTimeString(Date())),
        settingsRepository.formattingSettings,
        settingsRepository.walletsSortOption,
        _searchQuery
    ) { allWallets, settings, sortOptionName, searchQuery ->
        val sortOption = try {
            WalletSortOption.valueOf(sortOptionName)
        } catch (e: Exception) {
            WalletSortOption.CUSTOM
        }

        val globalCurr = settings.globalCurrency.ifEmpty { "USD" }

        // Active wallets contributing to total
        val walletsInTotal = allWallets.filter {
            it.wallet.countInTotal && (!settings.excludeArchivedFromTotal || !it.wallet.isArchived)
        }
        val totalBalance = walletsInTotal.sumOf { it.currentBalance }

        val distinctCurrencies = walletsInTotal
            .map { it.wallet.currency.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        val isTotalValid = distinctCurrencies.size <= 1
        val effectiveCurrency = if (distinctCurrencies.size == 1) {
            distinctCurrencies.first()
        } else {
            globalCurr
        }
        val effectiveCurrInstance = try {
            java.util.Currency.getInstance(effectiveCurrency)
        } catch (_: Exception) {
            null
        }
        val effectiveCurrencySymbol = if (distinctCurrencies.size == 1) {
            walletsInTotal.firstOrNull()?.currencySymbol ?: (effectiveCurrInstance?.symbol ?: effectiveCurrency)
        } else {
            effectiveCurrInstance?.symbol ?: effectiveCurrency
        }
        val effectiveCurrencyDecimals = if (distinctCurrencies.size == 1) {
            walletsInTotal.firstOrNull()?.decimals ?: (effectiveCurrInstance?.defaultFractionDigits ?: 2)
        } else {
            effectiveCurrInstance?.defaultFractionDigits ?: 2
        }

        val breakdown = if (!isTotalValid && walletsInTotal.isNotEmpty()) {
            walletsInTotal
                .groupBy { it.wallet.currency }
                .map { (currency, group) ->
                    val sum = group.sumOf { it.currentBalance }
                    val groupDecimals = group.firstOrNull()?.decimals ?: 2
                    MoneyFormatter.format(
                        amount = sum,
                        currencyCode = currency,
                        decimals = groupDecimals
                    )
                }.joinToString(", ")
        } else null

        val formatterConfig = MoneyFormatter.Config(
            showCurrency = settings.showCurrency,
            groupDigits = settings.groupDigits,
            roundDecimals = settings.roundDecimals,
            showPlusMinus = settings.showPlusMinus
        )

        fun WalletWithBalance.toUi(): WalletUiModel {
            val currencyCode = currencySymbol ?: wallet.currency
            val formattedBalance = MoneyFormatter.format(
                amount = currentBalance,
                currencyCode = currencyCode,
                decimals = decimals,
                config = formatterConfig
            )
            val formattedStartMoney = if (wallet.countInTotal && wallet.note.isNullOrBlank()) {
                val startFormatted = MoneyFormatter.format(
                    amount = wallet.startMoney,
                    currencyCode = currencyCode,
                    decimals = decimals,
                    config = formatterConfig
                )
                "Start: $startFormatted"
            } else null
            val isNegativeBalance = currentBalance < 0
            val iconData = parseIconData(wallet.icon, wallet.name)
            val note = wallet.note?.takeIf { it.isNotBlank() }

            return WalletUiModel(
                id = wallet.id,
                name = wallet.name,
                currency = wallet.currency,
                currentBalance = currentBalance,
                formattedBalance = formattedBalance,
                formattedStartMoney = formattedStartMoney,
                isNegativeBalance = isNegativeBalance,
                iconData = iconData,
                isExcludedFromTotal = !wallet.countInTotal,
                note = note,
                rawItem = this
            )
        }

        val sortedWallets = when (sortOption) {
            WalletSortOption.CUSTOM -> allWallets.sortedBy { it.wallet.index }
            WalletSortOption.LAST_EDIT -> allWallets.sortedByDescending { it.wallet.lastEdit }
            WalletSortOption.ALPHABETICAL -> allWallets.sortedWith(
                compareBy(String.CASE_INSENSITIVE_ORDER) { it.wallet.name }
            )
        }

        val filteredWallets = if (searchQuery.isBlank()) {
            sortedWallets
        } else {
            sortedWallets.filter { item ->
                item.wallet.name.contains(searchQuery, ignoreCase = true) ||
                item.wallet.currency.contains(searchQuery, ignoreCase = true) ||
                (!item.wallet.note.isNullOrBlank() && item.wallet.note.contains(searchQuery, ignoreCase = true))
            }
        }

        val (active, archived) = filteredWallets.partition { !it.wallet.isArchived }

        WalletListUiState(
            activeWallets = active.map { it.toUi() },
            archivedWallets = archived.map { it.toUi() },
            totalBalance = totalBalance,
            totalBreakdown = breakdown,
            isTotalValid = isTotalValid,
            globalCurrency = effectiveCurrency,
            globalCurrencySymbol = effectiveCurrencySymbol,
            globalCurrencyDecimals = effectiveCurrencyDecimals,
            sortOption = sortOption,
            searchQuery = searchQuery,
            isLoading = false,
            formattingSettings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WalletListUiState()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun setSortOption(sortOption: WalletSortOption) {
        viewModelScope.launch {
            settingsRepository.setWalletsSortOption(sortOption.name)
        }
    }

    fun reorderWallets(orderedWalletIds: List<String>) {
        viewModelScope.launch {
            walletRepository.reorderWallets(orderedWalletIds)
        }
    }

    fun toggleArchive(walletId: String) {
        viewModelScope.launch {
            val wallet = walletRepository.getWalletById(walletId) ?: return@launch
            walletRepository.updateWalletArchived(walletId, !wallet.isArchived)
        }
    }

    fun toggleCountInTotal(walletId: String) {
        viewModelScope.launch {
            val wallet = walletRepository.getWalletById(walletId) ?: return@launch
            walletRepository.updateWalletCountInTotal(walletId, !wallet.countInTotal)
        }
    }
}
