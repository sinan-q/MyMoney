package com.sinxn.mymoney.feature.budget

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.model.BudgetWithDetails
import com.sinxn.mymoney.core.data.local.model.WalletWithBalance
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.repository.BudgetRepository
import com.sinxn.mymoney.core.ui.components.IconData
import com.sinxn.mymoney.core.ui.components.parseIconData
import com.sinxn.mymoney.core.data.repository.BudgetPeriod
import com.sinxn.mymoney.core.util.BudgetType
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class BudgetUiModel(
    val id: String,
    val title: String,
    val type: Int,
    val iconData: IconData?,
    val periodLabel: String,
    val spentFormatted: String,
    val limitFormatted: String,
    val progressFraction: Float,
    val percentage: Int,
    val isOverBudget: Boolean,
    val walletNames: List<String>
)

@Immutable
data class BudgetListUiState(
    val filterWalletId: String? = null,
    val budgets: List<BudgetUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val formatterConfig: MoneyFormatter.Config = MoneyFormatter.Config(),
    val dateFormat: Int = 0,
    val globalCurrency: String = "USD"
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BudgetListViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val moneyDao: MoneyDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val allWallets: StateFlow<List<WalletWithBalance>> = moneyDao.getWalletsWithBalance(DateUtils.getSQLDateTimeString(java.util.Date()))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val budgetsFlow = settingsRepository.currentWalletId.flatMapLatest { wId ->
        val actualWId = if (wId == "total") null else wId
        budgetRepository.getBudgets(walletId = actualWId)
    }

    val uiState: StateFlow<BudgetListUiState> = combine(
        settingsRepository.currentWalletId,
        budgetsFlow,
        settingsRepository.formattingSettings
    ) { wId, list, formatting ->
        val actualWId = if (wId == "total") null else wId
        val formatterConfig = MoneyFormatter.Config(
            showCurrency = formatting.showCurrency,
            groupDigits = formatting.groupDigits,
            roundDecimals = formatting.roundDecimals,
            showPlusMinus = formatting.showPlusMinus
        )

        val uiModels = list.map { it.toUiModel(formatterConfig, formatting.dateFormat) }

        BudgetListUiState(
            filterWalletId = actualWId,
            budgets = uiModels,
            isLoading = false,
            formatterConfig = formatterConfig,
            dateFormat = formatting.dateFormat,
            globalCurrency = formatting.globalCurrency
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetListUiState()
    )

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            budgetRepository.deleteBudget(budgetId)
        }
    }
}

private fun BudgetWithDetails.toUiModel(
    formatterConfig: MoneyFormatter.Config,
    dateFormat: Int
): BudgetUiModel {
    val targetAmount = budget.money
    val progressAmount = progress
    val currency = budget.currency

    val percentageDouble = if (targetAmount > 0) {
        ((progressAmount.toDouble() / targetAmount.toDouble()) * 100.0)
    } else 0.0

    val progressFraction = (percentageDouble / 100.0).coerceIn(0.0, 1.0).toFloat()
    val isOverBudget = percentageDouble >= 100.0

    val title = when (budget.type) {
        BudgetType.EXPENSES -> "Expenses"
        BudgetType.INCOMES -> "Incomes"
        else -> categoryName ?: "Category"
    }

    val iconData = if (budget.type != BudgetType.EXPENSES && budget.type != BudgetType.INCOMES) {
        parseIconData(categoryIcon ?: "ic_category", categoryName ?: "Category")
    } else null

    val periodType = BudgetPeriod.fromTag(budget.tag)
    val periodName = when (periodType) {
        BudgetPeriod.WEEKLY -> "Weekly"
        BudgetPeriod.MONTHLY -> "Monthly"
        BudgetPeriod.ANNUAL -> "Annual"
        else -> "Custom"
    }
    val startObj = DateUtils.parseDate(budget.startDate)
    val endObj = DateUtils.parseDate(budget.endDate)
    val startStr = DateUtils.formatDate(startObj, dateFormat)
    val endStr = DateUtils.formatDate(endObj, dateFormat)
    val dateWindow = if (startStr == endStr) startStr else "$startStr - $endStr"
    val periodLabel = if (periodType == BudgetPeriod.CUSTOM) dateWindow else "$periodName • $dateWindow"

    val spentFormatted = "Spent: " + MoneyFormatter.format(amount = progressAmount, currencyCode = currency, config = formatterConfig)
    val limitFormatted = "Limit: " + MoneyFormatter.format(amount = targetAmount, currencyCode = currency, config = formatterConfig)

    return BudgetUiModel(
        id = budget.id,
        title = title,
        type = budget.type,
        iconData = iconData,
        periodLabel = periodLabel,
        spentFormatted = spentFormatted,
        limitFormatted = limitFormatted,
        progressFraction = progressFraction,
        percentage = percentageDouble.toInt(),
        isOverBudget = isOverBudget,
        walletNames = wallets.map { it.name }
    )
}

