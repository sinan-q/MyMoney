package com.sinxn.mymoney.feature.transaction

import com.sinxn.mymoney.core.data.local.entity.TransferEntity
import com.sinxn.mymoney.core.util.Direction

data class TransactionFormState(
    val amount: String = "",
    val note: String = "",
    val description: String = "",
    val date: String = "",
    val categoryId: String? = null,
    val walletId: String = "",
    val placeId: String? = null,
    val eventId: String? = null,
    val direction: Int = Direction.EXPENSE,
    val peopleIds: Set<String> = emptySet(),
    val confirmed: Boolean = true,
    val countInTotal: Boolean = true,
    // Transfer specific fields
    val isTransfer: Boolean = false,
    val targetWalletId: String? = null,
    val targetAmount: String = "",
    val transferFee: String = "",
    val transferEntity: TransferEntity? = null,
    // Saving & Debt arguments
    val savingId: String? = null,
    val savingCompletedOnSave: Boolean = false,
    val debtId: String? = null
)
