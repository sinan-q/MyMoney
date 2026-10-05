package com.sinxn.mymoney.feature.saving

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.model.SavingWithDetails
import com.sinxn.mymoney.core.data.local.model.WalletWithBalance
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.preferences.toFormatterConfig
import com.sinxn.mymoney.core.data.repository.SavingRepository
import com.sinxn.mymoney.core.ui.components.IconData
import com.sinxn.mymoney.core.ui.components.parseIconData
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MoneyFormatter
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

data class SavingItemUi(
    val id: String,
    val title: String,
    val walletName: String,
    val iconData: IconData,
    val currentAmountFormatted: String,
    val targetAmountFormatted: String,
    val neededAmountFormatted: String,
    val percentage: Double,
    val progressFraction: Float,
    val isGoalReached: Boolean,
    val targetDateLabel: String?,
    val isComplete: Boolean,
    val currentMoney: Long,
    val rawItem: SavingWithDetails
)

data class SavingListUiState(
    val selectedTab: Int = 0, // 0: IN_PROGRESS, 1: COMPLETED
    val filterWalletId: String? = null,
    val savings: List<SavingItemUi> = emptyList(),
    val isLoading: Boolean = true,
    val formatterConfig: MoneyFormatter.Config = MoneyFormatter.Config(),
    val dateFormat: Int = 0,
    val globalCurrency: String = "USD"
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SavingListViewModel @Inject constructor(
    private val savingRepository: SavingRepository,
    private val moneyDao: MoneyDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val allWallets: StateFlow<List<WalletWithBalance>> = moneyDao.getWalletsWithBalance(DateUtils.getSQLDateTimeString(java.util.Date()))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedTab = MutableStateFlow(0)

    private val savingsFlow = combine(_selectedTab, settingsRepository.currentWalletId) { tab, wId ->
        Pair(tab, wId)
    }.flatMapLatest { (tab, wId) ->
        val actualWId = if (wId == "total") null else wId
        savingRepository.getSavings(walletId = actualWId, isComplete = (tab == 1))
    }

    val uiState: StateFlow<SavingListUiState> = combine(
        _selectedTab,
        settingsRepository.currentWalletId,
        savingsFlow,
        settingsRepository.formattingSettings
    ) { tab, wId, list, formatting ->
        val actualWId = if (wId == "total") null else wId
        val formatterConfig = formatting.toFormatterConfig()

        val savingItems = list.map { item ->
            val saving = item.saving
            val targetAmount = saving.endMoney
            val currentAmount = item.currentMoney
            val neededAmount = item.neededMoney
            val currency = item.walletCurrency

            val percentage = if (targetAmount > 0) {
                ((currentAmount.toDouble() / targetAmount.toDouble()) * 100.0)
            } else 0.0

            val progressFraction = (percentage / 100.0).coerceIn(0.0, 1.0).toFloat()
            val isGoalReached = item.isGoalReached || saving.isComplete

            val targetDateLabel = saving.endDate?.let { exp ->
                val parsed = DateUtils.parseDate(exp)
                DateUtils.formatDate(parsed, formatting.dateFormat)
            }

            val iconData = parseIconData(saving.icon.ifBlank { "ic_saving" }, saving.description ?: "Saving Goal")

            val decimals = try {
                java.util.Currency.getInstance(currency).defaultFractionDigits
            } catch (_: Exception) {
                2
            }

            val currentAmountFormatted = MoneyFormatter.format(
                amount = currentAmount,
                currencyCode = currency,
                decimals = decimals,
                config = formatterConfig
            )
            val targetAmountFormatted = MoneyFormatter.format(
                amount = targetAmount,
                currencyCode = currency,
                decimals = decimals,
                config = formatterConfig
            )
            val neededAmountFormatted = MoneyFormatter.format(
                amount = neededAmount,
                currencyCode = currency,
                decimals = decimals,
                config = formatterConfig
            )

            SavingItemUi(
                id = saving.id,
                title = saving.description ?: "Saving Goal",
                walletName = item.walletName,
                iconData = iconData,
                currentAmountFormatted = currentAmountFormatted,
                targetAmountFormatted = targetAmountFormatted,
                neededAmountFormatted = neededAmountFormatted,
                percentage = percentage,
                progressFraction = progressFraction,
                isGoalReached = isGoalReached,
                targetDateLabel = targetDateLabel,
                isComplete = saving.isComplete,
                currentMoney = currentAmount,
                rawItem = item
            )
        }

        SavingListUiState(
            selectedTab = tab,
            filterWalletId = actualWId,
            savings = savingItems,
            isLoading = false,
            formatterConfig = formatterConfig,
            dateFormat = formatting.dateFormat,
            globalCurrency = formatting.globalCurrency
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SavingListUiState()
    )

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun toggleComplete(savingId: String, currentComplete: Boolean) {
        viewModelScope.launch {
            savingRepository.setSavingComplete(savingId, !currentComplete)
        }
    }

    fun deleteSaving(savingId: String, deleteTransactions: Boolean = true) {
        viewModelScope.launch {
            savingRepository.deleteSaving(savingId, deleteTransactions)
        }
    }
}
