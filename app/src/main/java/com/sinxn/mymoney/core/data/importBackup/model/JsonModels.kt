package com.sinxn.mymoney.core.data.importBackup.model

import com.google.gson.annotations.SerializedName

data class BackupRoot(
    @SerializedName("wallets") val wallets: List<JsonWallet>?,
    @SerializedName("categories") val categories: List<JsonCategory>?,
    @SerializedName("transactions") val transactions: List<JsonTransaction>?
)

data class JsonWallet(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String?,
    @SerializedName("icon") val icon: String?,
    @SerializedName("currency") val currency: String?,
    @SerializedName("start_money") val startMoney: Long?,
    @SerializedName("count_in_total") val countInTotal: Boolean?,
    @SerializedName("archived") val archived: Boolean?,
    @SerializedName("note") val note: String?,
    @SerializedName("index") val index: Int?
)

data class JsonCategory(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String?,
    @SerializedName("icon") val icon: String?,
    @SerializedName("type") val type: Int?,
    @SerializedName("parent") val parentId: Long?,
    @SerializedName("show_report") val showReport: Boolean?,
    @SerializedName("index") val index: Int?
)

data class JsonTransaction(
    @SerializedName("id") val id: Long,
    @SerializedName("money") val money: Long?,
    @SerializedName("date") val date: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("category") val categoryId: Long?,
    @SerializedName("wallet") val walletId: Long?,
    @SerializedName("direction") val direction: Int?,
    @SerializedName("type") val type: Int?,
    @SerializedName("note") val note: String?,
    @SerializedName("confirmed") val confirmed: Boolean?
)
