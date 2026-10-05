package com.sinxn.mymoney.feature.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.preferences.toFormatterConfig
import com.sinxn.mymoney.core.data.repository.EventRepository
import com.sinxn.mymoney.core.ui.components.IconData
import com.sinxn.mymoney.core.ui.components.SortOption
import com.sinxn.mymoney.core.ui.components.parseIconData
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class EventSortOption(override val title: String) : SortOption {
    LAST_EDIT("Recently Edited"),
    ALPHABETICAL("Alphabetical (A-Z)")
}

data class EventItemUi(
    val id: String,
    val name: String,
    val note: String? = null,
    val tag: String? = null,
    val iconData: IconData,
    val lastEdit: Long = 0L,
    val formattedDateRange: String = "",
    val amountText: String = "",
    val totalAmount: Long = 0L,
    val isPositive: Boolean = false,
    val isNegative: Boolean = false,
    val transactionCount: Int = 0
)

data class EventUiState(
    val events: List<EventItemUi> = emptyList(),
    val totalEventsCount: Int = 0,
    val searchQuery: String = "",
    val sortOption: EventSortOption = EventSortOption.LAST_EDIT,
    val isLoading: Boolean = false
)

@HiltViewModel
class EventViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<EventUiState> = combine(
        eventRepository.getEvents(),
        eventRepository.getAllEventTransactions(),
        settingsRepository.eventsSortOption,
        settingsRepository.formattingSettings,
        _searchQuery
    ) { events, allTransactions, sortOptionName, formattingSettings, searchQuery ->
        val sortOption = try {
            EventSortOption.valueOf(sortOptionName)
        } catch (e: Exception) {
            EventSortOption.LAST_EDIT
        }

        val txGrouped = allTransactions.groupBy { it.transaction.eventId }

        val formatterConfig = formattingSettings.toFormatterConfig()

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

            val startDateObj = DateUtils.parseDate(event.startDate)
            val endDateObj = DateUtils.parseDate(event.endDate)
            val startStr = DateUtils.formatDate(startDateObj, formattingSettings.dateFormat)
            val endStr = DateUtils.formatDate(endDateObj, formattingSettings.dateFormat)
            val formattedDateRange = if (startStr == endStr) startStr else "$startStr - $endStr"

            val formattedMoney = MoneyFormatter.format(
                amount = total,
                currencyCode = currencyCode,
                decimals = decimals,
                config = formatterConfig
            )
            val isPositive = total > 0
            val isNegative = total < 0
            val amountText = (if (isPositive && !formattedMoney.startsWith("+")) "+" else "") + formattedMoney
            val iconData = parseIconData(event.icon.ifBlank { "ic_event" }, event.name)

            EventItemUi(
                id = event.id,
                name = event.name,
                note = event.note,
                tag = event.tag,
                iconData = iconData,
                lastEdit = event.lastEdit,
                formattedDateRange = formattedDateRange,
                amountText = amountText,
                totalAmount = total,
                isPositive = isPositive,
                isNegative = isNegative,
                transactionCount = txs.size
            )
        }

        val sortedEvents = when (sortOption) {
            EventSortOption.LAST_EDIT -> eventItems.sortedByDescending { it.lastEdit }
            EventSortOption.ALPHABETICAL -> eventItems.sortedWith(
                compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            )
        }

        val filteredEvents = if (searchQuery.isBlank()) {
            sortedEvents
        } else {
            sortedEvents.filter { item ->
                item.name.contains(searchQuery, ignoreCase = true) ||
                (!item.note.isNullOrBlank() && item.note.contains(searchQuery, ignoreCase = true)) ||
                (!item.tag.isNullOrBlank() && item.tag.contains(searchQuery, ignoreCase = true))
            }
        }

        EventUiState(
            events = filteredEvents,
            totalEventsCount = sortedEvents.size,
            searchQuery = searchQuery,
            sortOption = sortOption,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EventUiState(isLoading = true)
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOption(sortOption: EventSortOption) {
        viewModelScope.launch {
            settingsRepository.setEventsSortOption(sortOption.name)
        }
    }
}

