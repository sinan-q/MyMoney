package com.sinxn.mymoney.feature.people

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.entity.PersonEntity
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import com.sinxn.mymoney.core.data.repository.PersonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.util.MoneyFormatter

sealed class PersonDetailsEvent {
    object Deleted : PersonDetailsEvent()
}

data class PersonDetailsUiState(
    val personId: String = "",
    val person: PersonEntity? = null,
    val transactions: List<TransactionWithCategory> = emptyList(),
    val totalExpense: Long = 0L,
    val totalIncome: Long = 0L,
    val currencyCode: String = "USD",
    val decimals: Int = 2,
    val formatterConfig: MoneyFormatter.Config = MoneyFormatter.Config(),
    val dateFormat: Int = 0,
    val isLoading: Boolean = true,
    val isEditDialogOpen: Boolean = false,
    val editName: String = "",
    val editNote: String = ""
)

@HiltViewModel
class PersonDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val personRepository: PersonRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val personId: String = checkNotNull(savedStateHandle["personId"])

    private val _uiState = MutableStateFlow(PersonDetailsUiState(personId = personId))
    val uiState: StateFlow<PersonDetailsUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<PersonDetailsEvent>()
    val eventFlow: SharedFlow<PersonDetailsEvent> = _eventFlow.asSharedFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                personRepository.getPeople(),
                personRepository.getTransactionsForPerson(personId),
                settingsRepository.formattingSettings
            ) { allPeople, transactions, settings ->
                val currentPerson = allPeople.find { it.id == personId }

                var totalExpense = 0L
                var totalIncome = 0L

                transactions.forEach { item ->
                    if (item.transaction.direction == 1) {
                        totalIncome += item.transaction.money
                    } else {
                        totalExpense += item.transaction.money
                    }
                }

                val transactionCurrencies = transactions.mapNotNull { it.currencySymbol ?: it.currencyCode }.distinct()
                val displayCurrency = if (transactionCurrencies.size == 1) transactionCurrencies.first() else settings.globalCurrency
                val displayDecimals = transactions.firstOrNull()?.decimals ?: 2

                val formatterConfig = MoneyFormatter.Config(
                    showCurrency = settings.showCurrency,
                    groupDigits = settings.groupDigits,
                    roundDecimals = settings.roundDecimals,
                    showPlusMinus = settings.showPlusMinus
                )

                _uiState.value = _uiState.value.copy(
                    person = currentPerson,
                    transactions = transactions,
                    totalExpense = totalExpense,
                    totalIncome = totalIncome,
                    currencyCode = displayCurrency,
                    decimals = displayDecimals,
                    formatterConfig = formatterConfig,
                    dateFormat = settings.dateFormat,
                    isLoading = false
                )
            }.collect {}
        }
    }

    fun openEditDialog() {
        val p = _uiState.value.person ?: return
        _uiState.value = _uiState.value.copy(
            isEditDialogOpen = true,
            editName = p.name,
            editNote = p.note ?: ""
        )
    }

    fun closeEditDialog() {
        _uiState.value = _uiState.value.copy(isEditDialogOpen = false)
    }

    fun onNameChange(name: String) {
        _uiState.value = _uiState.value.copy(editName = name)
    }

    fun onNoteChange(note: String) {
        _uiState.value = _uiState.value.copy(editNote = note)
    }

    fun savePerson() {
        viewModelScope.launch {
            val state = _uiState.value
            val name = state.editName.trim()
            if (name.isEmpty()) return@launch

            personRepository.savePerson(
                id = personId,
                name = name,
                icon = state.person?.icon ?: "ic_person",
                note = state.editNote.ifBlank { null },
                isArchived = state.person?.isArchived ?: false,
                tag = state.person?.tag
            )
            closeEditDialog()
        }
    }

    fun deletePerson() {
        viewModelScope.launch {
            personRepository.deletePerson(personId)
            _eventFlow.emit(PersonDetailsEvent.Deleted)
        }
    }
}
