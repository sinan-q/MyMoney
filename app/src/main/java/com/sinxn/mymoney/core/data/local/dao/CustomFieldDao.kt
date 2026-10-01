package com.sinxn.mymoney.core.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sinxn.mymoney.core.data.local.entity.CustomFieldDefinitionEntity
import com.sinxn.mymoney.core.data.local.entity.CustomFieldExtractionRuleEntity
import com.sinxn.mymoney.core.data.local.entity.CustomFieldOrphanValueEntity
import com.sinxn.mymoney.core.data.local.entity.CustomFieldSnapshotEntity
import com.sinxn.mymoney.core.data.local.entity.CustomFieldTombstoneEntity
import com.sinxn.mymoney.core.data.local.entity.CustomFieldValueEntity
import com.sinxn.mymoney.core.data.local.entity.TransactionSyncHashEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomFieldDao {

    // --- Definitions ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefinition(definition: CustomFieldDefinitionEntity)

    @Update
    suspend fun updateDefinition(definition: CustomFieldDefinitionEntity)

    @Delete
    suspend fun deleteDefinition(definition: CustomFieldDefinitionEntity)

    @Query("SELECT * FROM custom_field_definitions WHERE categoryId = :categoryId ORDER BY sortOrder ASC")
    fun getFieldsForCategory(categoryId: String): Flow<List<CustomFieldDefinitionEntity>>

    @Query("SELECT * FROM custom_field_definitions WHERE categoryId = :categoryId ORDER BY sortOrder ASC")
    suspend fun getFieldsForCategorySync(categoryId: String): List<CustomFieldDefinitionEntity>

    @Query("SELECT * FROM custom_field_definitions")
    suspend fun getAllDefinitions(): List<CustomFieldDefinitionEntity>

    @Query("SELECT * FROM custom_field_definitions WHERE id = :id")
    suspend fun getDefinitionById(id: String): CustomFieldDefinitionEntity?

    // --- Values ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertValue(value: CustomFieldValueEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertValues(values: List<CustomFieldValueEntity>)

    @Update
    suspend fun updateValue(value: CustomFieldValueEntity)

    @Delete
    suspend fun deleteValue(value: CustomFieldValueEntity)

    @Query("SELECT * FROM custom_field_values WHERE transactionId = :transactionId")
    fun getValuesForTransaction(transactionId: String): Flow<List<CustomFieldValueEntity>>

    @Query("SELECT * FROM custom_field_values")
    suspend fun getAllValues(): List<CustomFieldValueEntity>

    @Query("SELECT * FROM custom_field_values WHERE transactionId = :transactionId")
    suspend fun getValuesForTransactionSync(transactionId: String): List<CustomFieldValueEntity>

    @Query("SELECT DISTINCT normalizedValue FROM custom_field_values WHERE fieldId = :fieldId")
    suspend fun getDistinctValuesForField(fieldId: String): List<String>

    @Query("DELETE FROM custom_field_values WHERE fieldId = :fieldId")
    suspend fun deleteValuesForField(fieldId: String)

    @androidx.room.Transaction
    @Query("""
        SELECT DISTINCT t.*, c.name as categoryName, c.icon as categoryIcon,
               COALESCE(curr.decimals, 2) as decimals, curr.symbol as currencySymbol,
               w.currency as currencyCode
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        INNER JOIN wallets w ON t.walletId = w.id
        LEFT JOIN currencies curr ON w.currency = curr.iso
        INNER JOIN custom_field_definitions cfd ON (cfd.categoryId = t.categoryId OR cfd.categoryId = c.parentId) 
        LEFT JOIN custom_field_values cfv ON cfv.transactionId = t.id AND cfv.fieldId = cfd.id
        WHERE cfv.id IS NULL AND cfd.archivedAt IS NULL AND t.isDeleted = 0
        ORDER BY t.date DESC
    """)
    fun getTransactionsNeedingReview(): Flow<List<com.sinxn.mymoney.core.data.local.model.TransactionWithCategory>>

    // Autocomplete query based on spec:
    @Query("""
        SELECT DISTINCT custom_field_values.normalizedValue 
        FROM custom_field_values 
        INNER JOIN transactions ON transactions.id = custom_field_values.transactionId
        WHERE fieldId = :fieldId AND normalizedValue LIKE :prefix || '%'
        ORDER BY transactions.date DESC 
        LIMIT 10
    """)
    fun getAutocompleteSuggestions(fieldId: String, prefix: String): Flow<List<String>>

    // --- Tombstones ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTombstone(tombstone: CustomFieldTombstoneEntity)

    @Query("SELECT * FROM custom_field_tombstones WHERE categoryId = :categoryId")
    suspend fun getTombstonesForCategory(categoryId: String): List<CustomFieldTombstoneEntity>
    
    @Query("SELECT * FROM custom_field_tombstones")
    suspend fun getAllTombstones(): List<CustomFieldTombstoneEntity>

    // --- Orphans ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrphan(orphan: CustomFieldOrphanValueEntity)
    
    @Query("SELECT * FROM custom_field_orphan_values")
    suspend fun getAllOrphans(): List<CustomFieldOrphanValueEntity>
    
    @Query("DELETE FROM custom_field_orphan_values WHERE fieldKey = :fieldKey AND transactionId IN (SELECT id FROM transactions WHERE categoryId = :categoryId)")
    suspend fun deleteOrphansForCategoryAndKey(categoryId: String, fieldKey: String)

    // --- Extraction Rules ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExtractionRule(rule: CustomFieldExtractionRuleEntity)
    
    @Query("SELECT * FROM custom_field_extraction_rules WHERE fieldId = :fieldId ORDER BY ruleOrder ASC")
    suspend fun getExtractionRulesForField(fieldId: String): List<CustomFieldExtractionRuleEntity>

    @Delete
    suspend fun deleteExtractionRule(rule: CustomFieldExtractionRuleEntity)

    // --- Snapshots ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSnapshot(snapshot: CustomFieldSnapshotEntity)
    
    @Query("SELECT * FROM custom_field_snapshots WHERE operationId = :operationId")
    suspend fun getSnapshotsForOperation(operationId: String): List<CustomFieldSnapshotEntity>
    
    @Query("DELETE FROM custom_field_snapshots WHERE expiresAt < :currentTimeMillis")
    suspend fun cleanupExpiredSnapshots(currentTimeMillis: Long)

    // --- Sync Hashes ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSyncHash(hash: TransactionSyncHashEntity)

    @Query("SELECT * FROM transaction_sync_hashes WHERE transactionId = :transactionId")
    suspend fun getSyncHash(transactionId: String): TransactionSyncHashEntity?
}
