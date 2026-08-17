package com.sinxn.mymoney.core.data.importBackup.model

import com.google.gson.annotations.SerializedName

data class JsonDebt(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: Int?,
    @SerializedName("icon") val icon: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("date") val date: String?,
    @SerializedName("expiration_date") val expirationDate: String?,
    @SerializedName("wallet") val walletId: String?,
    @SerializedName("place") val placeId: String?,
    @SerializedName("money") val money: Long?,
    @SerializedName("archived") val archived: Boolean?,
    @SerializedName("note") val note: String?,
    @SerializedName("deleted") val deleted: Boolean?,
    @SerializedName("last_edit") val lastEdit: Long?,
    @SerializedName("tag") val tag: String?
)

data class JsonSaving(
    @SerializedName("id") val id: String,
    @SerializedName("description") val description: String?,
    @SerializedName("icon") val icon: String?,
    @SerializedName("start_money") val startMoney: Long?,
    @SerializedName("end_money") val endMoney: Long?,
    @SerializedName("wallet") val walletId: String?,
    @SerializedName("end_date") val endDate: String?,
    @SerializedName("complete") val complete: Boolean?,
    @SerializedName("note") val note: String?,
    @SerializedName("deleted") val deleted: Boolean?,
    @SerializedName("last_edit") val lastEdit: Long?,
    @SerializedName("tag") val tag: String?
)

data class JsonRecurrentTransaction(
    @SerializedName("id") val id: String,
    @SerializedName("money") val money: Long?,
    @SerializedName("description") val description: String?,
    @SerializedName("category") val categoryId: String?,
    @SerializedName("direction") val direction: Int?,
    @SerializedName("wallet") val walletId: String?,
    @SerializedName("place") val placeId: String?,
    @SerializedName("note") val note: String?,
    @SerializedName("event") val eventId: String?,
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

data class JsonTransfer(
    @SerializedName("id") val id: String,
    @SerializedName("description") val description: String?,
    @SerializedName("date") val date: String?,
    @SerializedName(value = "from", alternate = ["transaction_from", "transactionFrom"]) val transactionFromId: String?,
    @SerializedName(value = "to", alternate = ["transaction_to", "transactionTo"]) val transactionToId: String?,
    @SerializedName(value = "tax", alternate = ["transaction_tax", "transactionTax"]) val transactionTaxId: String?,
    @SerializedName("note") val note: String?,
    @SerializedName("place") val placeId: String?,
    @SerializedName("event") val eventId: String?,
    @SerializedName("recurrence") val recurrenceId: String?,
    @SerializedName("confirmed") val confirmed: Boolean?,
    @SerializedName("count_in_total") val countInTotal: Boolean?,
    @SerializedName("deleted") val deleted: Boolean?,
    @SerializedName("last_edit") val lastEdit: Long?,
    @SerializedName("tag") val tag: String?
)

data class JsonTransactionModel(
    @SerializedName("id") val id: String, // Value is the UUID
    @SerializedName(value = "money", alternate = ["model_transaction_money"]) val money: Long?,
    @SerializedName(value = "description", alternate = ["model_transaction_description"]) val description: String?,
    @SerializedName(value = "category", alternate = ["model_transaction_category", "categoryId"]) val categoryId: String?,
    @SerializedName(value = "direction", alternate = ["model_transaction_direction"]) val direction: Int?,
    @SerializedName(value = "wallet", alternate = ["model_transaction_wallet", "walletId"]) val walletId: String?,
    @SerializedName(value = "place", alternate = ["model_transaction_place", "placeId"]) val placeId: String?,
    @SerializedName(value = "note", alternate = ["model_transaction_note"]) val note: String?,
    @SerializedName(value = "event", alternate = ["model_transaction_event", "eventId"]) val eventId: String?,
    @SerializedName(value = "confirmed", alternate = ["model_transaction_confirmed"]) val confirmed: Boolean?,
    @SerializedName(value = "count_in_total", alternate = ["model_transaction_count_in_total"]) val countInTotal: Boolean?,
    @SerializedName(value = "tag", alternate = ["model_transaction_tag"]) val tag: String?,
    @SerializedName("last_edit") val lastEdit: Long?,
    @SerializedName("deleted") val deleted: Boolean?
)

data class JsonTransferModel(
    @SerializedName("id") val id: String, // Value is the UUID
    @SerializedName(value = "description", alternate = ["model_transfer_description"]) val description: String?,
    @SerializedName(value = "from_wallet", alternate = ["model_transfer_from_wallet", "walletFromId", "wallet_from"]) val walletFromId: String?,
    @SerializedName(value = "to_wallet", alternate = ["model_transfer_to_wallet", "walletToId", "wallet_to"]) val walletToId: String?,
    @SerializedName(value = "from_money", alternate = ["model_transfer_from_money", "moneyFrom", "money_from"]) val moneyFrom: Long?,
    @SerializedName(value = "to_money", alternate = ["model_transfer_to_money", "moneyTo", "money_to"]) val moneyTo: Long?,
    @SerializedName(value = "tax_money", alternate = ["model_transfer_tax_money", "moneyTax", "money_tax"]) val moneyTax: Long?,
    @SerializedName(value = "note", alternate = ["model_transfer_note"]) val note: String?,
    @SerializedName(value = "event", alternate = ["model_transfer_event", "eventId"]) val eventId: String?,
    @SerializedName(value = "place", alternate = ["model_transfer_place", "placeId"]) val placeId: String?,
    @SerializedName(value = "confirmed", alternate = ["model_transfer_confirmed"]) val confirmed: Boolean?,
    @SerializedName(value = "count_in_total", alternate = ["model_transfer_count_in_total"]) val countInTotal: Boolean?,
    @SerializedName(value = "tag", alternate = ["model_transfer_tag"]) val tag: String?,
    @SerializedName("last_edit") val lastEdit: Long?,
    @SerializedName("deleted") val deleted: Boolean?
)
