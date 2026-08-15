package com.sinxn.mymoney.feature.debt

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.model.DebtWithDetails
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.repository.DebtRepository
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.model.WalletWithBalance
import com.sinxn.mymoney.core.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DebtSummaryStats(
    val totalRemainingMoney: Long = 0L,
    val totalOriginalMoney: Long = 0L,
    val totalPaidMoney: Long = 0L,
    val activeCount: Int = 0,
    val settledCount: Int = 0,
    val currencyCode: String = "USD",
    val currencyDecimals: Int = 2
)

data class DebtListUiState(
    val selectedTab: Int = 0, // 0: DEBT, 1: CREDIT
    val includeArchived: Boolean = false,
    val filterWalletId: String? = null,
    val debts: List<DebtWithDetails> = emptyList(),
    val credits: List<DebtWithDetails> = emptyList(),
    val debtSummary: DebtSummaryStats = DebtSummaryStats(),
    val creditSummary: DebtSummaryStats = DebtSummaryStats(),
    val isLoading: Boolean = false,
    val currencyCode: String = "USD",
    val currencySymbol: String = "$",
    val currencyDecimals: Int = 2,
    val formattingSettings: com.sinxn.mymoney.core.data.preferences.FormattingSettings = com.sinxn.mymoney.core.data.preferences.FormattingSettings()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DebtListViewModel @Inject constructor(
    private val debtRepository: DebtRepository,
    private val settingsRepository: SettingsRepository,
    private val moneyDao: MoneyDao
) : ViewModel() {

    val allWallets: StateFlow<List<WalletWithBalance>> = moneyDao.getWalletsWithBalance(DateUtils.getSQLDateTimeString(java.util.Date()))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedTab = MutableStateFlow(0)
    private val _includeArchived = MutableStateFlow(false)

    private val debtsFlow = combine(_includeArchived, settingsRepository.currentWalletId) { archived, wId ->
        Pair(archived, wId)
    }.flatMapLatest { (archived, wId) ->
        val actualWId = if (wId == "total") null else wId
        debtRepository.getDebts(type = null, includeArchived = archived, walletId = actualWId)
    }

    val uiState: StateFlow<DebtListUiState> = combine(
        _selectedTab,
        _includeArchived,
        settingsRepository.currentWalletId,
        debtsFlow,
        settingsRepository.formattingSettings
    ) { tab, archived, wId, allDebtsList, formatting ->
        val debts = allDebtsList.filter { it.debt.type == 0 }
        val credits = allDebtsList.filter { it.debt.type == 1 }

        fun calculateStats(list: List<DebtWithDetails>): DebtSummaryStats {
            val totalRemaining = list.sumOf { it.remainingMoney }
            val totalOriginal = list.sumOf { kotlin.math.abs(it.debt.money) }
            val totalPaid = list.sumOf { kotlin.math.abs(it.progress) }
            val settled = list.count { it.remainingMoney == 0L && it.debt.money > 0 }
            val active = list.size - settled
            val currCode = if (list.isNotEmpty()) list.first().walletCurrency else formatting.globalCurrency
            val currDecimals = if (list.isNotEmpty()) list.first().walletDecimals else 2
            return DebtSummaryStats(
                totalRemainingMoney = totalRemaining,
                totalOriginalMoney = totalOriginal,
                totalPaidMoney = totalPaid,
                activeCount = active,
                settledCount = settled,
                currencyCode = currCode,
                currencyDecimals = currDecimals
            )
        }

        val debtSummary = calculateStats(debts)
        val creditSummary = calculateStats(credits)
        val actualWId = if (wId == "total") null else wId

        DebtListUiState(
            selectedTab = tab,
            includeArchived = archived,
            filterWalletId = actualWId,
            debts = debts,
            credits = credits,
            debtSummary = debtSummary,
            creditSummary = creditSummary,
            isLoading = false,
            currencyCode = formatting.globalCurrency,
            currencySymbol = "$",
            currencyDecimals = 2,
            formattingSettings = formatting
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DebtListUiState()
    )

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun setIncludeArchived(include: Boolean) {
        _includeArchived.value = include
    }

    fun toggleArchived(debtId: String, currentArchived: Boolean) {
        viewModelScope.launch {
            debtRepository.setDebtArchived(debtId, !currentArchived)
        }
    }

    fun deleteDebt(debtId: String, deleteTransactions: Boolean = true) {
        viewModelScope.launch {
            debtRepository.deleteDebt(debtId, deleteTransactions)
        }
    }
}
