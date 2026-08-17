package com.sinxn.mymoney.feature.saving

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.entity.WalletEntity
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.repository.SavingRepository
import com.sinxn.mymoney.core.util.MoneyFormatter
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
import kotlin.math.roundToLong

data class SavingAddEditUiState(
    val savingId: String? = null,
    val isNewSaving: Boolean = true,
    val editDescription: String = "",
    val editIcon: String = "ic_saving",
    val editAmount: String = "", // Target amount
    val editStartMoney: String = "", // Initial deposit
    val editWalletId: String = "",
    val editEndDate: String? = null,
    val editNote: String = "",
    val editTag: String = "",
    val availableWallets: List<WalletEntity> = emptyList(),
    val currencyCode: String = "USD",
    val currencySymbol: String = "$",
    val currencyDecimals: Int = 2,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

sealed interface SavingAddEditEvent {
    object Saved : SavingAddEditEvent
    object Deleted : SavingAddEditEvent
}

@HiltViewModel
class SavingAddEditViewModel @Inject constructor(
    private val savingRepository: SavingRepository,
    private val moneyDao: MoneyDao,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val savingIdArg: String? = savedStateHandle.get<String>("savingId")?.takeIf { it.isNotBlank() && it != "new" }

    private val _uiState = MutableStateFlow(
        SavingAddEditUiState(
            savingId = savingIdArg,
            isNewSaving = savingIdArg == null
        )
    )
    val uiState: StateFlow<SavingAddEditUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<SavingAddEditEvent>()
    val eventFlow: SharedFlow<SavingAddEditEvent> = _eventFlow.asSharedFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val wallets = moneyDao.getWallets().firstOrNull() ?: emptyList()
            val formatting = settingsRepository.formattingSettings.firstOrNull()
            val decimals = 2

            if (savingIdArg != null) {
                savingRepository.getSavingDetails(savingIdArg).collect { details ->
                    if (details != null) {
                        val saving = details.saving
                        val wallet = wallets.firstOrNull { it.id == saving.walletId }
                        val curr = wallet?.currency ?: details.walletCurrency.ifBlank { formatting?.globalCurrency ?: "USD" }
                        val decimals = try {
                            java.util.Currency.getInstance(curr).defaultFractionDigits.coerceAtLeast(0)
                        } catch (e: Exception) {
                            2
                        }

                        val targetFormatted = if (saving.endMoney > 0) {
                            val divisor = Math.pow(10.0, decimals.toDouble())
                            val doubleVal = saving.endMoney / divisor
                            if (decimals == 0) doubleVal.toLong().toString() else doubleVal.toString()
                        } else ""

                        val startFormatted = if (saving.startMoney > 0) {
                            val divisor = Math.pow(10.0, decimals.toDouble())
                            val doubleVal = saving.startMoney / divisor
                            if (decimals == 0) doubleVal.toLong().toString() else doubleVal.toString()
                        } else ""

                        _uiState.update { state ->
                            state.copy(
                                isNewSaving = false,
                                editDescription = saving.description ?: "",
                                editIcon = saving.icon.ifBlank { "ic_saving" },
                                editAmount = targetFormatted,
                                editStartMoney = startFormatted,
                                editWalletId = saving.walletId,
                                editEndDate = saving.endDate,
                                editNote = saving.note ?: "",
                                editTag = saving.tag ?: "",
                                availableWallets = wallets,
                                currencyCode = curr,
                                currencySymbol = MoneyFormatter.getCurrencySymbol(curr),
                                currencyDecimals = decimals,
                                isLoading = false
                            )
                        }
                    }
                }
            } else {
                val currentWalletId = settingsRepository.currentWalletId.firstOrNull()
                val defaultWallet = wallets.find { it.id == currentWalletId }
                    ?: wallets.find { it.countInTotal }
                    ?: wallets.firstOrNull()

                val defaultCurrency = defaultWallet?.currency ?: formatting?.globalCurrency ?: "USD"
                val defaultDecimals = try {
                    java.util.Currency.getInstance(defaultCurrency).defaultFractionDigits.coerceAtLeast(0)
                } catch (e: Exception) {
                    2
                }
                val defaultWalletId = defaultWallet?.id ?: ""

                _uiState.update { state ->
                    state.copy(
                        isNewSaving = true,
                        editWalletId = defaultWalletId,
                        availableWallets = wallets,
                        currencyCode = defaultCurrency,
                        currencySymbol = MoneyFormatter.getCurrencySymbol(defaultCurrency),
                        currencyDecimals = defaultDecimals,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onNumpadKeyPress(key: String) {
        val current = _uiState.value.editAmount
        val updated = when (key) {
            "BACKSPACE" -> if (current.isNotEmpty()) current.dropLast(1) else ""
            "CLEAR" -> ""
            else -> current + key
        }
        _uiState.update { it.copy(editAmount = updated) }
    }

    fun evaluateMathExpression() {
        val current = _uiState.value.editAmount
        if (current.isBlank()) return
        try {
            val result = evalSimpleExpression(current)
            if (result != null && result >= 0) {
                val decimals = _uiState.value.currencyDecimals
                val formatted = if (decimals == 0) result.roundToLong().toString()
                else String.format(java.util.Locale.US, "%.${decimals}f", result)
                _uiState.update { it.copy(editAmount = formatted) }
            }
        } catch (_: Exception) {}
    }

    private fun evalSimpleExpression(expr: String): Double? {
        val clean = expr.replace("×", "*").replace("÷", "/").replace("−", "-").trim()
        val tokens = mutableListOf<String>()
        var numBuffer = StringBuilder()

        for (char in clean) {
            if (char in "+-*/") {
                if (numBuffer.isNotEmpty()) {
                    tokens.add(numBuffer.toString())
                    numBuffer = StringBuilder()
                }
                tokens.add(char.toString())
            } else if (char.isDigit() || char == '.') {
                numBuffer.append(char)
            }
        }
        if (numBuffer.isNotEmpty()) {
            tokens.add(numBuffer.toString())
        }

        if (tokens.isEmpty()) return null
        var total = tokens[0].toDoubleOrNull() ?: return null
        var i = 1
        while (i < tokens.size - 1) {
            val op = tokens[i]
            val nextVal = tokens[i + 1].toDoubleOrNull() ?: return null
            when (op) {
                "+" -> total += nextVal
                "-" -> total -= nextVal
                "*" -> total *= nextVal
                "/" -> if (nextVal != 0.0) total /= nextVal
            }
            i += 2
        }
        return total
    }

    fun updateDescription(desc: String) {
        _uiState.update { it.copy(editDescription = desc, errorMessage = null) }
    }

    fun updateIcon(icon: String) {
        _uiState.update { it.copy(editIcon = icon) }
    }

    fun updateStartMoney(startMoney: String) {
        _uiState.update { it.copy(editStartMoney = startMoney) }
    }

    fun updateWalletId(walletId: String) {
        val wallet = _uiState.value.availableWallets.firstOrNull { it.id == walletId }
        val curr = wallet?.currency ?: _uiState.value.currencyCode
        val decimals = try {
            java.util.Currency.getInstance(curr).defaultFractionDigits.coerceAtLeast(0)
        } catch (e: Exception) {
            2
        }
        _uiState.update {
            it.copy(
                editWalletId = walletId,
                currencyCode = curr,
                currencySymbol = MoneyFormatter.getCurrencySymbol(curr),
                currencyDecimals = decimals,
                errorMessage = null
            )
        }
    }

    fun updateEndDate(endDate: String?) {
        _uiState.update { it.copy(editEndDate = endDate) }
    }

    fun updateNote(note: String) {
        _uiState.update { it.copy(editNote = note) }
    }

    fun updateTag(tag: String) {
        _uiState.update { it.copy(editTag = tag) }
    }

    fun saveSaving(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value

            if (state.editDescription.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Please enter a name for the savings goal.") }
                return@launch
            }

            if (state.editWalletId.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Please select a wallet.") }
                return@launch
            }

            evaluateMathExpression()
            val evaluated = evalSimpleExpression(_uiState.value.editAmount) ?: _uiState.value.editAmount.toDoubleOrNull()
            if (evaluated == null || evaluated <= 0) {
                _uiState.update { it.copy(errorMessage = "Please enter a valid target goal amount.") }
                return@launch
            }

            val multiplier = Math.pow(10.0, state.currencyDecimals.toDouble())
            val targetMoneyLong = (evaluated * multiplier).roundToLong()
            val startMoneyDouble = state.editStartMoney.toDoubleOrNull() ?: 0.0
            val startMoneyLong = (startMoneyDouble * multiplier).roundToLong()

            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            try {
                savingRepository.saveSaving(
                    id = if (!state.isNewSaving) state.savingId else null,
                    description = state.editDescription.trim(),
                    icon = state.editIcon.ifBlank { "ic_saving" },
                    startMoney = startMoneyLong,
                    endMoney = targetMoneyLong,
                    walletId = state.editWalletId,
                    endDate = state.editEndDate,
                    note = state.editNote.trim().ifBlank { null },
                    tag = state.editTag.trim().ifBlank { null }
                )
                _uiState.update { it.copy(isSaving = false) }
                _eventFlow.emit(SavingAddEditEvent.Saved)
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = e.message ?: "Failed to save savings goal") }
            }
        }
    }

    fun deleteSaving(deleteTransactions: Boolean, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val id = _uiState.value.savingId
            if (id != null) {
                savingRepository.deleteSaving(id, deleteTransactions)
                _eventFlow.emit(SavingAddEditEvent.Deleted)
                onSuccess()
            }
        }
    }
}
