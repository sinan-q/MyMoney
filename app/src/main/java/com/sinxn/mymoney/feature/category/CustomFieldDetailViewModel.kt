package com.sinxn.mymoney.feature.category

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.entity.CustomFieldDefinitionEntity
import com.sinxn.mymoney.core.data.local.model.ValueCount
import com.sinxn.mymoney.core.data.repository.CustomFieldRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomFieldDetailUiState(
    val fieldId: String = "",
    val fieldDefinition: CustomFieldDefinitionEntity? = null,
    val valueCounts: List<ValueCount> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class CustomFieldDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val customFieldRepository: CustomFieldRepository
) : ViewModel() {

    val fieldId: String = checkNotNull(savedStateHandle["fieldId"])

    private val _uiState = MutableStateFlow(CustomFieldDetailUiState(fieldId = fieldId))
    val uiState: StateFlow<CustomFieldDetailUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val definition = customFieldRepository.getDefinitionById(fieldId)
            val counts = customFieldRepository.getValueCountsForField(fieldId)
            
            _uiState.value = _uiState.value.copy(
                fieldDefinition = definition,
                valueCounts = counts,
                isLoading = false
            )
        }
    }
}
