package com.sinxn.mymoney.feature.transaction

import com.sinxn.mymoney.feature.overview.GroupType
import java.util.Calendar
import java.util.Date

/**
 * Filter settings for the Wallet Details transaction list.
 *
 * @param walletId         Wallet to filter by — defaults to the wallet's own ID (set at call site).
 * @param dateRangeEnabled Whether start/end date filtering is active (off by default).
 * @param startDate        Start of the date range (meaningful only when [dateRangeEnabled] is true).
 * @param endDate          End of the date range (meaningful only when [dateRangeEnabled] is true).
 * @param groupType        How transactions are grouped in the list (Monthly by default).
 * @param categoryIds      Set of category IDs to include; empty means "All categories".
 */
data class TransactionFilter(
    val walletId: String = "",
    val dateRangeEnabled: Boolean = false,
    val startDate: Date = run {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        cal.time
    },
    val endDate: Date = Date(),
    val groupType: GroupType = GroupType.MONTHLY,
    val categoryIds: Set<String> = emptySet()   // empty = All categories
)
