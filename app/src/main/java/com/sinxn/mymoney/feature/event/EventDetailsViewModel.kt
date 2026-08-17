package com.sinxn.mymoney.feature.event

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.entity.EventEntity
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.repository.EventRepository
import com.sinxn.mymoney.core.util.MoneyFormatter
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

sealed class EventDetailsEvent {
    object Deleted : EventDetailsEvent()
}

data class EventDetailsUiState(
    val eventId: String = "",
    val event: EventEntity? = null,
    val transactions: List<TransactionWithCategory> = emptyList(),
    val totalExpense: Long = 0L,
    val totalIncome: Long = 0L,
    val currencyCode: String = "USD",
    val decimals: Int = 2,
    val formatterConfig: MoneyFormatter.Config = MoneyFormatter.Config(),
    val dateFormat: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class EventDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val eventRepository: EventRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val eventId: String = checkNotNull(savedStateHandle["eventId"])

    private val _uiState = MutableStateFlow(EventDetailsUiState(eventId = eventId))
    val uiState: StateFlow<EventDetailsUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<EventDetailsEvent>()
    val eventFlow: SharedFlow<EventDetailsEvent> = _eventFlow.asSharedFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                eventRepository.getEvents(),
                eventRepository.getTransactionsForEvent(eventId),
                settingsRepository.formattingSettings
            ) { allEvents, transactions, settings ->
                val currentEvent = allEvents.find { it.id == eventId }

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
                    event = currentEvent,
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

    fun toggleArchive() {
        val currentEvent = _uiState.value.event ?: return
        viewModelScope.launch {
            eventRepository.updateEventArchived(eventId, !currentEvent.isArchived)
        }
    }

    fun deleteEvent() {
        viewModelScope.launch {
            eventRepository.deleteEvent(eventId)
            _eventFlow.emit(EventDetailsEvent.Deleted)
        }
    }
}
