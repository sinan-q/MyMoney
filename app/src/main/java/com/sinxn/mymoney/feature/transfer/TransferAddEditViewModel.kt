package com.sinxn.mymoney.feature.transfer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.entity.EventEntity
import com.sinxn.mymoney.core.data.local.entity.PersonEntity
import com.sinxn.mymoney.core.data.local.entity.PlaceEntity
import com.sinxn.mymoney.core.data.local.entity.TransactionEntity
import com.sinxn.mymoney.core.data.local.entity.TransferEntity
import com.sinxn.mymoney.core.data.local.entity.WalletEntity
import com.sinxn.mymoney.core.data.local.model.WalletWithBalance
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.repository.TemplateRepository
import com.sinxn.mymoney.core.data.repository.TransactionRepository
import com.sinxn.mymoney.core.util.AmountUtils.parseAmountToLong
import com.sinxn.mymoney.core.util.AmountUtils.toDecimalString
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.Direction
import com.sinxn.mymoney.core.util.MathExpressionEvaluator
import com.sinxn.mymoney.core.util.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Currency
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

data class TransferAddEditUiState(
    val isNewTransfer: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val editAmount: String = "",
    val editTargetAmount: String = "",
    val editTransferFee: String = "",
    val editWalletId: String = "",
    val targetWalletId: String? = null,
    val editDescription: String = "",
    val editNote: String = "",
    val editDate: String = "",
    val editPlaceId: String? = null,
    val editEventId: String? = null,
    val editPeopleIds: Set<String> = emptySet(),
    val editConfirmed: Boolean = true,
    val editCountInTotal: Boolean = true,
    val availableWallets: List<WalletEntity> = emptyList(),
    val availablePlaces: List<PlaceEntity> = emptyList(),
    val availableEvents: List<EventEntity> = emptyList(),
    val availablePeople: List<PersonEntity> = emptyList(),
    val currencyCode: String = "USD",
    val currencySymbol: String = "$",
    val currencyDecimals: Int = 2,
    val targetWalletCurrency: String = "USD",
    val targetWalletSymbol: String = "$",
    val targetWalletDecimals: Int = 2,
    val transferEntity: TransferEntity? = null
)

@HiltViewModel
class TransferAddEditViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val moneyDao: MoneyDao,
    private val settingsRepository: SettingsRepository,
    private val templateRepository: TemplateRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val transactionIdArg: String? = savedStateHandle.get<String>("transactionId")?.takeIf { it.isNotBlank() && it != "new" }
    private val transferIdArg: String? = savedStateHandle.get<String>("transferId")?.takeIf { it.isNotBlank() && it != "new" }
    private val templateIdArg: String? = savedStateHandle.get<String>("templateId")?.takeIf { it.isNotBlank() && it != "new" }
    val isNewTransfer = transactionIdArg == null && transferIdArg == null

    private val _isSaving = MutableStateFlow(false)
    private val _formState = MutableStateFlow(
        TransferFormState(
            date = if (isNewTransfer) DateUtils.getSQLDateTimeString(Date()) else "",
            walletId = savedStateHandle.get<String>("walletId") ?: ""
        )
    )

    init {
        if (!isNewTransfer) {
            initExistingTransfer()
        } else if (!templateIdArg.isNullOrBlank()) {
            initFromTemplate(templateIdArg)
        }
    }

    private fun initFromTemplate(templateId: String) {
        viewModelScope.launch {
            val template = templateRepository.getTransferModelById(templateId)
            if (template != null) {
                val wallets = moneyDao.getWalletsList()
                val fromWallet = wallets.find { it.id == template.walletFromId }
                val toWallet = wallets.find { it.id == template.walletToId }
                val fromCurr = fromWallet?.let { moneyDao.getCurrencyByIso(it.currency) }
                val toCurr = toWallet?.let { moneyDao.getCurrencyByIso(it.currency) }
                val fromDecimals = fromCurr?.decimals ?: 2
                val toDecimals = toCurr?.decimals ?: 2

                _formState.update { form ->
                    form.copy(
                        amount = template.moneyFrom.toDecimalString(fromDecimals),
                        targetAmount = template.moneyTo.toDecimalString(toDecimals),
                        transferFee = template.moneyTax?.toDecimalString(fromDecimals) ?: "",
                        walletId = template.walletFromId,
                        targetWalletId = template.walletToId,
                        description = template.description ?: "",
                        note = template.note ?: "",
                        placeId = template.placeId,
                        eventId = template.eventId,
                        confirmed = template.confirmed,
                        countInTotal = template.countInTotal
                    )
                }
            }
        }
    }

    private fun initExistingTransfer() {
        viewModelScope.launch {
            val transfer = when {
                transferIdArg != null -> transactionRepository.getTransferById(transferIdArg)
                transactionIdArg != null -> transactionRepository.getTransferByTransactionId(transactionIdArg)
                else -> null
            }

            if (transfer != null) {
                val fromTx = transactionRepository.getTransactionById(transfer.transactionFromId)
                val toTx = transactionRepository.getTransactionById(transfer.transactionToId)
                val taxTx = transfer.transactionTaxId?.let { transactionRepository.getTransactionById(it) }
                val people = transactionRepository.getPeopleForTransaction(transfer.transactionFromId).firstOrNull() ?: emptyList()

                val wallets = moneyDao.getWalletsList()
                val fromWallet = wallets.find { it.id == fromTx?.walletId }
                val toWallet = wallets.find { it.id == toTx?.walletId }
                val fromCurr = fromWallet?.let { moneyDao.getCurrencyByIso(it.currency) }
                val toCurr = toWallet?.let { moneyDao.getCurrencyByIso(it.currency) }
                val fromDecimals = fromCurr?.decimals ?: 2
                val toDecimals = toCurr?.decimals ?: 2

                _formState.update { form ->
                    form.copy(
                        amount = fromTx?.money?.toDecimalString(fromDecimals) ?: "",
                        targetAmount = toTx?.money?.toDecimalString(toDecimals) ?: "",
                        transferFee = taxTx?.money?.toDecimalString(fromDecimals) ?: "",
                        walletId = fromTx?.walletId ?: form.walletId,
                        targetWalletId = toTx?.walletId,
                        description = transfer.description ?: fromTx?.description ?: "",
                        note = transfer.note ?: fromTx?.note ?: "",
                        date = transfer.date,
                        placeId = transfer.placeId ?: fromTx?.placeId,
                        eventId = transfer.eventId ?: fromTx?.eventId,
                        peopleIds = people.map { it.id }.toSet(),
                        confirmed = transfer.confirmed,
                        countInTotal = transfer.countInTotal,
                        transferEntity = transfer
                    )
                }
            } else if (transactionIdArg != null) {
                val tx = transactionRepository.getTransactionById(transactionIdArg)
                if (tx != null) {
                    val sibling = transactionRepository.findSiblingTransferTransaction(tx.money, tx.date, tx.id)
                    val fromTx = if (tx.direction == Direction.EXPENSE) tx else sibling
                    val toTx = if (tx.direction == Direction.INCOME) tx else sibling

                    val wallets = moneyDao.getWalletsList()
                    val fromWallet = wallets.find { it.id == fromTx?.walletId }
                    val toWallet = wallets.find { it.id == toTx?.walletId }
                    val fromCurr = fromWallet?.let { moneyDao.getCurrencyByIso(it.currency) }
                    val toCurr = toWallet?.let { moneyDao.getCurrencyByIso(it.currency) }
                    val fromDecimals = fromCurr?.decimals ?: 2
                    val toDecimals = toCurr?.decimals ?: 2

                    _formState.update { form ->
                        form.copy(
                            amount = (fromTx?.money ?: tx.money).toDecimalString(fromDecimals),
                            targetAmount = (toTx?.money ?: tx.money).toDecimalString(toDecimals),
                            walletId = fromTx?.walletId ?: tx.walletId,
                            targetWalletId = toTx?.walletId,
                            description = tx.description ?: "",
                            note = tx.note ?: "",
                            date = tx.date,
                            placeId = tx.placeId,
                            eventId = tx.eventId,
                            confirmed = tx.confirmed,
                            countInTotal = tx.countInTotal
                        )
                    }
                }
            }
        }
    }

    private val listsFlow = combine(
        moneyDao.getWalletsWithBalance(DateUtils.getSQLDateTimeString(Date())),
        moneyDao.getPlaces(),
        moneyDao.getEvents(),
        moneyDao.getPeople()
    ) { wallets, places, events, people ->
        ListsWrapper(wallets, places, events, people)
    }

    val formattingSettings: StateFlow<FormattingSettings> = settingsRepository.formattingSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FormattingSettings())

    val uiState: StateFlow<TransferAddEditUiState> = combine(
        _formState,
        _isSaving,
        listsFlow
    ) { form, isSaving, lists ->
        val rawWallets = lists.wallets.map { it.wallet }
        val effectiveWalletId = form.walletId.ifEmpty { rawWallets.firstOrNull()?.id ?: "" }
        val fromWalletWithBalance = lists.wallets.find { it.wallet.id == effectiveWalletId }
        val fromWallet = fromWalletWithBalance?.wallet

        var targetWalletId = form.targetWalletId

        val toWalletWithBalance = lists.wallets.find { it.wallet.id == targetWalletId }
        val toWallet = toWalletWithBalance?.wallet

        val fromCurrency = fromWallet?.currency ?: "USD"
        val fromDecimals = fromWalletWithBalance?.decimals ?: 2
        val fromSymbol = try {
            Currency.getInstance(fromCurrency).getSymbol(Locale.getDefault())
        } catch (_: Exception) {
            fromCurrency
        }

        val toCurrency = toWallet?.currency ?: fromCurrency
        val toDecimals = toWalletWithBalance?.decimals ?: fromDecimals
        val toSymbol = try {
            Currency.getInstance(toCurrency).getSymbol(Locale.getDefault())
        } catch (_: Exception) {
            toCurrency
        }

        TransferAddEditUiState(
            isNewTransfer = isNewTransfer,
            isLoading = false,
            isSaving = isSaving,
            editAmount = form.amount,
            editTargetAmount = form.targetAmount,
            editTransferFee = form.transferFee,
            editWalletId = effectiveWalletId,
            targetWalletId = targetWalletId,
            editDescription = form.description,
            editNote = form.note,
            editDate = form.date,
            editPlaceId = form.placeId,
            editEventId = form.eventId,
            editPeopleIds = form.peopleIds,
            editConfirmed = form.confirmed,
            editCountInTotal = form.countInTotal,
            availableWallets = rawWallets,
            availablePlaces = lists.places,
            availableEvents = lists.events,
            availablePeople = lists.people,
            currencyCode = fromCurrency,
            currencySymbol = fromSymbol,
            currencyDecimals = fromDecimals,
            targetWalletCurrency = toCurrency,
            targetWalletSymbol = toSymbol,
            targetWalletDecimals = toDecimals,
            transferEntity = form.transferEntity
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransferAddEditUiState()
    )

    fun onAmountChange(amount: String) {
        _formState.update { it.copy(amount = amount) }
    }

    fun onNumpadKeyPress(key: String) {
        _formState.update { it.copy(amount = MathExpressionEvaluator.processNumpadKeyPress(it.amount, key)) }
    }

    fun evaluateMathExpression() {
        val decimals = uiState.value.currencyDecimals
        _formState.update { it.copy(amount = MathExpressionEvaluator.evaluateMathExpression(it.amount, decimals)) }
    }

    fun onTargetAmountChange(targetAmount: String) {
        _formState.update { it.copy(targetAmount = targetAmount) }
    }

    fun onTransferFeeChange(fee: String) {
        _formState.update { it.copy(transferFee = fee) }
    }

    fun onFromWalletSelect(walletId: String) {
        _formState.update { current ->
            if (current.targetWalletId == walletId) {
                current.copy(walletId = walletId, targetWalletId = current.walletId)
            } else {
                current.copy(walletId = walletId)
            }
        }
    }

    fun onToWalletSelect(targetWalletId: String) {
        _formState.update { current ->
            if (current.walletId == targetWalletId) {
                current.copy(walletId = current.targetWalletId ?: "", targetWalletId = targetWalletId)
            } else {
                current.copy(targetWalletId = targetWalletId)
            }
        }
    }

    fun swapWallets() {
        _formState.update { current ->
            val from = current.walletId
            val to = current.targetWalletId
            val currentAmount = current.amount
            val currentTargetAmount = current.targetAmount
            current.copy(
                walletId = to ?: from,
                targetWalletId = from,
                amount = if (currentTargetAmount.isNotEmpty()) currentTargetAmount else currentAmount,
                targetAmount = if (currentTargetAmount.isNotEmpty()) currentAmount else ""
            )
        }
    }

    fun onDescriptionChange(desc: String) {
        _formState.update { it.copy(description = desc) }
    }

    fun onNoteChange(note: String) {
        _formState.update { it.copy(note = note) }
    }

    fun onDateChange(date: String) {
        _formState.update { it.copy(date = date) }
    }

    fun onPlaceSelect(placeId: String?) {
        _formState.update { it.copy(placeId = placeId) }
    }

    fun onEventSelect(eventId: String?) {
        _formState.update { it.copy(eventId = eventId) }
    }

    fun onPeopleChange(peopleIds: Set<String>) {
        _formState.update { it.copy(peopleIds = peopleIds) }
    }

    fun onConfirmedChange(confirmed: Boolean) {
        _formState.update { it.copy(confirmed = confirmed) }
    }

    fun onCountInTotalChange(countInTotal: Boolean) {
        _formState.update { it.copy(countInTotal = countInTotal) }
    }

    fun getImmediateResult(editAmount: String): String {
        return MathExpressionEvaluator.getImmediateResult(editAmount, uiState.value.currencyDecimals)
    }

    fun saveTransfer(onSuccess: () -> Unit) {
        if (_isSaving.value) return
        _isSaving.value = true

        viewModelScope.launch {
            try {
                val state = uiState.value
                val form = _formState.value
                val fromDecimals = state.currencyDecimals
                val toDecimals = state.targetWalletDecimals

                val evaluatedAmount = getImmediateResult(form.amount)
                val fromMoneyValue = parseAmountToLong(evaluatedAmount, fromDecimals)

                val evaluatedTargetAmount = if (form.targetAmount.isNotBlank()) {
                    getImmediateResult(form.targetAmount)
                } else evaluatedAmount
                val toMoneyValue = parseAmountToLong(evaluatedTargetAmount, toDecimals)

                val feeValue = if (form.transferFee.isNotBlank()) {
                    parseAmountToLong(getImmediateResult(form.transferFee), fromDecimals)
                } else 0L

                val walletFromId = form.walletId.ifEmpty { state.editWalletId }
                val walletToId = form.targetWalletId ?: state.targetWalletId ?: walletFromId

                val categories = moneyDao.getCategories().firstOrNull() ?: emptyList()
                val transferCategory = categories.find { it.tag == "system::transfer" || it.tag == "transfer" }?.id

                val existingTransfer = form.transferEntity
                val transferId = existingTransfer?.id ?: UUID.randomUUID().toString()
                val fromTxId = existingTransfer?.transactionFromId ?: UUID.randomUUID().toString()
                val toTxId = existingTransfer?.transactionToId ?: UUID.randomUUID().toString()

                val fromTx = TransactionEntity(
                    id = fromTxId,
                    money = fromMoneyValue,
                    date = form.date,
                    categoryId = transferCategory,
                    walletId = walletFromId,
                    note = form.note.takeIf { it.isNotEmpty() },
                    description = form.description.takeIf { it.isNotEmpty() },
                    placeId = form.placeId,
                    eventId = form.eventId,
                    direction = Direction.EXPENSE,
                    confirmed = form.confirmed,
                    countInTotal = form.countInTotal,
                    type = TransactionType.TRANSFER,
                    isDeleted = false,
                    debtId = null,
                    savingId = null,
                    recurrenceId = null,
                    tag = null,
                    lastEdit = System.currentTimeMillis()
                )

                val toTx = TransactionEntity(
                    id = toTxId,
                    money = toMoneyValue,
                    date = form.date,
                    categoryId = transferCategory,
                    walletId = walletToId,
                    note = form.note.takeIf { it.isNotEmpty() },
                    description = form.description.takeIf { it.isNotEmpty() },
                    placeId = form.placeId,
                    eventId = form.eventId,
                    direction = Direction.INCOME,
                    confirmed = form.confirmed,
                    countInTotal = form.countInTotal,
                    type = TransactionType.TRANSFER,
                    isDeleted = false,
                    debtId = null,
                    savingId = null,
                    recurrenceId = null,
                    tag = null,
                    lastEdit = System.currentTimeMillis()
                )

                var existingTaxIdToRemove: String? = null
                val taxTx = if (feeValue > 0) {
                    val taxId = existingTransfer?.transactionTaxId ?: UUID.randomUUID().toString()
                    val taxCategory = categories.find {
                        it.tag == "system::transfer_tax" || it.tag == "transfer_tax" || it.name.contains("Tax", ignoreCase = true) || it.name.contains("Fee", ignoreCase = true)
                    }?.id ?: transferCategory

                    TransactionEntity(
                        id = taxId,
                        money = feeValue,
                        date = form.date,
                        categoryId = taxCategory,
                        walletId = walletFromId,
                        note = "Transfer fee",
                        description = form.description.ifEmpty { "Transfer Fee" },
                        placeId = form.placeId,
                        eventId = form.eventId,
                        direction = Direction.EXPENSE,
                        confirmed = form.confirmed,
                        countInTotal = form.countInTotal,
                        type = TransactionType.TRANSFER,
                        isDeleted = false,
                        debtId = null,
                        savingId = null,
                        recurrenceId = null,
                        tag = null,
                        lastEdit = System.currentTimeMillis()
                    )
                } else {
                    existingTaxIdToRemove = existingTransfer?.transactionTaxId
                    null
                }

                val newTransfer = TransferEntity(
                    id = transferId,
                    description = form.description.takeIf { it.isNotEmpty() },
                    date = form.date,
                    transactionFromId = fromTxId,
                    transactionToId = toTxId,
                    transactionTaxId = taxTx?.id ?: existingTransfer?.transactionTaxId,
                    note = form.note.takeIf { it.isNotEmpty() },
                    placeId = form.placeId,
                    eventId = form.eventId,
                    recurrenceId = null,
                    confirmed = form.confirmed,
                    countInTotal = form.countInTotal,
                    isDeleted = false,
                    lastEdit = System.currentTimeMillis(),
                    tag = null
                )

                transactionRepository.saveTransferTransaction(
                    fromTx = fromTx,
                    toTx = toTx,
                    transfer = newTransfer,
                    taxTx = taxTx,
                    existingTaxIdToRemove = existingTaxIdToRemove,
                    isNewTransfer = isNewTransfer && existingTransfer == null,
                    peopleIds = form.peopleIds
                )

                onSuccess()
            } finally {
                _isSaving.value = false
            }
        }
    }

    private data class ListsWrapper(
        val wallets: List<WalletWithBalance>,
        val places: List<PlaceEntity>,
        val events: List<EventEntity>,
        val people: List<PersonEntity>
    )
}
