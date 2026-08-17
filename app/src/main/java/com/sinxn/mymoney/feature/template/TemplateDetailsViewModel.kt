package com.sinxn.mymoney.feature.template

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.model.TransactionModelWithDetails
import com.sinxn.mymoney.core.data.local.model.TransferModelWithDetails
import com.sinxn.mymoney.core.data.repository.TemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class TemplateDetailsEvent {
    object Deleted : TemplateDetailsEvent()
    data class Applied(val txId: String) : TemplateDetailsEvent()
}

data class TemplateDetailsUiState(
    val isLoading: Boolean = true,
    val templateId: String = "",
    val isTransfer: Boolean = false,
    val transactionModel: TransactionModelWithDetails? = null,
    val transferModel: TransferModelWithDetails? = null,
    val isApplying: Boolean = false
)

@HiltViewModel
class TemplateDetailsViewModel @Inject constructor(
    private val templateRepository: TemplateRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val templateId: String = savedStateHandle.get<String>("templateId") ?: ""
    val isTransfer: Boolean = savedStateHandle.get<String>("isTransfer")?.toBooleanStrictOrNull()
        ?: savedStateHandle.get<Boolean>("isTransfer") ?: false

    private val _uiState = MutableStateFlow(
        TemplateDetailsUiState(
            templateId = templateId,
            isTransfer = isTransfer
        )
    )
    val uiState: StateFlow<TemplateDetailsUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<TemplateDetailsEvent>()
    val eventFlow: SharedFlow<TemplateDetailsEvent> = _eventFlow.asSharedFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            if (isTransfer) {
                templateRepository.getTransferModelWithDetails(templateId).collectLatest { model ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            transferModel = model
                        )
                    }
                }
            } else {
                templateRepository.getTransactionModelWithDetails(templateId).collectLatest { model ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            transactionModel = model
                        )
                    }
                }
            }
        }
    }

    fun applyTemplate() {
        if (_uiState.value.isApplying) return

        viewModelScope.launch {
            _uiState.update { it.copy(isApplying = true) }
            try {
                val txId = if (isTransfer) {
                    templateRepository.applyTransferModel(templateId)
                } else {
                    templateRepository.applyTransactionModel(templateId)
                }
                _eventFlow.emit(TemplateDetailsEvent.Applied(txId))
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _uiState.update { it.copy(isApplying = false) }
            }
        }
    }

    fun deleteTemplate() {
        viewModelScope.launch {
            if (isTransfer) {
                templateRepository.deleteTransferModel(templateId)
            } else {
                templateRepository.deleteTransactionModel(templateId)
            }
            _eventFlow.emit(TemplateDetailsEvent.Deleted)
        }
    }
}
