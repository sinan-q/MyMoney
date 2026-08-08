package com.sinxn.mymoney.feature.recurrence

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.data.local.entity.EventEntity
import com.sinxn.mymoney.core.data.local.entity.PlaceEntity
import com.sinxn.mymoney.core.data.local.entity.RecurrentTransactionEntity
import com.sinxn.mymoney.core.data.local.entity.WalletEntity
import com.sinxn.mymoney.core.data.repository.RecurrenceRepository
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.RecurrenceSetting
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.dmfs.rfc5545.recur.Freq
import org.dmfs.rfc5545.recur.RecurrenceRule
import java.util.Date
import java.util.UUID
import javax.inject.Inject

data class RecurrentTransactionDetailsUiState(
    val id: String = "",
    val isNew: Boolean = true,
    val isLoading: Boolean = true,
    val moneyStr: String = "",
    val description: String = "",
    val categoryId: String = "",
    val direction: Int = 0, // 0 Expense, 1 Income
    val walletId: String = "",
    val placeId: String? = null,
    val note: String = "",
    val eventId: String? = null,
    val confirmed: Boolean = true,
    val countInTotal: Boolean = true,
    val startDate: Date = Date(),
    val rule: String = RecurrenceRule(Freq.DAILY).toString(),
    val availableWallets: List<WalletEntity> = emptyList(),
    val availableCategories: List<CategoryEntity> = emptyList(),
    val availablePlaces: List<PlaceEntity> = emptyList(),
    val availableEvents: List<EventEntity> = emptyList()
)

@HiltViewModel
class RecurrentTransactionDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recurrenceRepository: RecurrenceRepository,
    private val moneyDao: MoneyDao
) : ViewModel() {

    private val recurrenceId: String = savedStateHandle["id"] ?: "new"

    private val _uiState = MutableStateFlow(RecurrentTransactionDetailsUiState(id = recurrenceId, isNew = recurrenceId == "new"))
    val uiState: StateFlow<RecurrentTransactionDetailsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val wallets = moneyDao.getWalletsList()
            val categories = moneyDao.getCategoriesList()
            val places = moneyDao.getPlacesList()
            val events = moneyDao.getEventsList()

            val defaultWalletId = wallets.firstOrNull()?.id ?: ""
            val defaultCategoryId = categories.firstOrNull { it.type == _uiState.value.direction }?.id
                ?: categories.firstOrNull()?.id ?: ""

            _uiState.update {
                it.copy(
                    availableWallets = wallets,
                    availableCategories = categories,
                    availablePlaces = places,
                    availableEvents = events,
                    walletId = defaultWalletId,
                    categoryId = defaultCategoryId,
                    isLoading = recurrenceId != "new"
                )
            }

            if (recurrenceId != "new") {
                val entity = moneyDao.getRecurrentTransactionById(recurrenceId)
                if (entity != null) {
                    val parsedStartDate = DateUtils.parseDate(entity.startDate)
                    val moneyFormatted = (entity.money / 100.0).toString()
                    _uiState.update {
                        it.copy(
                            isNew = false,
                            isLoading = false,
                            moneyStr = moneyFormatted,
                            description = entity.description ?: "",
                            categoryId = entity.categoryId,
                            direction = entity.direction,
                            walletId = entity.walletId,
                            placeId = entity.placeId,
                            note = entity.note ?: "",
                            eventId = entity.eventId,
                            confirmed = entity.confirmed,
                            countInTotal = entity.countInTotal,
                            startDate = parsedStartDate,
                            rule = entity.rule
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun onMoneyChanged(value: String) {
        _uiState.update { it.copy(moneyStr = value) }
    }

    fun onDescriptionChanged(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun onCategoryChanged(value: String) {
        _uiState.update { it.copy(categoryId = value) }
    }

    fun onDirectionChanged(value: Int) {
        _uiState.update { current ->
            val matchingCat = current.availableCategories.firstOrNull { it.type == value }?.id ?: current.categoryId
            current.copy(direction = value, categoryId = matchingCat)
        }
    }

    fun onWalletChanged(value: String) {
        _uiState.update { it.copy(walletId = value) }
    }

    fun onPlaceChanged(value: String?) {
        _uiState.update { it.copy(placeId = value) }
    }

    fun onEventChanged(value: String?) {
        _uiState.update { it.copy(eventId = value) }
    }

    fun onNoteChanged(value: String) {
        _uiState.update { it.copy(note = value) }
    }

    fun onConfirmedChanged(value: Boolean) {
        _uiState.update { it.copy(confirmed = value) }
    }

    fun onCountInTotalChanged(value: Boolean) {
        _uiState.update { it.copy(countInTotal = value) }
    }

    fun onRecurrenceRuleUpdated(startDate: Date, rule: String) {
        _uiState.update { it.copy(startDate = startDate, rule = rule) }
    }

    fun save(onSuccess: () -> Unit) {
        val state = _uiState.value
        val amountLong = ((state.moneyStr.toDoubleOrNull() ?: 0.0) * 100).toLong()
        if (amountLong <= 0 || state.walletId.isBlank() || state.categoryId.isBlank()) return

        viewModelScope.launch {
            val id = if (state.isNew) UUID.randomUUID().toString() else state.id
            val startDateStr = DateUtils.getSQLDateTimeString(state.startDate)

            // Compute next occurrence
            val setting = RecurrenceSetting.fromStringOrFallback(state.startDate, state.rule)
            val nextOccurrence = setting.getNextOccurrence(state.startDate)
            val nextOccurrenceStr = nextOccurrence?.let { DateUtils.getSQLDateTimeString(it) }

            val entity = RecurrentTransactionEntity(
                id = id,
                money = amountLong,
                description = state.description.takeIf { it.isNotBlank() },
                categoryId = state.categoryId,
                direction = state.direction,
                walletId = state.walletId,
                placeId = state.placeId,
                note = state.note.takeIf { it.isNotBlank() },
                eventId = state.eventId,
                confirmed = state.confirmed,
                countInTotal = state.countInTotal,
                startDate = startDateStr,
                lastOccurrence = startDateStr,
                nextOccurrence = nextOccurrenceStr,
                rule = state.rule,
                isDeleted = false,
                lastEdit = System.currentTimeMillis(),
                tag = null
            )

            recurrenceRepository.saveRecurrentTransaction(entity)
            onSuccess()
        }
    }
}
