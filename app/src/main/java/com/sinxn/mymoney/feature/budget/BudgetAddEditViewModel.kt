package com.sinxn.mymoney.feature.budget

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.data.local.entity.WalletEntity
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.repository.BudgetPeriod
import com.sinxn.mymoney.core.data.repository.BudgetRepository
import com.sinxn.mymoney.core.util.BudgetType
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MoneyFormatter
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
import kotlin.math.roundToLong

data class BudgetAddEditUiState(
    val budgetId: String? = null,
    val isNewBudget: Boolean = true,
    val editType: Int = BudgetType.EXPENSES, // 0: Expenses, 1: Incomes, 2: Category
    val editCategoryId: String? = null,
    val selectedCategory: CategoryEntity? = null,
    val editAmount: String = "",
    val editPeriod: Int = BudgetPeriod.MONTHLY,
    val editStartDate: String = "",
    val editEndDate: String = "",
    val selectedWalletIds: Set<String> = emptySet(),
    val editTag: String = "",
    val availableWallets: List<WalletEntity> = emptyList(),
    val availableCategories: List<CategoryEntity> = emptyList(),
    val expenseCategories: List<CategoryEntity> = emptyList(),
    val incomeCategories: List<CategoryEntity> = emptyList(),
    val currencyCode: String = "USD",
    val currencySymbol: String = "$",
    val currencyDecimals: Int = 2,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val walletWarning: String? = null
)

sealed interface BudgetAddEditEvent {
    object Saved : BudgetAddEditEvent
    object Deleted : BudgetAddEditEvent
}

@HiltViewModel
class BudgetAddEditViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val moneyDao: MoneyDao,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val budgetIdArg: String? = savedStateHandle.get<String>("budgetId")?.takeIf { it.isNotBlank() && it != "new" }

    private val _uiState = MutableStateFlow(
        BudgetAddEditUiState(
            budgetId = budgetIdArg,
            isNewBudget = budgetIdArg == null
        )
    )
    val uiState: StateFlow<BudgetAddEditUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<BudgetAddEditEvent>()
    val eventFlow: SharedFlow<BudgetAddEditEvent> = _eventFlow.asSharedFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val wallets = moneyDao.getWallets().firstOrNull() ?: emptyList()
            val categories = moneyDao.getCategories().firstOrNull() ?: emptyList()
            val formatting = settingsRepository.formattingSettings.firstOrNull()

            val expenseCats = categories.filter { it.type == 0 }
            val incomeCats = categories.filter { it.type == 1 }

            if (budgetIdArg != null) {
                budgetRepository.getBudgetDetails(budgetIdArg).collect { details ->
                    if (details != null) {
                        val budget = details.budget
                        val periodType = BudgetPeriod.fromTag(budget.tag)
                        val cat = categories.firstOrNull { it.id == budget.categoryId }
                        val customTag = budget.tag?.takeIf { !it.startsWith("period::") } ?: ""

                        val firstWallet = details.wallets.firstOrNull()
                        val curr = firstWallet?.currency ?: budget.currency.ifBlank { formatting?.globalCurrency ?: "USD" }
                        val decimals = try {
                            java.util.Currency.getInstance(curr).defaultFractionDigits.coerceAtLeast(0)
                        } catch (e: Exception) {
                            2
                        }

                        val moneyFormatted = if (budget.money > 0) {
                            val divisor = Math.pow(10.0, decimals.toDouble())
                            val doubleVal = budget.money / divisor
                            if (decimals == 0) doubleVal.toLong().toString() else doubleVal.toString()
                        } else ""

                        _uiState.update { state ->
                            state.copy(
                                isNewBudget = false,
                                editType = budget.type,
                                editCategoryId = budget.categoryId,
                                selectedCategory = cat,
                                editAmount = moneyFormatted,
                                editPeriod = periodType,
                                editStartDate = budget.startDate,
                                editEndDate = budget.endDate,
                                selectedWalletIds = details.wallets.map { it.id }.toSet(),
                                editTag = customTag,
                                availableWallets = wallets,
                                availableCategories = categories,
                                expenseCategories = expenseCats,
                                incomeCategories = incomeCats,
                                currencyCode = curr,
                                currencySymbol = MoneyFormatter.getCurrencySymbol(curr),
                                currencyDecimals = decimals,
                                isLoading = false
                            )
                        }
                    }
                }
            } else {
                // New budget defaults
                val currentWalletId = settingsRepository.currentWalletId.firstOrNull()
                val defaultWallet = wallets.find { it.id == currentWalletId }
                    ?: wallets.find { it.countInTotal }
                    ?: wallets.firstOrNull()

                val defaultCurrency = defaultWallet?.currency ?: formatting?.globalCurrency ?: "USD"
                val defaultDecimals = try {
                    java.util.Currency.getInstance(defaultCurrency).defaultFractionDigits.coerceAtLeast(0)
                } catch (e: Exception) {
                    2
                }
                val defaultWalletIds = if (defaultWallet != null) setOf(defaultWallet.id) else emptySet()

                val cal = Calendar.getInstance()
                val (windowStart, windowEnd) = computeWindowForPeriod(BudgetPeriod.MONTHLY, cal)

                _uiState.update { state ->
                    state.copy(
                        isNewBudget = true,
                        editType = BudgetType.EXPENSES,
                        editPeriod = BudgetPeriod.MONTHLY,
                        editStartDate = windowStart,
                        editEndDate = windowEnd,
                        selectedWalletIds = defaultWalletIds,
                        availableWallets = wallets,
                        availableCategories = categories,
                        expenseCategories = expenseCats,
                        incomeCategories = incomeCats,
                        currencyCode = defaultCurrency,
                        currencySymbol = MoneyFormatter.getCurrencySymbol(defaultCurrency),
                        currencyDecimals = defaultDecimals,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun computeWindowForPeriod(period: Int, now: Calendar): Pair<String, String> {
        val cal = Calendar.getInstance().apply { time = now.time }
        return when (period) {
            BudgetPeriod.WEEKLY -> {
                cal.firstDayOfWeek = Calendar.MONDAY
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.time
                cal.add(Calendar.DAY_OF_YEAR, 6)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                val end = cal.time
                Pair(DateUtils.getSQLDateTimeString(start), DateUtils.getSQLDateTimeString(end))
            }
            BudgetPeriod.MONTHLY -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.time
                cal.add(Calendar.MONTH, 1)
                cal.add(Calendar.DAY_OF_YEAR, -1)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                val end = cal.time
                Pair(DateUtils.getSQLDateTimeString(start), DateUtils.getSQLDateTimeString(end))
            }
            BudgetPeriod.ANNUAL -> {
                cal.set(Calendar.MONTH, Calendar.JANUARY)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val start = cal.time
                cal.set(Calendar.MONTH, Calendar.DECEMBER)
                cal.set(Calendar.DAY_OF_MONTH, 31)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                val end = cal.time
                Pair(DateUtils.getSQLDateTimeString(start), DateUtils.getSQLDateTimeString(end))
            }
            else -> {
                val start = cal.time
                cal.add(Calendar.MONTH, 1)
                val end = cal.time
                Pair(DateUtils.getSQLDateTimeString(start), DateUtils.getSQLDateTimeString(end))
            }
        }
    }

    fun onNumpadKeyPress(key: String) {
        val current = _uiState.value.editAmount
        val updated = when (key) {
            "BACKSPACE" -> if (current.isNotEmpty()) current.dropLast(1) else ""
            "CLEAR" -> ""
            else -> current + key
        }
        _uiState.update { it.copy(editAmount = updated) }
    }

    fun evaluateMathExpression() {
        val current = _uiState.value.editAmount
        if (current.isBlank()) return
        try {
            val result = evalSimpleExpression(current)
            if (result != null && result >= 0) {
                val decimals = _uiState.value.currencyDecimals
                val formatted = if (decimals == 0) result.roundToLong().toString()
                else String.format(java.util.Locale.US, "%.${decimals}f", result)
                _uiState.update { it.copy(editAmount = formatted) }
            }
        } catch (_: Exception) {}
    }

    private fun evalSimpleExpression(expr: String): Double? {
        val clean = expr.replace("×", "*").replace("÷", "/").replace("−", "-").trim()
        val tokens = mutableListOf<String>()
        var numBuffer = StringBuilder()

        for (char in clean) {
            if (char in "+-*/") {
                if (numBuffer.isNotEmpty()) {
                    tokens.add(numBuffer.toString())
                    numBuffer = StringBuilder()
                }
                tokens.add(char.toString())
            } else if (char.isDigit() || char == '.') {
                numBuffer.append(char)
            }
        }
        if (numBuffer.isNotEmpty()) {
            tokens.add(numBuffer.toString())
        }

        if (tokens.isEmpty()) return null
        var total = tokens[0].toDoubleOrNull() ?: return null
        var i = 1
        while (i < tokens.size - 1) {
            val op = tokens[i]
            val nextVal = tokens[i + 1].toDoubleOrNull() ?: return null
            when (op) {
                "+" -> total += nextVal
                "-" -> total -= nextVal
                "*" -> total *= nextVal
                "/" -> if (nextVal != 0.0) total /= nextVal
            }
            i += 2
        }
        return total
    }

    fun updateType(type: Int) {
        _uiState.update { state ->
            state.copy(
                editType = type,
                editCategoryId = if (type == BudgetType.CATEGORY) state.editCategoryId else null,
                selectedCategory = if (type == BudgetType.CATEGORY) state.selectedCategory else null
            )
        }
    }

    fun updateCategory(category: CategoryEntity) {
        _uiState.update {
            it.copy(
                editCategoryId = category.id,
                selectedCategory = category,
                errorMessage = null
            )
        }
    }

    fun updatePeriod(period: Int) {
        val (windowStart, windowEnd) = computeWindowForPeriod(period, Calendar.getInstance())
        _uiState.update {
            it.copy(
                editPeriod = period,
                editStartDate = windowStart,
                editEndDate = windowEnd
            )
        }
    }

    fun updateStartDate(dateStr: String) {
        _uiState.update { it.copy(editStartDate = dateStr) }
    }

    fun updateEndDate(dateStr: String) {
        _uiState.update { it.copy(editEndDate = dateStr) }
    }

    fun updateTag(tag: String) {
        _uiState.update { it.copy(editTag = tag) }
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

            // Check currency consistency
            val selectedWallets = state.availableWallets.filter { current.contains(it.id) }
            val distinctCurrencies = selectedWallets.map { it.currency }.distinct()
            val warning = if (distinctCurrencies.size > 1) {
                "Selected wallets use different currencies: ${distinctCurrencies.joinToString(", ")}. All linked wallets must use the same currency."
            } else null

            val firstCurr = selectedWallets.firstOrNull()?.currency ?: state.currencyCode
            state.copy(
                selectedWalletIds = current,
                currencyCode = firstCurr,
                currencySymbol = MoneyFormatter.getCurrencySymbol(firstCurr),
                walletWarning = warning,
                errorMessage = null
            )
        }
    }

    fun saveBudget(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            val walletIds = state.selectedWalletIds.toList()

            if (walletIds.isEmpty()) {
                _uiState.update { it.copy(errorMessage = "Please select at least one wallet.") }
                return@launch
            }

            if (state.walletWarning != null) {
                _uiState.update { it.copy(errorMessage = state.walletWarning) }
                return@launch
            }

            if (state.editType == BudgetType.CATEGORY && state.editCategoryId.isNullOrBlank()) {
                _uiState.update { it.copy(errorMessage = "Please select a category.") }
                return@launch
            }

            evaluateMathExpression()
            val evaluated = evalSimpleExpression(_uiState.value.editAmount) ?: _uiState.value.editAmount.toDoubleOrNull()
            if (evaluated == null || evaluated <= 0) {
                _uiState.update { it.copy(errorMessage = "Please enter a valid budget limit.") }
                return@launch
            }

            val multiplier = Math.pow(10.0, state.currencyDecimals.toDouble())
            val moneyAmountLong = (evaluated * multiplier).roundToLong()

            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            val result = budgetRepository.saveBudget(
                id = if (!state.isNewBudget) state.budgetId else null,
                type = state.editType,
                categoryId = state.editCategoryId,
                startDate = state.editStartDate,
                endDate = state.editEndDate,
                money = moneyAmountLong,
                currency = state.currencyCode,
                tag = state.editTag.takeIf { it.isNotBlank() },
                walletIds = walletIds,
                periodType = state.editPeriod
            )

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false) }
                    _eventFlow.emit(BudgetAddEditEvent.Saved)
                    onSuccess()
                },
                onFailure = { err ->
                    _uiState.update { it.copy(isSaving = false, errorMessage = err.message ?: "Failed to save budget") }
                }
            )
        }
    }

    fun deleteBudget(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val id = _uiState.value.budgetId
            if (id != null) {
                budgetRepository.deleteBudget(id)
                _eventFlow.emit(BudgetAddEditEvent.Deleted)
                onSuccess()
            }
        }
    }
}
