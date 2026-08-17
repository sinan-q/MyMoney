package com.sinxn.mymoney.feature.budget

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.model.BudgetWithDetails
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.repository.BudgetRepository
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class BudgetDetailsUiState(
    val budgetId: String = "",
    val isLoading: Boolean = true,
    val budgetWithDetails: BudgetWithDetails? = null,
    val transactions: List<TransactionWithCategory> = emptyList(),
    val daysRemaining: Int = 0,
    val dailyAllowance: Long = 0L,
    val currencyCode: String = "USD",
    val currencyDecimals: Int = 2,
    val formatterConfig: MoneyFormatter.Config = MoneyFormatter.Config(),
    val dateFormat: Int = 0,
    val errorMessage: String? = null
)

sealed interface BudgetDetailsEvent {
    object Deleted : BudgetDetailsEvent
}

@HiltViewModel
class BudgetDetailsViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val budgetId: String = savedStateHandle.get<String>("budgetId") ?: ""

    private val _uiState = MutableStateFlow(BudgetDetailsUiState(budgetId = budgetId))
    val uiState: StateFlow<BudgetDetailsUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<BudgetDetailsEvent>()
    val eventFlow: SharedFlow<BudgetDetailsEvent> = _eventFlow.asSharedFlow()

    init {
        if (budgetId.isNotBlank() && budgetId != "new") {
            loadData()
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                budgetRepository.getBudgetDetails(budgetId),
                budgetRepository.getTransactionsForBudget(budgetId),
                settingsRepository.formattingSettings
            ) { details, transactions, formatting ->
                Triple(details, transactions, formatting)
            }.collect { (details, transactions, formatting) ->
                if (details != null) {
                    val targetAmount = details.budget.money
                    val spentAmount = details.progress

                    val endDate = DateUtils.parseDate(details.budget.endDate)
                    val calToday = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val calEnd = Calendar.getInstance().apply {
                        time = endDate
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }

                    val diffMs = calEnd.timeInMillis - calToday.timeInMillis
                    val daysRemaining = if (diffMs < 0) 0 else ((diffMs / (24 * 60 * 60 * 1000L)) + 1).toInt()

                    val remainingBudget = (targetAmount - spentAmount).coerceAtLeast(0L)
                    val dailyAllowance = if (daysRemaining > 0) remainingBudget / daysRemaining else 0L

                    val txCurrencies = transactions.mapNotNull { it.currencySymbol ?: it.currencyCode }.distinct()
                    val displayCurrency = details.budget.currency.ifBlank {
                        if (txCurrencies.size == 1) txCurrencies.first() else formatting.globalCurrency
                    }
                    val displayDecimals = transactions.firstOrNull()?.decimals ?: 2

                    val formatterConfig = MoneyFormatter.Config(
                        showCurrency = formatting.showCurrency,
                        groupDigits = formatting.groupDigits,
                        roundDecimals = formatting.roundDecimals,
                        showPlusMinus = formatting.showPlusMinus
                    )

                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            budgetWithDetails = details,
                            transactions = transactions,
                            daysRemaining = daysRemaining,
                            dailyAllowance = dailyAllowance,
                            currencyCode = displayCurrency,
                            currencyDecimals = displayDecimals,
                            formatterConfig = formatterConfig,
                            dateFormat = formatting.dateFormat
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun deleteBudget(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            if (budgetId.isNotBlank() && budgetId != "new") {
                budgetRepository.deleteBudget(budgetId)
                _eventFlow.emit(BudgetDetailsEvent.Deleted)
                onSuccess()
            }
        }
    }
}
