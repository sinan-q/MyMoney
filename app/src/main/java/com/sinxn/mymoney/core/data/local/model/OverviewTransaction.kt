package com.sinxn.mymoney.core.data.local.model

/**
 * Lightweight projection for overview data loading.
 * Matches the legacy OverviewDataLoader query columns.
 */
data class OverviewTransaction(
    val date: String,
    val direction: Int,  // 0: Expense, 1: Income
    val money: Long,
    val walletCurrency: String,
    val walletDecimals: Int,
    val categoryId: String?,
    val categoryParentId: String?
)
