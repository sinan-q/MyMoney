package com.sinxn.mymoney.core.data.importBackup.model

import com.google.gson.annotations.SerializedName

data class JsonPlace(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String?,
    @SerializedName("icon") val icon: String?,
    @SerializedName("address") val address: String?,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("archived") val archived: Boolean? = null,
    @SerializedName("deleted") val deleted: Boolean?,
    @SerializedName("last_edit") val lastEdit: Long?,
    @SerializedName("tag") val tag: String?
)

data class JsonPerson(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String?,
    @SerializedName("icon") val icon: String?,
    @SerializedName("note") val note: String?,
    @SerializedName("archived") val archived: Boolean? = null,
    @SerializedName("deleted") val deleted: Boolean?,
    @SerializedName("last_edit") val lastEdit: Long?,
    @SerializedName("tag") val tag: String?
)

data class JsonEvent(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String?,
    @SerializedName("icon") val icon: String?,
    @SerializedName("note") val note: String?,
    @SerializedName("start_date") val startDate: String?,
    @SerializedName("end_date") val endDate: String?,
    @SerializedName("archived") val archived: Boolean? = null,
    @SerializedName("deleted") val deleted: Boolean?,
    @SerializedName("last_edit") val lastEdit: Long?,
    @SerializedName("tag") val tag: String?
)

data class JsonCurrency(
    @SerializedName(value = "iso", alternate = ["currency_iso"]) val iso: String?,
    @SerializedName(value = "name", alternate = ["currency_name"]) val name: String?,
    @SerializedName(value = "symbol", alternate = ["currency_symbol"]) val symbol: String?,
    @SerializedName(value = "decimals", alternate = ["currency_decimals"]) val decimals: Int?,
    @SerializedName(value = "favourite", alternate = ["currency_favourite"]) val favourite: Boolean?,
    @SerializedName(value = "id", alternate = ["uuid"]) val id: String? = null,
    @SerializedName("last_edit") val lastEdit: Long?,
    @SerializedName("deleted") val deleted: Boolean?
)
