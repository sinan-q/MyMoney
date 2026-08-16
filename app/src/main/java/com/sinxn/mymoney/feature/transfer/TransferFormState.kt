package com.sinxn.mymoney.feature.transfer

import com.sinxn.mymoney.core.data.local.entity.TransferEntity

data class TransferFormState(
    val amount: String = "",
    val targetAmount: String = "",
    val transferFee: String = "",
    val walletId: String = "",
    val targetWalletId: String? = null,
    val note: String = "",
    val description: String = "",
    val date: String = "",
    val placeId: String? = null,
    val eventId: String? = null,
    val peopleIds: Set<String> = emptySet(),
    val confirmed: Boolean = true,
    val countInTotal: Boolean = true,
    val transferEntity: TransferEntity? = null
)
