package com.sinxn.mymoney.feature.saving

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.model.SavingWithDetails
import com.sinxn.mymoney.core.data.repository.SavingRepository
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
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val initialWalletId: String? = savedStateHandle.get<String>("walletId")
    private val _selectedTab = MutableStateFlow(0)
    private val _walletIdFlow = MutableStateFlow(initialWalletId)

    private val savingsFlow = combine(_selectedTab, _walletIdFlow) { tab, wId ->
        Pair(tab, wId)
    }.flatMapLatest { (tab, wId) ->
        savingRepository.getSavings(walletId = wId, isComplete = (tab == 1))
    }

    val uiState: StateFlow<SavingListUiState> = combine(
        _selectedTab,
        _walletIdFlow,
        savingsFlow
    ) { tab, wId, list ->
        SavingListUiState(
            selectedTab = tab,
            filterWalletId = wId,
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

    fun setWalletId(wId: String?) {
        _walletIdFlow.value = wId
    }

    fun toggleComplete(savingId: String, currentComplete: Boolean) {
        viewModelScope.launch {
            savingRepository.setSavingComplete(savingId, !currentComplete)
        }
    }

    fun deleteSaving(savingId: String) {
        viewModelScope.launch {
            savingRepository.deleteSaving(savingId)
        }
    }
}
