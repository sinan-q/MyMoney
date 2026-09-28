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
import com.sinxn.mymoney.core.data.local.dao.CustomFieldDao
import com.sinxn.mymoney.core.data.local.entity.CustomFieldDefinitionEntity
import com.sinxn.mymoney.core.data.local.entity.CustomFieldExtractionRuleEntity
import com.sinxn.mymoney.core.data.local.entity.CustomFieldOrphanValueEntity
import com.sinxn.mymoney.core.data.local.entity.CustomFieldSnapshotEntity
import com.sinxn.mymoney.core.data.local.entity.CustomFieldTombstoneEntity
import com.sinxn.mymoney.core.data.local.entity.CustomFieldValueEntity
import com.sinxn.mymoney.core.data.local.entity.TransactionSyncHashEntity
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
        DebtPeopleEntity::class,
        CustomFieldDefinitionEntity::class,
        CustomFieldValueEntity::class,
        CustomFieldTombstoneEntity::class,
        CustomFieldOrphanValueEntity::class,
        CustomFieldExtractionRuleEntity::class,
        CustomFieldSnapshotEntity::class,
        TransactionSyncHashEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun moneyDao(): MoneyDao
    abstract fun customFieldDao(): CustomFieldDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // CustomFieldDefinitionEntity
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `custom_field_definitions` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `categoryId` TEXT NOT NULL,
                        `key` TEXT NOT NULL,
                        `label` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL DEFAULT 0,
                        `isRequired` INTEGER NOT NULL DEFAULT 0,
                        `archivedAt` INTEGER,
                        `visibilityDependsOnFieldId` TEXT,
                        `visibilityDependsOnValue` TEXT,
                        `lastEdit` INTEGER NOT NULL,
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`visibilityDependsOnFieldId`)
                            REFERENCES `custom_field_definitions`(`id`) ON DELETE SET NULL
                    )
                """)
                db.execSQL("""CREATE UNIQUE INDEX IF NOT EXISTS `idx_cfd_category_key`
                    ON `custom_field_definitions`(`categoryId`, `key`)""")
                db.execSQL("""CREATE INDEX IF NOT EXISTS `idx_cfd_category_sort`
                    ON `custom_field_definitions`(`categoryId`, `sortOrder`)""")
                db.execSQL("""CREATE INDEX IF NOT EXISTS `idx_cfd_visibility`
                    ON `custom_field_definitions`(`visibilityDependsOnFieldId`)""")

                // CustomFieldValueEntity
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `custom_field_values` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `transactionId` TEXT NOT NULL,
                        `fieldId` TEXT NOT NULL,
                        `value` TEXT NOT NULL,
                        `normalizedValue` TEXT NOT NULL,
                        `source` TEXT NOT NULL,
                        `lastEdit` INTEGER NOT NULL,
                        FOREIGN KEY(`transactionId`) REFERENCES `transactions`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`fieldId`)
                            REFERENCES `custom_field_definitions`(`id`) ON DELETE CASCADE
                    )
                """)
                db.execSQL("""CREATE UNIQUE INDEX IF NOT EXISTS `idx_cfv_tx_field`
                    ON `custom_field_values`(`transactionId`, `fieldId`)""")
                db.execSQL("""CREATE INDEX IF NOT EXISTS `idx_cfv_field_norm`
                    ON `custom_field_values`(`fieldId`, `normalizedValue`)""")
                db.execSQL("""CREATE INDEX IF NOT EXISTS `idx_cfv_tx`
                    ON `custom_field_values`(`transactionId`)""")

                // CustomFieldTombstoneEntity
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `custom_field_tombstones` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `categoryId` TEXT NOT NULL,
                        `fieldKey` TEXT NOT NULL,
                        `deletedAt` INTEGER NOT NULL
                    )
                """)
                db.execSQL("""CREATE UNIQUE INDEX IF NOT EXISTS `idx_cft_cat_key`
                    ON `custom_field_tombstones`(`categoryId`, `fieldKey`)""")

                // CustomFieldOrphanValueEntity
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `custom_field_orphan_values` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `transactionId` TEXT NOT NULL,
                        `fieldKey` TEXT NOT NULL,
                        `value` TEXT NOT NULL,
                        `lastEdit` INTEGER NOT NULL,
                        FOREIGN KEY(`transactionId`) REFERENCES `transactions`(`id`) ON DELETE CASCADE
                    )
                """)
                db.execSQL("""CREATE UNIQUE INDEX IF NOT EXISTS `idx_cfov_tx_key`
                    ON `custom_field_orphan_values`(`transactionId`, `fieldKey`)""")
                db.execSQL("""CREATE INDEX IF NOT EXISTS `idx_cfov_tx`
                    ON `custom_field_orphan_values`(`transactionId`)""")

                // CustomFieldExtractionRuleEntity
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `custom_field_extraction_rules` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `fieldId` TEXT NOT NULL,
                        `ruleOrder` INTEGER NOT NULL DEFAULT 0,
                        `mode` TEXT NOT NULL,
                        `pattern` TEXT NOT NULL,
                        `targetColumns` TEXT NOT NULL DEFAULT 'description',
                        `fieldMappings` TEXT NOT NULL DEFAULT '{}',
                        `lastEdit` INTEGER NOT NULL,
                        FOREIGN KEY(`fieldId`)
                            REFERENCES `custom_field_definitions`(`id`) ON DELETE CASCADE
                    )
                """)
                db.execSQL("""CREATE INDEX IF NOT EXISTS `idx_cfer_field`
                    ON `custom_field_extraction_rules`(`fieldId`)""")

                // CustomFieldSnapshotEntity
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `custom_field_snapshots` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `operationId` TEXT NOT NULL,
                        `transactionId` TEXT NOT NULL,
                        `fieldId` TEXT NOT NULL,
                        `previousValue` TEXT,
                        `previousSource` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `expiresAt` INTEGER NOT NULL
                    )
                """)
                db.execSQL("""CREATE INDEX IF NOT EXISTS `idx_cfs_op`
                    ON `custom_field_snapshots`(`operationId`)""")

                // TransactionSyncHashEntity
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `transaction_sync_hashes` (
                        `transactionId` TEXT NOT NULL PRIMARY KEY,
                        `legacyHash` TEXT NOT NULL,
                        `originalDescription` TEXT,
                        `originalNote` TEXT,
                        `lastSyncAt` INTEGER NOT NULL,
                        FOREIGN KEY(`transactionId`) REFERENCES `transactions`(`id`) ON DELETE CASCADE
                    )
                """)
            }
        }
    }
}
