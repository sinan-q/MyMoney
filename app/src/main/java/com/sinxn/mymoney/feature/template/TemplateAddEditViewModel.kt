package com.sinxn.mymoney.feature.template

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.data.local.entity.EventEntity
import com.sinxn.mymoney.core.data.local.entity.PlaceEntity
import com.sinxn.mymoney.core.data.local.entity.TransactionModelEntity
import com.sinxn.mymoney.core.data.local.entity.TransferModelEntity
import com.sinxn.mymoney.core.data.local.entity.WalletEntity
import com.sinxn.mymoney.core.data.repository.TemplateRepository
import com.sinxn.mymoney.core.util.MathExpressionEvaluator
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
import java.util.UUID
import javax.inject.Inject
import kotlin.math.pow

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
    val currencySymbol: String = "$",
    val targetCurrencySymbol: String = "$"
)

@HiltViewModel
class TemplateAddEditViewModel @Inject constructor(
    private val templateRepository: TemplateRepository,
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

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val wallets = templateRepository.wallets.firstOrNull() ?: emptyList()
            val categories = templateRepository.categories.firstOrNull() ?: emptyList()
            val places = templateRepository.places.firstOrNull() ?: emptyList()
            val events = templateRepository.events.firstOrNull() ?: emptyList()

            val defaultWallet = wallets.firstOrNull()?.id ?: ""
            val defaultTargetWallet = wallets.getOrNull(1)?.id ?: defaultWallet
            val defaultCat = categories.firstOrNull()?.id ?: ""

            if (!templateIdArg.isNullOrBlank()) {
                if (isTransferArg) {
                    val transferModel = templateRepository.getTransferModelById(templateIdArg)
                    if (transferModel != null) {
                        val walletFrom = wallets.find { it.id == transferModel.walletFromId }
                        val walletTo = wallets.find { it.id == transferModel.walletToId }
                        val fromDecimals = 2 // default decimals
                        val toDecimals = 2
                        val fromAmount = transferModel.moneyFrom.toDouble() / 10.0.pow(fromDecimals)
                        val toAmount = transferModel.moneyTo.toDouble() / 10.0.pow(toDecimals)
                        val taxAmount = transferModel.moneyTax?.let { it.toDouble() / 10.0.pow(fromDecimals) }

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                type = TemplateType.TRANSFER,
                                amountStr = if (fromAmount % 1.0 == 0.0) fromAmount.toLong().toString() else fromAmount.toString(),
                                amountToStr = if (toAmount % 1.0 == 0.0) toAmount.toLong().toString() else toAmount.toString(),
                                taxAmountStr = taxAmount?.let { t -> if (t % 1.0 == 0.0) t.toLong().toString() else t.toString() } ?: "",
                                description = transferModel.description ?: "",
                                walletId = transferModel.walletFromId,
                                targetWalletId = transferModel.walletToId,
                                placeId = transferModel.placeId,
                                eventId = transferModel.eventId,
                                note = transferModel.note ?: "",
                                tag = transferModel.tag ?: "",
                                confirmed = transferModel.confirmed,
                                countInTotal = transferModel.countInTotal,
                                availableWallets = wallets,
                                availableCategories = categories,
                                availablePlaces = places,
                                availableEvents = events
                            )
                        }
                        return@launch
                    }
                } else {
                    val txModel = templateRepository.getTransactionModelById(templateIdArg)
                    if (txModel != null) {
                        val decimals = 2
                        val amount = txModel.money.toDouble() / 10.0.pow(decimals)
                        val type = if (txModel.direction == 1) TemplateType.INCOME else TemplateType.EXPENSE

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                type = type,
                                amountStr = if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString(),
                                description = txModel.description ?: "",
                                categoryId = txModel.categoryId,
                                walletId = txModel.walletId,
                                placeId = txModel.placeId,
                                eventId = txModel.eventId,
                                note = txModel.note ?: "",
                                tag = txModel.tag ?: "",
                                confirmed = txModel.confirmed,
                                countInTotal = txModel.countInTotal,
                                availableWallets = wallets,
                                availableCategories = categories,
                                availablePlaces = places,
                                availableEvents = events
                            )
                        }
                        return@launch
                    }
                }
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    walletId = defaultWallet,
                    targetWalletId = defaultTargetWallet,
                    categoryId = defaultCat,
                    availableWallets = wallets,
                    availableCategories = categories,
                    availablePlaces = places,
                    availableEvents = events
                )
            }
        }
    }

    fun onTypeChange(newType: TemplateType) {
        _uiState.update { it.copy(type = newType) }
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
        _uiState.update { it.copy(categoryId = categoryId) }
    }

    fun onWalletChange(walletId: String) {
        _uiState.update { it.copy(walletId = walletId) }
    }

    fun onTargetWalletChange(walletId: String) {
        _uiState.update { it.copy(targetWalletId = walletId) }
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
        return MathExpressionEvaluator.getImmediateResult(input, 2)
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
                val fromDecimals = 2
                val toDecimals = 2
                val moneyFrom = (evaluatedAmount * 10.0.pow(fromDecimals)).toLong()
                val evaluatedTo = (getImmediateResult(state.amountToStr).toDoubleOrNull() ?: evaluatedAmount)
                val moneyTo = (evaluatedTo * 10.0.pow(toDecimals)).toLong()
                val moneyTax = getImmediateResult(state.taxAmountStr).toDoubleOrNull()?.let {
                    (it * 10.0.pow(fromDecimals)).toLong()
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
                val decimals = 2
                val money = (evaluatedAmount * 10.0.pow(decimals)).toLong()
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
