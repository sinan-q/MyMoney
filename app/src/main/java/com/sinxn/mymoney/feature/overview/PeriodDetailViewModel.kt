package com.sinxn.mymoney.feature.overview

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.data.local.model.TransactionMonthGroup
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.repository.CategoryRepository
import com.sinxn.mymoney.core.ui.components.IconData
import com.sinxn.mymoney.core.ui.components.groupTransactionsIntoMonthGroups
import com.sinxn.mymoney.core.ui.components.parseIconData
import com.sinxn.mymoney.core.util.Constants
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@Immutable
data class SubcategoryBreakdownItem(
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
data class ParentCategoryBreakdownItem(
    val categoryId: String?,
    val categoryName: String,
    val categoryIcon: String?,
    val iconData: IconData,
    val amount: Long,
    val percentage: Float,
    val percentageFormatted: String,
    val formattedAmount: String,
    val subcategories: List<SubcategoryBreakdownItem> = emptyList()
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
    val incomeCategories: List<ParentCategoryBreakdownItem> = emptyList(),
    val expenseCategories: List<ParentCategoryBreakdownItem> = emptyList(),
    val transactions: List<TransactionWithCategory> = emptyList(),
    val groupedTransactions: List<TransactionMonthGroup> = emptyList(),
    val selectedTab: Int = 0, // 0: Incomes, 1: Expenses, 2: Transactions
    val isLoading: Boolean = true,
    val formattingSettings: FormattingSettings = FormattingSettings()
)

@HiltViewModel
class PeriodDetailViewModel @Inject constructor(
    private val moneyDao: MoneyDao,
    private val settingsRepository: SettingsRepository,
    private val categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val startDate: String = savedStateHandle.get<String>("startDate") ?: ""
    val endDate: String = savedStateHandle.get<String>("endDate") ?: ""

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val rawPeriodDataFlow = combine(
        settingsRepository.currentWalletId,
        settingsRepository.formattingSettings
    ) { walletId, formatting ->
        Pair(walletId.ifEmpty { Constants.TOTAL_WALLET_ID }, formatting)
    }.flatMapLatest { (walletId, formatting) ->
        val maxDate = DateUtils.getSQLDateTimeString(Date())
        val transactionsFlow = if (walletId == Constants.TOTAL_WALLET_ID) {
            moneyDao.getTransactionsForTotalInPeriod(startDate, endDate, maxDate)
        } else {
            moneyDao.getTransactionsForWalletInPeriod(walletId, startDate, endDate, maxDate)
        }

        combine(
            transactionsFlow,
            moneyDao.getWallets(),
            categoryRepository.getCategories()
        ) { txList, wallets, categories ->
            val currentWallet = if (walletId == Constants.TOTAL_WALLET_ID) null else wallets.find { it.id == walletId }
            val currCode = if (walletId == Constants.TOTAL_WALLET_ID) {
                val walletsInTotal = wallets.filter {
                    it.countInTotal && (!formatting.excludeArchivedFromTotal || !it.isArchived)
                }
                val distinctCurrencies = walletsInTotal
                    .map { it.currency.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                if (distinctCurrencies.size == 1) {
                    distinctCurrencies.first()
                } else {
                    val txCurrencies = txList.mapNotNull { it.currencyCode?.trim() }.filter { it.isNotEmpty() }.distinct()
                    if (txCurrencies.size == 1) {
                        txCurrencies.first()
                    } else {
                        formatting.globalCurrency.ifEmpty { "USD" }
                    }
                }
            } else {
                currentWallet?.currency ?: formatting.globalCurrency.ifEmpty { "USD" }
            }
            val walletName = if (walletId == Constants.TOTAL_WALLET_ID) "Total" else {
                currentWallet?.name ?: "Wallet"
            }
            val decimals = try {
                java.util.Currency.getInstance(currCode).defaultFractionDigits
            } catch (e: Exception) { 2 }
            val symbol = MoneyFormatter.getCurrencySymbol(currCode)

            var incomeSum = 0L
            var expenseSum = 0L

            txList.forEach { txWithCat ->
                val tx = txWithCat.transaction
                if (tx.direction == 1) {
                    incomeSum += tx.money
                } else {
                    expenseSum += tx.money
                }
            }

            val config = MoneyFormatter.Config(
                showCurrency = formatting.showCurrency,
                groupDigits = formatting.groupDigits,
                roundDecimals = formatting.roundDecimals,
                showPlusMinus = false
            )

            val incomeBreakdown = buildCategoryBreakdowns(
                txList = txList,
                direction = 1,
                totalSum = incomeSum,
                categories = categories,
                currencyCode = currCode,
                decimals = decimals,
                config = config
            )

            val expenseBreakdown = buildCategoryBreakdowns(
                txList = txList,
                direction = 0,
                totalSum = expenseSum,
                categories = categories,
                currencyCode = currCode,
                decimals = decimals,
                config = config
            )

            val groupedItems = groupTransactionsIntoMonthGroups(
                transactions = txList,
                decimals = decimals,
                currencyCode = currCode,
                formatterConfig = config,
                dateFormat = formatting.dateFormat
            )

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
                groupedTransactions = groupedItems,
                isLoading = false,
                formattingSettings = formatting
            )
        }.flowOn(Dispatchers.Default)
    }

    val uiState: StateFlow<PeriodDetailUiState> = rawPeriodDataFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PeriodDetailUiState(startDate = startDate, endDate = endDate)
        )

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    private fun buildCategoryBreakdowns(
        txList: List<TransactionWithCategory>,
        direction: Int,
        totalSum: Long,
        categories: List<CategoryEntity>,
        currencyCode: String,
        decimals: Int,
        config: MoneyFormatter.Config
    ): List<ParentCategoryBreakdownItem> {
        val categoryMap = categories.associateBy { it.id }

        val parentInfoMap = mutableMapOf<String?, Pair<String, String?>>()
        val parentDirectAmountMap = mutableMapOf<String?, Long>()
        val parentSubcategoriesMap = mutableMapOf<String?, MutableMap<String, Triple<String, String?, Long>>>()

        txList.forEach { txWithCat ->
            val tx = txWithCat.transaction
            val isMatch = if (direction == 1) tx.direction == 1 else tx.direction != 1
            if (!isMatch) return@forEach

            val catId = tx.categoryId
            val catEntity = catId?.let { categoryMap[it] }

            if (catEntity == null) {
                val parentId: String? = null
                val catName = txWithCat.categoryName ?: "Uncategorized"
                val catIcon = txWithCat.categoryIcon ?: "ic_category"
                parentInfoMap[parentId] = Pair(catName, catIcon)
                parentDirectAmountMap[parentId] = (parentDirectAmountMap[parentId] ?: 0L) + tx.money
            } else if (catEntity.parentId == null) {
                val parentId = catEntity.id
                parentInfoMap[parentId] = Pair(catEntity.name, catEntity.icon)
                parentDirectAmountMap[parentId] = (parentDirectAmountMap[parentId] ?: 0L) + tx.money
            } else {
                val parentEntity = categoryMap[catEntity.parentId]
                val parentId = parentEntity?.id ?: catEntity.parentId
                val parentName = parentEntity?.name ?: (txWithCat.categoryName ?: "Category")
                val parentIcon = parentEntity?.icon ?: "ic_category"

                parentInfoMap[parentId] = Pair(parentName, parentIcon)

                val subMap = parentSubcategoriesMap.getOrPut(parentId) { mutableMapOf() }
                val currentSub = subMap[catEntity.id]
                val currentSubAmount = currentSub?.third ?: 0L
                subMap[catEntity.id] = Triple(catEntity.name, catEntity.icon, currentSubAmount + tx.money)
            }
        }

        return parentInfoMap.mapNotNull { (parentId, info) ->
            val directAmount = parentDirectAmountMap[parentId] ?: 0L
            val subMap = parentSubcategoriesMap[parentId] ?: emptyMap()
            val subTotal = subMap.values.sumOf { it.third }
            val parentTotal = directAmount + subTotal

            if (parentTotal <= 0L) return@mapNotNull null

            val subcategoryItems = mutableListOf<SubcategoryBreakdownItem>()

            subMap.forEach { (subId, triple) ->
                val (subName, subIcon, subAmount) = triple
                val subPercentage = if (totalSum > 0) (subAmount.toFloat() / totalSum) * 100f else 0f
                subcategoryItems.add(
                    SubcategoryBreakdownItem(
                        categoryId = subId,
                        categoryName = subName,
                        categoryIcon = subIcon,
                        iconData = parseIconData(subIcon ?: "ic_category", subName),
                        amount = subAmount,
                        percentage = subPercentage,
                        percentageFormatted = String.format(Locale.US, "%.1f%%", subPercentage),
                        formattedAmount = MoneyFormatter.format(subAmount, currencyCode, decimals, config)
                    )
                )
            }

            if (subcategoryItems.isNotEmpty() && directAmount > 0L) {
                val directPercentage = if (totalSum > 0) (directAmount.toFloat() / totalSum) * 100f else 0f
                subcategoryItems.add(
                    SubcategoryBreakdownItem(
                        categoryId = parentId,
                        categoryName = "Direct",
                        categoryIcon = info.second,
                        iconData = parseIconData(info.second ?: "ic_category", "Direct"),
                        amount = directAmount,
                        percentage = directPercentage,
                        percentageFormatted = String.format(Locale.US, "%.1f%%", directPercentage),
                        formattedAmount = MoneyFormatter.format(directAmount, currencyCode, decimals, config)
                    )
                )
            }

            val sortedSubs = subcategoryItems.sortedByDescending { it.amount }
            val parentPercentage = if (totalSum > 0) (parentTotal.toFloat() / totalSum) * 100f else 0f

            ParentCategoryBreakdownItem(
                categoryId = parentId,
                categoryName = info.first,
                categoryIcon = info.second,
                iconData = parseIconData(info.second ?: "ic_category", info.first),
                amount = parentTotal,
                percentage = parentPercentage,
                percentageFormatted = String.format(Locale.US, "%.1f%%", parentPercentage),
                formattedAmount = MoneyFormatter.format(parentTotal, currencyCode, decimals, config),
                subcategories = sortedSubs
            )
        }.sortedByDescending { it.amount }
    }
}
