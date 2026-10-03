package com.sinxn.mymoney.feature.wallet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.data.local.entity.WalletEntity
import com.sinxn.mymoney.core.data.local.model.TransactionListItem
import com.sinxn.mymoney.core.data.local.model.WalletWithBalance
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.util.Constants
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MoneyFormatter
import com.sinxn.mymoney.feature.overview.GroupType
import com.sinxn.mymoney.feature.transaction.TransactionFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import javax.inject.Inject

@HiltViewModel
class WalletDetailsViewModel @Inject constructor(
    private val moneyDao: MoneyDao,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val walletId: String = checkNotNull(savedStateHandle["walletId"])

    // ── Filter state ─────────────────────────────────────────────────────────
    private val _filter = MutableStateFlow(TransactionFilter(walletId = walletId))
    val filter: StateFlow<TransactionFilter> = _filter.asStateFlow()

    private val _showFilterSheet = MutableStateFlow(false)
    val showFilterSheet: StateFlow<Boolean> = _showFilterSheet.asStateFlow()

    fun showFilterSheet() { _showFilterSheet.update { true } }
    fun dismissFilterSheet() { _showFilterSheet.update { false } }

    fun applyFilter(newFilter: TransactionFilter) {
        _filter.update { newFilter }
        _showFilterSheet.update { false }
    }

    // ── Categories for the filter sheet ──────────────────────────────────────
    val allCategories: StateFlow<List<CategoryEntity>> = moneyDao.getCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Pending transactions ─────────────────────────────────────────────────
    val pendingTransactions: StateFlow<List<TransactionWithCategory>> =
        if (walletId == Constants.TOTAL_WALLET_ID) {
            moneyDao.getPendingUnconfirmedTransactions()
        } else {
            moneyDao.getPendingUnconfirmedTransactionsForWallet(walletId)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Save as current wallet accessible on launch
        viewModelScope.launch {
            try {
                if (walletId == Constants.TOTAL_WALLET_ID) {
                    settingsRepository.setCurrentWalletId(walletId)
                } else {
                    moneyDao.getWalletById(walletId)?.let { _ ->
                        settingsRepository.setCurrentWalletId(walletId)
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    val allWallets: StateFlow<List<WalletWithBalance>> =
        moneyDao.getWalletsWithBalance(DateUtils.getSQLDateTimeString(java.util.Date()))
            .map { list -> list.filter { !it.wallet.isArchived } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    val wallet: StateFlow<WalletWithBalance?> = combine(
        settingsRepository.formattingSettings,
        moneyDao.getWalletsWithBalance(DateUtils.getSQLDateTimeString(java.util.Date()))
    ) { settings, allWallets ->
        if (walletId == Constants.TOTAL_WALLET_ID) {
            val walletsInTotal = allWallets.filter {
                it.wallet.countInTotal && (!settings.excludeArchivedFromTotal || !it.wallet.isArchived)
            }
            val totalBalance = walletsInTotal.sumOf { it.currentBalance }
            val distinctCurrencies = walletsInTotal
                .map { it.wallet.currency.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
            val isTotalValid = distinctCurrencies.size <= 1
            val effectiveCurrency = if (distinctCurrencies.size == 1) {
                distinctCurrencies.first()
            } else {
                settings.globalCurrency.ifEmpty { "USD" }
            }
            val currency = try {
                java.util.Currency.getInstance(effectiveCurrency)
            } catch (e: Exception) {
                null
            }
            val decimals = if (distinctCurrencies.size == 1) {
                walletsInTotal.firstOrNull()?.decimals ?: (currency?.defaultFractionDigits ?: 2)
            } else {
                currency?.defaultFractionDigits ?: 2
            }
            val currencySymbol = if (distinctCurrencies.size == 1) {
                walletsInTotal.firstOrNull()?.currencySymbol ?: (currency?.symbol ?: effectiveCurrency)
            } else {
                currency?.symbol ?: effectiveCurrency
            }

            val breakdown = if (!isTotalValid && walletsInTotal.isNotEmpty()) {
                walletsInTotal
                    .groupBy { it.wallet.currency }
                    .map { (currency, group) ->
                        val sum = group.sumOf { it.currentBalance }
                        val groupDecimals = group.firstOrNull()?.decimals ?: 2
                        MoneyFormatter.format(
                            amount = sum,
                            currencyCode = currency,
                            decimals = groupDecimals
                        )
                    }.joinToString(", ")
            } else null

            WalletWithBalance(
                wallet = WalletEntity(
                    id = Constants.TOTAL_WALLET_ID,
                    name = "Total",
                    icon = "sigma",
                    currency = effectiveCurrency,
                    startMoney = 0,
                    isArchived = false,
                    note = null,
                    countInTotal = false,
                    index = -1,
                    isDeleted = false,
                    lastEdit = 0,
                    tag = null
                ),
                currentBalance = totalBalance,
                decimals = decimals,
                currencySymbol = currencySymbol,
                isTotalValid = isTotalValid,
                balanceBreakdown = breakdown
            )
        } else {
            allWallets.find { it.wallet.id == walletId }
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val formattingSettings = settingsRepository.formattingSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FormattingSettings()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: StateFlow<List<TransactionListItem>> = combine(
        formattingSettings,
        wallet,
        _filter
    ) { settings, walletInfo, txFilter ->
        val isTotalValid = walletInfo?.isTotalValid ?: true
        val maxDate = if (settings.includeFutureTransactions) "9999-12-31 23:59:59"
                      else DateUtils.getSQLDateTimeString(java.util.Date())

        // Determine effective wallet to query; filter may override the screen wallet
        val effectiveWalletId = if (txFilter.walletId.isNotEmpty()) txFilter.walletId else walletId

        val transactionsFlow: Flow<List<TransactionWithCategory>> = when {
            txFilter.dateRangeEnabled -> {
                val startSql = DateUtils.getSQLDateTimeString(txFilter.startDate)
                val endSql = DateUtils.getSQLDateTimeString(txFilter.endDate)
                if (effectiveWalletId == Constants.TOTAL_WALLET_ID) {
                    moneyDao.getTransactionsForTotalInPeriod(startSql, endSql, maxDate)
                } else {
                    moneyDao.getTransactionsForWalletInPeriod(effectiveWalletId, startSql, endSql, maxDate)
                }
            }
            effectiveWalletId == Constants.TOTAL_WALLET_ID ->
                moneyDao.getAllTransactions(maxDate)
            else ->
                moneyDao.getTransactionsForWallet(effectiveWalletId, maxDate)
        }

        transactionsFlow.map { list ->
            // Apply category filter in-memory (empty = All)
            val filtered = if (txFilter.categoryIds.isEmpty()) list
                           else list.filter { it.transaction.categoryId in txFilter.categoryIds }

            // Pre-parse dates to avoid repeated parsing during sort and group
            val validTransactions = filtered.map {
                it to DateUtils.parseDate(it.transaction.date)
            }

            // Determine grouping function based on filter.groupType
            val groupFn: (java.util.Date) -> java.util.Date = when (txFilter.groupType) {
                GroupType.DAILY -> { date -> DateUtils.getStartOfDay(date) }
                GroupType.WEEKLY -> { date -> DateUtils.getStartOfWeek(date) }
                GroupType.MONTHLY -> { date -> DateUtils.getStartOfBudgetMonth(date, settings.firstDayOfMonth) }
                GroupType.YEARLY -> { date -> DateUtils.getStartOfYear(date) }
            }

            val grouped = validTransactions
                .sortedByDescending { it.second }
                .groupBy { (_, date) -> groupFn(date) }

            val result = ArrayList<TransactionListItem>(filtered.size + grouped.size)

            grouped.forEach { (monthDate, transactionsInGroup) ->
                var total = 0L
                var income = 0L
                var expense = 0L

                transactionsInGroup.forEach { (t, _) ->
                    if (t.transaction.countInTotal && t.transaction.confirmed) {
                        if (t.transaction.direction == 1) {
                            total += t.transaction.money
                            income += t.transaction.money
                        } else {
                            total -= t.transaction.money
                            expense += t.transaction.money
                        }
                    }
                }

                val monthBreakdown = if (!isTotalValid && transactionsInGroup.isNotEmpty()) {
                    transactionsInGroup
                        .map { it.first }
                        .groupBy { it.currencyCode ?: "" }
                        .map { (currency, group) ->
                            var groupTotal = 0L
                            group.forEach { t ->
                                if (t.transaction.countInTotal && t.transaction.confirmed) {
                                    if (t.transaction.direction == 1) groupTotal += t.transaction.money
                                    else groupTotal -= t.transaction.money
                                }
                            }
                            val groupDecimals = group.firstOrNull()?.decimals ?: 2
                            MoneyFormatter.format(
                                amount = groupTotal,
                                currencyCode = currency,
                                decimals = groupDecimals
                            )
                        }.joinToString(", ")
                } else null

                result.add(
                    TransactionListItem.Header(
                        monthDate, total, income, expense, isTotalValid, monthBreakdown,
                        transactionsInGroup.size
                    )
                )
                transactionsInGroup.forEach { (t, _) ->
                    result.add(TransactionListItem.Transaction(t))
                }
            }
            result
        }
    }.flatMapLatest { it }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun toggleCountInTotal() {
        if (walletId == Constants.TOTAL_WALLET_ID) return
        viewModelScope.launch {
            moneyDao.getWalletById(walletId)?.let { current ->
                moneyDao.updateWalletCountInTotal(
                    walletId = walletId,
                    countInTotal = !current.countInTotal,
                    lastEdit = System.currentTimeMillis()
                )
            }
        }
    }

    fun toggleArchived() {
        if (walletId == Constants.TOTAL_WALLET_ID) return
        viewModelScope.launch {
            moneyDao.getWalletById(walletId)?.let { current ->
                moneyDao.updateWalletArchived(
                    walletId = walletId,
                    isArchived = !current.isArchived,
                    lastEdit = System.currentTimeMillis()
                )
            }
        }
    }

    fun confirmTransaction(transactionId: String) {
        viewModelScope.launch {
            moneyDao.confirmTransaction(transactionId, System.currentTimeMillis())
        }
    }

    fun dismissTransaction(transactionId: String) {
        viewModelScope.launch {
            moneyDao.softDeleteTransaction(transactionId, System.currentTimeMillis())
        }
    }
}
