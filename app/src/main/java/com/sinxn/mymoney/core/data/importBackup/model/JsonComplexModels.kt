package com.sinxn.mymoney.core.data.importBackup.model

import com.google.gson.annotations.SerializedName

data class JsonBudget(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: Int?,
    @SerializedName("category") val categoryId: String?,
    @SerializedName("start_date") val startDate: String?,
    @SerializedName("end_date") val endDate: String?,
    @SerializedName("money") val money: Long?,
    @SerializedName("currency") val currency: String?,
    @SerializedName("deleted") val deleted: Boolean?,
    @SerializedName("last_edit") val lastEdit: Long?,
    @SerializedName("tag") val tag: String?
)

data class JsonBudgetWallet(
    @SerializedName("id") val id: String,
    @SerializedName("budget") val budgetId: String,
    @SerializedName("wallet") val walletId: String,
    @SerializedName("deleted") val deleted: Boolean?,
    @SerializedName("last_edit") val lastEdit: Long?
)

data class JsonAttachment(
    @SerializedName("id") val id: String,
    @SerializedName("file") val file: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("size") val size: Long?,
    @SerializedName("deleted") val deleted: Boolean?,
    @SerializedName("last_edit") val lastEdit: Long?,
    @SerializedName("tag") val tag: String?
)

data class JsonTransactionAttachment(
    @SerializedName("id") val id: String,
    @SerializedName("transaction") val transactionId: String,
    @SerializedName("attachment") val attachmentId: String,
    @SerializedName("deleted") val deleted: Boolean?,
    @SerializedName("last_edit") val lastEdit: Long?
)

data class JsonTransferAttachment(
    @SerializedName("id") val id: String,
    @SerializedName("transfer") val transferId: String,
    @SerializedName("attachment") val attachmentId: String,
    @SerializedName("deleted") val deleted: Boolean?,
    @SerializedName("last_edit") val lastEdit: Long?
)

data class JsonTransactionPerson(
    @SerializedName("id") val id: String,
    @SerializedName("transaction") val transactionId: String,
    @SerializedName("person") val personId: String,
    @SerializedName("deleted") val deleted: Boolean?,
    @SerializedName("last_edit") val lastEdit: Long?
)

data class JsonTransferPerson(
    @SerializedName("id") val id: String,
    @SerializedName("transfer") val transferId: String,
    @SerializedName("person") val personId: String,
    @SerializedName("deleted") val deleted: Boolean?,
    @SerializedName("last_edit") val lastEdit: Long?
)

data class JsonRecurrentTransfer(
    @SerializedName("id") val id: String,
    @SerializedName("description") val description: String?,
    @SerializedName("from_wallet") val walletFromId: String?,
    @SerializedName("to_wallet") val walletToId: String?,
    @SerializedName("from_money") val moneyFrom: Long?,
    @SerializedName("to_money") val moneyTo: Long?,
    @SerializedName("tax_money") val moneyTax: Long?,
    @SerializedName("note") val note: String?,
    @SerializedName("event") val eventId: String?,
    @SerializedName("place") val placeId: String?,
    @SerializedName("confirmed") val confirmed: Boolean?,
    @SerializedName("count_in_total") val countInTotal: Boolean?,
    @SerializedName("start_date") val startDate: String?,
    @SerializedName("rule") val rule: String?,
    @SerializedName("last_occurrence") val lastOccurrence: String?,
    @SerializedName("next_occurrence") val nextOccurrence: String?,
    @SerializedName("deleted") val deleted: Boolean?,
    @SerializedName("last_edit") val lastEdit: Long?,
    @SerializedName("tag") val tag: String?
)

data class JsonEventPerson(
    @SerializedName("id") val id: String,
    @SerializedName("event") val eventId: String,
    @SerializedName("person") val personId: String,
    @SerializedName("deleted") val deleted: Boolean?,
    @SerializedName("last_edit") val lastEdit: Long?
)

data class JsonDebtPerson(
    @SerializedName("id") val id: String,
    @SerializedName("debt") val debtId: String,
    @SerializedName("person") val personId: String,
    @SerializedName("deleted") val deleted: Boolean?,
    @SerializedName("last_edit") val lastEdit: Long?
)
