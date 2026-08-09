package com.sinxn.mymoney.feature.budget

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.data.local.entity.WalletEntity
import com.sinxn.mymoney.core.data.local.model.BudgetWithDetails
import com.sinxn.mymoney.core.data.repository.BudgetRepository
import com.sinxn.mymoney.core.util.BudgetType
import com.sinxn.mymoney.core.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

data class BudgetDetailsUiState(
    val budgetId: String = "new",
    val isEditing: Boolean = false,
    val type: Int = 0, // 0: Expenses, 1: Incomes, 2: Category
    val categoryId: String? = null,
    val startDate: String = DateUtils.getSQLDateTimeString(Date()),
    val endDate: String = DateUtils.getSQLDateTimeString(DateUtils.addMonths(Calendar.getInstance(), 1)),
    val moneyInput: String = "0",
    val currency: String = "USD",
    val selectedWalletIds: Set<String> = emptySet(),
    val availableWallets: List<WalletEntity> = emptyList(),
    val availableCategories: List<CategoryEntity> = emptyList(),
    val budgetWithDetails: BudgetWithDetails? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

sealed interface BudgetDetailsEvent {
    object Saved : BudgetDetailsEvent
    object Deleted : BudgetDetailsEvent
}

@HiltViewModel
class BudgetDetailsViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val moneyDao: MoneyDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val budgetId: String = savedStateHandle.get<String>("budgetId") ?: "new"

    private val _uiState = MutableStateFlow(BudgetDetailsUiState(budgetId = budgetId, isEditing = budgetId != "new"))
    val uiState: StateFlow<BudgetDetailsUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<BudgetDetailsEvent>()
    val eventFlow: SharedFlow<BudgetDetailsEvent> = _eventFlow.asSharedFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val wallets = moneyDao.getWallets().firstOrNull() ?: emptyList()
            val categories = moneyDao.getCategories().firstOrNull() ?: emptyList()

            if (budgetId != "new") {
                budgetRepository.getBudgetDetails(budgetId).collect { details ->
                    if (details != null) {
                        _uiState.update { state ->
                            state.copy(
                                type = details.budget.type,
                                categoryId = details.budget.categoryId,
                                startDate = details.budget.startDate,
                                endDate = details.budget.endDate,
                                moneyInput = (details.budget.money / 100.0).toString(),
                                currency = details.budget.currency,
                                selectedWalletIds = details.wallets.map { it.id }.toSet(),
                                availableWallets = wallets,
                                availableCategories = categories,
                                budgetWithDetails = details,
                                isLoading = false
                            )
                        }
                    }
                }
            } else {
                val cal = Calendar.getInstance()
                val now = DateUtils.getSQLDateTimeString(cal.time)
                cal.add(Calendar.MONTH, 1)
                val oneMonthLater = DateUtils.getSQLDateTimeString(cal.time)

                val defaultWallet = wallets.firstOrNull { it.countInTotal } ?: wallets.firstOrNull()
                val defaultCurrency = defaultWallet?.currency ?: "USD"
                val defaultWalletIds = if (defaultWallet != null) setOf(defaultWallet.id) else emptySet()

                _uiState.update { state ->
                    state.copy(
                        startDate = now,
                        endDate = oneMonthLater,
                        currency = defaultCurrency,
                        selectedWalletIds = defaultWalletIds,
                        availableWallets = wallets,
                        availableCategories = categories,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun setType(type: Int) {
        _uiState.update { it.copy(type = type) }
    }

    fun setCategory(categoryId: String?) {
        _uiState.update { it.copy(categoryId = categoryId) }
    }

    fun setDates(startDate: String, endDate: String) {
        _uiState.update { it.copy(startDate = startDate, endDate = endDate) }
    }

    fun setMoneyInput(input: String) {
        _uiState.update { it.copy(moneyInput = input) }
    }

    fun toggleWalletSelection(walletId: String) {
        _uiState.update { state ->
            val current = state.selectedWalletIds.toMutableSet()
            if (current.contains(walletId)) {
                if (current.size > 1) {
                    current.remove(walletId)
                }
            } else {
                current.add(walletId)
            }
            val firstSelectedWallet = state.availableWallets.firstOrNull { current.contains(it.id) }
            val newCurrency = firstSelectedWallet?.currency ?: state.currency
            state.copy(selectedWalletIds = current, currency = newCurrency, errorMessage = null)
        }
    }

    fun saveBudget() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val walletIds = currentState.selectedWalletIds.toList()

            if (walletIds.isEmpty()) {
                _uiState.update { it.copy(errorMessage = "Please select at least one wallet.") }
                return@launch
            }

            if (currentState.type == BudgetType.CATEGORY && currentState.categoryId.isNullOrBlank()) {
                _uiState.update { it.copy(errorMessage = "Please select a category for Category Budget.") }
                return@launch
            }

            val moneyAmount = (currentState.moneyInput.toDoubleOrNull() ?: 0.0) * 100.0
            if (moneyAmount <= 0) {
                _uiState.update { it.copy(errorMessage = "Please enter a valid budget amount.") }
                return@launch
            }

            val result = budgetRepository.saveBudget(
                id = if (budgetId != "new") budgetId else null,
                type = currentState.type,
                categoryId = currentState.categoryId,
                startDate = currentState.startDate,
                endDate = currentState.endDate,
                money = moneyAmount.toLong(),
                currency = currentState.currency,
                tag = null,
                walletIds = walletIds
            )

            result.fold(
                onSuccess = {
                    _eventFlow.emit(BudgetDetailsEvent.Saved)
                },
                onFailure = { err ->
                    _uiState.update { it.copy(errorMessage = err.message ?: "Failed to save budget") }
                }
            )
        }
    }

    fun deleteBudget() {
        viewModelScope.launch {
            if (budgetId != "new") {
                budgetRepository.deleteBudget(budgetId)
                _eventFlow.emit(BudgetDetailsEvent.Deleted)
            }
        }
    }
}
