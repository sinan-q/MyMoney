package com.sinxn.mymoney.feature.recap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.data.local.entity.TransactionEntity
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.DateUtils.formatDate
import com.sinxn.mymoney.core.util.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.lang.Math.pow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.pow

@HiltViewModel
class YearRecapViewModel @Inject constructor(
    private val moneyDao: MoneyDao
) : ViewModel() {

    private val _recapState = MutableStateFlow<RecapData?>(null)
    val recapState: StateFlow<RecapData?> = _recapState.asStateFlow()

    private val _loadingState = MutableStateFlow(false)
    val loadingState: StateFlow<Boolean> = _loadingState.asStateFlow()
    
    // Wallets for selection
    val wallets = moneyDao.getAllWalletsIncludingArchived()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Generate Recap for a specific Year and set of Wallets
    fun generateRecap(walletIds: Set<String>, year: Int = 2025) {
        viewModelScope.launch {
            _loadingState.value = true
            try {
                // 1. Fetch Data
                // Since we need to join with categories manually for complex logic or just use TransactionWithCategory
                // MoneyDao doesn't have a specific "getTransactionsByDateRangeAndWallets" easily accessible without custom query
                // So we will fetch all transactions for the max date and filter in memory (not ideal for huge datasets but fine for local DB < 10k items)
                
                val endDate = "$year-12-31 23:59:59"
                val startDate = "$year-01-01 00:00:00"
                
                // Using getAllTransactions and filtering.
                // Optimally we should add a specific query in DAO, but let's stick to existing DAOs if possible or add one.
                // The plan said we added a query? No, we just "ensured ability". 
                // Let's use getAllTransactions(endDate) which returns flow. We take first()
                val allTransactions = moneyDao.getAllTransactions(endDate).first()
                val categories = moneyDao.getCategories().first()
                
                // 2. Filter by Year and Wallet
                val validTransactions = allTransactions.filter { txn ->
                    val txnYear = try {
                        txn.transaction.date.take(4).toInt()
                    } catch (e: Exception) { 0 }
                    
                    
                    val walletId = txn.transaction.walletId
                    val isSystemCategory = txn.transaction.categoryId?.startsWith("system-uuid-") == true
                    
                    txnYear == year && walletIds.contains(walletId) && !isSystemCategory
                }

                // 3. Process Data
                val data = calculateRecapData(validTransactions, categories, year)
                _recapState.value = data
                
            } catch (e: Exception) {
                e.printStackTrace()
                // Handle error
            } finally {
                _loadingState.value = false
            }
        }
    }

    private fun calculateRecapData(
        transactions: List<TransactionWithCategory>, 
        categories: List<CategoryEntity>,
        year: Int
    ): RecapData {
        // --- Helpers ---
        val categoryMap = categories.associateBy { it.id }
        
        fun getParentCategory(id: String?): CategoryEntity? {
            if (id == null) return null
            val cat = categoryMap[id] ?: return null
            return if (cat.parentId != null) getParentCategory(cat.parentId) else cat
        }
        
        fun isChildOf(childId: String?, parentNameQuery: String): Boolean {
            if (childId == null) return false
            var current = categoryMap[childId]
            while (current != null) {
                if (current.name.contains(parentNameQuery, ignoreCase = true)) return true
                current = if (current.parentId != null) categoryMap[current.parentId] else null
            }
            return false
        }
        
        // --- 1. General Stats ---
        val totalSpentLong = transactions
            .filter { it.transaction.direction == 0 } // 0: Expense
            .sumOf { it.transaction.money }
            
        val totalIncomeLong = transactions
            .filter { it.transaction.direction == 1 } // 1: Income
            .sumOf { it.transaction.money }
            
        // Formatting: Assuming standard currency (e.g. INR/USD) where database stores x100
        val currencySymbol = transactions.firstOrNull()?.currencySymbol ?: "₹"
        val decimals = transactions.firstOrNull()?.decimals ?: 2
        val multiplier = 10.0.pow(decimals.toDouble()).toLong()
        
        fun formatMoney(amount: Long): String {
            // MoneyFormatter takes the raw long and handles division internally
            return MoneyFormatter.format(amount, transactions.firstOrNull()?.currencyCode ?: "INR", decimals)
        }

        // --- 2. Top Categories ---
        // Group by Parent Category
        val categoryGroups = transactions
            .filter { it.transaction.direction == 0 }
            .groupBy { 
                val parent = getParentCategory(it.transaction.categoryId)
                parent?.name ?: it.categoryName ?: "Uncategorized" 
            }
            
        val topCategoriesList = categoryGroups.map { (name, txns) ->
            val sum = txns.sumOf { it.transaction.money }
            name to sum
        }.sortedByDescending { it.second }.take(5)
        
        val topCategoriesByCountList = categoryGroups.map { (name, txns) ->
            name to txns.size
        }.sortedByDescending { it.second }.take(5)

        // --- 3. Coffee Metric ---
        // Count txns < 50 vs > 500
        // Database values: 50.00 -> 5000, 500.00 -> 50000 (assuming 2 decimals)
        val thresholdLow = 50 * multiplier
        val thresholdHigh = 500 * multiplier
        
        val expenses = transactions.filter { it.transaction.direction == 0 }
        
        val coffeeLowCount = expenses.count { it.transaction.money < thresholdLow }
        val coffeeHighCount = expenses.count { it.transaction.money > thresholdHigh }
        
        // --- 4. Foodie Factor ---
        val foodTxns = expenses.filter { 
            isChildOf(it.transaction.categoryId, "Food") || isChildOf(it.transaction.categoryId, "Drink")
        }
        val foodTotal = foodTxns.sumOf { it.transaction.money }
        val highestFoodTxn = foodTxns.maxByOrNull { it.transaction.money }
        val foodCount = foodTxns.size
        val foodiePercentage = if (totalIncomeLong > 0) ((foodTotal.toDouble() / totalIncomeLong) * 100).toInt() else 0

        // --- 5. Travel Stats ---
        // "Travel" + Bus, Metro, Oil, Train, Uber
        val travelKeywords = listOf("Bus", "Metro", "Oil", "Train", "Uber", "Travel")
        val travelTxns = expenses.filter { txn ->
            val catName = txn.categoryName ?: ""
             isChildOf(txn.transaction.categoryId, "Travel") || travelKeywords.any { k -> catName.contains(k, ignoreCase = true) }
        }
        
        // Breakdowns
        fun countByKeyword(keyword: String) = travelTxns.count { (it.categoryName ?: "").contains(keyword, ignoreCase = true) }
        fun amountByKeyword(keyword: String) = travelTxns.filter { (it.categoryName ?: "").contains(keyword, ignoreCase = true) }.sumOf { it.transaction.money }

        val uberCount = countByKeyword("Uber")
        val busCount = countByKeyword("Bus")
        val trainCount = countByKeyword("Train")
        val fuelCount = countByKeyword("Oil") + countByKeyword("Fuel") 
        val metroCount = countByKeyword("Metro")
        
        val totalTravelAmt = travelTxns.sumOf { it.transaction.money }

        // --- 6. Other Stats ---
        // Movies
        val movieTxns = expenses.filter { isChildOf(it.transaction.categoryId, "Movie") || isChildOf(it.transaction.categoryId, "Cinema") }
        val movieAmount = movieTxns.sumOf { it.transaction.money }
        val movieCount = movieTxns.size
        val estMovieHours = (movieCount * 2.5).toInt()  
        
        // Lifestyle
        val lifestyleTxns = expenses.filter { isChildOf(it.transaction.categoryId, "Lifestyle") || isChildOf(it.transaction.categoryId, "Shopping") }
        val lifestyleAmount = lifestyleTxns.sumOf { it.transaction.money }

        // --- 7. Busiest Day ---
        val dayGroups = expenses.groupBy { 
             try {
                // strict parsing might fail if format varies
                val dateStr = it.transaction.date.substring(0, 10) 
                 LocalDate.parse(dateStr).dayOfWeek
             } catch(e: Exception) {
                 null
             }
        }.filterKeys { it != null }
        
        val busiestDayByCount = dayGroups.maxByOrNull { it.value.size }
        val busiestDayByAmount = dayGroups.maxByOrNull { it.value.sumOf { txn -> txn.transaction.money } }
        
        // --- 8. Biggest Purchase ---
        val biggestPurchase = expenses.maxByOrNull { it.transaction.money }
        val smallestPurchase = expenses.filter { it.transaction.money > 0 }.minByOrNull { it.transaction.money }

        // --- 9. Streak ---
        val distinctDays = expenses.mapNotNull { 
             try { LocalDate.parse(it.transaction.date.substring(0, 10)) } catch(e:Exception) { null } 
        }.distinct().sorted()
        
        var maxStreak = 0
        var currentStreak = 0
        var streakEndIndex = -1
        
        for (i in 0 until distinctDays.size) {
            if (i > 0) {
                 val prev = distinctDays[i-1]
                 val curr = distinctDays[i]
                 if (prev.plusDays(1) == curr) {
                     currentStreak++
                 } else {
                     currentStreak = 1
                 }
            } else {
                currentStreak = 1
            }
            
            if (currentStreak > maxStreak) {
                maxStreak = currentStreak
                streakEndIndex = i
            }
        }
        
        var streakString = ""
        if (streakEndIndex != -1) {
            val end = distinctDays[streakEndIndex]
            val start = end.minusDays((maxStreak - 1).toLong())
             val fmt = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
            streakString = "${start.format(fmt)} - ${end.format(fmt)}"
        }

        // --- Construct Recap Data ---
        // For Animation: Pass values divided by multiplier to show "Major Units" (e.g. 5000 -> 50)
        // This avoids animating cents which look like huge numbers
        
        // Weekend Warrior: Calculate % of expenses on Sat/Sun
        val weekendExpenses = expenses.filter { 
            try {
                 val dateStr = it.transaction.date.substring(0, 10)
                 val day = LocalDate.parse(dateStr).dayOfWeek
                 day == java.time.DayOfWeek.SATURDAY || day == java.time.DayOfWeek.SUNDAY
            } catch(e: Exception) { false }
        }
        val weekendTotal = weekendExpenses.sumOf { it.transaction.money }
        val weekendPct = if (totalSpentLong > 0) ((weekendTotal.toDouble() / totalSpentLong) * 100).toInt() else 0
        // Spending Distribution < 50, 51-100, 101-200, 201-400, 401-800, 800+
        val dist = mutableMapOf(
            "< 50" to 0, "50-100" to 0, "100-200" to 0, "200-500" to 0, "500+" to 0
        )
        expenses.forEach {
            val amount = it.transaction.money / multiplier
            when {
                amount <= 50 -> dist["< 50"] = (dist["< 50"] ?: 0) + 1
                amount <= 100 -> dist["50-100"] = (dist["50-100"] ?: 0) + 1
                amount <= 200 -> dist["100-200"] = (dist["100-200"] ?: 0) + 1
                amount <= 500 -> dist["200-500"] = (dist["200-500"] ?: 0) + 1
                else -> dist["500+"] = (dist["500+"] ?: 0) + 1
            }
        }
        return RecapData(
            year = year,
            totalSpent = formatMoney(totalSpentLong),
            totalSpentRaw = totalSpentLong / multiplier,
            totalTransactionCount = transactions.size,
            
            busiestDayByCount = busiestDayByCount?.key?.name?.lowercase()?.capitalize() ?: "-",
            busiestDayCount = busiestDayByCount?.value?.size ?: 0,
            
            busiestDayByAmount = busiestDayByAmount?.key?.name?.lowercase()?.capitalize() ?: "-",
            busiestDayAmount = formatMoney(busiestDayByAmount?.value?.sumOf { it.transaction.money } ?: 0),
            busiestDayAmountRaw = (busiestDayByAmount?.value?.sumOf { it.transaction.money } ?: 0) / multiplier,
            
            
            
            weekendPercentage = weekendPct,
            weekendAmount = formatMoney(weekendTotal),
            weekendAmountRaw = weekendTotal / multiplier,
            


            spendingDistribution = dist,
            
            foodiePercentage = foodiePercentage,
            foodieCount = foodCount,
            foodieAmount = formatMoney(foodTotal),
            foodieAmountRaw = foodTotal / multiplier,
            highestFoodPurchaseName = "${highestFoodTxn?.transaction?.description ?: "Unknown"} (${highestFoodTxn?.categoryName ?: "Food"})",
            highestFoodPurchaseAmount = formatMoney(highestFoodTxn?.transaction?.money ?: 0),
            highestFoodPurchaseAmountRaw = (highestFoodTxn?.transaction?.money ?: 0) / multiplier,
            highestFoodPurchaseDate = highestFoodTxn?.transaction?.date?.let { 
                try {
                    val date = LocalDate.parse(it.substring(0, 10))
                    val fmt = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
                    date.format(fmt)
                } catch (e: Exception) {
                    "-"
                }
            } ?: "-",
            
            longestStreakDays = maxStreak,
            longestStreakDateRange = streakString,
            longestStreakCount = if (streakEndIndex != -1) maxStreak else 0, // Simplified: count is days for now
            longestStreakAmount = if (streakEndIndex != -1) {
                val end = distinctDays[streakEndIndex]
                val start = end.minusDays((maxStreak - 1).toLong())
                val streakTxns = expenses.filter { 
                    try {
                         val d = LocalDate.parse(it.transaction.date.substring(0, 10))
                         !d.isBefore(start) && !d.isAfter(end)
                    } catch(e: Exception) { false }
                }
                formatMoney(streakTxns.sumOf { it.transaction.money })
            } else "",
            longestStreakAmountRaw = if (streakEndIndex != -1) {
                val end = distinctDays[streakEndIndex]
                val start = end.minusDays((maxStreak - 1).toLong())
                val streakTxns = expenses.filter { 
                    try {
                         val d = LocalDate.parse(it.transaction.date.substring(0, 10))
                         !d.isBefore(start) && !d.isAfter(end)
                    } catch(e: Exception) { false }
                }
                streakTxns.sumOf { it.transaction.money } / multiplier
            } else 0,
            
            // For list, we keep raw long because we don't animate them yet or logic is simpler
            topCategories = topCategoriesList.map { it.first to (it.second / multiplier) },
            topCategoriesByCount = topCategoriesByCountList,
            
            // Top Month
            topMonthByCount = expenses.groupBy { 
                 try { LocalDate.parse(it.transaction.date.substring(0, 10)).month.name } catch(e:Exception) { "" } 
            }.filterKeys { it.isNotEmpty() }.maxByOrNull { it.value.size }?.key?.lowercase()?.capitalize() ?: "-",
            topMonthCount = expenses.groupBy { 
                 try { LocalDate.parse(it.transaction.date.substring(0, 10)).month.name } catch(e:Exception) { "" } 
            }.filterKeys { it.isNotEmpty() }.maxByOrNull { it.value.size }?.value?.size ?: 0,
            
            // Top Week (Simplified to start date)
            topWeekByCount = expenses.groupBy { 
                 try { 
                     val d = LocalDate.parse(it.transaction.date.substring(0, 10))
                     val field = java.time.temporal.WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear()
                     "${d.year}-W${d.get(field)}"
                 } catch(e:Exception) { "" } 
            }.filterKeys { it.isNotEmpty() }.maxByOrNull { it.value.size }?.key ?: "-",
            topWeekCount = expenses.groupBy { 
                 try { 
                     val d = LocalDate.parse(it.transaction.date.substring(0, 10))
                     val field = java.time.temporal.WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear()
                     "${d.year}-W${d.get(field)}"
                 } catch(e:Exception) { "" } 
            }.filterKeys { it.isNotEmpty() }.maxByOrNull { it.value.size }?.value?.size ?: 0,
            
            // Top Day (By Count)
            topDayByCount = expenses.groupBy {
                 try { LocalDate.parse(it.transaction.date.substring(0, 10)) } catch (e: Exception) { LocalDate.MIN }
            }.filterKeys { it != LocalDate.MIN }.maxByOrNull { it.value.size }?.key?.let {
                 val fmt = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
                 it.format(fmt)
            } ?: "-",
            topDayCount = expenses.groupBy {
                 try { LocalDate.parse(it.transaction.date.substring(0, 10)) } catch (e: Exception) { LocalDate.MIN }
            }.filterKeys { it != LocalDate.MIN }.maxByOrNull { it.value.size }?.value?.size ?: 0,
            
            // Top Month Amount
            topMonthByAmount = expenses.groupBy { 
                 try { LocalDate.parse(it.transaction.date.substring(0, 10)).month.name } catch(e:Exception) { "" } 
            }.filterKeys { it.isNotEmpty() }.maxByOrNull { it.value.sumOf { t -> t.transaction.money } }?.key?.lowercase()?.capitalize() ?: "-",
            topMonthAmount = formatMoney(expenses.groupBy { 
                 try { LocalDate.parse(it.transaction.date.substring(0, 10)).month.name } catch(e:Exception) { "" } 
            }.filterKeys { it.isNotEmpty() }.maxByOrNull { it.value.sumOf { t -> t.transaction.money } }?.value?.sumOf { t -> t.transaction.money } ?: 0),
            topMonthAmountRaw = (expenses.groupBy { 
                 try { LocalDate.parse(it.transaction.date.substring(0, 10)).month.name } catch(e:Exception) { "" } 
            }.filterKeys { it.isNotEmpty() }.maxByOrNull { it.value.sumOf { t -> t.transaction.money } }?.value?.sumOf { t -> t.transaction.money } ?: 0) / multiplier,
            
             // Top Week Amount
            topWeekByAmount = expenses.groupBy { 
                 try { 
                     val d = LocalDate.parse(it.transaction.date.substring(0, 10))
                     d.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                 } catch(e:Exception) { LocalDate.MIN } 
            }.filterKeys { it != LocalDate.MIN }.maxByOrNull { it.value.sumOf { t -> t.transaction.money } }?.let { entry ->
                val start = entry.key
                val end = start.plusDays(6)
                val fmt = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
                "${start.format(fmt)} - ${end.format(fmt)}"
            } ?: "-",
            topWeekAmount = formatMoney(expenses.groupBy { 
                  try { 
                     val d = LocalDate.parse(it.transaction.date.substring(0, 10))
                     d.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                 } catch(e:Exception) { LocalDate.MIN } 
            }.filterKeys { it != LocalDate.MIN }.maxByOrNull { it.value.sumOf { t -> t.transaction.money } }?.value?.sumOf { t -> t.transaction.money } ?: 0),
            topWeekAmountRaw = (expenses.groupBy { 
                  try { 
                     val d = LocalDate.parse(it.transaction.date.substring(0, 10))
                     d.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                 } catch(e:Exception) { LocalDate.MIN } 
            }.filterKeys { it != LocalDate.MIN }.maxByOrNull { it.value.sumOf { t -> t.transaction.money } }?.value?.sumOf { t -> t.transaction.money } ?: 0) / multiplier,
            
            // Top Day Amount (Specific Date)
            topDayByAmount = expenses.groupBy {
                 try { LocalDate.parse(it.transaction.date.substring(0, 10)) } catch (e: Exception) { LocalDate.MIN }
            }.filterKeys { it != LocalDate.MIN }.maxByOrNull { it.value.sumOf { t -> t.transaction.money } }?.key?.let {
                 val fmt = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
                 it.format(fmt)
            } ?: "-",
            topDayAmount = formatMoney(expenses.groupBy {
                 try { LocalDate.parse(it.transaction.date.substring(0, 10)) } catch (e: Exception) { LocalDate.MIN }
            }.filterKeys { it != LocalDate.MIN }.maxByOrNull { it.value.sumOf { t -> t.transaction.money } }?.value?.sumOf { t -> t.transaction.money } ?: 0),
            topDayAmountRaw = (expenses.groupBy {
                 try { LocalDate.parse(it.transaction.date.substring(0, 10)) } catch (e: Exception) { LocalDate.MIN }
            }.filterKeys { it != LocalDate.MIN }.maxByOrNull { it.value.sumOf { t -> t.transaction.money } }?.value?.sumOf { t -> t.transaction.money } ?: 0) / multiplier,
            
            // Travel
            uberCount = uberCount,
            uberAmount = formatMoney(amountByKeyword("Uber")),
            uberAmountRaw = amountByKeyword("Uber") / multiplier,
            busCount = busCount,
            busAmount = formatMoney(amountByKeyword("Bus")),
            busAmountRaw = amountByKeyword("Bus") / multiplier,
            trainCount = trainCount,
            trainAmount = formatMoney(amountByKeyword("Train")),
            trainAmountRaw = amountByKeyword("Train") / multiplier,
            metroRechargeCount = metroCount,
            metroAmount = formatMoney(amountByKeyword("Metro")),
            metroAmountRaw = amountByKeyword("Metro") / multiplier,
            fuelCount = fuelCount,
            fuelAmount = formatMoney(amountByKeyword("Oil") + amountByKeyword("Fuel")),
            fuelAmountRaw = (amountByKeyword("Oil") + amountByKeyword("Fuel")) / multiplier,
            travelTotalAmount = formatMoney(totalTravelAmt),
            travelTotalAmountRaw = totalTravelAmt / multiplier,
            
            movieHours = estMovieHours,
            movieCount = movieCount,
            movieAmount = formatMoney(movieAmount),
            movieAmountRaw = movieAmount / multiplier,
            
            lifestyleAmount = formatMoney(lifestyleAmount),
            lifestyleAmountRaw = lifestyleAmount / multiplier,
            
            biggestPurchaseName = "${biggestPurchase?.transaction?.description ?: "Unknown"} (${biggestPurchase?.categoryName ?: "General"})",
            biggestPurchaseAmount = formatMoney(biggestPurchase?.transaction?.money ?: 0),
            biggestPurchaseAmountRaw = (biggestPurchase?.transaction?.money ?: 0) / multiplier,
            biggestPurchaseDate = biggestPurchase?.transaction?.date?.substring(0, 10) ?: "",
            
            smallestPurchaseName = "${smallestPurchase?.transaction?.description ?: "Unknown"} (${smallestPurchase?.categoryName ?: "General"})",
            smallestPurchaseAmount = formatMoney(smallestPurchase?.transaction?.money ?: 0),
            // Pass full raw amount (cents) for smallest purchase so we can animate decimals
            smallestPurchaseAmountRaw = smallestPurchase?.transaction?.money ?: 0,
            
            totalIncome = formatMoney(totalIncomeLong),
            totalIncomeRaw = totalIncomeLong / multiplier,
            totalExpense = formatMoney(totalSpentLong),
            totalExpenseRaw = totalSpentLong / multiplier,
            incomeExpenseRatio = if (totalIncomeLong > 0) ((totalSpentLong.toDouble() / totalIncomeLong) * 100).toInt() else 0
        )
    }
}

// Extension to help capitalizing
private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}
