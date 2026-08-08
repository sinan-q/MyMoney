package com.sinxn.mymoney.feature.saving

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.entity.WalletEntity
import com.sinxn.mymoney.core.data.local.model.SavingWithDetails
import com.sinxn.mymoney.core.data.repository.SavingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SavingDetailsUiState(
    val savingId: String = "new",
    val isEditing: Boolean = false,
    val description: String = "",
    val icon: String = "ic_saving",
    val startMoneyInput: String = "0",
    val targetMoneyInput: String = "0",
    val walletId: String = "",
    val endDate: String? = null,
    val note: String = "",
    val availableWallets: List<WalletEntity> = emptyList(),
    val savingWithDetails: SavingWithDetails? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

sealed interface SavingDetailsEvent {
    object Saved : SavingDetailsEvent
    object Deleted : SavingDetailsEvent
}

@HiltViewModel
class SavingDetailsViewModel @Inject constructor(
    private val savingRepository: SavingRepository,
    private val moneyDao: MoneyDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val savingId: String = savedStateHandle.get<String>("savingId") ?: "new"

    private val _uiState = MutableStateFlow(SavingDetailsUiState(savingId = savingId, isEditing = savingId != "new"))
    val uiState: StateFlow<SavingDetailsUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<SavingDetailsEvent>()
    val eventFlow: SharedFlow<SavingDetailsEvent> = _eventFlow.asSharedFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val wallets = moneyDao.getWallets().firstOrNull() ?: emptyList()

            if (savingId != "new") {
                savingRepository.getSavingDetails(savingId).collect { details ->
                    if (details != null) {
                        _uiState.update { state ->
                            state.copy(
                                description = details.saving.description ?: "",
                                icon = details.saving.icon,
                                startMoneyInput = (details.saving.startMoney / 100.0).toString(),
                                targetMoneyInput = (details.saving.endMoney / 100.0).toString(),
                                walletId = details.saving.walletId,
                                endDate = details.saving.endDate,
                                note = details.saving.note ?: "",
                                availableWallets = wallets,
                                savingWithDetails = details,
                                isLoading = false
                            )
                        }
                    }
                }
            } else {
                val defaultWallet = wallets.firstOrNull { it.countInTotal } ?: wallets.firstOrNull()
                _uiState.update { state ->
                    state.copy(
                        walletId = defaultWallet?.id ?: "",
                        availableWallets = wallets,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun setDescription(desc: String) {
        _uiState.update { it.copy(description = desc) }
    }

    fun setIcon(icon: String) {
        _uiState.update { it.copy(icon = icon) }
    }

    fun setStartMoneyInput(input: String) {
        _uiState.update { it.copy(startMoneyInput = input) }
    }

    fun setTargetMoneyInput(input: String) {
        _uiState.update { it.copy(targetMoneyInput = input) }
    }

    fun setWalletId(wId: String) {
        _uiState.update { it.copy(walletId = wId) }
    }

    fun setEndDate(date: String?) {
        _uiState.update { it.copy(endDate = date) }
    }

    fun setNote(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    fun saveSaving() {
        viewModelScope.launch {
            val currentState = _uiState.value

            if (currentState.description.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Please enter a description for the saving goal.") }
                return@launch
            }

            if (currentState.walletId.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Please select a wallet.") }
                return@launch
            }

            val targetMoney = (currentState.targetMoneyInput.toDoubleOrNull() ?: 0.0) * 100.0
            val startMoney = (currentState.startMoneyInput.toDoubleOrNull() ?: 0.0) * 100.0

            if (targetMoney <= 0) {
                _uiState.update { it.copy(errorMessage = "Please enter a valid target goal amount.") }
                return@launch
            }

            try {
                savingRepository.saveSaving(
                    id = if (savingId != "new") savingId else null,
                    description = currentState.description,
                    icon = currentState.icon,
                    startMoney = startMoney.toLong(),
                    endMoney = targetMoney.toLong(),
                    walletId = currentState.walletId,
                    endDate = currentState.endDate,
                    note = currentState.note.ifBlank { null }
                )
                _eventFlow.emit(SavingDetailsEvent.Saved)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to save goal") }
            }
        }
    }

    fun deleteSaving() {
        viewModelScope.launch {
            if (savingId != "new") {
                savingRepository.deleteSaving(savingId)
                _eventFlow.emit(SavingDetailsEvent.Deleted)
            }
        }
    }
}
