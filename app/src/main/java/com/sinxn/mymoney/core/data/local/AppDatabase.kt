package com.sinxn.mymoney.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.entity.AttachmentEntity
import com.sinxn.mymoney.core.data.local.entity.BudgetEntity
import com.sinxn.mymoney.core.data.local.entity.BudgetWalletEntity
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.data.local.entity.DebtEntity
import com.sinxn.mymoney.core.data.local.entity.TransactionEntity
import com.sinxn.mymoney.core.data.local.entity.WalletEntity
import com.sinxn.mymoney.core.data.local.entity.PlaceEntity
import com.sinxn.mymoney.core.data.local.entity.PersonEntity
import com.sinxn.mymoney.core.data.local.entity.EventEntity
import com.sinxn.mymoney.core.data.local.entity.RecurrentTransactionEntity
import com.sinxn.mymoney.core.data.local.entity.RecurrentTransferEntity
import com.sinxn.mymoney.core.data.local.entity.SavingEntity
import com.sinxn.mymoney.core.data.local.entity.TransactionAttachmentEntity
import com.sinxn.mymoney.core.data.local.entity.TransactionPeopleEntity
import com.sinxn.mymoney.core.data.local.entity.TransferAttachmentEntity
import com.sinxn.mymoney.core.data.local.entity.TransferEntity
import com.sinxn.mymoney.core.data.local.entity.TransferPeopleEntity
import com.sinxn.mymoney.core.data.local.entity.CurrencyEntity
import com.sinxn.mymoney.core.data.local.entity.TransactionModelEntity
import com.sinxn.mymoney.core.data.local.entity.TransferModelEntity
import com.sinxn.mymoney.core.data.local.entity.EventPeopleEntity
import com.sinxn.mymoney.core.data.local.entity.DebtPeopleEntity

@Database(
    entities = [
        WalletEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        PlaceEntity::class,
        PersonEntity::class,
        EventEntity::class,
        DebtEntity::class,
        SavingEntity::class,
        RecurrentTransactionEntity::class,
        TransferEntity::class,
        RecurrentTransferEntity::class,
        BudgetEntity::class,
        BudgetWalletEntity::class,
        TransactionPeopleEntity::class,
        TransferPeopleEntity::class,
        AttachmentEntity::class,
        TransactionAttachmentEntity::class,
        TransferAttachmentEntity::class,
        CurrencyEntity::class,
        TransactionModelEntity::class,
        TransferModelEntity::class,
        EventPeopleEntity::class,
        DebtPeopleEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun moneyDao(): MoneyDao
}
