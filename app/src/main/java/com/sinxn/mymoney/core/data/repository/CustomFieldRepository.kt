package com.sinxn.mymoney.core.data.repository

import com.sinxn.mymoney.core.data.local.dao.CustomFieldDao
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.entity.CustomFieldDefinitionEntity
import com.sinxn.mymoney.core.data.local.entity.CustomFieldValueEntity
import com.sinxn.mymoney.core.data.local.entity.CustomFieldTombstoneEntity
import com.sinxn.mymoney.core.data.local.entity.CustomFieldExtractionRuleEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

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
    
    // --- Autocomplete ---

    fun getAutocompleteSuggestions(fieldId: String, prefix: String): Flow<List<String>> {
        val normalizedPrefix = prefix.trim().replace(Regex("\\s+"), " ").lowercase()
        return customFieldDao.getAutocompleteSuggestions(fieldId, normalizedPrefix)
    }
    
    // --- Extraction Rules ---
    
    suspend fun getExtractionRulesForField(fieldId: String): List<CustomFieldExtractionRuleEntity> {
        return customFieldDao.getExtractionRulesForField(fieldId)
    }
    
    suspend fun saveExtractionRule(rule: CustomFieldExtractionRuleEntity) {
        customFieldDao.insertExtractionRule(rule.copy(lastEdit = System.currentTimeMillis()))
    }

    suspend fun deleteExtractionRule(rule: CustomFieldExtractionRuleEntity) {
        customFieldDao.deleteExtractionRule(rule)
    }
}
