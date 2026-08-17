package com.sinxn.mymoney.feature.wallet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.entity.CurrencyEntity
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.repository.WalletInUseInTransferException
import com.sinxn.mymoney.core.data.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

data class WalletAddEditUiState(
    val id: String? = null,
    val name: String = "",
    val icon: String = "",
    val currency: String = "USD",
    val currencySymbol: String = "$",
    val currencyDecimals: Int = 2,
    val startMoney: Long = 0L,
    val editAmount: String = "0",
    val countInTotal: Boolean = true,
    val note: String = "",
    val isArchived: Boolean = false,
    val isEditMode: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val nameError: String? = null,
    val availableCurrencies: List<CurrencyEntity> = emptyList()
)

sealed interface WalletAddEditEvent {
    object Saved : WalletAddEditEvent
    object Deleted : WalletAddEditEvent
    object DeleteErrorTransferInUse : WalletAddEditEvent
}

@HiltViewModel
class WalletAddEditViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val walletId: String? = savedStateHandle.get<String>("walletId")?.takeIf { it.isNotEmpty() && it != "new" }

    private val _uiState = MutableStateFlow(WalletAddEditUiState(id = walletId, isEditMode = walletId != null))
    val uiState: StateFlow<WalletAddEditUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<WalletAddEditEvent>()
    val eventFlow: SharedFlow<WalletAddEditEvent> = _eventFlow.asSharedFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val currencies = walletRepository.getCurrenciesList()
            val settings = settingsRepository.formattingSettings.first()
            val defaultIso = settings.globalCurrency.ifEmpty { "USD" }
            val defaultCurrencyEntity = currencies.find { it.iso.equals(defaultIso, ignoreCase = true) }

            if (walletId != null) {
                val existing = walletRepository.getWalletById(walletId)
                if (existing != null) {
                    val curr = currencies.find { it.iso.equals(existing.currency, ignoreCase = true) }
                    val decimals = curr?.decimals ?: 2
                    val symbol = curr?.symbol ?: existing.currency
                    val amountStr = if (decimals > 0) {
                        String.format(java.util.Locale.US, "%.${decimals}f", existing.startMoney / Math.pow(10.0, decimals.toDouble()))
                    } else {
                        existing.startMoney.toString()
                    }

                    _uiState.update {
                        it.copy(
                            id = existing.id,
                            name = existing.name,
                            icon = existing.icon,
                            currency = existing.currency,
                            currencySymbol = symbol,
                            currencyDecimals = decimals,
                            startMoney = existing.startMoney,
                            editAmount = amountStr,
                            countInTotal = existing.countInTotal,
                            note = existing.note.orEmpty(),
                            isArchived = existing.isArchived,
                            isEditMode = true,
                            isLoading = false,
                            availableCurrencies = currencies
                        )
                    }
                    return@launch
                }
            }

            // New wallet defaults
            val initialDecimals = defaultCurrencyEntity?.decimals ?: 2
            val initialSymbol = defaultCurrencyEntity?.symbol ?: defaultIso
            val initialIconJson = JSONObject().apply {
                put("type", "color")
                put("color", "#10B981")
                put("name", "W")
            }.toString()

            _uiState.update {
                it.copy(
                    currency = defaultIso,
                    currencySymbol = initialSymbol,
                    currencyDecimals = initialDecimals,
                    icon = initialIconJson,
                    availableCurrencies = currencies,
                    isLoading = false
                )
            }
        }
    }

    fun onNameChange(name: String) {
        _uiState.update { state ->
            val updatedIcon = if (state.icon.isBlank()) {
                JSONObject().apply {
                    put("type", "color")
                    put("color", "#10B981")
                    put("name", name.trim().take(2).uppercase().ifEmpty { "W" })
                }.toString()
            } else {
                state.icon
            }
            state.copy(
                name = name,
                nameError = if (name.isBlank()) "Wallet name cannot be empty" else null,
                icon = updatedIcon
            )
        }
    }

    fun onIconChange(iconJson: String) {
        _uiState.update { it.copy(icon = iconJson) }
    }

    fun onCurrencyChange(currency: CurrencyEntity) {
        val decimals = currency.decimals
        val symbol = currency.symbol ?: currency.iso
        _uiState.update { state ->
            state.copy(
                currency = currency.iso,
                currencySymbol = symbol,
                currencyDecimals = decimals
            )
        }
    }

    fun onAmountChange(amountStr: String) {
        _uiState.update { it.copy(editAmount = amountStr) }
    }

    fun onCountInTotalChange(countInTotal: Boolean) {
        _uiState.update { it.copy(countInTotal = countInTotal) }
    }

    fun onNoteChange(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    fun toggleArchived() {
        _uiState.update { it.copy(isArchived = !it.isArchived) }
    }

    fun saveWallet() {
        val currentState = _uiState.value
        if (currentState.name.isBlank()) {
            _uiState.update { it.copy(nameError = "Wallet name cannot be empty") }
            return
        }

        val decimals = currentState.currencyDecimals
        val rawAmountDouble = currentState.editAmount.toDoubleOrNull() ?: 0.0
        val parsedStartMoney = (rawAmountDouble * Math.pow(10.0, decimals.toDouble())).toLong()

        val finalIcon = currentState.icon.ifBlank {
            JSONObject().apply {
                put("type", "color")
                put("color", "#10B981")
                put("name", currentState.name.trim().take(2).uppercase().ifEmpty { "W" })
            }.toString()
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                walletRepository.saveWallet(
                    id = currentState.id,
                    name = currentState.name,
                    icon = finalIcon,
                    currency = currentState.currency,
                    startMoney = parsedStartMoney,
                    countInTotal = currentState.countInTotal,
                    note = currentState.note,
                    isArchived = currentState.isArchived
                )
                _eventFlow.emit(WalletAddEditEvent.Saved)
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun deleteWallet() {
        val id = walletId ?: return
        viewModelScope.launch {
            val result = walletRepository.deleteWallet(id)
            if (result.isSuccess) {
                _eventFlow.emit(WalletAddEditEvent.Deleted)
            } else {
                val exception = result.exceptionOrNull()
                if (exception is WalletInUseInTransferException) {
                    _eventFlow.emit(WalletAddEditEvent.DeleteErrorTransferInUse)
                }
            }
        }
    }
}
