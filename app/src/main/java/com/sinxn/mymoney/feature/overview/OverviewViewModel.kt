package com.sinxn.mymoney.feature.overview

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.data.local.model.WalletWithBalance
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.preferences.toFormatterConfig
import com.sinxn.mymoney.core.util.Constants
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import androidx.core.graphics.toColorInt
import com.sinxn.mymoney.core.ui.components.parseIconData
import com.sinxn.mymoney.feature.overview.component.CategoryChartData

private data class ChartAndTreemapResult(
    val treemapNodes: List<com.sinxn.mymoney.feature.overview.component.TreemapNode>,
    val chartPeriods: List<com.sinxn.mymoney.feature.overview.component.ChartPeriodData>,
    val maxChartValue: Float,
    val categoryCharts: Map<String, CategoryChartData>,
    val showTreemap: Boolean
)

@Immutable
data class OverviewPeriodUiModel(
    val id: String,
    val index: Int,
    val dateRangeText: String,
    val startDateTimeSql: String,
    val endDateTimeSql: String,
    val formattedIncome: String?,
    val formattedExpense: String?,
    val formattedNetAmount: String,
    val netAmountColorType: Int // 1: positive, -1: negative, 0: neutral
)

@Immutable
data class OverviewUiState(
    val overviewData: OverviewData? = null,
    val periodsUi: List<OverviewPeriodUiModel> = emptyList(),
    val settings: OverviewSettings? = null,
    val currentWalletId: String = Constants.TOTAL_WALLET_ID,
    val walletName: String = "Total",
    val walletIcon: String? = "sigma",
    val currencyCode: String = "USD",
    val currencySymbol: String = "$",
    val currencyDecimals: Int = 2,
    val formattingSettings: FormattingSettings = FormattingSettings(),
    val selectedCategoryName: String? = null,
    val isLoading: Boolean = true,
    val showSettingsSheet: Boolean = false,
    val showWalletPickerSheet: Boolean = false,
    val isDivergingChart: Boolean = false,
    val insightsText: String = "",
    val comparisonText: String = "",
    val comparisonIsPositive: Boolean = true,
    val chartPeriods: List<com.sinxn.mymoney.feature.overview.component.ChartPeriodData> = emptyList(),
    val chartMaxValue: Float = 0f,
    val categoryCharts: Map<String, CategoryChartData> = emptyMap(),
    val treemapNodes: List<com.sinxn.mymoney.feature.overview.component.TreemapNode> = emptyList(),
    val showTreemap: Boolean = true
)

@HiltViewModel
class OverviewViewModel @Inject constructor(
    private val overviewRepository: OverviewRepository,
    private val settingsRepository: SettingsRepository,
    private val moneyDao: MoneyDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(OverviewUiState())
    val uiState: StateFlow<OverviewUiState> = _uiState.asStateFlow()

    val allWallets: StateFlow<List<WalletWithBalance>> = moneyDao
        .getWalletsWithBalance(DateUtils.getSQLDateTimeString(Date()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<CategoryEntity>> = moneyDao
        .getCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var currentWalletId: String = Constants.TOTAL_WALLET_ID

    init {
        // Observe current wallet, formatting settings, and wallets, reload on changes
        viewModelScope.launch {
            combine(
                settingsRepository.currentWalletId,
                settingsRepository.formattingSettings,
                moneyDao.getWallets()
            ) { walletId, formatting, wallets -> Triple(walletId, formatting, wallets) }
                .collect { (walletId, formatting, wallets) ->
                    val effectiveWalletId = walletId.ifEmpty { Constants.TOTAL_WALLET_ID }
                    currentWalletId = effectiveWalletId

                    // Resolve wallet info
                    val walletInfo = resolveWalletInfo(effectiveWalletId, formatting, wallets)

                    _uiState.update {
                        it.copy(
                            currentWalletId = effectiveWalletId,
                            walletName = walletInfo.name,
                            walletIcon = walletInfo.icon,
                            currencyCode = walletInfo.currencyCode,
                            currencySymbol = walletInfo.currencySymbol,
                            currencyDecimals = walletInfo.decimals,
                            formattingSettings = formatting
                        )
                    }

                    // Load data with current or default settings
                    val settings = _uiState.value.settings
                        ?: overviewRepository.getDefaultSettings()
                    loadData(effectiveWalletId, settings)
                }
        }
    }



    fun toggleSettingsSheet() {
        _uiState.update { it.copy(showSettingsSheet = !it.showSettingsSheet) }
    }

    fun dismissSettingsSheet() {
        _uiState.update { it.copy(showSettingsSheet = false) }
    }

    fun setDivergingChart(isDiverging: Boolean) {
        _uiState.update { it.copy(isDivergingChart = isDiverging) }
    }

    fun toggleWalletPickerSheet() {
        _uiState.update { it.copy(showWalletPickerSheet = !it.showWalletPickerSheet) }
    }

    fun dismissWalletPickerSheet() {
        _uiState.update { it.copy(showWalletPickerSheet = false) }
    }

    fun selectWallet(walletId: String) {
        dismissWalletPickerSheet()
        viewModelScope.launch {
            settingsRepository.setCurrentWalletId(walletId)
        }
    }

    fun setCashFlowFilter(filter: CashFlowFilter) {
        val currentSettings = _uiState.value.settings ?: return
        val newSettings = currentSettings.copy(cashFlowFilter = filter)
        _uiState.update { it.copy(settings = newSettings) }
        viewModelScope.launch {
            loadData(currentWalletId, newSettings)
        }
    }

    fun applyOverviewConfig(newSettings: OverviewSettings, newWalletId: String) {
        _uiState.update { it.copy(settings = newSettings, showSettingsSheet = false) }
        viewModelScope.launch {
            if (newWalletId != currentWalletId) {
                settingsRepository.setCurrentWalletId(newWalletId)
            } else {
                loadData(currentWalletId, newSettings)
            }
        }
    }

    private suspend fun loadData(walletId: String, settings: OverviewSettings) {
        _uiState.update { it.copy(isLoading = true) }
        try {
            val data = overviewRepository.loadOverviewData(walletId, settings)
            val currentState = _uiState.value

            val effectiveCurrencyCode = if (walletId == Constants.TOTAL_WALLET_ID) {
                val dataCurrencies = data.totalNetIncomes.getCurrencies()
                if (dataCurrencies.size == 1) {
                    dataCurrencies.first()
                } else if (!dataCurrencies.contains(currentState.currencyCode) && dataCurrencies.isNotEmpty()) {
                    dataCurrencies.first()
                } else {
                    currentState.currencyCode
                }
            } else {
                currentState.currencyCode
            }

            val decimals = try {
                java.util.Currency.getInstance(effectiveCurrencyCode).defaultFractionDigits
            } catch (e: Exception) { currentState.currencyDecimals }
            val symbol = MoneyFormatter.getCurrencySymbol(effectiveCurrencyCode)

            val periodsUi = buildPeriodUiModels(
                periods = data.periods,
                currencyCode = effectiveCurrencyCode,
                decimals = decimals,
                formattingSettings = currentState.formattingSettings
            )
            val categoryName = if (settings.overviewType == OverviewType.CATEGORY && !settings.categoryId.isNullOrEmpty()) {
                moneyDao.getCategoryById(settings.categoryId)?.name ?: "Category"
            } else {
                null
            }
            val chartAndTreemapResult = buildChartAndTreemap(
                data = data,
                settings = settings,
                currencyCode = effectiveCurrencyCode,
                decimals = decimals,
                formattingSettings = currentState.formattingSettings
            )
            val treemapNodes = chartAndTreemapResult.treemapNodes
            val chartPeriods = chartAndTreemapResult.chartPeriods
            val maxVal = chartAndTreemapResult.maxChartValue
            val categoryCharts = chartAndTreemapResult.categoryCharts
            
            val (insights, compText, compIsPositive) = buildInsightsAndComparison(
                data = data,
                settings = settings,
                currencyCode = effectiveCurrencyCode,
                decimals = decimals,
                treemapNodes = treemapNodes
            )

            _uiState.update {
                it.copy(
                    overviewData = data,
                    periodsUi = periodsUi,
                    settings = settings,
                    selectedCategoryName = categoryName,
                    currencyCode = effectiveCurrencyCode,
                    currencySymbol = symbol,
                    currencyDecimals = decimals,
                    isLoading = false,
                    treemapNodes = treemapNodes,
                    chartPeriods = chartPeriods,
                    chartMaxValue = maxVal,
                    categoryCharts = categoryCharts,
                    showTreemap = chartAndTreemapResult.showTreemap,
                    insightsText = insights,
                    comparisonText = compText,
                    comparisonIsPositive = compIsPositive
                )
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun buildChartAndTreemap(
        data: OverviewData,
        settings: OverviewSettings,
        currencyCode: String,
        decimals: Int,
        formattingSettings: FormattingSettings
    ): ChartAndTreemapResult {
        val allCategories = moneyDao.getCategories().first()
        val catMap = allCategories.associateBy { it.id }

        val noSignConfig = formattingSettings.toFormatterConfig(showPlusMinus = false)

        val categoryAmounts = mutableMapOf<String, Long>()
        val categoryNetSigns = mutableMapOf<String, Boolean>()

        if (settings.overviewType == OverviewType.CATEGORY) {
            val allCatIds = data.totalCategoryIncomes.keys + data.totalCategoryExpenses.keys
            for (catId in allCatIds) {
                val inc = data.totalCategoryIncomes[catId]?.getMoney(currencyCode) ?: 0L
                val exp = data.totalCategoryExpenses[catId]?.getMoney(currencyCode) ?: 0L
                val amt = if (inc > 0L && exp == 0L) inc else if (exp > 0L && inc == 0L) exp else Math.abs(inc - exp)
                if (amt > 0L) {
                    categoryAmounts[catId] = amt
                    categoryNetSigns[catId] = inc >= exp
                }
            }
        } else if (settings.cashFlowFilter == CashFlowFilter.INCOMES) {
            for ((catId, money) in data.totalCategoryIncomes) {
                val amt = money.getMoney(currencyCode)
                if (amt > 0L) {
                    categoryAmounts[catId] = amt
                    categoryNetSigns[catId] = true
                }
            }
        } else if (settings.cashFlowFilter == CashFlowFilter.EXPENSES) {
            for ((catId, money) in data.totalCategoryExpenses) {
                val amt = money.getMoney(currencyCode)
                if (amt > 0L) {
                    categoryAmounts[catId] = amt
                    categoryNetSigns[catId] = false
                }
            }
        } else {
            // NET_INCOMES: Include both income and expense categories
            val allCatIds = data.totalCategoryIncomes.keys + data.totalCategoryExpenses.keys
            for (catId in allCatIds) {
                val inc = data.totalCategoryIncomes[catId]?.getMoney(currencyCode) ?: 0L
                val exp = data.totalCategoryExpenses[catId]?.getMoney(currencyCode) ?: 0L
                val net = inc - exp
                if (net != 0L) {
                    categoryAmounts[catId] = Math.abs(net)
                    categoryNetSigns[catId] = net > 0L
                }
            }
        }

        val totalAmount = categoryAmounts.values.sum()

        // Group categories by parent category
        val parentInfoMap = mutableMapOf<String, Pair<String, String?>>() // parentId -> Pair(name, icon)
        val parentDirectAmountMap = mutableMapOf<String, Long>() // parentId -> direct amount
        val parentSubcategoriesMap = mutableMapOf<String, MutableMap<String, Triple<String, String?, Long>>>() // parentId -> subId -> Triple(name, icon, amount)

        for ((catId, amount) in categoryAmounts) {
            if (amount <= 0L) continue
            val catEntity = catMap[catId]
            if (catEntity == null) {
                val parentId = catId
                parentInfoMap[parentId] = Pair("Uncategorized", null)
                parentDirectAmountMap[parentId] = (parentDirectAmountMap[parentId] ?: 0L) + amount
            } else if (catEntity.parentId == null) {
                val parentId = catEntity.id
                parentInfoMap[parentId] = Pair(catEntity.name, catEntity.icon)
                parentDirectAmountMap[parentId] = (parentDirectAmountMap[parentId] ?: 0L) + amount
            } else {
                val parentEntity = catMap[catEntity.parentId]
                val parentId = parentEntity?.id ?: catEntity.parentId
                val parentName = parentEntity?.name ?: "Category"
                val parentIcon = parentEntity?.icon
                parentInfoMap[parentId] = Pair(parentName, parentIcon)

                val subMap = parentSubcategoriesMap.getOrPut(parentId) { mutableMapOf() }
                val currentSub = subMap[catEntity.id]
                val currentSubAmount = currentSub?.third ?: 0L
                subMap[catEntity.id] = Triple(catEntity.name, catEntity.icon, currentSubAmount + amount)
            }
        }

        val treemapNodes = parentInfoMap.mapNotNull { (parentId, info) ->
            val directAmount = parentDirectAmountMap[parentId] ?: 0L
            val subMap = parentSubcategoriesMap[parentId] ?: emptyMap()
            val subTotal = subMap.values.sumOf { it.third }
            val parentTotal = directAmount + subTotal

            if (parentTotal <= 0L) return@mapNotNull null

            val subNodes = mutableListOf<com.sinxn.mymoney.feature.overview.component.TreemapSubNode>()
            subMap.forEach { (subId, triple) ->
                val (subName, _, subAmount) = triple
                if (subAmount > 0L) {
                    val subPct = if (parentTotal > 0) Math.round((subAmount.toDouble() / parentTotal) * 100).toString() + "%" else "0%"
                    val subPrefix = if (settings.cashFlowFilter == CashFlowFilter.NET_INCOMES) {
                        if (categoryNetSigns[subId] == true) "+" else ""
                    } else ""
                    subNodes.add(
                        com.sinxn.mymoney.feature.overview.component.TreemapSubNode(
                            categoryId = subId,
                            name = subName,
                            value = subAmount.toFloat(),
                            formattedAmount = subPrefix + MoneyFormatter.format(subAmount, currencyCode, decimals, noSignConfig),
                            percent = subPct
                        )
                    )
                }
            }
            if (subNodes.isNotEmpty() && directAmount > 0L) {
                val directPct = if (parentTotal > 0) Math.round((directAmount.toDouble() / parentTotal) * 100).toString() + "%" else "0%"
                val directPrefix = if (settings.cashFlowFilter == CashFlowFilter.NET_INCOMES) {
                    if (categoryNetSigns[parentId] == true) "+" else ""
                } else ""
                subNodes.add(
                    com.sinxn.mymoney.feature.overview.component.TreemapSubNode(
                        categoryId = parentId,
                        name = "Direct",
                        value = directAmount.toFloat(),
                        formattedAmount = directPrefix + MoneyFormatter.format(directAmount, currencyCode, decimals, noSignConfig),
                        percent = directPct
                    )
                )
            }

            val sortedSubs = subNodes.sortedByDescending { it.value }
            val parentColor = parseIconData(info.second, info.first).color
            val prefix = if (settings.cashFlowFilter == CashFlowFilter.NET_INCOMES) {
                val isParentPositive = (data.totalCategoryIncomes[parentId]?.getMoney(currencyCode) ?: 0L) >= (data.totalCategoryExpenses[parentId]?.getMoney(currencyCode) ?: 0L)
                if (isParentPositive) "+" else ""
            } else ""
            val formatted = prefix + MoneyFormatter.format(parentTotal, currencyCode, decimals, noSignConfig)
            val pct = if (totalAmount > 0) Math.round((parentTotal.toDouble() / totalAmount) * 100).toString() + "%" else "0%"

            com.sinxn.mymoney.feature.overview.component.TreemapNode(
                categoryId = parentId,
                name = info.first,
                value = parentTotal.toFloat(),
                color = parentColor,
                formattedAmount = formatted,
                percent = pct,
                subNodes = sortedSubs
            )
        }.sortedByDescending { it.value }

        val divider = Math.pow(10.0, decimals.toDouble()).toFloat()
        var maxChartValue = 0f
        val chartPeriods = data.periods.mapIndexed { i, pm ->
            val label = if (settings.groupType == GroupType.MONTHLY) {
                DateUtils.formatShortMonth(pm.startDate)
            } else if (settings.groupType == GroupType.DAILY) {
                DateUtils.formatMonthDay(pm.startDate)
            } else {
                "${i+1}"
            }
            val segments = mutableListOf<com.sinxn.mymoney.feature.overview.component.ChartSegment>()
            val parentPeriodAmounts = mutableMapOf<String, Float>()

            if (settings.overviewType == OverviewType.CATEGORY) {
                val periodCatIds = pm.categoryIncomes.keys + pm.categoryExpenses.keys
                for (catId in periodCatIds) {
                    val inc = pm.categoryIncomes[catId]?.getMoney(currencyCode) ?: 0L
                    val exp = pm.categoryExpenses[catId]?.getMoney(currencyCode) ?: 0L
                    val amt = (if (inc > 0L && exp == 0L) inc else if (exp > 0L && inc == 0L) exp else Math.abs(inc - exp)).toFloat() / divider
                    if (amt > 0f) {
                        val catEntity = catMap[catId]
                        val parentId = if (catEntity?.parentId != null) catEntity.parentId else catId
                        parentPeriodAmounts[parentId] = (parentPeriodAmounts[parentId] ?: 0f) + amt
                    }
                }
            } else if (settings.cashFlowFilter == CashFlowFilter.INCOMES) {
                for ((catId, money) in pm.categoryIncomes) {
                    val amt = money.getMoney(currencyCode).toFloat() / divider
                    if (amt > 0) {
                        val catEntity = catMap[catId]
                        val parentId = if (catEntity?.parentId != null) catEntity.parentId else catId
                        parentPeriodAmounts[parentId] = (parentPeriodAmounts[parentId] ?: 0f) + amt
                    }
                }
            } else if (settings.cashFlowFilter == CashFlowFilter.EXPENSES) {
                for ((catId, money) in pm.categoryExpenses) {
                    val amt = money.getMoney(currencyCode).toFloat() / divider
                    if (amt > 0) {
                        val catEntity = catMap[catId]
                        val parentId = if (catEntity?.parentId != null) catEntity.parentId else catId
                        parentPeriodAmounts[parentId] = (parentPeriodAmounts[parentId] ?: 0f) + amt
                    }
                }
            } else {
                // NET_INCOMES: Combine both incomes and expenses
                val periodCatIds = pm.categoryIncomes.keys + pm.categoryExpenses.keys
                for (catId in periodCatIds) {
                    val inc = pm.categoryIncomes[catId]?.getMoney(currencyCode) ?: 0L
                    val exp = pm.categoryExpenses[catId]?.getMoney(currencyCode) ?: 0L
                    val net = Math.abs(inc - exp).toFloat() / divider
                    if (net > 0f) {
                        val catEntity = catMap[catId]
                        val parentId = if (catEntity?.parentId != null) catEntity.parentId else catId
                        parentPeriodAmounts[parentId] = (parentPeriodAmounts[parentId] ?: 0f) + net
                    }
                }
            }

            var periodTotal = 0f
            for ((parentId, amt) in parentPeriodAmounts) {
                val parentCat = catMap[parentId]
                val color = parseIconData(parentCat?.icon, parentCat?.name ?: "Unknown").color
                segments.add(com.sinxn.mymoney.feature.overview.component.ChartSegment(amt, color, parentId))
                periodTotal += amt
            }
            if (periodTotal > maxChartValue) maxChartValue = periodTotal
            com.sinxn.mymoney.feature.overview.component.ChartPeriodData(label, segments.sortedByDescending { it.value })
        }

        val categoryCharts = mutableMapOf<String, CategoryChartData>()

        treemapNodes.forEach { node ->
            val parentId = node.categoryId
            val detailPeriods = data.periods.mapIndexed { i, pm ->
                val label = if (settings.groupType == GroupType.MONTHLY) {
                    DateUtils.formatShortMonth(pm.startDate)
                } else if (settings.groupType == GroupType.DAILY) {
                    DateUtils.formatMonthDay(pm.startDate)
                } else {
                    "${i+1}"
                }
                val subSegments = mutableListOf<com.sinxn.mymoney.feature.overview.component.ChartSegment>()

                if (node.subNodes.isNotEmpty()) {
                    node.subNodes.forEach { sub ->
                        val subId = sub.categoryId
                        val inc = pm.categoryIncomes[subId]?.getMoney(currencyCode) ?: 0L
                        val exp = pm.categoryExpenses[subId]?.getMoney(currencyCode) ?: 0L
                        val subAmt = if (settings.overviewType == OverviewType.CATEGORY) {
                            if (inc > 0L && exp == 0L) inc else if (exp > 0L && inc == 0L) exp else Math.abs(inc - exp)
                        } else when (settings.cashFlowFilter) {
                            CashFlowFilter.INCOMES -> inc
                            CashFlowFilter.EXPENSES -> exp
                            CashFlowFilter.NET_INCOMES -> Math.abs(inc - exp)
                        }
                        if (subAmt > 0L) {
                            val subColor = parseIconData(catMap[subId]?.icon, sub.name).color
                            subSegments.add(
                                com.sinxn.mymoney.feature.overview.component.ChartSegment(
                                    value = subAmt.toFloat() / divider,
                                    color = subColor,
                                    categoryId = subId
                                )
                            )
                        }
                    }
                } else {
                    val inc = pm.categoryIncomes[parentId]?.getMoney(currencyCode) ?: 0L
                    val exp = pm.categoryExpenses[parentId]?.getMoney(currencyCode) ?: 0L
                    val pAmt = if (settings.overviewType == OverviewType.CATEGORY) {
                        if (inc > 0L && exp == 0L) inc else if (exp > 0L && inc == 0L) exp else Math.abs(inc - exp)
                    } else when (settings.cashFlowFilter) {
                        CashFlowFilter.INCOMES -> inc
                        CashFlowFilter.EXPENSES -> exp
                        CashFlowFilter.NET_INCOMES -> Math.abs(inc - exp)
                    }
                    if (pAmt > 0L) {
                        subSegments.add(
                            com.sinxn.mymoney.feature.overview.component.ChartSegment(
                                value = pAmt.toFloat() / divider,
                                color = node.color,
                                categoryId = parentId
                            )
                        )
                    }
                }

                com.sinxn.mymoney.feature.overview.component.ChartPeriodData(
                    label = label,
                    segments = subSegments.sortedByDescending { it.value }
                )
            }

            val catMax = detailPeriods.maxOfOrNull { p -> p.segments.sumOf { it.value.toDouble() }.toFloat() } ?: 0f

            categoryCharts[parentId] = CategoryChartData(
                categoryId = parentId,
                categoryName = node.name,
                categoryColor = node.color,
                totalFormatted = node.formattedAmount,
                periods = detailPeriods,
                maxValue = catMax,
                subcategories = node.subNodes
            )
        }

        val isCategoryFilter = settings.overviewType == OverviewType.CATEGORY && !settings.categoryId.isNullOrEmpty()
        val showTreemap = if (!isCategoryFilter) {
            true
        } else {
            val filterCatId = settings.categoryId
            val filterCat = catMap[filterCatId]
            val isSubcategory = filterCat?.parentId != null
            if (isSubcategory) {
                false
            } else {
                val hasChildCategories = allCategories.any { it.parentId == filterCatId }
                val node = treemapNodes.find { it.categoryId == filterCatId } ?: treemapNodes.firstOrNull()
                hasChildCategories && (node?.subNodes?.size ?: 0) > 1
            }
        }

        return ChartAndTreemapResult(treemapNodes, chartPeriods, maxChartValue, categoryCharts, showTreemap)
    }

    private fun buildInsightsAndComparison(
        data: OverviewData,
        settings: OverviewSettings,
        currencyCode: String,
        decimals: Int,
        treemapNodes: List<com.sinxn.mymoney.feature.overview.component.TreemapNode>
    ): Triple<String, String, Boolean> {
        var insights = "No activity in this range."
        if (treemapNodes.isNotEmpty()) {
            val total = treemapNodes.sumOf { it.value.toDouble() }
            if (settings.overviewType == OverviewType.CATEGORY) {
                val node = treemapNodes.firstOrNull()
                if (node != null && node.subNodes.size > 1) {
                    val topSub = node.subNodes[0]
                    insights = "${topSub.name} took ${topSub.percent} of it."
                } else if (node != null && node.subNodes.size == 1) {
                    insights = "All of it went to ${node.subNodes[0].name}."
                } else if (node != null) {
                    insights = "All of it was direct ${node.name} activity."
                }
            } else if (treemapNodes.size == 1) {
                insights = "All of it came ${if(settings.cashFlowFilter == CashFlowFilter.INCOMES) "via" else "from"} ${treemapNodes[0].name}."
            } else if (treemapNodes.size > 1) {
                val top2 = treemapNodes[0].value + treemapNodes[1].value
                val pct = Math.round((top2 / total) * 100)
                if (settings.cashFlowFilter == CashFlowFilter.INCOMES) {
                    insights = "${treemapNodes[0].name} and ${treemapNodes[1].name} brought in $pct% of it."
                } else {
                    insights = "${treemapNodes[0].name} and ${treemapNodes[1].name} took $pct% of it."
                }
            }
        }

        var compText = "${data.transactionCount} transactions · no earlier period to compare"
        var compIsPositive = true
        
        val currAmt = if (settings.overviewType == OverviewType.CATEGORY) {
            if (data.totalIncomes.getMoney(currencyCode) > 0L) data.totalIncomes.getMoney(currencyCode) else data.totalExpenses.getMoney(currencyCode)
        } else when(settings.cashFlowFilter) {
            CashFlowFilter.INCOMES -> data.totalIncomes.getMoney(currencyCode)
            CashFlowFilter.EXPENSES -> data.totalExpenses.getMoney(currencyCode)
            CashFlowFilter.NET_INCOMES -> data.totalNetIncomes.getMoney(currencyCode)
        }
        val prevAmt = if (settings.overviewType == OverviewType.CATEGORY) {
            if (data.totalIncomes.getMoney(currencyCode) > 0L) data.previousTotalIncomes.getMoney(currencyCode) else data.previousTotalExpenses.getMoney(currencyCode)
        } else when(settings.cashFlowFilter) {
            CashFlowFilter.INCOMES -> data.previousTotalIncomes.getMoney(currencyCode)
            CashFlowFilter.EXPENSES -> data.previousTotalExpenses.getMoney(currencyCode)
            CashFlowFilter.NET_INCOMES -> data.previousTotalNetIncomes.getMoney(currencyCode)
        }
        
        if (data.previousTransactionCount > 0 && prevAmt != 0L) {
            val diff = (currAmt - prevAmt).toDouble() / Math.abs(prevAmt).toDouble()
            val pctStr = Math.round(Math.abs(diff) * 100).toString() + "%"
            val up = diff > 0
            val isIncome = if (settings.overviewType == OverviewType.CATEGORY) {
                data.totalIncomes.getMoney(currencyCode) > 0L
            } else {
                settings.cashFlowFilter == CashFlowFilter.INCOMES
            }
            val good = if (isIncome) up else !up
            val arrow = if (up) "▲" else "▼"
            compText = "$arrow $pctStr vs the ${data.daysCount} days before"
            compIsPositive = good
        }

        return Triple(insights, compText, compIsPositive)
    }

    private fun buildPeriodUiModels(
        periods: List<PeriodMoney>,
        currencyCode: String,
        decimals: Int,
        formattingSettings: FormattingSettings
    ): List<OverviewPeriodUiModel> {
        val config = formattingSettings.toFormatterConfig(showPlusMinus = true)
        val noSignConfig = config.copy(showPlusMinus = false)

        return periods.mapIndexed { index, period ->
            val startStr = DateUtils.formatMonthDay(period.startDate)
            val endStr = DateUtils.formatMonthDay(period.endDate)
            val dateRangeText = "$startStr — $endStr"
            val startDateTimeSql = DateUtils.getSQLDateTimeString(period.startDate)
            val endDateTimeSql = DateUtils.getSQLDateTimeString(period.endDate)

            val incomeAmt = period.incomes.getMoney(currencyCode)
            val expenseAmt = period.expenses.getMoney(currencyCode)

            val formattedIncome = if (incomeAmt > 0) {
                "↑ " + MoneyFormatter.format(incomeAmt, currencyCode, decimals, noSignConfig)
            } else null

            val formattedExpense = if (expenseAmt > 0) {
                "↓ " + MoneyFormatter.format(expenseAmt, currencyCode, decimals, noSignConfig)
            } else null

            val netAmount = period.netIncomes.getMoney(currencyCode)
            val formattedNetAmount = MoneyFormatter.format(netAmount, currencyCode, decimals, config)
            val netColorType = when {
                netAmount > 0 -> 1
                netAmount < 0 -> -1
                else -> 0
            }

            OverviewPeriodUiModel(
                id = "period_${index}_${period.startDate.time}",
                index = index + 1,
                dateRangeText = dateRangeText,
                startDateTimeSql = startDateTimeSql,
                endDateTimeSql = endDateTimeSql,
                formattedIncome = formattedIncome,
                formattedExpense = formattedExpense,
                formattedNetAmount = formattedNetAmount,
                netAmountColorType = netColorType
            )
        }.reversed()
    }

    private fun resolveWalletInfo(
        walletId: String,
        formatting: FormattingSettings,
        wallets: List<com.sinxn.mymoney.core.data.local.entity.WalletEntity>
    ): WalletInfo {
        return if (walletId == Constants.TOTAL_WALLET_ID) {
            val walletsInTotal = wallets.filter {
                it.countInTotal && (!formatting.excludeArchivedFromTotal || !it.isArchived)
            }
            val distinctCurrencies = walletsInTotal
                .map { it.currency.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
            val currCode = if (distinctCurrencies.size == 1) {
                distinctCurrencies.first()
            } else {
                formatting.globalCurrency.ifEmpty { "USD" }
            }
            val currency = try {
                java.util.Currency.getInstance(currCode)
            } catch (e: Exception) { null }
            val symbol = MoneyFormatter.getCurrencySymbol(currCode)
            val decimals = currency?.defaultFractionDigits ?: 2
            WalletInfo(
                name = "Total",
                icon = "sigma",
                currencyCode = currCode,
                currencySymbol = symbol,
                decimals = decimals
            )
        } else {
            val wallet = wallets.find { it.id == walletId }
            if (wallet != null) {
                val currSymbol = MoneyFormatter.getCurrencySymbol(wallet.currency)
                val decimals = try {
                    java.util.Currency.getInstance(wallet.currency).defaultFractionDigits
                } catch (e: Exception) { 2 }
                WalletInfo(
                    name = wallet.name,
                    icon = wallet.icon,
                    currencyCode = wallet.currency,
                    currencySymbol = currSymbol,
                    decimals = decimals
                )
            } else {
                WalletInfo("Total", "sigma", formatting.globalCurrency, formatting.globalCurrency, 2)
            }
        }
    }

    private data class WalletInfo(
        val name: String,
        val icon: String?,
        val currencyCode: String,
        val currencySymbol: String,
        val decimals: Int
    )
}
