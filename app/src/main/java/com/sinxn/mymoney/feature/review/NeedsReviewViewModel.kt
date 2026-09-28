package com.sinxn.mymoney.feature.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.entity.CustomFieldDefinitionEntity
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.repository.CustomFieldRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class NeedsReviewUiState(
    val reviewItems: List<ReviewItemUiModel> = emptyList(),
    val isLoading: Boolean = true
)

data class ReviewItemUiModel(
    val transaction: TransactionWithCategory,
    val missingFields: List<MissingFieldUiModel>
)

data class MissingFieldUiModel(
    val field: CustomFieldDefinitionEntity,
    val suggestions: List<String>
)

@HiltViewModel
class NeedsReviewViewModel @Inject constructor(
    private val customFieldRepository: CustomFieldRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val formattingSettings: StateFlow<FormattingSettings> = settingsRepository.formattingSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FormattingSettings()
        )

    val uiState: StateFlow<NeedsReviewUiState> = customFieldRepository.getTransactionsNeedingReview()
        .map { transactions ->
            val reviewItems = withContext(Dispatchers.IO) {
                transactions.mapNotNull { tx ->
                    val catId = tx.transaction.categoryId ?: return@mapNotNull null
                    val effectiveFields = customFieldRepository.getEffectiveFieldsForCategory(catId)
                        .filter { it.archivedAt == null }
                    
                    val existingValues = customFieldRepository.getValuesForTransactionSync(tx.transaction.id)
                    val existingFieldIds = existingValues.map { it.fieldId }.toSet()
                    
                    val missing = effectiveFields.filter { it.id !in existingFieldIds }
                    
                    if (missing.isEmpty()) {
                        null
                    } else {
                        val missingUiModels = missing.map { field ->
                            val knownValues = customFieldRepository.getDistinctValuesForField(field.id)
                            
                            val textToSearch = "${tx.transaction.description.orEmpty()} ${tx.transaction.note.orEmpty()}".lowercase()
                            val suggestions = knownValues.filter { knownValue ->
                                knownValue.isNotBlank() && textToSearch.contains(knownValue.lowercase())
                            }.distinct()
                            
                            MissingFieldUiModel(field, suggestions)
                        }
                        ReviewItemUiModel(tx, missingUiModels)
                    }
                }
            }
            NeedsReviewUiState(reviewItems = reviewItems, isLoading = false)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NeedsReviewUiState(isLoading = true)
        )

    fun acceptSuggestion(transactionId: String, fieldId: String, value: String) {
        viewModelScope.launch(Dispatchers.IO) {
            customFieldRepository.saveValue(transactionId, fieldId, value, source = "manual")
        }
    }

    fun applyBulkValue(fieldId: String, value: String, transactionIds: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            transactionIds.forEach { txId ->
                customFieldRepository.saveValue(txId, fieldId, value, source = "manual")
            }
        }
    }
}
