package com.sinxn.mymoney.feature.overview

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.repository.CategoryRepository
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
    val subcategories: List<SubcategoryBreakdownItem> = emptyList(),
    val isExpanded: Boolean = false
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

    private val _expandedParentIds = MutableStateFlow<Set<String>>(emptySet())

    private data class CombinedFilterParams(
        val walletId: String,
        val formatting: FormattingSettings,
        val tab: Int,
        val expandedIds: Set<String>
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<PeriodDetailUiState> = combine(
        settingsRepository.currentWalletId,
        settingsRepository.formattingSettings,
        _selectedTab,
        _expandedParentIds
    ) { walletId, formatting, tab, expandedIds ->
        CombinedFilterParams(walletId.ifEmpty { Constants.TOTAL_WALLET_ID }, formatting, tab, expandedIds)
    }.flatMapLatest { params ->
        val maxDate = DateUtils.getSQLDateTimeString(Date())
        val transactionsFlow = if (params.walletId == Constants.TOTAL_WALLET_ID) {
            moneyDao.getTransactionsForTotalInPeriod(startDate, endDate, maxDate)
        } else {
            moneyDao.getTransactionsForWalletInPeriod(params.walletId, startDate, endDate, maxDate)
        }

        combine(
            transactionsFlow,
            moneyDao.getWallets(),
            categoryRepository.getCategories()
        ) { txList, wallets, categories ->
            val currentWallet = if (params.walletId == Constants.TOTAL_WALLET_ID) null else wallets.find { it.id == params.walletId }
            val currCode = if (params.walletId == Constants.TOTAL_WALLET_ID) {
                params.formatting.globalCurrency.ifEmpty { "USD" }
            } else {
                currentWallet?.currency ?: params.formatting.globalCurrency.ifEmpty { "USD" }
            }
            val walletName = if (params.walletId == Constants.TOTAL_WALLET_ID) "Total" else {
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
                showCurrency = params.formatting.showCurrency,
                groupDigits = params.formatting.groupDigits,
                roundDecimals = params.formatting.roundDecimals,
                showPlusMinus = false
            )

            val incomeBreakdown = buildCategoryBreakdowns(
                txList = txList,
                direction = 1,
                totalSum = incomeSum,
                categories = categories,
                expandedIds = params.expandedIds,
                currencyCode = currCode,
                decimals = decimals,
                config = config
            )

            val expenseBreakdown = buildCategoryBreakdowns(
                txList = txList,
                direction = 0,
                totalSum = expenseSum,
                categories = categories,
                expandedIds = params.expandedIds,
                currencyCode = currCode,
                decimals = decimals,
                config = config
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
                selectedTab = params.tab,
                isLoading = false,
                formattingSettings = params.formatting
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

    fun toggleParentExpanded(parentKey: String) {
        _expandedParentIds.update { current ->
            if (parentKey in current) {
                current - parentKey
            } else {
                current + parentKey
            }
        }
    }

    private fun buildCategoryBreakdowns(
        txList: List<TransactionWithCategory>,
        direction: Int,
        totalSum: Long,
        categories: List<CategoryEntity>,
        expandedIds: Set<String>,
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
            val parentKey = parentId ?: info.first

            ParentCategoryBreakdownItem(
                categoryId = parentId,
                categoryName = info.first,
                categoryIcon = info.second,
                iconData = parseIconData(info.second ?: "ic_category", info.first),
                amount = parentTotal,
                percentage = parentPercentage,
                percentageFormatted = String.format(Locale.US, "%.1f%%", parentPercentage),
                formattedAmount = MoneyFormatter.format(parentTotal, currencyCode, decimals, config),
                subcategories = sortedSubs,
                isExpanded = parentKey in expandedIds
            )
        }.sortedByDescending { it.amount }
    }
}
