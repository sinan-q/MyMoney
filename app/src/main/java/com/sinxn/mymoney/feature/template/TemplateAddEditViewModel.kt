package com.sinxn.mymoney.feature.template

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.data.local.entity.CurrencyEntity
import com.sinxn.mymoney.core.data.local.entity.EventEntity
import com.sinxn.mymoney.core.data.local.entity.PlaceEntity
import com.sinxn.mymoney.core.data.local.entity.TransactionModelEntity
import com.sinxn.mymoney.core.data.local.entity.TransferModelEntity
import com.sinxn.mymoney.core.data.local.entity.WalletEntity
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.repository.TemplateRepository
import com.sinxn.mymoney.core.util.MathExpressionEvaluator
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
import java.util.Currency
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlin.math.pow
import kotlin.math.roundToLong

sealed class TemplateAddEditEvent {
    object Saved : TemplateAddEditEvent()
    object Deleted : TemplateAddEditEvent()
}

enum class TemplateType {
    EXPENSE,
    INCOME,
    TRANSFER
}

data class TemplateAddEditUiState(
    val isEditMode: Boolean = false,
    val templateId: String? = null,
    val type: TemplateType = TemplateType.EXPENSE,
    val amountStr: String = "",
    val amountToStr: String = "",
    val taxAmountStr: String = "",
    val description: String = "",
    val categoryId: String = "",
    val walletId: String = "",
    val targetWalletId: String = "",
    val placeId: String? = null,
    val eventId: String? = null,
    val note: String = "",
    val tag: String = "",
    val confirmed: Boolean = true,
    val countInTotal: Boolean = true,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val availableWallets: List<WalletEntity> = emptyList(),
    val availableCategories: List<CategoryEntity> = emptyList(),
    val availablePlaces: List<PlaceEntity> = emptyList(),
    val availableEvents: List<EventEntity> = emptyList(),
    val availableCurrencies: List<CurrencyEntity> = emptyList(),
    val currencySymbol: String = "$",
    val currencyDecimals: Int = 2,
    val targetCurrencySymbol: String = "$",
    val targetCurrencyDecimals: Int = 2
)

@HiltViewModel
class TemplateAddEditViewModel @Inject constructor(
    private val templateRepository: TemplateRepository,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val templateIdArg: String? = savedStateHandle["templateId"]
    private val isTransferArg: Boolean = savedStateHandle.get<String>("isTransfer")?.toBooleanStrictOrNull()
        ?: savedStateHandle.get<Boolean>("isTransfer") ?: false

    private val _uiState = MutableStateFlow(
        TemplateAddEditUiState(
            isEditMode = !templateIdArg.isNullOrBlank(),
            templateId = templateIdArg?.takeIf { it.isNotBlank() },
            type = if (isTransferArg) TemplateType.TRANSFER else TemplateType.EXPENSE
        )
    )
    val uiState: StateFlow<TemplateAddEditUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<TemplateAddEditEvent>()
    val eventFlow: SharedFlow<TemplateAddEditEvent> = _eventFlow.asSharedFlow()

    init {
        loadData()
    }

    private fun getCurrencyDecimals(iso: String?, currencies: List<CurrencyEntity>): Int {
        if (iso.isNullOrBlank()) return 2
        val entity = currencies.find { it.iso.equals(iso, ignoreCase = true) }
        if (entity != null) return entity.decimals
        return try {
            val digits = Currency.getInstance(iso).defaultFractionDigits
            if (digits >= 0) digits else 2
        } catch (e: Exception) {
            2
        }
    }

    private fun getCurrencySymbol(iso: String?, currencies: List<CurrencyEntity>): String {
        if (iso.isNullOrBlank()) return "$"
        val entity = currencies.find { it.iso.equals(iso, ignoreCase = true) }
        if (!entity?.symbol.isNullOrBlank()) return entity!!.symbol!!
        return try {
            Currency.getInstance(iso).getSymbol(Locale.getDefault())
        } catch (e: Exception) {
            MoneyFormatter.getCurrencySymbol(iso)
        }
    }

    private fun formatBaseUnitsToDecimal(amount: Long, decimals: Int): String {
        val divider = 10.0.pow(decimals.toDouble())
        val value = amount.toDouble() / divider
        return if (decimals == 0) {
            value.toLong().toString()
        } else if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            "%.${decimals}f".format(Locale.US, value).trimEnd('0').trimEnd('.')
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val wallets = templateRepository.wallets.firstOrNull() ?: emptyList()
            val categories = templateRepository.categories.firstOrNull() ?: emptyList()
            val places = templateRepository.places.firstOrNull() ?: emptyList()
            val events = templateRepository.events.firstOrNull() ?: emptyList()
            val currencies = templateRepository.currencies.firstOrNull() ?: emptyList()
            val currentWalletId = settingsRepository.currentWalletId.firstOrNull()

            val preferredWallet = if (!currentWalletId.isNullOrBlank() && currentWalletId != "total" && currentWalletId != com.sinxn.mymoney.core.util.Constants.TOTAL_WALLET_ID) {
                wallets.find { it.id == currentWalletId && !it.isArchived }
                    ?: wallets.find { it.id == currentWalletId }
            } else {
                null
            }

            val defaultWalletId = preferredWallet?.id ?: ""
            val defaultTargetWallet = if (defaultWalletId.isNotEmpty()) {
                wallets.firstOrNull { it.id != defaultWalletId && !it.isArchived }
                    ?: wallets.firstOrNull { it.id != defaultWalletId }
            } else null
            val defaultTargetWalletId = defaultTargetWallet?.id ?: ""

            val initialType = if (isTransferArg) TemplateType.TRANSFER else TemplateType.EXPENSE
            val targetCategoryType = if (initialType == TemplateType.INCOME) 1 else 0
            val defaultCatId = categories.firstOrNull { it.type == targetCategoryType && !it.isArchived }?.id
                ?: categories.firstOrNull { it.type == targetCategoryType }?.id
                ?: categories.firstOrNull()?.id ?: ""

            val defaultDecimals = getCurrencyDecimals(preferredWallet?.currency, currencies)
            val defaultSymbol = getCurrencySymbol(preferredWallet?.currency, currencies)
            val defaultTargetDecimals = getCurrencyDecimals(defaultTargetWallet?.currency, currencies)
            val defaultTargetSymbol = getCurrencySymbol(defaultTargetWallet?.currency, currencies)

            if (!templateIdArg.isNullOrBlank()) {
                if (isTransferArg) {
                    val transferModel = templateRepository.getTransferModelById(templateIdArg)
                    if (transferModel != null) {
                        val walletFrom = wallets.find { it.id == transferModel.walletFromId }
                        val walletTo = wallets.find { it.id == transferModel.walletToId }
                        val fromDecimals = getCurrencyDecimals(walletFrom?.currency, currencies)
                        val toDecimals = getCurrencyDecimals(walletTo?.currency, currencies)
                        val fromSymbol = getCurrencySymbol(walletFrom?.currency, currencies)
                        val toSymbol = getCurrencySymbol(walletTo?.currency, currencies)

                        val fromAmountStr = formatBaseUnitsToDecimal(transferModel.moneyFrom, fromDecimals)
                        val toAmountStr = formatBaseUnitsToDecimal(transferModel.moneyTo, toDecimals)
                        val taxAmountStr = transferModel.moneyTax?.let { formatBaseUnitsToDecimal(it, fromDecimals) } ?: ""

                        val activeWalletIds = setOf(transferModel.walletFromId, transferModel.walletToId)
                        val filteredWallets = wallets.filter { !it.isArchived || it.id in activeWalletIds }

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                type = TemplateType.TRANSFER,
                                amountStr = fromAmountStr,
                                amountToStr = toAmountStr,
                                taxAmountStr = taxAmountStr,
                                description = transferModel.description ?: "",
                                walletId = transferModel.walletFromId,
                                targetWalletId = transferModel.walletToId,
                                placeId = transferModel.placeId,
                                eventId = transferModel.eventId,
                                note = transferModel.note ?: "",
                                tag = transferModel.tag ?: "",
                                confirmed = transferModel.confirmed,
                                countInTotal = transferModel.countInTotal,
                                availableWallets = filteredWallets,
                                availableCategories = categories.filter { !it.isArchived },
                                availablePlaces = places.filter { !it.isArchived || it.id == transferModel.placeId },
                                availableEvents = events.filter { !it.isArchived || it.id == transferModel.eventId },
                                availableCurrencies = currencies,
                                currencySymbol = fromSymbol,
                                currencyDecimals = fromDecimals,
                                targetCurrencySymbol = toSymbol,
                                targetCurrencyDecimals = toDecimals
                            )
                        }
                        return@launch
                    }
                } else {
                    val txModel = templateRepository.getTransactionModelById(templateIdArg)
                    if (txModel != null) {
                        val wallet = wallets.find { it.id == txModel.walletId }
                        val decimals = getCurrencyDecimals(wallet?.currency, currencies)
                        val symbol = getCurrencySymbol(wallet?.currency, currencies)
                        val amountStr = formatBaseUnitsToDecimal(txModel.money, decimals)
                        val type = if (txModel.direction == 1) TemplateType.INCOME else TemplateType.EXPENSE

                        val filteredWallets = wallets.filter { !it.isArchived || it.id == txModel.walletId }
                        val filteredCategories = categories.filter { !it.isArchived || it.id == txModel.categoryId }

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                type = type,
                                amountStr = amountStr,
                                description = txModel.description ?: "",
                                categoryId = txModel.categoryId,
                                walletId = txModel.walletId,
                                placeId = txModel.placeId,
                                eventId = txModel.eventId,
                                note = txModel.note ?: "",
                                tag = txModel.tag ?: "",
                                confirmed = txModel.confirmed,
                                countInTotal = txModel.countInTotal,
                                availableWallets = filteredWallets,
                                availableCategories = filteredCategories,
                                availablePlaces = places.filter { !it.isArchived || it.id == txModel.placeId },
                                availableEvents = events.filter { !it.isArchived || it.id == txModel.eventId },
                                availableCurrencies = currencies,
                                currencySymbol = symbol,
                                currencyDecimals = decimals
                            )
                        }
                        return@launch
                    }
                }
            }

            val filteredWallets = wallets.filter { !it.isArchived || it.id == defaultWalletId || it.id == defaultTargetWalletId }
            val filteredCategories = categories.filter { !it.isArchived || it.id == defaultCatId }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    walletId = defaultWalletId,
                    targetWalletId = defaultTargetWalletId,
                    categoryId = defaultCatId,
                    availableWallets = filteredWallets,
                    availableCategories = filteredCategories,
                    availablePlaces = places.filter { !it.isArchived },
                    availableEvents = events.filter { !it.isArchived },
                    availableCurrencies = currencies,
                    currencySymbol = defaultSymbol,
                    currencyDecimals = defaultDecimals,
                    targetCurrencySymbol = defaultTargetSymbol,
                    targetCurrencyDecimals = defaultTargetDecimals
                )
            }
        }
    }

    fun onTypeChange(newType: TemplateType) {
        _uiState.update { current ->
            val targetCategoryType = when (newType) {
                TemplateType.INCOME -> 1
                TemplateType.EXPENSE -> 0
                TemplateType.TRANSFER -> null
            }

            val newCatId = if (targetCategoryType == null) {
                ""
            } else {
                val currentCat = current.availableCategories.find { it.id == current.categoryId }
                if (currentCat?.type == targetCategoryType) {
                    current.categoryId
                } else {
                    current.availableCategories.firstOrNull { it.type == targetCategoryType && !it.isArchived }?.id
                        ?: current.availableCategories.firstOrNull { it.type == targetCategoryType }?.id
                        ?: current.categoryId
                }
            }

            current.copy(type = newType, categoryId = newCatId)
        }
    }

    fun onAmountChange(value: String) {
        _uiState.update { it.copy(amountStr = value) }
    }

    fun onAmountToChange(value: String) {
        _uiState.update { it.copy(amountToStr = value) }
    }

    fun onTaxAmountChange(value: String) {
        _uiState.update { it.copy(taxAmountStr = value) }
    }

    fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun onCategoryChange(categoryId: String) {
        val cat = _uiState.value.availableCategories.find { it.id == categoryId }
        val newType = when (cat?.type) {
            1 -> TemplateType.INCOME
            0 -> TemplateType.EXPENSE
            else -> _uiState.value.type
        }
        _uiState.update { it.copy(categoryId = categoryId, type = newType) }
    }

    fun onWalletChange(walletId: String) {
        val wallet = _uiState.value.availableWallets.find { it.id == walletId }
        val decimals = getCurrencyDecimals(wallet?.currency, _uiState.value.availableCurrencies)
        val symbol = getCurrencySymbol(wallet?.currency, _uiState.value.availableCurrencies)

        _uiState.update {
            it.copy(
                walletId = walletId,
                currencySymbol = symbol,
                currencyDecimals = decimals
            )
        }
    }

    fun onTargetWalletChange(walletId: String) {
        val wallet = _uiState.value.availableWallets.find { it.id == walletId }
        val decimals = getCurrencyDecimals(wallet?.currency, _uiState.value.availableCurrencies)
        val symbol = getCurrencySymbol(wallet?.currency, _uiState.value.availableCurrencies)

        _uiState.update {
            it.copy(
                targetWalletId = walletId,
                targetCurrencySymbol = symbol,
                targetCurrencyDecimals = decimals
            )
        }
    }

    fun onPlaceChange(placeId: String?) {
        _uiState.update { it.copy(placeId = placeId) }
    }

    fun onEventChange(eventId: String?) {
        _uiState.update { it.copy(eventId = eventId) }
    }

    fun onNoteChange(value: String) {
        _uiState.update { it.copy(note = value) }
    }

    fun onTagChange(value: String) {
        _uiState.update { it.copy(tag = value) }
    }

    fun onConfirmedChange(value: Boolean) {
        _uiState.update { it.copy(confirmed = value) }
    }

    fun onCountInTotalChange(value: Boolean) {
        _uiState.update { it.copy(countInTotal = value) }
    }

    fun getImmediateResult(input: String): String {
        return MathExpressionEvaluator.getImmediateResult(input, _uiState.value.currencyDecimals)
    }

    fun saveTemplate() {
        val state = _uiState.value
        val evaluatedAmount = getImmediateResult(state.amountStr).toDoubleOrNull() ?: 0.0
        if (evaluatedAmount <= 0.0) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val now = System.currentTimeMillis()
            val templateId = state.templateId ?: UUID.randomUUID().toString()

            if (state.type == TemplateType.TRANSFER) {
                if (state.walletId.isBlank() || state.targetWalletId.isBlank()) {
                    _uiState.update { it.copy(isSaving = false) }
                    return@launch
                }
                val fromDecimals = state.currencyDecimals
                val toDecimals = state.targetCurrencyDecimals
                val moneyFrom = (evaluatedAmount * 10.0.pow(fromDecimals.toDouble())).roundToLong()
                val evaluatedTo = (getImmediateResult(state.amountToStr).toDoubleOrNull() ?: evaluatedAmount)
                val moneyTo = (evaluatedTo * 10.0.pow(toDecimals.toDouble())).roundToLong()
                val moneyTax = getImmediateResult(state.taxAmountStr).toDoubleOrNull()?.let {
                    (it * 10.0.pow(fromDecimals.toDouble())).roundToLong()
                }

                val transferModel = TransferModelEntity(
                    id = templateId,
                    description = state.description.takeIf { it.isNotBlank() },
                    walletFromId = state.walletId,
                    walletToId = state.targetWalletId,
                    moneyFrom = moneyFrom,
                    moneyTo = moneyTo,
                    moneyTax = moneyTax,
                    note = state.note.takeIf { it.isNotBlank() },
                    eventId = state.eventId,
                    placeId = state.placeId,
                    confirmed = state.confirmed,
                    countInTotal = state.countInTotal,
                    isDeleted = false,
                    lastEdit = now,
                    tag = state.tag.takeIf { it.isNotBlank() }
                )
                templateRepository.saveTransferModel(transferModel)
            } else {
                if (state.walletId.isBlank() || state.categoryId.isBlank()) {
                    _uiState.update { it.copy(isSaving = false) }
                    return@launch
                }
                val decimals = state.currencyDecimals
                val money = (evaluatedAmount * 10.0.pow(decimals.toDouble())).roundToLong()
                val direction = if (state.type == TemplateType.INCOME) 1 else 0

                val txModel = TransactionModelEntity(
                    id = templateId,
                    money = money,
                    description = state.description.takeIf { it.isNotBlank() },
                    categoryId = state.categoryId,
                    direction = direction,
                    walletId = state.walletId,
                    placeId = state.placeId,
                    note = state.note.takeIf { it.isNotBlank() },
                    eventId = state.eventId,
                    confirmed = state.confirmed,
                    countInTotal = state.countInTotal,
                    isDeleted = false,
                    lastEdit = now,
                    tag = state.tag.takeIf { it.isNotBlank() }
                )
                templateRepository.saveTransactionModel(txModel)
            }

            _eventFlow.emit(TemplateAddEditEvent.Saved)
        }
    }

    fun deleteTemplate() {
        val state = _uiState.value
        val id = state.templateId ?: return

        viewModelScope.launch {
            if (state.type == TemplateType.TRANSFER) {
                templateRepository.deleteTransferModel(id)
            } else {
                templateRepository.deleteTransactionModel(id)
            }
            _eventFlow.emit(TemplateAddEditEvent.Deleted)
        }
    }
}
