package com.sinxn.mymoney.feature.recurrence

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.entity.CurrencyEntity
import com.sinxn.mymoney.core.data.local.entity.EventEntity
import com.sinxn.mymoney.core.data.local.entity.PlaceEntity
import com.sinxn.mymoney.core.data.local.entity.RecurrentTransferEntity
import com.sinxn.mymoney.core.data.local.entity.WalletEntity
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.repository.RecurrenceRepository
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MathExpressionEvaluator
import com.sinxn.mymoney.core.util.MoneyFormatter
import com.sinxn.mymoney.core.util.RecurrenceSetting
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.dmfs.rfc5545.recur.Freq
import org.dmfs.rfc5545.recur.RecurrenceRule
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import kotlin.math.pow
import kotlin.math.roundToLong

data class RecurrentTransferAddEditUiState(
    val id: String = "",
    val isNew: Boolean = true,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
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
    val currencySymbol: String = "$",
    val currencyDecimals: Int = 2,
    val targetCurrencySymbol: String = "$",
    val targetCurrencyDecimals: Int = 2,
    val availableWallets: List<WalletEntity> = emptyList(),
    val availablePlaces: List<PlaceEntity> = emptyList(),
    val availableEvents: List<EventEntity> = emptyList()
)

@HiltViewModel
class RecurrentTransferAddEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recurrenceRepository: RecurrenceRepository,
    private val settingsRepository: SettingsRepository,
    private val moneyDao: MoneyDao
) : ViewModel() {

    private val recurrenceId: String? = savedStateHandle.get<String>("id")?.takeIf { it.isNotBlank() && it != "new" }
    private var existingEntity: RecurrentTransferEntity? = null
    private var currencyList: List<CurrencyEntity> = emptyList()

    private val _uiState = MutableStateFlow(
        RecurrentTransferAddEditUiState(
            id = recurrenceId ?: "",
            isNew = recurrenceId == null
        )
    )
    val uiState: StateFlow<RecurrentTransferAddEditUiState> = _uiState.asStateFlow()

    val formattingSettings: StateFlow<FormattingSettings> = settingsRepository.formattingSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FormattingSettings())

    private fun getDecimalsForCurrency(currencyCode: String): Int {
        return currencyList.find { it.iso == currencyCode }?.decimals
            ?: MoneyFormatter.getCurrencyDecimals(currencyCode)
    }

    private fun getSymbolForCurrency(currencyCode: String): String {
        return currencyList.find { it.iso == currencyCode }?.symbol
            ?: MoneyFormatter.getCurrencySymbol(currencyCode)
    }

    init {
        viewModelScope.launch {
            val wallets = moneyDao.getWalletsList()
            val places = moneyDao.getPlacesList()
            val events = moneyDao.getEventsList()
            currencyList = moneyDao.getCurrenciesList()
            val currentWalletId = settingsRepository.currentWalletId.firstOrNull()

            val preferredWallet = if (!currentWalletId.isNullOrBlank() && currentWalletId != "total" && currentWalletId != com.sinxn.mymoney.core.util.Constants.TOTAL_WALLET_ID) {
                wallets.find { it.id == currentWalletId && !it.isArchived }
                    ?: wallets.find { it.id == currentWalletId }
            } else {
                null
            }

            val fromId = preferredWallet?.id ?: ""
            val toId = if (fromId.isNotEmpty()) {
                wallets.firstOrNull { it.id != fromId && !it.isArchived }?.id
                    ?: wallets.firstOrNull { it.id != fromId }?.id
                    ?: ""
            } else ""

            val fromWallet = wallets.find { it.id == fromId }
            val toWallet = wallets.find { it.id == toId }

            val currSymbol = getSymbolForCurrency(fromWallet?.currency ?: "")
            val currDecimals = getDecimalsForCurrency(fromWallet?.currency ?: "")
            val targetCurrSymbol = getSymbolForCurrency(toWallet?.currency ?: "")
            val targetCurrDecimals = getDecimalsForCurrency(toWallet?.currency ?: "")

            val filteredWallets = wallets.filter { !it.isArchived || it.id == fromId || it.id == toId }

            _uiState.update {
                it.copy(
                    availableWallets = filteredWallets,
                    availablePlaces = places.filter { p -> !p.isArchived },
                    availableEvents = events.filter { e -> !e.isArchived },
                    walletFromId = fromId,
                    walletToId = toId,
                    currencySymbol = currSymbol,
                    currencyDecimals = currDecimals,
                    targetCurrencySymbol = targetCurrSymbol,
                    targetCurrencyDecimals = targetCurrDecimals,
                    isLoading = recurrenceId != null
                )
            }

            if (recurrenceId != null) {
                val entity = moneyDao.getRecurrentTransferById(recurrenceId)
                existingEntity = entity
                if (entity != null) {
                    val parsedStartDate = DateUtils.parseDate(entity.startDate)
                    val matchingFromWallet = wallets.find { w -> w.id == entity.walletFromId }
                    val matchingToWallet = wallets.find { w -> w.id == entity.walletToId }

                    val fromDecimals = getDecimalsForCurrency(matchingFromWallet?.currency ?: "")
                    val fromCurrSymbol = getSymbolForCurrency(matchingFromWallet?.currency ?: "")
                    val toDecimals = getDecimalsForCurrency(matchingToWallet?.currency ?: "")
                    val toCurrSymbol = getSymbolForCurrency(matchingToWallet?.currency ?: "")

                    val fromFormatted = MoneyFormatter.format(
                        amount = entity.moneyFrom,
                        currencyCode = matchingFromWallet?.currency ?: "",
                        decimals = fromDecimals,
                        config = MoneyFormatter.Config(
                            showCurrency = false,
                            groupDigits = false,
                            roundDecimals = false,
                            showPlusMinus = false
                        )
                    )
                    val toFormatted = MoneyFormatter.format(
                        amount = entity.moneyTo,
                        currencyCode = matchingToWallet?.currency ?: "",
                        decimals = toDecimals,
                        config = MoneyFormatter.Config(
                            showCurrency = false,
                            groupDigits = false,
                            roundDecimals = false,
                            showPlusMinus = false
                        )
                    )
                    val taxFormatted = entity.moneyTax?.let { tax ->
                        MoneyFormatter.format(
                            amount = tax,
                            currencyCode = matchingFromWallet?.currency ?: "",
                            decimals = fromDecimals,
                            config = MoneyFormatter.Config(
                                showCurrency = false,
                                groupDigits = false,
                                roundDecimals = false,
                                showPlusMinus = false
                            )
                        )
                    } ?: ""

                    val editWallets = wallets.filter { w -> !w.isArchived || w.id == entity.walletFromId || w.id == entity.walletToId }

                    _uiState.update {
                        it.copy(
                            isNew = false,
                            isLoading = false,
                            description = entity.description ?: "",
                            walletFromId = entity.walletFromId,
                            walletToId = entity.walletToId,
                            moneyFromStr = fromFormatted,
                            moneyToStr = toFormatted,
                            moneyTaxStr = taxFormatted,
                            note = entity.note ?: "",
                            placeId = entity.placeId,
                            eventId = entity.eventId,
                            confirmed = entity.confirmed,
                            countInTotal = entity.countInTotal,
                            startDate = parsedStartDate,
                            rule = entity.rule,
                            currencySymbol = fromCurrSymbol,
                            currencyDecimals = fromDecimals,
                            targetCurrencySymbol = toCurrSymbol,
                            targetCurrencyDecimals = toDecimals,
                            availableWallets = editWallets,
                            availablePlaces = places.filter { p -> !p.isArchived || p.id == entity.placeId },
                            availableEvents = events.filter { e -> !e.isArchived || e.id == entity.eventId }
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun onMoneyFromChanged(value: String) {
        _uiState.update { current ->
            val mirrorTo = current.moneyToStr.isEmpty() || current.moneyToStr == current.moneyFromStr
            current.copy(
                moneyFromStr = value,
                moneyToStr = if (mirrorTo) value else current.moneyToStr
            )
        }
    }

    fun onNumpadKeyPress(key: String) {
        _uiState.update { current ->
            val newAmount = MathExpressionEvaluator.processNumpadKeyPress(current.moneyFromStr, key)
            val mirrorTo = current.moneyToStr.isEmpty() || current.moneyToStr == current.moneyFromStr
            current.copy(
                moneyFromStr = newAmount,
                moneyToStr = if (mirrorTo) newAmount else current.moneyToStr
            )
        }
    }

    fun evaluateMathExpression() {
        val decimals = _uiState.value.currencyDecimals
        _uiState.update { current ->
            val evaluated = MathExpressionEvaluator.evaluateMathExpression(current.moneyFromStr, decimals)
            val mirrorTo = current.moneyToStr.isEmpty() || current.moneyToStr == current.moneyFromStr
            current.copy(
                moneyFromStr = evaluated,
                moneyToStr = if (mirrorTo) evaluated else current.moneyToStr
            )
        }
    }

    fun getImmediateResult(amountStr: String): String {
        return MathExpressionEvaluator.getImmediateResult(amountStr, _uiState.value.currencyDecimals)
    }

    fun onDescriptionChanged(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun onWalletFromChanged(value: String) {
        _uiState.update { current ->
            val matchingWallet = current.availableWallets.find { it.id == value }
            val currSymbol = getSymbolForCurrency(matchingWallet?.currency ?: "")
            val currDecimals = getDecimalsForCurrency(matchingWallet?.currency ?: "")
            current.copy(
                walletFromId = value,
                currencySymbol = currSymbol,
                currencyDecimals = currDecimals
            )
        }
    }

    fun onWalletToChanged(value: String) {
        _uiState.update { current ->
            val matchingWallet = current.availableWallets.find { it.id == value }
            val targetCurrSymbol = getSymbolForCurrency(matchingWallet?.currency ?: "")
            val targetCurrDecimals = getDecimalsForCurrency(matchingWallet?.currency ?: "")
            current.copy(
                walletToId = value,
                targetCurrencySymbol = targetCurrSymbol,
                targetCurrencyDecimals = targetCurrDecimals
            )
        }
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
        val evaluatedMoney = getImmediateResult(state.moneyFromStr)
        val fromDivider = 10.0.pow(state.currencyDecimals.toDouble())
        val toDivider = 10.0.pow(state.targetCurrencyDecimals.toDouble())

        val fromLong = (evaluatedMoney.toDoubleOrNull()?.times(fromDivider))?.roundToLong() ?: 0L
        val toEvaluated = state.moneyToStr.takeIf { it.isNotBlank() } ?: evaluatedMoney
        val toLong = (toEvaluated.toDoubleOrNull()?.times(toDivider))?.roundToLong() ?: fromLong
        val taxLong = state.moneyTaxStr.toDoubleOrNull()?.let { (it * fromDivider).roundToLong() }

        if (fromLong <= 0 || toLong <= 0 || state.walletFromId.isBlank() || state.walletToId.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val id = if (state.isNew) UUID.randomUUID().toString() else state.id
                val startDateStr = DateUtils.getSQLDateTimeString(state.startDate)

                val lastOccurrenceStr: String?
                val nextOccurrenceStr: String?

                if (state.isNew || existingEntity == null) {
                    lastOccurrenceStr = startDateStr
                    nextOccurrenceStr = startDateStr
                } else {
                    val oldStartDate = existingEntity?.startDate
                    val oldRule = existingEntity?.rule
                    if (oldStartDate == startDateStr && oldRule == state.rule) {
                        lastOccurrenceStr = existingEntity?.lastOccurrence ?: startDateStr
                        nextOccurrenceStr = existingEntity?.nextOccurrence
                    } else {
                        lastOccurrenceStr = startDateStr
                        nextOccurrenceStr = startDateStr
                    }
                }

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
                    lastOccurrence = lastOccurrenceStr,
                    nextOccurrence = nextOccurrenceStr,
                    rule = state.rule,
                    isDeleted = false,
                    lastEdit = System.currentTimeMillis(),
                    tag = null
                )

                recurrenceRepository.saveRecurrentTransfer(entity)
                onSuccess()
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
