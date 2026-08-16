package com.sinxn.mymoney.feature.recurrence

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.model.RecurrentTransferWithDetails
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.repository.RecurrenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecurrentTransferDetailsUiState(
    val item: RecurrentTransferWithDetails? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class RecurrentTransferDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recurrenceRepository: RecurrenceRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val recurrenceId: String = checkNotNull(savedStateHandle.get<String>("id"))

    val formattingSettings: StateFlow<FormattingSettings> = settingsRepository.formattingSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FormattingSettings())

    val uiState: StateFlow<RecurrentTransferDetailsUiState> = recurrenceRepository
        .getRecurrentTransferWithDetails(recurrenceId)
        .map { item ->
            RecurrentTransferDetailsUiState(
                item = item,
                isLoading = false
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            RecurrentTransferDetailsUiState()
        )

    fun delete(onSuccess: () -> Unit) {
        viewModelScope.launch {
            recurrenceRepository.deleteRecurrentTransfer(recurrenceId)
            onSuccess()
        }
    }
}
