package com.sinxn.mymoney.core.data.importBackup.model

data class BackupAnalysisReport(
    val wallets: EntityStats,
    val categories: EntityStats,
    val transactions: EntityStats,
    val transfers: EntityStats,
    val debts: EntityStats,
    val savings: EntityStats,
    val recurrentTransactions: EntityStats,
    val recurrentTransfers: EntityStats,
    val places: EntityStats,
    val people: EntityStats,
    val events: EntityStats,
    val budgets: EntityStats,
    val attachments: EntityStats,
    val currencies: EntityStats,
    val transactionModels: EntityStats,
    val transferModels: EntityStats,
    val eventPeople: EntityStats,
    val debtPeople: EntityStats,
    val debugInfo: String = ""
)

data class EntityStats(
    val total: Int,
    val softDeleted: Int,
    val zombies: Int = 0, // Items with invalid FKs
    val invalid: Int = 0 // Malformed items (e.g. missing primary keys)
)
