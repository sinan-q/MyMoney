package com.sinxn.mymoney.feature.wallet

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.model.WalletWithBalance
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.repository.WalletRepository
import com.sinxn.mymoney.core.ui.components.IconData
import com.sinxn.mymoney.core.ui.components.parseIconData
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
import java.util.Collections
import java.util.Date
import javax.inject.Inject

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
    val sortedWalletsForReorder: List<WalletUiModel> = emptyList(),
    val rawWalletsForReorder: List<WalletWithBalance> = emptyList(),
    val totalBalance: Long = 0L,
    val totalBreakdown: String? = null,
    val isTotalValid: Boolean = true,
    val globalCurrency: String = "USD",
    val globalCurrencySymbol: String = "$",
    val globalCurrencyDecimals: Int = 2,
    val isSortMode: Boolean = false,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val formattingSettings: FormattingSettings = FormattingSettings()
)

@HiltViewModel
class WalletListViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _isSortMode = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")
    private val _reorderList = MutableStateFlow<List<WalletWithBalance>>(emptyList())

    val uiState: StateFlow<WalletListUiState> = combine(
        walletRepository.getWalletsWithBalance(DateUtils.getSQLDateTimeString(Date())),
        settingsRepository.formattingSettings,
        _isSortMode,
        _searchQuery,
        _reorderList
    ) { allWallets, settings, isSortMode, searchQuery, reorderList ->
        val globalCurr = settings.globalCurrency.ifEmpty { "USD" }
        val currInstance = try {
            java.util.Currency.getInstance(globalCurr)
        } catch (_: Exception) {
            null
        }

        // Active wallets contributing to total
        val walletsInTotal = allWallets.filter {
            it.wallet.countInTotal && (!settings.excludeArchivedFromTotal || !it.wallet.isArchived)
        }
        val totalBalance = walletsInTotal.sumOf { it.currentBalance }

        val distinctCurrencies = walletsInTotal.map { it.wallet.currency }.distinct()
        val isTotalValid = distinctCurrencies.size <= 1 && (distinctCurrencies.isEmpty() || distinctCurrencies.first() == globalCurr)

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

        val filteredWallets = if (searchQuery.isBlank()) {
            allWallets
        } else {
            allWallets.filter { item ->
                item.wallet.name.contains(searchQuery, ignoreCase = true) ||
                item.wallet.currency.contains(searchQuery, ignoreCase = true) ||
                (!item.wallet.note.isNullOrBlank() && item.wallet.note.contains(searchQuery, ignoreCase = true))
            }
        }

        val (active, archived) = filteredWallets.partition { !it.wallet.isArchived }

        val effectiveReorderList = if (reorderList.isNotEmpty() && reorderList.size == allWallets.size) {
            reorderList
        } else {
            allWallets
        }

        WalletListUiState(
            activeWallets = active.map { it.toUi() },
            archivedWallets = archived.map { it.toUi() },
            sortedWalletsForReorder = effectiveReorderList.map { it.toUi() },
            rawWalletsForReorder = effectiveReorderList,
            totalBalance = totalBalance,
            totalBreakdown = breakdown,
            isTotalValid = isTotalValid,
            globalCurrency = globalCurr,
            globalCurrencySymbol = currInstance?.symbol ?: globalCurr,
            globalCurrencyDecimals = currInstance?.defaultFractionDigits ?: 2,
            isSortMode = isSortMode,
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

    fun toggleSortMode() {
        val current = _isSortMode.value
        if (!current) {
            // entering sort mode -> initialize reorder list with current all wallets
            val currentWallets = uiState.value.rawWalletsForReorder
            _reorderList.value = currentWallets.sortedBy { it.wallet.index }
        } else {
            // exiting sort mode -> save changes
            saveReorderedWallets()
        }
        _isSortMode.value = !current
    }

    fun moveWallet(fromPosition: Int, toPosition: Int) {
        val currentList = _reorderList.value.toMutableList()
        if (fromPosition in currentList.indices && toPosition in currentList.indices) {
            Collections.swap(currentList, fromPosition, toPosition)
            _reorderList.value = currentList
        }
    }

    fun moveWalletUp(index: Int) {
        if (index > 0) {
            moveWallet(index, index - 1)
        }
    }

    fun moveWalletDown(index: Int) {
        val currentList = _reorderList.value
        if (index < currentList.size - 1) {
            moveWallet(index, index + 1)
        }
    }

    fun saveReorderedWallets() {
        val list = _reorderList.value
        if (list.isNotEmpty()) {
            viewModelScope.launch {
                val walletIds = list.map { it.wallet.id }
                walletRepository.reorderWallets(walletIds)
            }
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
