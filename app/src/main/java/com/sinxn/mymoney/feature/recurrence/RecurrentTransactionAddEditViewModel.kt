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
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.repository.RecurrenceRepository
import com.sinxn.mymoney.core.util.CategoryType
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.Direction
import com.sinxn.mymoney.core.util.MathExpressionEvaluator
import com.sinxn.mymoney.core.util.RecurrenceSetting
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.dmfs.rfc5545.recur.Freq
import org.dmfs.rfc5545.recur.RecurrenceRule
import java.util.Currency
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

data class RecurrentTransactionAddEditUiState(
    val id: String = "",
    val isNew: Boolean = true,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
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
    val currencySymbol: String = "$",
    val currencyDecimals: Int = 2,
    val availableWallets: List<WalletEntity> = emptyList(),
    val availableCategories: List<CategoryEntity> = emptyList(),
    val availableIncomeCategories: List<CategoryEntity> = emptyList(),
    val availableExpenseCategories: List<CategoryEntity> = emptyList(),
    val availablePlaces: List<PlaceEntity> = emptyList(),
    val availableEvents: List<EventEntity> = emptyList()
)

@HiltViewModel
class RecurrentTransactionAddEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recurrenceRepository: RecurrenceRepository,
    private val settingsRepository: SettingsRepository,
    private val moneyDao: MoneyDao
) : ViewModel() {

    private val recurrenceId: String? = savedStateHandle.get<String>("id")?.takeIf { it.isNotBlank() && it != "new" }

    private val _uiState = MutableStateFlow(
        RecurrentTransactionAddEditUiState(
            id = recurrenceId ?: "",
            isNew = recurrenceId == null
        )
    )
    val uiState: StateFlow<RecurrentTransactionAddEditUiState> = _uiState.asStateFlow()

    val formattingSettings: StateFlow<FormattingSettings> = settingsRepository.formattingSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FormattingSettings())

    init {
        viewModelScope.launch {
            val wallets = moneyDao.getWalletsList()
            val categories = moneyDao.getCategoriesList()
            val places = moneyDao.getPlacesList()
            val events = moneyDao.getEventsList()

            val defaultWallet = wallets.firstOrNull()
            val defaultWalletId = defaultWallet?.id ?: ""
            val targetCatType = if (_uiState.value.direction == Direction.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
            val defaultCategoryId = categories.firstOrNull { it.type == targetCatType }?.id
                ?: categories.firstOrNull()?.id ?: ""

            val currSymbol = try {
                defaultWallet?.currency?.let { Currency.getInstance(it).getSymbol(Locale.getDefault()) } ?: "$"
            } catch (e: Exception) {
                defaultWallet?.currency ?: "$"
            }

            _uiState.update {
                it.copy(
                    availableWallets = wallets,
                    availableCategories = categories,
                    availableIncomeCategories = categories.filter { c -> c.type == CategoryType.INCOME },
                    availableExpenseCategories = categories.filter { c -> c.type == CategoryType.EXPENSE },
                    availablePlaces = places,
                    availableEvents = events,
                    walletId = defaultWalletId,
                    categoryId = defaultCategoryId,
                    currencySymbol = currSymbol,
                    currencyDecimals = 2,
                    isLoading = recurrenceId != null
                )
            }

            if (recurrenceId != null) {
                val entity = moneyDao.getRecurrentTransactionById(recurrenceId)
                if (entity != null) {
                    val parsedStartDate = DateUtils.parseDate(entity.startDate)
                    val matchingWallet = wallets.find { w -> w.id == entity.walletId }
                    val entityCurrSymbol = try {
                        matchingWallet?.currency?.let { Currency.getInstance(it).getSymbol(Locale.getDefault()) } ?: "$"
                    } catch (e: Exception) {
                        matchingWallet?.currency ?: "$"
                    }
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
                            rule = entity.rule,
                            currencySymbol = entityCurrSymbol,
                            currencyDecimals = 2
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

    fun onNumpadKeyPress(key: String) {
        _uiState.update { it.copy(moneyStr = MathExpressionEvaluator.processNumpadKeyPress(it.moneyStr, key)) }
    }

    fun evaluateMathExpression() {
        val decimals = _uiState.value.currencyDecimals
        _uiState.update { it.copy(moneyStr = MathExpressionEvaluator.evaluateMathExpression(it.moneyStr, decimals)) }
    }

    fun getImmediateResult(amountStr: String): String {
        return MathExpressionEvaluator.getImmediateResult(amountStr, _uiState.value.currencyDecimals)
    }

    fun onDescriptionChanged(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun onCategoryChanged(value: String) {
        _uiState.update { current ->
            val cat = current.availableCategories.find { it.id == value }
            val newDir = if (cat?.type == CategoryType.INCOME) Direction.INCOME else Direction.EXPENSE
            current.copy(categoryId = value, direction = newDir)
        }
    }

    fun onDirectionChanged(value: Int) {
        _uiState.update { current ->
            val targetCatType = if (value == Direction.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
            val matchingCat = current.availableCategories.firstOrNull { it.type == targetCatType }?.id ?: current.categoryId
            current.copy(direction = value, categoryId = matchingCat)
        }
    }

    fun onWalletChanged(value: String) {
        _uiState.update { current ->
            val matchingWallet = current.availableWallets.find { it.id == value }
            val currSymbol = try {
                matchingWallet?.currency?.let { Currency.getInstance(it).getSymbol(Locale.getDefault()) } ?: "$"
            } catch (e: Exception) {
                matchingWallet?.currency ?: "$"
            }
            current.copy(
                walletId = value,
                currencySymbol = currSymbol,
                currencyDecimals = 2
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
        val evaluatedMoney = getImmediateResult(state.moneyStr)
        val amountLong = ((evaluatedMoney.toDoubleOrNull() ?: 0.0) * 100).toLong()
        if (amountLong <= 0 || state.walletId.isBlank() || state.categoryId.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
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
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
