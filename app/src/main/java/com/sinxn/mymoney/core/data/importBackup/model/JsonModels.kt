package com.sinxn.mymoney.core.data.importBackup.model

import com.google.gson.annotations.SerializedName

data class JsonHeader(
    @SerializedName("version_code") val versionCode: Int = 2
)

data class BackupRoot(
    @SerializedName("header") val header: JsonHeader? = JsonHeader(),
    @SerializedName("currencies") val currencies: List<JsonCurrency>?,
    @SerializedName("wallets") val wallets: List<JsonWallet>?,
    @SerializedName("categories") val categories: List<JsonCategory>?,
    @SerializedName("events") val events: List<JsonEvent>?,
    @SerializedName("places") val places: List<JsonPlace>?,
    @SerializedName("people") val people: List<JsonPerson>?,
    @SerializedName("event_people") val eventPeople: List<JsonEventPerson>?,
    @SerializedName("debts") val debts: List<JsonDebt>?,
    @SerializedName("debt_people") val debtPeople: List<JsonDebtPerson>?,
    @SerializedName("budgets") val budgets: List<JsonBudget>?,
    @SerializedName("budget_wallets") val budgetWallets: List<JsonBudgetWallet>?,
    @SerializedName("savings") val savings: List<JsonSaving>?,
    @SerializedName("recurrent_transactions") val recurrentTransactions: List<JsonRecurrentTransaction>?,
    @SerializedName("recurrent_transfers") val recurrentTransfers: List<JsonRecurrentTransfer>?,
    @SerializedName("transactions") val transactions: List<JsonTransaction>?,
    @SerializedName("transaction_people") val transactionPeople: List<JsonTransactionPerson>?,
    @SerializedName("transaction_models") val transactionModels: List<JsonTransactionModel>?,
    @SerializedName("transfers") val transfers: List<JsonTransfer>?,
    @SerializedName("transfer_people") val transferPeople: List<JsonTransferPerson>?,
    @SerializedName("transfer_models") val transferModels: List<JsonTransferModel>?,
    @SerializedName("attachments") val attachments: List<JsonAttachment>?,
    @SerializedName(value = "transaction_attachment", alternate = ["transaction_attachments"]) val transactionAttachments: List<JsonTransactionAttachment>?,
    @SerializedName(value = "transfer_attachment", alternate = ["transfer_attachments"]) val transferAttachments: List<JsonTransferAttachment>?
)

data class JsonWallet(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String?,
    @SerializedName("icon") val icon: String?,
    @SerializedName("currency") val currency: String?,
    @SerializedName("start_money") val startMoney: Long?,
    @SerializedName("count_in_total") val countInTotal: Boolean?,
    @SerializedName("archived") val archived: Boolean?,
    @SerializedName("note") val note: String?,
    @SerializedName("index") val index: Int?,
    @SerializedName("deleted") val deleted: Boolean?,
    @SerializedName("last_edit") val lastEdit: Long?,
    @SerializedName("tag") val tag: String?
)

data class JsonCategory(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String?,
    @SerializedName("icon") val icon: String?,
    @SerializedName("type") val type: Int?,
    @SerializedName("parent") val parentId: String?,
    @SerializedName("show_report") val showReport: Boolean?,
    @SerializedName("archived") val archived: Boolean? = null,
    @SerializedName("index") val index: Int?,
    @SerializedName("deleted") val deleted: Boolean?,
    @SerializedName("last_edit") val lastEdit: Long?,
    @SerializedName("tag") val tag: String?
)

data class JsonTransaction(
    @SerializedName("id") val id: String,
    @SerializedName("money") val money: Long?,
    @SerializedName("date") val date: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("category") val categoryId: String?,
    @SerializedName("wallet") val walletId: String?,
    @SerializedName("direction") val direction: Int?,
    @SerializedName("type") val type: Int?,
    @SerializedName("note") val note: String?,
    @SerializedName("confirmed") val confirmed: Boolean?,
    @SerializedName("count_in_total") val countInTotal: Boolean?,
    @SerializedName("deleted") val deleted: Boolean?,
    @SerializedName("place") val placeId: String?,
    @SerializedName("event") val eventId: String?,
    @SerializedName("saving") val savingId: String?,
    @SerializedName("debt") val debtId: String?,
    @SerializedName("recurrence") val recurrenceId: String?,
    @SerializedName("last_edit") val lastEdit: Long?,
    @SerializedName("tag") val tag: String?
)
