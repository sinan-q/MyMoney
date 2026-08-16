package com.sinxn.mymoney.feature.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.entity.EventEntity
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.repository.EventRepository
import com.sinxn.mymoney.core.util.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class EventItemUi(
    val event: EventEntity,
    val totalAmount: Long = 0L,
    val income: Long = 0L,
    val expense: Long = 0L,
    val transactionCount: Int = 0,
    val currencyCode: String = "USD",
    val decimals: Int = 2
)

data class EventUiState(
    val events: List<EventItemUi> = emptyList(),
    val isLoading: Boolean = false,
    val formatterConfig: MoneyFormatter.Config = MoneyFormatter.Config(),
    val dateFormat: Int = 0
)

@HiltViewModel
class EventViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<EventUiState> = combine(
        eventRepository.getEvents(),
        eventRepository.getAllEventTransactions(),
        settingsRepository.formattingSettings
    ) { events, allTransactions, formattingSettings ->
        val txGrouped = allTransactions.groupBy { it.transaction.eventId }

        val eventItems = events.map { event ->
            val txs = txGrouped[event.id] ?: emptyList()
            var total = 0L
            var income = 0L
            var expense = 0L

            txs.forEach { item ->
                if (item.transaction.direction == 1) {
                    total += item.transaction.money
                    income += item.transaction.money
                } else {
                    total -= item.transaction.money
                    expense += item.transaction.money
                }
            }

            val distinctCurrencies = txs.mapNotNull { it.currencySymbol ?: it.currencyCode }.distinct()
            val currencyCode = if (distinctCurrencies.size == 1) distinctCurrencies.first() else formattingSettings.globalCurrency
            val decimals = txs.firstOrNull()?.decimals ?: 2

            EventItemUi(
                event = event,
                totalAmount = total,
                income = income,
                expense = expense,
                transactionCount = txs.size,
                currencyCode = currencyCode,
                decimals = decimals
            )
        }

        val formatterConfig = MoneyFormatter.Config(
            showCurrency = formattingSettings.showCurrency,
            groupDigits = formattingSettings.groupDigits,
            roundDecimals = formattingSettings.roundDecimals,
            showPlusMinus = formattingSettings.showPlusMinus
        )

        EventUiState(
            events = eventItems,
            isLoading = false,
            formatterConfig = formatterConfig,
            dateFormat = formattingSettings.dateFormat
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EventUiState(isLoading = true)
    )
}

