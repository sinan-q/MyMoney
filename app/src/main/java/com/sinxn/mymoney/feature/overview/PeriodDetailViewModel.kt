package com.sinxn.mymoney.feature.overview

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.ui.components.IconData
import com.sinxn.mymoney.core.ui.components.parseIconData
import com.sinxn.mymoney.core.util.Constants
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.util.Date
import javax.inject.Inject

@Immutable
data class CategoryBreakdownItem(
    val categoryId: String?,
    val categoryName: String,
    val categoryIcon: String?,
    val iconData: IconData,
    val amount: Long,
    val percentage: Float,
    val percentageFormatted: String,
    val formattedAmount: String
)

@Immutable
data class PeriodDetailUiState(
    val startDate: String = "",
    val endDate: String = "",
    val walletName: String = "Total",
    val currencyCode: String = "USD",
    val currencySymbol: String = "$",
    val currencyDecimals: Int = 2,
    val totalIncomes: Long = 0L,
    val totalExpenses: Long = 0L,
    val netTotal: Long = 0L,
    val incomeCategories: List<CategoryBreakdownItem> = emptyList(),
    val expenseCategories: List<CategoryBreakdownItem> = emptyList(),
    val transactions: List<TransactionWithCategory> = emptyList(),
    val selectedTab: Int = 0, // 0: Incomes, 1: Expenses, 2: Transactions
    val isLoading: Boolean = true,
    val formattingSettings: FormattingSettings = FormattingSettings()
)

@HiltViewModel
class PeriodDetailViewModel @Inject constructor(
    private val moneyDao: MoneyDao,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val startDate: String = savedStateHandle.get<String>("startDate") ?: ""
    val endDate: String = savedStateHandle.get<String>("endDate") ?: ""

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<PeriodDetailUiState> = combine(
        settingsRepository.currentWalletId,
        settingsRepository.formattingSettings,
        _selectedTab
    ) { walletId, formatting, tab ->
        Triple(walletId.ifEmpty { Constants.TOTAL_WALLET_ID }, formatting, tab)
    }.flatMapLatest { (walletId, formatting, tab) ->
        val maxDate = DateUtils.getSQLDateTimeString(Date())
        val transactionsFlow = if (walletId == Constants.TOTAL_WALLET_ID) {
            moneyDao.getTransactionsForTotalInPeriod(startDate, endDate, maxDate)
        } else {
            moneyDao.getTransactionsForWalletInPeriod(walletId, startDate, endDate, maxDate)
        }

        transactionsFlow.combine(moneyDao.getWallets()) { txList, wallets ->
            val currCode = if (walletId == Constants.TOTAL_WALLET_ID) {
                formatting.globalCurrency.ifEmpty { "USD" }
            } else {
                wallets.find { it.id == walletId }?.currency ?: formatting.globalCurrency.ifEmpty { "USD" }
            }
            val walletName = if (walletId == Constants.TOTAL_WALLET_ID) "Total" else {
                wallets.find { it.id == walletId }?.name ?: "Wallet"
            }
            val decimals = try {
                java.util.Currency.getInstance(currCode).defaultFractionDigits
            } catch (e: Exception) { 2 }
            val symbol = MoneyFormatter.getCurrencySymbol(currCode)

            var incomeSum = 0L
            var expenseSum = 0L

            val incomeGroupMap = mutableMapOf<String?, Pair<String, String?>>() // id -> name, icon
            val incomeAmountMap = mutableMapOf<String?, Long>()

            val expenseGroupMap = mutableMapOf<String?, Pair<String, String?>>()
            val expenseAmountMap = mutableMapOf<String?, Long>()

            txList.forEach { txWithCat ->
                val tx = txWithCat.transaction
                val catName = txWithCat.categoryName ?: "Uncategorized"
                val catIcon = txWithCat.categoryIcon

                if (tx.direction == 1) { // Income
                    incomeSum += tx.money
                    incomeGroupMap[tx.categoryId] = Pair(catName, catIcon)
                    incomeAmountMap[tx.categoryId] = (incomeAmountMap[tx.categoryId] ?: 0L) + tx.money
                } else { // Expense
                    expenseSum += tx.money
                    expenseGroupMap[tx.categoryId] = Pair(catName, catIcon)
                    expenseAmountMap[tx.categoryId] = (expenseAmountMap[tx.categoryId] ?: 0L) + tx.money
                }
            }

            val config = MoneyFormatter.Config(
                showCurrency = formatting.showCurrency,
                groupDigits = formatting.groupDigits,
                roundDecimals = formatting.roundDecimals,
                showPlusMinus = false
            )

            val incomeBreakdown = incomeAmountMap.map { (catId, amount) ->
                val info = incomeGroupMap[catId]
                val catName = info?.first ?: "Uncategorized"
                val catIcon = info?.second ?: "ic_category"
                val percentage = if (incomeSum > 0) (amount.toFloat() / incomeSum) * 100f else 0f
                CategoryBreakdownItem(
                    categoryId = catId,
                    categoryName = catName,
                    categoryIcon = info?.second,
                    iconData = parseIconData(catIcon, catName),
                    amount = amount,
                    percentage = percentage,
                    percentageFormatted = String.format(java.util.Locale.US, "%.1f%%", percentage),
                    formattedAmount = MoneyFormatter.format(amount, currCode, decimals, config)
                )
            }.sortedByDescending { it.amount }

            val expenseBreakdown = expenseAmountMap.map { (catId, amount) ->
                val info = expenseGroupMap[catId]
                val catName = info?.first ?: "Uncategorized"
                val catIcon = info?.second ?: "ic_category"
                val percentage = if (expenseSum > 0) (amount.toFloat() / expenseSum) * 100f else 0f
                CategoryBreakdownItem(
                    categoryId = catId,
                    categoryName = catName,
                    categoryIcon = info?.second,
                    iconData = parseIconData(catIcon, catName),
                    amount = amount,
                    percentage = percentage,
                    percentageFormatted = String.format(java.util.Locale.US, "%.1f%%", percentage),
                    formattedAmount = MoneyFormatter.format(amount, currCode, decimals, config)
                )
            }.sortedByDescending { it.amount }

            PeriodDetailUiState(
                startDate = startDate,
                endDate = endDate,
                walletName = walletName,
                currencyCode = currCode,
                currencySymbol = symbol,
                currencyDecimals = decimals,
                totalIncomes = incomeSum,
                totalExpenses = expenseSum,
                netTotal = incomeSum - expenseSum,
                incomeCategories = incomeBreakdown,
                expenseCategories = expenseBreakdown,
                transactions = txList,
                selectedTab = tab,
                isLoading = false,
                formattingSettings = formatting
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PeriodDetailUiState(startDate = startDate, endDate = endDate)
    )

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }
}
