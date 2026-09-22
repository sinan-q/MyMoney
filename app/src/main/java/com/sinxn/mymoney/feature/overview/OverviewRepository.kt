package com.sinxn.mymoney.feature.overview

import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.model.OverviewTransaction
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.util.Constants
import com.sinxn.mymoney.core.util.DateUtils
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Overview data models — matching legacy MoneyWallet exactly.
 */

enum class GroupType { DAILY, WEEKLY, MONTHLY, YEARLY }

enum class OverviewType { CASH_FLOW, CATEGORY }

enum class CashFlowFilter { INCOMES, EXPENSES, NET_INCOMES }

data class OverviewSettings(
    val startDate: Date,
    val endDate: Date,
    val groupType: GroupType,
    val overviewType: OverviewType = OverviewType.CASH_FLOW,
    val cashFlowFilter: CashFlowFilter = CashFlowFilter.NET_INCOMES,
    val categoryId: String? = null
)

/**
 * Multi-currency money container matching legacy Money.java
 */
data class MultiCurrencyMoney(
    private val currencies: MutableMap<String, Long> = mutableMapOf()
) {
    fun addMoney(currency: String, money: Long) {
        currencies[currency] = (currencies[currency] ?: 0L) + money
    }

    fun removeMoney(currency: String, money: Long) {
        currencies[currency] = (currencies[currency] ?: 0L) - money
    }

    fun getMoney(currency: String): Long = currencies[currency] ?: 0L

    fun getCurrencies(): Set<String> = currencies.keys

    fun getCurrencyMoneys(): Map<String, Long> = currencies
}

/**
 * Data for a single time period — matching legacy PeriodMoney.java
 */
data class PeriodMoney(
    val startDate: Date,
    val endDate: Date,
    val incomes: MultiCurrencyMoney = MultiCurrencyMoney(),
    val expenses: MultiCurrencyMoney = MultiCurrencyMoney(),
    val netIncomes: MultiCurrencyMoney = MultiCurrencyMoney()
) {
    fun addIncome(currency: String, money: Long) {
        incomes.addMoney(currency, money)
        netIncomes.addMoney(currency, money)
    }

    fun addExpense(currency: String, money: Long) {
        expenses.addMoney(currency, money)
        netIncomes.removeMoney(currency, money)
    }
}

/**
 * Chart data point for Vico charts
 */
data class ChartDataPoint(
    val index: Int,
    val value: Float,
    val income: Float = 0f,
    val expense: Float = 0f,
    val label: String
)

/**
 * Complete overview result
 */
data class OverviewData(
    val periods: List<PeriodMoney>,
    val chartDataByCurrency: Map<String, List<ChartDataPoint>>,
    val totalNetIncomes: MultiCurrencyMoney,
    val totalIncomes: MultiCurrencyMoney = MultiCurrencyMoney(),
    val totalExpenses: MultiCurrencyMoney = MultiCurrencyMoney(),
    val transactionCount: Int = 0,
    val globalTotalExpenses: Long = 0L
)

@Singleton
class OverviewRepository @Inject constructor(
    private val moneyDao: MoneyDao,
    private val settingsRepository: SettingsRepository
) {

    /**
     * Default overview settings: Year start to current month end, grouped monthly.
     */
    fun getDefaultSettings(): OverviewSettings {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)

        calendar.set(currentYear, Calendar.JANUARY, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time

        calendar.set(currentYear, currentMonth, 1)
        val lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        calendar.set(Calendar.DAY_OF_MONTH, lastDay)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.time

        return OverviewSettings(
            startDate = startDate,
            endDate = endDate,
            groupType = GroupType.MONTHLY,
            overviewType = OverviewType.CASH_FLOW,
            cashFlowFilter = CashFlowFilter.NET_INCOMES
        )
    }

    /**
     * Load overview data — matching legacy OverviewDataLoader.loadInBackground() 1:1
     */
    suspend fun loadOverviewData(
        walletId: String,
        settings: OverviewSettings
    ): OverviewData {
        val normalizedSettings = settings.copy(
            startDate = setTimeStart(settings.startDate),
            endDate = setTimeEnd(settings.endDate)
        )
        val startDateStr = DateUtils.getSQLDateTimeString(normalizedSettings.startDate)
        val endDateStr = DateUtils.getSQLDateTimeString(normalizedSettings.endDate)
        val maxDateStr = DateUtils.getSQLDateTimeString(Date())

        // Fetch transactions
        val transactions = if (walletId == Constants.TOTAL_WALLET_ID || walletId.isEmpty()) {
            moneyDao.getOverviewTransactionsForTotal(startDateStr, endDateStr, maxDateStr)
        } else {
            moneyDao.getOverviewTransactionsForWallet(walletId, startDateStr, endDateStr, maxDateStr)
        }

        // Apply direction/category filters (matching legacy lines 102-119)
        val filtered = filterTransactions(transactions, normalizedSettings)

        // Build periods (matching legacy getNextPeriod / isAnotherPeriodNeeded)
        val formattingSettings = settingsRepository.formattingSettings.first()
        val totalNetIncomes = MultiCurrencyMoney()
        val totalIncomes = MultiCurrencyMoney()
        val totalExpenses = MultiCurrencyMoney()
        val periods = mutableListOf<PeriodMoney>()

        var transactionIndex = 0
        var currentPeriod: PeriodMoney? = null

        while (isAnotherPeriodNeeded(currentPeriod, normalizedSettings.endDate)) {
            currentPeriod = getNextPeriod(currentPeriod, normalizedSettings, formattingSettings)

            while (transactionIndex < filtered.size) {
                val t = filtered[transactionIndex]
                val txDate = DateUtils.parseDate(t.date) ?: break

                if (belongsToPeriod(currentPeriod, txDate)) {
                    if (t.direction == 1) { // INCOME
                        currentPeriod.addIncome(t.walletCurrency, t.money)
                        totalNetIncomes.addMoney(t.walletCurrency, t.money)
                        totalIncomes.addMoney(t.walletCurrency, t.money)
                    } else { // EXPENSE
                        currentPeriod.addExpense(t.walletCurrency, t.money)
                        totalNetIncomes.removeMoney(t.walletCurrency, t.money)
                        totalExpenses.addMoney(t.walletCurrency, t.money)
                    }
                    transactionIndex++
                } else {
                    break
                }
            }

            periods.add(currentPeriod)
        }

        // Calculate global unfiltered total expenses for category share
        val globalTotalExpenses = transactions.filter { it.direction == 0 }.sumOf { it.money }

        // Generate chart data (matching legacy lines 150-207, excluding radar)
        val chartData = mutableMapOf<String, MutableList<ChartDataPoint>>()
        for (currency in totalNetIncomes.getCurrencies()) {
            val decimals = try {
                java.util.Currency.getInstance(currency).defaultFractionDigits
            } catch (e: Exception) { 2 }
            val divider = Math.pow(10.0, decimals.toDouble())
            val points = mutableListOf<ChartDataPoint>()

            for (i in periods.indices) {
                val pm = periods[i]
                val money = when (settings.cashFlowFilter) {
                    CashFlowFilter.INCOMES -> pm.incomes.getMoney(currency)
                    CashFlowFilter.EXPENSES -> pm.expenses.getMoney(currency)
                    CashFlowFilter.NET_INCOMES -> pm.netIncomes.getMoney(currency)
                }
                val value = (money.toDouble() / divider).toFloat()
                val income = (pm.incomes.getMoney(currency).toDouble() / divider).toFloat()
                val expense = (pm.expenses.getMoney(currency).toDouble() / divider).toFloat()
                points.add(ChartDataPoint(i, value, income = income, expense = expense, label = "${i + 1}"))
            }
            chartData[currency] = points
        }

        return OverviewData(
            periods = periods,
            chartDataByCurrency = chartData,
            totalNetIncomes = totalNetIncomes,
            totalIncomes = totalIncomes,
            totalExpenses = totalExpenses,
            transactionCount = filtered.size,
            globalTotalExpenses = globalTotalExpenses
        )
    }

    private fun filterTransactions(
        transactions: List<OverviewTransaction>,
        settings: OverviewSettings
    ): List<OverviewTransaction> {
        return when (settings.overviewType) {
            OverviewType.CASH_FLOW -> when (settings.cashFlowFilter) {
                CashFlowFilter.INCOMES -> transactions.filter { it.direction == 1 }
                CashFlowFilter.EXPENSES -> transactions.filter { it.direction == 0 }
                CashFlowFilter.NET_INCOMES -> transactions // all
            }
            OverviewType.CATEGORY -> {
                val catId = settings.categoryId ?: return transactions
                transactions.filter { it.categoryId == catId || it.categoryParentId == catId }
            }
        }
    }

    /**
     * Matching legacy OverviewDataLoader.isAnotherPeriodNeeded()
     */
    private fun isAnotherPeriodNeeded(periodMoney: PeriodMoney?, endDate: Date): Boolean {
        return periodMoney == null || periodMoney.endDate.before(endDate)
    }

    /**
     * Matching legacy OverviewDataLoader.getNextPeriod() 1:1
     */
    private fun getNextPeriod(
        lastPeriod: PeriodMoney?,
        settings: OverviewSettings,
        formattingSettings: FormattingSettings
    ): PeriodMoney {
        val startCalendar = Calendar.getInstance()
        if (lastPeriod != null) {
            startCalendar.time = lastPeriod.endDate
            startCalendar.add(Calendar.MILLISECOND, 1)
        } else {
            startCalendar.time = settings.startDate
            startCalendar.set(Calendar.HOUR_OF_DAY, 0)
            startCalendar.set(Calendar.MINUTE, 0)
            startCalendar.set(Calendar.SECOND, 0)
            startCalendar.set(Calendar.MILLISECOND, 0)
        }
        val startDate = startCalendar.time

        val endCalendar = Calendar.getInstance()
        endCalendar.time = startDate

        when (settings.groupType) {
            GroupType.YEARLY -> {
                endCalendar.set(Calendar.MONTH, Calendar.DECEMBER)
                endCalendar.set(Calendar.DAY_OF_MONTH, 31)
            }
            GroupType.MONTHLY -> {
                val firstDayOfMonth = formattingSettings.firstDayOfMonth
                val currentDayOfMonth = endCalendar.get(Calendar.DAY_OF_MONTH)
                if (currentDayOfMonth >= firstDayOfMonth) {
                    endCalendar.set(Calendar.DAY_OF_MONTH, 1)
                    endCalendar.add(Calendar.MONTH, 1)
                }
                endCalendar.set(Calendar.DAY_OF_MONTH, firstDayOfMonth)
                endCalendar.add(Calendar.DAY_OF_MONTH, -1)
            }
            GroupType.WEEKLY -> {
                val firstDayOfWeek = formattingSettings.firstDayOfWeek
                val currentDayOfWeek = endCalendar.get(Calendar.DAY_OF_WEEK)
                var offset = firstDayOfWeek - currentDayOfWeek
                if (offset <= 0) offset += 7
                val requiredDays = offset - 1
                endCalendar.add(Calendar.DAY_OF_MONTH, requiredDays)
            }
            GroupType.DAILY -> {
                // no special calculation needed — period is a single day
            }
        }

        endCalendar.set(Calendar.HOUR_OF_DAY, 23)
        endCalendar.set(Calendar.MINUTE, 59)
        endCalendar.set(Calendar.SECOND, 59)
        endCalendar.set(Calendar.MILLISECOND, 999)

        if (endCalendar.timeInMillis > settings.endDate.time) {
            endCalendar.time = settings.endDate
        }

        return PeriodMoney(startDate, endCalendar.time)
    }

    private fun belongsToPeriod(period: PeriodMoney, date: Date): Boolean {
        return !date.before(period.startDate) && !date.after(period.endDate)
    }

    private fun setTimeStart(date: Date): Date {
        val c = Calendar.getInstance()
        c.time = date
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.time
    }

    private fun setTimeEnd(date: Date): Date {
        val c = Calendar.getInstance()
        c.time = date
        c.set(Calendar.HOUR_OF_DAY, 23)
        c.set(Calendar.MINUTE, 59)
        c.set(Calendar.SECOND, 59)
        c.set(Calendar.MILLISECOND, 999)
        return c.time
    }
}
