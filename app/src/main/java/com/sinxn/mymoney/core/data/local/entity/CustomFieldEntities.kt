package com.sinxn.mymoney.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "custom_field_definitions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CustomFieldDefinitionEntity::class,
            parentColumns = ["id"],
            childColumns = ["visibilityDependsOnFieldId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(name = "idx_cfd_category_key", value = ["categoryId", "key"], unique = true),
        Index(name = "idx_cfd_category_sort", value = ["categoryId", "sortOrder"]),
        Index(name = "idx_cfd_visibility", value = ["visibilityDependsOnFieldId"])
    ]
)
data class CustomFieldDefinitionEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val key: String,
    val label: String,
    val type: String,
    @androidx.room.ColumnInfo(defaultValue = "0") val sortOrder: Int = 0,
    @androidx.room.ColumnInfo(defaultValue = "0") val isRequired: Boolean = false,
    val archivedAt: Long?,
    val visibilityDependsOnFieldId: String?,
    val visibilityDependsOnValue: String?,
    val lastEdit: Long
)

@Entity(
    tableName = "custom_field_values",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CustomFieldDefinitionEntity::class,
            parentColumns = ["id"],
            childColumns = ["fieldId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(name = "idx_cfv_tx_field", value = ["transactionId", "fieldId"], unique = true),
        Index(name = "idx_cfv_field_norm", value = ["fieldId", "normalizedValue"]),
        Index(name = "idx_cfv_tx", value = ["transactionId"])
    ]
)
data class CustomFieldValueEntity(
    @PrimaryKey val id: String,
    val transactionId: String,
    val fieldId: String,
    val value: String,
    val normalizedValue: String,
    val source: String,
    val lastEdit: Long
)

@Entity(
    tableName = "custom_field_tombstones",
    indices = [
        Index(name = "idx_cft_cat_key", value = ["categoryId", "fieldKey"], unique = true)
    ]
)
data class CustomFieldTombstoneEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val fieldKey: String,
    val deletedAt: Long
)

@Entity(
    tableName = "custom_field_orphan_values",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(name = "idx_cfov_tx_key", value = ["transactionId", "fieldKey"], unique = true),
        Index(name = "idx_cfov_tx", value = ["transactionId"])
    ]
)
data class CustomFieldOrphanValueEntity(
    @PrimaryKey val id: String,
    val transactionId: String,
    val fieldKey: String,
    val value: String,
    val lastEdit: Long
)

@Entity(
    tableName = "custom_field_extraction_rules",
    foreignKeys = [
        ForeignKey(
            entity = CustomFieldDefinitionEntity::class,
            parentColumns = ["id"],
            childColumns = ["fieldId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(name = "idx_cfer_field", value = ["fieldId"])
    ]
)
data class CustomFieldExtractionRuleEntity(
    @PrimaryKey val id: String,
    val fieldId: String,
    @androidx.room.ColumnInfo(defaultValue = "0") val ruleOrder: Int = 0,
    val mode: String,
    val pattern: String,
    @androidx.room.ColumnInfo(defaultValue = "'description'") val targetColumns: String = "description",
    @androidx.room.ColumnInfo(defaultValue = "'{}'") val fieldMappings: String = "{}",
    val lastEdit: Long
)

@Entity(
    tableName = "custom_field_snapshots",
    indices = [
        Index(name = "idx_cfs_op", value = ["operationId"])
    ]
)
data class CustomFieldSnapshotEntity(
    @PrimaryKey val id: String,
    val operationId: String,
    val transactionId: String,
    val fieldId: String,
    val previousValue: String?,
    val previousSource: String?,
    val createdAt: Long,
    val expiresAt: Long
)

@Entity(
    tableName = "transaction_sync_hashes",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TransactionSyncHashEntity(
    @PrimaryKey val transactionId: String,
    val legacyHash: String,
    val originalDescription: String?,
    val originalNote: String?,
    val lastSyncAt: Long
)
