package com.sinxn.mymoney.feature.recurrence

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.entity.EventEntity
import com.sinxn.mymoney.core.data.local.entity.PlaceEntity
import com.sinxn.mymoney.core.data.local.entity.RecurrentTransferEntity
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

data class RecurrentTransferDetailsUiState(
    val id: String = "",
    val isNew: Boolean = true,
    val isLoading: Boolean = true,
    val description: String = "",
    val walletFromId: String = "",
    val walletToId: String = "",
    val moneyFromStr: String = "",
    val moneyToStr: String = "",
    val moneyTaxStr: String = "",
    val note: String = "",
    val placeId: String? = null,
    val eventId: String? = null,
    val confirmed: Boolean = true,
    val countInTotal: Boolean = true,
    val startDate: Date = Date(),
    val rule: String = RecurrenceRule(Freq.DAILY).toString(),
    val availableWallets: List<WalletEntity> = emptyList(),
    val availablePlaces: List<PlaceEntity> = emptyList(),
    val availableEvents: List<EventEntity> = emptyList()
)

@HiltViewModel
class RecurrentTransferDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recurrenceRepository: RecurrenceRepository,
    private val moneyDao: MoneyDao
) : ViewModel() {

    private val recurrenceId: String = savedStateHandle["id"] ?: "new"

    private val _uiState = MutableStateFlow(RecurrentTransferDetailsUiState(id = recurrenceId, isNew = recurrenceId == "new"))
    val uiState: StateFlow<RecurrentTransferDetailsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val wallets = moneyDao.getWalletsList()
            val places = moneyDao.getPlacesList()
            val events = moneyDao.getEventsList()

            val fromId = wallets.firstOrNull()?.id ?: ""
            val toId = wallets.getOrNull(1)?.id ?: fromId

            _uiState.update {
                it.copy(
                    availableWallets = wallets,
                    availablePlaces = places,
                    availableEvents = events,
                    walletFromId = fromId,
                    walletToId = toId,
                    isLoading = recurrenceId != "new"
                )
            }

            if (recurrenceId != "new") {
                val entity = moneyDao.getRecurrentTransferById(recurrenceId)
                if (entity != null) {
                    val parsedStartDate = DateUtils.parseDate(entity.startDate)
                    _uiState.update {
                        it.copy(
                            isNew = false,
                            isLoading = false,
                            description = entity.description ?: "",
                            walletFromId = entity.walletFromId,
                            walletToId = entity.walletToId,
                            moneyFromStr = (entity.moneyFrom / 100.0).toString(),
                            moneyToStr = (entity.moneyTo / 100.0).toString(),
                            moneyTaxStr = entity.moneyTax?.let { tax -> (tax / 100.0).toString() } ?: "",
                            note = entity.note ?: "",
                            placeId = entity.placeId,
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

    fun onDescriptionChanged(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun onWalletFromChanged(value: String) {
        _uiState.update { it.copy(walletFromId = value) }
    }

    fun onWalletToChanged(value: String) {
        _uiState.update { it.copy(walletToId = value) }
    }

    fun onMoneyFromChanged(value: String) {
        _uiState.update { current ->
            // If moneyTo was empty or equal to previous moneyFrom, mirror it automatically
            val mirrorTo = current.moneyToStr.isEmpty() || current.moneyToStr == current.moneyFromStr
            current.copy(
                moneyFromStr = value,
                moneyToStr = if (mirrorTo) value else current.moneyToStr
            )
        }
    }

    fun onMoneyToChanged(value: String) {
        _uiState.update { it.copy(moneyToStr = value) }
    }

    fun onMoneyTaxChanged(value: String) {
        _uiState.update { it.copy(moneyTaxStr = value) }
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
        val fromLong = ((state.moneyFromStr.toDoubleOrNull() ?: 0.0) * 100).toLong()
        val toLong = ((state.moneyToStr.toDoubleOrNull() ?: 0.0) * 100).toLong()
        val taxLong = state.moneyTaxStr.toDoubleOrNull()?.let { (it * 100).toLong() }

        if (fromLong <= 0 || toLong <= 0 || state.walletFromId.isBlank() || state.walletToId.isBlank()) return

        viewModelScope.launch {
            val id = if (state.isNew) UUID.randomUUID().toString() else state.id
            val startDateStr = DateUtils.getSQLDateTimeString(state.startDate)

            val setting = RecurrenceSetting.fromStringOrFallback(state.startDate, state.rule)
            val nextOccurrence = setting.getNextOccurrence(state.startDate)
            val nextOccurrenceStr = nextOccurrence?.let { DateUtils.getSQLDateTimeString(it) }

            val entity = RecurrentTransferEntity(
                id = id,
                description = state.description.takeIf { it.isNotBlank() },
                walletFromId = state.walletFromId,
                walletToId = state.walletToId,
                moneyFrom = fromLong,
                moneyTo = toLong,
                moneyTax = taxLong,
                note = state.note.takeIf { it.isNotBlank() },
                eventId = state.eventId,
                placeId = state.placeId,
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

            recurrenceRepository.saveRecurrentTransfer(entity)
            onSuccess()
        }
    }
}
