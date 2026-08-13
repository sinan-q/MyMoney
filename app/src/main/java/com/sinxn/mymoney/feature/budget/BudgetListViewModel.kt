package com.sinxn.mymoney.feature.budget

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.model.BudgetWithDetails
import com.sinxn.mymoney.core.data.repository.BudgetRepository
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

data class BudgetListUiState(
    val filterWalletId: String? = null,
    val budgets: List<BudgetWithDetails> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BudgetListViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val moneyDao: MoneyDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val allWallets: StateFlow<List<WalletWithBalance>> = moneyDao.getWalletsWithBalance(DateUtils.getSQLDateTimeString(java.util.Date()))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val initialWalletId: String? = savedStateHandle.get<String>("walletId")
    private val _walletIdFlow = MutableStateFlow(initialWalletId)

    private val budgetsFlow = _walletIdFlow.flatMapLatest { wId ->
        budgetRepository.getBudgets(walletId = wId)
    }

    val uiState: StateFlow<BudgetListUiState> = combine(
        _walletIdFlow,
        budgetsFlow
    ) { wId, list ->
        BudgetListUiState(
            filterWalletId = wId,
            budgets = list,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetListUiState()
    )

    fun setWalletId(wId: String?) {
        _walletIdFlow.value = wId
    }

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            budgetRepository.deleteBudget(budgetId)
        }
    }
}
