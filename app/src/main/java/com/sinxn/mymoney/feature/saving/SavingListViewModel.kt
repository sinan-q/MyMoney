package com.sinxn.mymoney.feature.saving

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.model.SavingWithDetails
import com.sinxn.mymoney.core.data.local.model.WalletWithBalance
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.repository.SavingRepository
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

data class SavingListUiState(
    val selectedTab: Int = 0, // 0: IN_PROGRESS, 1: COMPLETED
    val filterWalletId: String? = null,
    val savings: List<SavingWithDetails> = emptyList(),
    val isLoading: Boolean = true
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
        savingsFlow
    ) { tab, wId, list ->
        val actualWId = if (wId == "total") null else wId
        SavingListUiState(
            selectedTab = tab,
            filterWalletId = actualWId,
            savings = list,
            isLoading = false
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
