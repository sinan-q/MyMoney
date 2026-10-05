package com.sinxn.mymoney.feature.saving

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.model.SavingWithDetails
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.preferences.toFormatterConfig
import com.sinxn.mymoney.core.data.repository.SavingRepository
import com.sinxn.mymoney.core.util.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SavingDetailsUiState(
    val savingId: String = "",
    val savingWithDetails: SavingWithDetails? = null,
    val transactions: List<TransactionWithCategory> = emptyList(),
    val formatterConfig: MoneyFormatter.Config = MoneyFormatter.Config(),
    val dateFormat: Int = 0,
    val globalCurrency: String = "USD",
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

sealed interface SavingDetailsEvent {
    object Deleted : SavingDetailsEvent
    object StatusUpdated : SavingDetailsEvent
}

@HiltViewModel
class SavingDetailsViewModel @Inject constructor(
    private val savingRepository: SavingRepository,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val savingId: String = savedStateHandle.get<String>("savingId") ?: ""

    private val _uiState = MutableStateFlow(SavingDetailsUiState(savingId = savingId))
    val uiState: StateFlow<SavingDetailsUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<SavingDetailsEvent>()
    val eventFlow: SharedFlow<SavingDetailsEvent> = _eventFlow.asSharedFlow()

    init {
        loadData()
    }

    private fun loadData() {
        if (savingId.isBlank() || savingId == "new") return

        viewModelScope.launch {
            combine(
                savingRepository.getSavingDetails(savingId),
                savingRepository.getTransactionsForSaving(savingId),
                settingsRepository.formattingSettings
            ) { savingDetails, transactions, formatting ->
                val formatterConfig = formatting.toFormatterConfig()

                _uiState.update { state ->
                    state.copy(
                        savingWithDetails = savingDetails,
                        transactions = transactions,
                        formatterConfig = formatterConfig,
                        dateFormat = formatting.dateFormat,
                        globalCurrency = formatting.globalCurrency,
                        isLoading = false
                    )
                }
            }.collect {}
        }
    }

    fun toggleComplete() {
        viewModelScope.launch {
            val current = _uiState.value.savingWithDetails?.saving ?: return@launch
            savingRepository.setSavingComplete(savingId, !current.isComplete)
            _eventFlow.emit(SavingDetailsEvent.StatusUpdated)
        }
    }

    fun deleteSaving(deleteTransactions: Boolean, onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (savingId.isNotBlank()) {
                savingRepository.deleteSaving(savingId, deleteTransactions)
                _eventFlow.emit(SavingDetailsEvent.Deleted)
                onSuccess()
            }
        }
    }
}
