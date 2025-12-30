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
    @SerializedName("model_transaction_money") val money: Long?,
    @SerializedName("model_transaction_description") val description: String?,
    @SerializedName("model_transaction_category") val categoryId: String?,
    @SerializedName("model_transaction_direction") val direction: Int?,
    @SerializedName("model_transaction_wallet") val walletId: String?,
    @SerializedName("model_transaction_place") val placeId: String?,
    @SerializedName("model_transaction_note") val note: String?,
    @SerializedName("model_transaction_event") val eventId: String?,
    @SerializedName("model_transaction_confirmed") val confirmed: Boolean?,
    @SerializedName("model_transaction_count_in_total") val countInTotal: Boolean?,
    @SerializedName("model_transaction_tag") val tag: String?,
    @SerializedName("last_edit") val lastEdit: Long?,
    @SerializedName("deleted") val deleted: Boolean?
)

data class JsonTransferModel(
    @SerializedName("id") val id: String, // Value is the UUID
    @SerializedName("model_transfer_description") val description: String?,
    @SerializedName("model_transfer_from_wallet") val walletFromId: String?,
    @SerializedName("model_transfer_to_wallet") val walletToId: String?,
    @SerializedName("model_transfer_from_money") val moneyFrom: Long?,
    @SerializedName("model_transfer_to_money") val moneyTo: Long?,
    @SerializedName("model_transfer_tax_money") val moneyTax: Long?,
    @SerializedName("model_transfer_note") val note: String?,
    @SerializedName("model_transfer_event") val eventId: String?,
    @SerializedName("model_transfer_place") val placeId: String?,
    @SerializedName("model_transfer_confirmed") val confirmed: Boolean?,
    @SerializedName("model_transfer_count_in_total") val countInTotal: Boolean?,
    @SerializedName("model_transfer_tag") val tag: String?,
    @SerializedName("last_edit") val lastEdit: Long?,
    @SerializedName("deleted") val deleted: Boolean?
)
