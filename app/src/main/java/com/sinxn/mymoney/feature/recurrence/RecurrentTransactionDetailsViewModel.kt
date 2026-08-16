package com.sinxn.mymoney.feature.recurrence

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.model.RecurrentTransactionWithDetails
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.repository.RecurrenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecurrentTransactionDetailsUiState(
    val item: RecurrentTransactionWithDetails? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class RecurrentTransactionDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recurrenceRepository: RecurrenceRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val recurrenceId: String = checkNotNull(savedStateHandle.get<String>("id"))

    val formattingSettings: StateFlow<FormattingSettings> = settingsRepository.formattingSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FormattingSettings())

    val uiState: StateFlow<RecurrentTransactionDetailsUiState> = recurrenceRepository
        .getRecurrentTransactionWithDetails(recurrenceId)
        .map { item ->
            RecurrentTransactionDetailsUiState(
                item = item,
                isLoading = false
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            RecurrentTransactionDetailsUiState()
        )

    fun delete(onSuccess: () -> Unit) {
        viewModelScope.launch {
            recurrenceRepository.deleteRecurrentTransaction(recurrenceId)
            onSuccess()
        }
    }
}
