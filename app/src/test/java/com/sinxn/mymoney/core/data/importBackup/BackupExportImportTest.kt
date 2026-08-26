package com.sinxn.mymoney.core.data.importBackup

import com.google.gson.Gson
import com.sinxn.mymoney.core.data.importBackup.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BackupExportImportTest {

    private val gson = Gson()

    @Test
    fun testJsonExportImportRoundtrip() {
        val root = BackupRoot(
            header = JsonHeader(versionCode = 2),
            currencies = listOf(
                JsonCurrency(
                    iso = "USD",
                    name = "US Dollar",
                    symbol = "$",
                    decimals = 2,
                    favourite = true,
                    lastEdit = 123456789L,
                    deleted = false
                )
            ),
            wallets = listOf(
                JsonWallet(
                    id = "wallet-1",
                    name = "Cash",
                    icon = "wallet",
                    currency = "USD",
                    startMoney = 1000L,
                    countInTotal = true,
                    archived = false,
                    note = "My cash wallet",
                    index = 0,
                    deleted = false,
                    lastEdit = 123456789L,
                    tag = null
                )
            ),
            categories = listOf(
                JsonCategory(
                    id = "cat-1",
                    name = "Food",
                    icon = "food",
                    type = 0,
                    parentId = null,
                    showReport = true,
                    index = 0,
                    deleted = false,
                    lastEdit = 123456789L,
                    tag = null
                )
            ),
            events = emptyList(),
            places = emptyList(),
            people = emptyList(),
            eventPeople = emptyList(),
            debts = emptyList(),
            debtPeople = emptyList(),
            budgets = emptyList(),
            budgetWallets = emptyList(),
            savings = emptyList(),
            recurrentTransactions = emptyList(),
            recurrentTransfers = emptyList(),
            transactions = listOf(
                JsonTransaction(
                    id = "tx-1",
                    money = 500L,
                    date = "2026-08-25",
                    description = "Lunch",
                    categoryId = "cat-1",
                    walletId = "wallet-1",
                    direction = -1,
                    type = 0,
                    note = null,
                    confirmed = true,
                    countInTotal = true,
                    deleted = false,
                    placeId = null,
                    eventId = null,
                    savingId = null,
                    debtId = null,
                    recurrenceId = null,
                    lastEdit = 123456789L,
                    tag = null
                )
            ),
            transactionPeople = emptyList(),
            transactionModels = emptyList(),
            transfers = emptyList(),
            transferPeople = emptyList(),
            transferModels = emptyList(),
            attachments = emptyList(),
            transactionAttachments = emptyList(),
            transferAttachments = emptyList()
        )

        val json = gson.toJson(root)
        
        // Verify key names matching legacy
        assert(json.contains("\"header\":{\"version_code\":2}"))
        assert(json.contains("\"currencies\":[{\"iso\":\"USD\""))
        assert(json.contains("\"wallets\":[{\"id\":\"wallet-1\""))
        assert(json.contains("\"categories\":[{\"id\":\"cat-1\""))
        assert(json.contains("\"transactions\":[{\"id\":\"tx-1\""))
        assert(json.contains("\"transaction_attachment\":[]"))
        assert(json.contains("\"transfer_attachment\":[]"))

        // Deserialize back
        val parsed = gson.fromJson(json, BackupRoot::class.java)
        assertNotNull(parsed)
        assertEquals(2, parsed.header?.versionCode)
        assertEquals(1, parsed.currencies?.size)
        assertEquals("USD", parsed.currencies?.first()?.iso)
        assertEquals(1, parsed.wallets?.size)
        assertEquals("Cash", parsed.wallets?.first()?.name)
        assertEquals(1, parsed.transactions?.size)
        assertEquals("Lunch", parsed.transactions?.first()?.description)
    }
}
