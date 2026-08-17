package com.sinxn.mymoney.feature.template

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.model.TransactionModelWithDetails
import com.sinxn.mymoney.core.data.local.model.TransferModelWithDetails
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.repository.TemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TemplateUiState(
    val transactionTemplates: List<TransactionModelWithDetails> = emptyList(),
    val transferTemplates: List<TransferModelWithDetails> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TemplateViewModel @Inject constructor(
    private val templateRepository: TemplateRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val templatesFlow = settingsRepository.currentWalletId.flatMapLatest { walletId ->
        combine(
            templateRepository.getTransactionModels(walletId),
            templateRepository.getTransferModels(walletId)
        ) { txModels, trModels ->
            Pair(txModels, trModels)
        }
    }

    val uiState: StateFlow<TemplateUiState> = combine(
        settingsRepository.currentWalletId,
        templatesFlow
    ) { _, (txModels, trModels) ->
        TemplateUiState(
            transactionTemplates = txModels,
            transferTemplates = trModels,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TemplateUiState()
    )

    fun applyTransactionTemplate(item: TransactionModelWithDetails) {
        viewModelScope.launch {
            templateRepository.applyTransactionModel(item.model.id)
        }
    }

    fun applyTransferTemplate(item: TransferModelWithDetails) {
        viewModelScope.launch {
            templateRepository.applyTransferModel(item.model.id)
        }
    }

    fun deleteTransactionTemplate(item: TransactionModelWithDetails) {
        viewModelScope.launch {
            templateRepository.deleteTransactionModel(item.model.id)
        }
    }

    fun deleteTransferTemplate(item: TransferModelWithDetails) {
        viewModelScope.launch {
            templateRepository.deleteTransferModel(item.model.id)
        }
    }
}
