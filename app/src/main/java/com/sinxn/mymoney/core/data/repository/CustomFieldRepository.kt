package com.sinxn.mymoney.core.data.repository

import com.sinxn.mymoney.core.data.local.dao.CustomFieldDao
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.entity.CustomFieldDefinitionEntity
import com.sinxn.mymoney.core.data.local.entity.CustomFieldValueEntity
import com.sinxn.mymoney.core.data.local.entity.CustomFieldTombstoneEntity
import com.sinxn.mymoney.core.data.local.entity.CustomFieldExtractionRuleEntity
import com.sinxn.mymoney.core.data.local.entity.CustomFieldSnapshotEntity
import com.sinxn.mymoney.core.data.importBackup.ExtractionEngine
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class ExtractionPreviewResult(
    val totalTransactions: Int,
    val matchedCount: Int,
    val unmatchedCount: Int,
    val newValueCount: Int,
    val updatedValueCount: Int,
    val skippedManualCount: Int,
    val allMatches: List<ExtractionPreviewMatch>,
    val unmatchedTransactions: List<ExtractionPreviewMatch>
)

data class ExtractionPreviewMatch(
    val transactionId: String,
    val date: String,
    val description: String?,
    val note: String?,
    val extractedValues: Map<String, String>,
    val existingValues: Map<String, Pair<String, String>>, // fieldKey -> (value, source)
    val action: String // "new", "update", "skip_manual", "unmatched"
)

@Singleton
class CustomFieldRepository @Inject constructor(
    private val customFieldDao: CustomFieldDao,
    private val moneyDao: MoneyDao
) {
    // --- Definitions ---
    
    suspend fun saveDefinition(
        id: String? = null,
        categoryId: String,
        label: String,
        type: String,
        sortOrder: Int = 0,
        isRequired: Boolean = false,
        visibilityDependsOnFieldId: String? = null,
        visibilityDependsOnValue: String? = null
    ): String {
        val now = System.currentTimeMillis()
        val definitionId = id ?: UUID.randomUUID().toString()
        val key = id?.let { customFieldDao.getDefinitionById(it)?.key } ?: generateKey(label)
        
        val definition = CustomFieldDefinitionEntity(
            id = definitionId,
            categoryId = categoryId,
            key = key,
            label = label,
            type = type,
            sortOrder = sortOrder,
            isRequired = isRequired,
            archivedAt = null,
            visibilityDependsOnFieldId = visibilityDependsOnFieldId,
            visibilityDependsOnValue = visibilityDependsOnValue,
            lastEdit = now
        )
        
        if (id == null) {
            customFieldDao.insertDefinition(definition)
        } else {
            customFieldDao.updateDefinition(definition)
        }
        return definitionId
    }
    
    private fun generateKey(label: String): String {
        return label.trim().lowercase().replace(Regex("\\s+"), "_").replace(Regex("[^a-z0-9_]"), "")
    }

    suspend fun archiveDefinition(id: String) {
        val definition = customFieldDao.getDefinitionById(id) ?: return
        customFieldDao.updateDefinition(definition.copy(
            archivedAt = System.currentTimeMillis(),
            lastEdit = System.currentTimeMillis()
        ))
    }

    suspend fun unarchiveDefinition(id: String) {
        val definition = customFieldDao.getDefinitionById(id) ?: return
        customFieldDao.updateDefinition(definition.copy(
            archivedAt = null,
            lastEdit = System.currentTimeMillis()
        ))
    }

    suspend fun permanentlyDeleteDefinition(id: String) {
        val definition = customFieldDao.getDefinitionById(id) ?: return
        
        // Save tombstone
        val tombstone = CustomFieldTombstoneEntity(
            id = UUID.randomUUID().toString(),
            categoryId = definition.categoryId,
            fieldKey = definition.key,
            deletedAt = System.currentTimeMillis()
        )
        customFieldDao.insertTombstone(tombstone)
        
        // Delete orphans with this key for the category
        customFieldDao.deleteOrphansForCategoryAndKey(definition.categoryId, definition.key)
        
        // Values are cascade-deleted due to foreign key
        customFieldDao.deleteDefinition(definition)
    }

    suspend fun getEffectiveFieldsForCategory(categoryId: String): List<CustomFieldDefinitionEntity> {
        val result = mutableListOf<CustomFieldDefinitionEntity>()
        var currentId: String? = categoryId
        
        while (currentId != null) {
            val category = moneyDao.getCategoryById(currentId) ?: break
            val fields = customFieldDao.getFieldsForCategorySync(currentId)
            result.addAll(fields)
            currentId = category.parentId
        }
        
        return result.sortedBy { it.sortOrder }
    }

    fun getFieldsForCategory(categoryId: String): Flow<List<CustomFieldDefinitionEntity>> {
        return customFieldDao.getFieldsForCategory(categoryId)
    }

    suspend fun getDefinitionById(id: String): CustomFieldDefinitionEntity? {
        return customFieldDao.getDefinitionById(id)
    }

    // --- Values ---
    
    suspend fun saveValue(
        transactionId: String,
        fieldId: String,
        value: String,
        source: String = "manual"
    ) {
        if (value.isBlank()) return // Empty values are not stored

        val normalizedValue = value.trim().replace(Regex("\\s+"), " ").lowercase()
        val existingValues = customFieldDao.getValuesForTransactionSync(transactionId)
        val existingValue = existingValues.find { it.fieldId == fieldId }

        val entity = CustomFieldValueEntity(
            id = existingValue?.id ?: UUID.randomUUID().toString(),
            transactionId = transactionId,
            fieldId = fieldId,
            value = value,
            normalizedValue = normalizedValue,
            source = source,
            lastEdit = System.currentTimeMillis()
        )
        
        customFieldDao.insertValue(entity)
    }

    suspend fun removeValue(transactionId: String, fieldId: String) {
        val existingValues = customFieldDao.getValuesForTransactionSync(transactionId)
        val existingValue = existingValues.find { it.fieldId == fieldId }
        if (existingValue != null) {
            customFieldDao.deleteValue(existingValue)
        }
    }

    fun getValuesForTransaction(transactionId: String): Flow<List<CustomFieldValueEntity>> {
        return customFieldDao.getValuesForTransaction(transactionId)
    }

    suspend fun getValuesForTransactionSync(transactionId: String): List<CustomFieldValueEntity> {
        return customFieldDao.getValuesForTransactionSync(transactionId)
    }

    fun getTransactionsNeedingReview(): Flow<List<com.sinxn.mymoney.core.data.local.model.TransactionWithCategory>> {
        return customFieldDao.getTransactionsNeedingReview()
    }
    
    suspend fun getDistinctValuesForField(fieldId: String): List<String> {
        return customFieldDao.getDistinctValuesForField(fieldId)
    }

    suspend fun getValueCountsForField(fieldId: String): List<com.sinxn.mymoney.core.data.local.model.ValueCount> {
        return customFieldDao.getValueCountsForField(fieldId)
    }

    fun getTransactionsForCustomFieldValue(fieldId: String, normalizedValue: String): Flow<List<com.sinxn.mymoney.core.data.local.model.TransactionWithCategory>> {
        return customFieldDao.getTransactionsForCustomFieldValue(fieldId, normalizedValue)
    }
    
    // --- Autocomplete ---

    fun getAutocompleteSuggestions(fieldId: String, prefix: String): Flow<List<String>> {
        val normalizedPrefix = prefix.trim().replace(Regex("\\s+"), " ").lowercase()
        return customFieldDao.getAutocompleteSuggestions(fieldId, normalizedPrefix)
    }
    
    // --- Extraction Rules ---
    
    suspend fun getExtractionRulesForField(fieldId: String): List<CustomFieldExtractionRuleEntity> {
        return customFieldDao.getExtractionRulesForField(fieldId)
    }

    suspend fun getExtractionRulesForCategory(categoryId: String): List<CustomFieldExtractionRuleEntity> {
        val effectiveFields = getEffectiveFieldsForCategory(categoryId)
            .filter { it.archivedAt == null }
        return effectiveFields.flatMap { field ->
            customFieldDao.getExtractionRulesForField(field.id)
        }.sortedBy { it.ruleOrder }
    }
    
    suspend fun saveExtractionRule(rule: CustomFieldExtractionRuleEntity) {
        customFieldDao.insertExtractionRule(rule.copy(lastEdit = System.currentTimeMillis()))
    }

    suspend fun deleteExtractionRule(rule: CustomFieldExtractionRuleEntity) {
        customFieldDao.deleteExtractionRule(rule)
    }

    // --- Extraction Execution ---

    /**
     * Previews what extraction would produce for a given category without writing anything.
     * Returns match rate, samples, and counts per §8.9 of the spec.
     */
    suspend fun previewExtraction(categoryId: String, startDate: String? = null): ExtractionPreviewResult {
        val effectiveFields = getEffectiveFieldsForCategory(categoryId)
            .filter { it.archivedAt == null }
        val allRules = effectiveFields.flatMap { field ->
            customFieldDao.getExtractionRulesForField(field.id)
        }

        if (allRules.isEmpty()) {
            return ExtractionPreviewResult(0, 0, 0, 0, 0, 0, emptyList(), emptyList())
        }

        val transactions = moneyDao.getTransactionEntitiesForCategory(categoryId).filter {
            if (startDate != null) it.date >= startDate else true
        }
        val fieldsByKey = effectiveFields.associateBy { it.key }
        val fieldsById = effectiveFields.associateBy { it.id }

        var matchedCount = 0
        var unmatchedCount = 0
        var newValueCount = 0
        var updatedValueCount = 0
        var skippedManualCount = 0
        val allMatches = mutableListOf<ExtractionPreviewMatch>()
        val unmatchedTransactions = mutableListOf<ExtractionPreviewMatch>()

        val fieldKeyById = effectiveFields.associate { it.id to it.key }

        for (tx in transactions) {
            val extracted = ExtractionEngine.extract(tx.description, tx.note, allRules, fieldKeyById)
            if (extracted.isEmpty()) {
                unmatchedCount++
                unmatchedTransactions.add(
                    ExtractionPreviewMatch(
                        transactionId = tx.id,
                        date = tx.date,
                        description = tx.description,
                        note = tx.note,
                        extractedValues = emptyMap(),
                        existingValues = emptyMap(),
                        action = "unmatched"
                    )
                )
                continue
            }
            matchedCount++

            val existingValues = customFieldDao.getValuesForTransactionSync(tx.id)
            val existingByFieldId = existingValues.associateBy { it.fieldId }

            val resolvedExtracted = mutableMapOf<String, String>()
            val existingMap = mutableMapOf<String, Pair<String, String>>()
            var txAction = "new"

            for ((fieldKey, value) in extracted) {
                val fieldDef = fieldsByKey[fieldKey] ?: continue
                if (value.isBlank()) continue
                val existing = existingByFieldId[fieldDef.id]

                val effectiveValue = if (fieldDef.type.lowercase() == "boolean") {
                    when (value.trim().lowercase()) {
                        "true", "yes", "1", "online", "t", "y" -> "true"
                        "false", "no", "0", "offline", "f", "n" -> "false"
                        else -> if (value.isNotBlank()) "true" else "false"
                    }
                } else {
                    value.trim()
                }

                if (existing != null) {
                    existingMap[fieldKey] = existing.value to existing.source
                    if (existing.source == "manual") {
                        skippedManualCount++
                        txAction = "skip_manual"
                        continue
                    }
                    if (existing.value != effectiveValue) {
                        updatedValueCount++
                        txAction = "update"
                    }
                } else {
                    newValueCount++
                }
                resolvedExtracted[fieldKey] = effectiveValue
            }

            allMatches.add(
                ExtractionPreviewMatch(
                    transactionId = tx.id,
                    date = tx.date,
                    description = tx.description,
                    note = tx.note,
                    extractedValues = resolvedExtracted,
                    existingValues = existingMap,
                    action = txAction
                )
            )
        }

        return ExtractionPreviewResult(
            totalTransactions = transactions.size,
            matchedCount = matchedCount,
            unmatchedCount = unmatchedCount,
            newValueCount = newValueCount,
            updatedValueCount = updatedValueCount,
            skippedManualCount = skippedManualCount,
            allMatches = allMatches,
            unmatchedTransactions = unmatchedTransactions
        )
    }

    /**
     * Applies extraction rules to all transactions in the category scope.
     * Respects precedence: manual > embedded > parsed.
     * Takes snapshots of affected values for undo (§9, §8.9).
     * Returns the number of values written.
     */
    suspend fun applyExtraction(categoryId: String, startDate: String? = null): Int {
        val effectiveFields = getEffectiveFieldsForCategory(categoryId)
            .filter { it.archivedAt == null }
        val allRules = effectiveFields.flatMap { field ->
            customFieldDao.getExtractionRulesForField(field.id)
        }
        if (allRules.isEmpty()) return 0

        val transactions = moneyDao.getTransactionEntitiesForCategory(categoryId).filter {
            if (startDate != null) it.date >= startDate else true
        }
        val fieldsByKey = effectiveFields.associateBy { it.key }

        val operationId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val undoWindowMs = 5 * 60 * 1000L // 5 minutes
        var valuesWritten = 0
        val fieldKeyById = effectiveFields.associate { it.id to it.key }

        for (tx in transactions) {
            val extracted = ExtractionEngine.extract(tx.description, tx.note, allRules, fieldKeyById)
            if (extracted.isEmpty()) continue

            val existingValues = customFieldDao.getValuesForTransactionSync(tx.id)
            val existingByFieldId = existingValues.associateBy { it.fieldId }

            for ((fieldKey, value) in extracted) {
                val fieldDef = fieldsByKey[fieldKey] ?: continue
                if (value.isBlank()) continue

                val effectiveValue = if (fieldDef.type.lowercase() == "boolean") {
                    when (value.trim().lowercase()) {
                        "true", "yes", "1", "online", "t", "y" -> "true"
                        "false", "no", "0", "offline", "f", "n" -> "false"
                        else -> if (value.isNotBlank()) "true" else "false"
                    }
                } else {
                    value.trim()
                }

                val existing = existingByFieldId[fieldDef.id]

                // Manual values are never overwritten (§8.6)
                if (existing?.source == "manual") continue
                // Embedded values outrank parsed (§8.6)
                if (existing?.source == "embedded") continue

                // Snapshot the previous value before overwriting
                if (existing != null) {
                    customFieldDao.insertSnapshot(
                        CustomFieldSnapshotEntity(
                            id = UUID.randomUUID().toString(),
                            operationId = operationId,
                            transactionId = tx.id,
                            fieldId = fieldDef.id,
                            previousValue = existing.value,
                            previousSource = existing.source,
                            createdAt = now,
                            expiresAt = now + undoWindowMs
                        )
                    )
                }

                val normalizedValue = if (fieldDef.type.lowercase() == "boolean") {
                    effectiveValue
                } else {
                    effectiveValue.replace(Regex("\\s+"), " ").lowercase()
                }

                customFieldDao.insertValue(
                    CustomFieldValueEntity(
                        id = existing?.id ?: UUID.randomUUID().toString(),
                        transactionId = tx.id,
                        fieldId = fieldDef.id,
                        value = effectiveValue,
                        normalizedValue = normalizedValue,
                        source = "parsed",
                        lastEdit = now
                    )
                )
                valuesWritten++
            }
        }

        return valuesWritten
    }
}
