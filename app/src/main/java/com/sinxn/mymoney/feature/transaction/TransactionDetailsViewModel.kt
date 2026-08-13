package com.sinxn.mymoney.feature.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.entity.AttachmentEntity
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.data.local.entity.EventEntity
import com.sinxn.mymoney.core.data.local.entity.PersonEntity
import com.sinxn.mymoney.core.data.local.entity.PlaceEntity
import com.sinxn.mymoney.core.data.local.entity.TransactionEntity
import com.sinxn.mymoney.core.data.local.entity.TransferEntity
import com.sinxn.mymoney.core.data.local.entity.WalletEntity
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.repository.DebtRepository
import com.sinxn.mymoney.core.data.repository.SavingRepository
import com.sinxn.mymoney.core.data.repository.TransactionRepository
import com.sinxn.mymoney.core.util.AmountUtils.toDecimalString
import com.sinxn.mymoney.core.util.AmountUtils.parseAmountToLong
import com.sinxn.mymoney.core.util.CategoryType
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Currency
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

data class TransactionDetailsUiState(
    val transaction: TransactionWithCategory? = null,
    val isEditMode: Boolean = false,
    val isNewTransaction: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    // Fields for viewing (enriched)
    val place: PlaceEntity? = null,
    val event: EventEntity? = null,
    val people: List<PersonEntity> = emptyList(),
    val attachments: List<AttachmentEntity> = emptyList(),
    // Fields for editing
    val editAmount: String = "",
    val editNote: String = "",
    val editDescription: String = "",
    val editDate: String = "",
    val editCategoryId: String? = null,
    val editWalletId: String = "",
    val editPlaceId: String? = null,
    val editEventId: String? = null,
    val editDirection: Int = 0,
    val editPeopleIds: Set<String> = emptySet(),
    val editConfirmed: Boolean = true,
    val editCountInTotal: Boolean = true,
    // Available items for selectors
    val availableWallets: List<WalletEntity> = emptyList(),
    val availableCategories: List<CategoryEntity> = emptyList(),
    val availableIncomeCategories: List<CategoryEntity> = emptyList(),
    val availableExpenseCategories: List<CategoryEntity> = emptyList(),
    val availablePlaces: List<PlaceEntity> = emptyList(),
    val availableEvents: List<EventEntity> = emptyList(),
    val availablePeople: List<PersonEntity> = emptyList(),
    // Currency info for formatting
    val currencyCode: String = "USD",
    val currencySymbol: String = "$",
    val currencyDecimals: Int = 2,
    // Redesign & Transfer Fields
    val walletName: String = "",
    val categoryColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Gray,
    val isTransfer: Boolean = false,
    val targetWalletId: String? = null,
    val targetWalletName: String = "",
    val editTargetAmount: String = "",
    val editTransferFee: String = "",
    val targetWalletCurrency: String = "",
    val targetWalletDecimals: Int = 2,
    val transferEntity: TransferEntity? = null
)

@HiltViewModel
class TransactionDetailsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val moneyDao: MoneyDao,
    private val settingsRepository: SettingsRepository,
    private val savingRepository: SavingRepository,
    private val debtRepository: DebtRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val transactionId: String = checkNotNull(savedStateHandle["transactionId"])
    private val isNewTransaction = transactionId == "new"

    private val _isEditMode = MutableStateFlow(isNewTransaction)
    private val _isSaving = MutableStateFlow(false)

    // Single unified state flow for edit form fields
    private val _formState = MutableStateFlow(
        TransactionFormState(
            date = if (isNewTransaction) DateUtils.getSQLDateTimeString(Date()) else "",
            savingId = savedStateHandle.get<String>("savingId"),
            debtId = savedStateHandle.get<String>("debtId")
        )
    )

    init {
        val savingIdArg: String? = savedStateHandle.get<String>("savingId")
        val savingActionArg: String? = savedStateHandle.get<String>("action")
        val debtIdArg: String? = savedStateHandle.get<String>("debtId")
        val debtActionArg: String? = savedStateHandle.get<String>("debtAction")

        if (isNewTransaction) {
            initNewTransactionDefaults(savingIdArg, savingActionArg, debtIdArg, debtActionArg)
        } else {
            initExistingTransaction()
        }
    }

    private fun initNewTransactionDefaults(
        savingIdArg: String?,
        savingActionArg: String?,
        debtIdArg: String?,
        debtActionArg: String?
    ) {
        if (!savingIdArg.isNullOrBlank()) {
            viewModelScope.launch {
                savingRepository.getSavingDetails(savingIdArg).firstOrNull()?.let { savingDetails ->
                    val isDeposit = savingActionArg == "deposit"
                    val tag = if (isDeposit) SavingRepository.TAG_SAVING_DEPOSIT else SavingRepository.TAG_SAVING_WITHDRAW
                    val cat = savingRepository.getOrCreateSystemCategory(tag)
                    val isWithdrawEverything = savingActionArg == "withdraw_everything"
                    val amountStr = if (isWithdrawEverything) {
                        val targetOrCurrent = if (savingDetails.neededMoney == 0L) savingDetails.saving.endMoney else savingDetails.currentMoney
                        (targetOrCurrent / 100.0).toString()
                    } else ""

                    _formState.update { current ->
                        current.copy(
                            savingId = savingIdArg,
                            walletId = savingDetails.saving.walletId,
                            description = savingDetails.saving.description ?: "Saving",
                            categoryId = cat.id,
                            direction = if (isDeposit) Direction.EXPENSE else Direction.INCOME,
                            amount = amountStr.ifEmpty { current.amount },
                            savingCompletedOnSave = isWithdrawEverything
                        )
                    }
                }
            }
        } else if (!debtIdArg.isNullOrBlank()) {
            viewModelScope.launch {
                debtRepository.getDebtDetails(debtIdArg).firstOrNull()?.let { debtDetails ->
                    val debt = debtDetails.debt
                    val isPay = debtActionArg.equals("PAY", ignoreCase = true) || (debtActionArg == null && debt.type == 0)
                    val catTag = if (isPay) DebtRepository.TAG_PAID_DEBT else DebtRepository.TAG_PAID_CREDIT
                    val systemCat = debtRepository.getOrCreateSystemCategory(catTag)

                    _formState.update { current ->
                        current.copy(
                            debtId = debtIdArg,
                            walletId = current.walletId.ifEmpty { debt.walletId },
                            direction = if (isPay) Direction.EXPENSE else Direction.INCOME,
                            categoryId = systemCat.id,
                            description = current.description.ifEmpty { debt.description ?: "" },
                            peopleIds = if (debtDetails.people.isNotEmpty()) debtDetails.people.map { it.id }.toSet() else current.peopleIds
                        )
                    }
                }
            }
        }
    }

    private fun initExistingTransaction() {
        viewModelScope.launch {
            val tx = transactionRepository.getTransactionById(transactionId)
            val transfer = transactionRepository.getTransferByTransactionId(transactionId)

            if (tx != null && tx.debtId != null) {
                _formState.update { it.copy(debtId = tx.debtId) }
            }

            if (transfer != null) {
                val fromTx = transactionRepository.getTransactionById(transfer.transactionFromId)
                val toTx = transactionRepository.getTransactionById(transfer.transactionToId)
                _formState.update { current ->
                    current.copy(
                        isTransfer = true,
                        transferEntity = transfer,
                        walletId = fromTx?.walletId ?: current.walletId,
                        targetWalletId = toTx?.walletId ?: current.targetWalletId
                    )
                }
            } else if (tx != null && tx.debtId == null && (tx.type == 1 || tx.type == 2 || tx.direction == 2)) {
                val siblingTx = transactionRepository.findSiblingTransferTransaction(tx.money, tx.date, tx.id)
                if (siblingTx != null) {
                    val fromTx = if (tx.direction == Direction.EXPENSE) tx else siblingTx
                    val toTx = if (tx.direction == Direction.INCOME) tx else siblingTx

                    val autoTransfer = TransferEntity(
                        id = UUID.randomUUID().toString(),
                        description = tx.description,
                        date = tx.date,
                        transactionFromId = fromTx.id,
                        transactionToId = toTx.id,
                        transactionTaxId = null,
                        note = tx.note,
                        placeId = tx.placeId,
                        eventId = tx.eventId,
                        recurrenceId = null,
                        confirmed = tx.confirmed,
                        countInTotal = tx.countInTotal,
                        isDeleted = false,
                        lastEdit = System.currentTimeMillis(),
                        tag = null
                    )
                    moneyDao.insertTransfer(autoTransfer)
                    _formState.update { current ->
                        current.copy(
                            isTransfer = true,
                            direction = Direction.TRANSFER,
                            walletId = fromTx.walletId,
                            targetWalletId = toTx.walletId,
                            transferEntity = autoTransfer
                        )
                    }
                }
            }
        }
    }

    private val transactionDataFlow = if (isNewTransaction) {
        flowOf(TransactionData(null, emptyList(), emptyList()))
    } else {
        combine(
            transactionRepository.getTransactionWithCategory(transactionId),
            transactionRepository.getPeopleForTransaction(transactionId),
            transactionRepository.getAttachmentsForTransaction(transactionId)
        ) { transaction, people, attachments ->
            TransactionData(transaction, people, attachments)
        }
    }

    private val listsFlow = combine(
        moneyDao.getWalletsWithBalance(DateUtils.getSQLDateTimeString(Date())),
        moneyDao.getCategories(),
        moneyDao.getPlaces(),
        moneyDao.getEvents(),
        moneyDao.getPeople()
    ) { w, c, p, e, pp -> ListsWrapper(w, TransactionDetailsUiMapper.flattenCategories(c), p, e, pp) }

    private data class FormAndEditState(
        val isEditMode: Boolean,
        val isSaving: Boolean,
        val form: TransactionFormState
    )

    private val formAndEditState = combine(
        _isEditMode,
        _isSaving,
        _formState
    ) { isEdit, isSaving, form ->
        FormAndEditState(isEdit, isSaving, form)
    }

    val uiState: StateFlow<TransactionDetailsUiState> = combine(
        transactionDataFlow,
        formAndEditState,
        settingsRepository.currentWalletId,
        listsFlow
    ) { data, state, currentWalletId, lists ->
        val isEditMode = state.isEditMode
        val isSaving = state.isSaving
        val form = state.form
        val transaction = data.transaction
        val people = data.people
        val attachments = data.attachments
        val enriched = enrichTransaction(transaction)

        // For new transactions, set default wallet if unselected
        if (isNewTransaction && form.walletId.isEmpty() && lists.wallets.isNotEmpty()) {
            val preferredWallet = lists.wallets.find { it.wallet.id == currentWalletId }
                ?: lists.wallets.firstOrNull { !it.wallet.isArchived }
                ?: lists.wallets.firstOrNull()

            if (preferredWallet != null) {
                _formState.update { it.copy(walletId = preferredWallet.wallet.id) }
            }
        }

        val activeWalletId = if (form.walletId.isNotEmpty()) form.walletId else transaction?.transaction?.walletId
        val activeWallet = lists.wallets.find { it.wallet.id == activeWalletId }
        val targetWallet = lists.wallets.find { it.wallet.id == form.targetWalletId }

        val activeWalletIds = setOfNotNull(
            transaction?.transaction?.walletId,
            form.walletId.takeIf { it.isNotEmpty() },
            form.targetWalletId
        )

        val selectedCatId = form.categoryId ?: transaction?.transaction?.categoryId
        val selectedCat = lists.categories.find { it.id == selectedCatId }
        val selectedParentId = selectedCat?.parentId
        val baseActiveCatIds = setOfNotNull(selectedCatId, selectedParentId)
        val activeCatIds = baseActiveCatIds + lists.categories.filter { !it.isArchived || it.id in baseActiveCatIds }.mapNotNull { it.parentId }

        val selectedPlaceId = form.placeId ?: transaction?.transaction?.placeId
        val selectedEventId = form.eventId ?: transaction?.transaction?.eventId
        val activePeopleIds = form.peopleIds + people.map { it.id }.toSet()

        val filteredWallets = lists.wallets.filter { !it.wallet.isArchived || it.wallet.id in activeWalletIds }.map { it.wallet }
        val filteredCategories = lists.categories.filter { !it.isArchived || it.id in activeCatIds }
        val filteredPlaces = lists.places.filter { !it.isArchived || it.id == selectedPlaceId }
        val filteredEvents = lists.events.filter { !it.isArchived || it.id == selectedEventId }
        val filteredPeople = lists.people.filter { !it.isArchived || it.id in activePeopleIds }

        TransactionDetailsUiState(
            transaction = transaction,
            isEditMode = isEditMode,
            isNewTransaction = isNewTransaction,
            isLoading = !isNewTransaction && transaction == null,
            isSaving = isSaving,
            place = enriched.place,
            event = enriched.event,
            people = people,
            attachments = attachments,
            availableWallets = filteredWallets,
            availableCategories = filteredCategories,
            availableIncomeCategories = filteredCategories.filter { it.type == CategoryType.INCOME },
            availableExpenseCategories = filteredCategories.filter { it.type == CategoryType.EXPENSE },
            availablePlaces = filteredPlaces,
            availableEvents = filteredEvents,
            availablePeople = filteredPeople,
            currencyCode = activeWallet?.wallet?.currency ?: "USD",
            currencySymbol = try {
                Currency.getInstance(activeWallet?.wallet?.currency ?: "USD").getSymbol(Locale.getDefault())
            } catch (e: Exception) {
                activeWallet?.currencySymbol ?: activeWallet?.wallet?.currency ?: "$"
            },
            currencyDecimals = activeWallet?.decimals ?: 2,
            walletName = activeWallet?.wallet?.name ?: "",
            categoryColor = transaction?.categoryName?.let { TransactionDetailsUiMapper.generateCategoryColor(it) } ?: androidx.compose.ui.graphics.Color.Gray,
            editAmount = form.amount,
            editNote = form.note,
            editDescription = form.description,
            editDate = form.date,
            editCategoryId = form.categoryId,
            editWalletId = form.walletId.ifEmpty { activeWalletId ?: "" },
            editPlaceId = form.placeId,
            editEventId = form.eventId,
            editDirection = form.direction,
            editPeopleIds = form.peopleIds,
            editConfirmed = form.confirmed,
            editCountInTotal = form.countInTotal,
            isTransfer = form.isTransfer,
            targetWalletId = form.targetWalletId,
            targetWalletName = targetWallet?.wallet?.name ?: "",
            editTargetAmount = form.targetAmount,
            editTransferFee = form.transferFee,
            targetWalletCurrency = targetWallet?.wallet?.currency ?: "",
            targetWalletDecimals = targetWallet?.decimals ?: 2,
            transferEntity = form.transferEntity
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionDetailsUiState()
    )

    private suspend fun enrichTransaction(transaction: TransactionWithCategory?): EnrichedData {
        if (transaction == null) return EnrichedData()
        val place = transaction.transaction.placeId?.let { moneyDao.getPlaceById(it) }
        val event = transaction.transaction.eventId?.let { moneyDao.getEventById(it) }
        return EnrichedData(place, event)
    }

    fun toggleEditMode() {
        val current = uiState.value.transaction
        val currentPeople = uiState.value.people
        val decimals = uiState.value.currencyDecimals

        if (!_isEditMode.value && current != null) {
            val transfer = _formState.value.transferEntity
            val isTransferMode = transfer != null || current.transaction.type == 1 || current.transaction.type == 2

            _formState.update { form ->
                form.copy(
                    amount = current.transaction.money.toDecimalString(decimals),
                    note = current.transaction.note ?: "",
                    description = current.transaction.description ?: "",
                    date = current.transaction.date,
                    categoryId = current.transaction.categoryId,
                    placeId = current.transaction.placeId,
                    eventId = current.transaction.eventId,
                    peopleIds = currentPeople.map { it.id }.toSet(),
                    confirmed = current.transaction.confirmed,
                    countInTotal = current.transaction.countInTotal,
                    isTransfer = isTransferMode,
                    direction = if (isTransferMode) Direction.TRANSFER else current.transaction.direction,
                    walletId = if (isTransferMode) form.walletId else current.transaction.walletId
                )
            }
        }
        _isEditMode.value = !_isEditMode.value
    }

    fun onAmountChange(value: String) {
        _formState.update { it.copy(amount = value) }
    }

    fun onNumpadKeyPress(key: String) {
        _formState.update { it.copy(amount = MathExpressionEvaluator.processNumpadKeyPress(it.amount, key)) }
    }

    fun evaluateMathExpression() {
        val decimals = uiState.value.currencyDecimals
        _formState.update { it.copy(amount = MathExpressionEvaluator.evaluateMathExpression(it.amount, decimals)) }
    }

    fun setQuickDate(type: String) {
        val cal = Calendar.getInstance()
        when (type) {
            "TODAY" -> {
                val now = Calendar.getInstance()
                cal.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH))
            }
            "YESTERDAY" -> {
                val now = Calendar.getInstance()
                now.add(Calendar.DAY_OF_YEAR, -1)
                cal.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH))
            }
        }
        _formState.update { it.copy(date = DateUtils.getSQLDateTimeString(cal.time)) }
    }

    fun deleteTransaction(onComplete: () -> Unit) {
        viewModelScope.launch {
            if (!isNewTransaction) {
                val current = uiState.value.transaction?.transaction ?: return@launch
                val transfer = _formState.value.transferEntity ?: transactionRepository.getTransferByTransactionId(transactionId)
                transactionRepository.deleteTransaction(current, transfer)
            }
            onComplete()
        }
    }

    fun onTransferToggle(isTransfer: Boolean) {
        _formState.update { current ->
            val targetId = if (isTransfer && current.targetWalletId.isNullOrEmpty()) {
                uiState.value.availableWallets.firstOrNull { it.id != current.walletId }?.id
            } else current.targetWalletId

            current.copy(
                isTransfer = isTransfer,
                direction = if (isTransfer) Direction.TRANSFER else Direction.EXPENSE,
                targetWalletId = targetId
            )
        }
    }

    fun onTargetWalletIdChange(walletId: String) {
        _formState.update { it.copy(targetWalletId = walletId) }
    }

    fun swapTransferWallets() {
        _formState.update { current ->
            val from = current.walletId
            val to = current.targetWalletId
            if (!to.isNullOrEmpty()) {
                current.copy(walletId = to, targetWalletId = from)
            } else current
        }
    }

    fun onTargetAmountChange(value: String) { _formState.update { it.copy(targetAmount = value) } }
    fun onTransferFeeChange(value: String) { _formState.update { it.copy(transferFee = value) } }
    fun onNoteChange(value: String) { _formState.update { it.copy(note = value) } }
    fun onDescriptionChange(value: String) { _formState.update { it.copy(description = value) } }
    fun onConfirmedChange(value: Boolean) { _formState.update { it.copy(confirmed = value) } }
    fun onCountInTotalChange(value: Boolean) { _formState.update { it.copy(countInTotal = value) } }

    fun onCategoryIdChange(value: String?) {
        _formState.update { current ->
            val newDir = value?.let { id ->
                uiState.value.availableCategories.find { it.id == id }?.let { cat ->
                    if (cat.type == CategoryType.INCOME) Direction.INCOME else Direction.EXPENSE
                }
            } ?: current.direction
            current.copy(categoryId = value, direction = newDir)
        }
    }

    fun onWalletIdChange(value: String) {
        _formState.update { current ->
            val newTarget = if (current.targetWalletId == value) {
                uiState.value.availableWallets.firstOrNull { it.id != value }?.id
            } else current.targetWalletId
            current.copy(walletId = value, targetWalletId = newTarget)
        }
    }

    fun onPlaceIdChange(value: String?) { _formState.update { it.copy(placeId = value) } }
    fun onEventIdChange(value: String?) { _formState.update { it.copy(eventId = value) } }
    fun onDirectionChange(value: Int) { _formState.update { it.copy(direction = value) } }

    fun onDateChange(millis: Long) {
        val currentDate = DateUtils.parseDate(_formState.value.date)
        val newDate = Date(millis)
        val calendar = Calendar.getInstance().apply {
            time = currentDate
            val currentHour = get(Calendar.HOUR_OF_DAY)
            val currentMinute = get(Calendar.MINUTE)
            time = newDate
            set(Calendar.HOUR_OF_DAY, currentHour)
            set(Calendar.MINUTE, currentMinute)
        }
        _formState.update { it.copy(date = DateUtils.getSQLDateTimeString(calendar.time)) }
    }

    fun onTimeChange(hour: Int, minute: Int) {
        val currentDate = DateUtils.parseDate(_formState.value.date)
        val calendar = Calendar.getInstance().apply {
            time = currentDate
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        _formState.update { it.copy(date = DateUtils.getSQLDateTimeString(calendar.time)) }
    }

    fun onPeopleToggle(personId: String) {
        _formState.update { current ->
            val set = current.peopleIds.toMutableSet()
            if (!set.add(personId)) set.remove(personId)
            current.copy(peopleIds = set)
        }
    }

    fun getImmediateResult(editAmount: String): String {
        return MathExpressionEvaluator.getImmediateResult(editAmount, uiState.value.currencyDecimals)
    }

    fun saveChanges() {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val decimals = uiState.value.currencyDecimals
                val form = _formState.value

                if (form.isTransfer) {
                    saveTransfer(form, decimals)
                } else {
                    saveSingleTransaction(form, decimals)
                }
                _isEditMode.value = false
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isSaving.value = false
            }
        }
    }

    private suspend fun saveTransfer(form: TransactionFormState, defaultDecimals: Int) {
        val walletFromId = form.walletId
        val walletToId = form.targetWalletId ?: form.walletId
        val walletFrom = moneyDao.getWalletById(walletFromId)
        val walletTo = moneyDao.getWalletById(walletToId)

        val fromDecimals = walletFrom?.currency?.let { moneyDao.getCurrencyByIso(it)?.decimals } ?: defaultDecimals
        val toDecimals = walletTo?.currency?.let { moneyDao.getCurrencyByIso(it)?.decimals } ?: defaultDecimals

        val fromMoneyValue = parseAmountToLong(getImmediateResult(form.amount), fromDecimals)
        val toMoneyValue = if (walletFrom?.currency != null && walletTo?.currency != null &&
            !walletFrom.currency.equals(walletTo.currency, ignoreCase = true) && form.targetAmount.isNotBlank()) {
            parseAmountToLong(getImmediateResult(form.targetAmount), toDecimals)
        } else fromMoneyValue

        val feeValue = if (form.transferFee.isNotBlank()) parseAmountToLong(getImmediateResult(form.transferFee), fromDecimals) else 0L

        val existingTransfer = form.transferEntity
        val transferId = existingTransfer?.id ?: UUID.randomUUID().toString()
        val fromTxId = existingTransfer?.transactionFromId ?: if (isNewTransaction) UUID.randomUUID().toString() else transactionId
        val toTxId = existingTransfer?.transactionToId ?: UUID.randomUUID().toString()

        val transferCategory = uiState.value.availableCategories.find {
            it.tag == "transfer" || it.name.equals("Transfer", ignoreCase = true)
        }?.id ?: form.categoryId

        val fromTx = buildTransactionEntity(
            id = fromTxId,
            money = fromMoneyValue,
            date = form.date,
            categoryId = transferCategory,
            walletId = walletFromId,
            note = form.note,
            description = form.description,
            placeId = form.placeId,
            eventId = form.eventId,
            direction = Direction.EXPENSE,
            confirmed = form.confirmed,
            countInTotal = form.countInTotal,
            type = TransactionType.TRANSFER
        )

        val toTx = buildTransactionEntity(
            id = toTxId,
            money = toMoneyValue,
            date = form.date,
            categoryId = transferCategory,
            walletId = walletToId,
            note = form.note,
            description = form.description,
            placeId = form.placeId,
            eventId = form.eventId,
            direction = Direction.INCOME,
            confirmed = form.confirmed,
            countInTotal = form.countInTotal,
            type = TransactionType.TRANSFER
        )

        var existingTaxIdToRemove: String? = null
        val taxTx = if (feeValue > 0) {
            val taxId = existingTransfer?.transactionTaxId ?: UUID.randomUUID().toString()
            val taxCategory = uiState.value.availableCategories.find {
                it.tag == "system::transfer_tax" || it.tag == "transfer_tax" || it.name.contains("Tax", ignoreCase = true) || it.name.contains("Fee", ignoreCase = true)
            }?.id ?: transferCategory

            buildTransactionEntity(
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
                type = TransactionType.TRANSFER
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
            isNewTransfer = isNewTransaction && existingTransfer == null,
            peopleIds = form.peopleIds
        )

        _formState.update { it.copy(transferEntity = newTransfer) }
    }

    private suspend fun saveSingleTransaction(form: TransactionFormState, decimals: Int) {
        val targetId = if (isNewTransaction) UUID.randomUUID().toString() else transactionId
        val moneyValue = parseAmountToLong(getImmediateResult(form.amount), decimals)

        val txEntity = buildTransactionEntity(
            id = targetId,
            money = moneyValue,
            date = form.date,
            categoryId = form.categoryId,
            walletId = form.walletId,
            note = form.note,
            description = form.description,
            placeId = form.placeId,
            eventId = form.eventId,
            direction = form.direction,
            confirmed = form.confirmed,
            countInTotal = form.countInTotal,
            type = if (form.debtId != null) 2 else 0,
            debtId = form.debtId ?: uiState.value.transaction?.transaction?.debtId,
            savingId = form.savingId
        )

        val previousTransfer = if (!isNewTransaction) form.transferEntity else null

        transactionRepository.saveSingleTransaction(
            transaction = txEntity,
            isNewTransaction = isNewTransaction,
            savingId = form.savingId,
            isSavingCompleted = form.savingCompletedOnSave,
            peopleIds = form.peopleIds,
            previousTransfer = previousTransfer
        )

        if (previousTransfer != null) {
            _formState.update { it.copy(transferEntity = null) }
        }
    }

    private fun buildTransactionEntity(
        id: String,
        money: Long,
        date: String,
        categoryId: String?,
        walletId: String,
        note: String?,
        description: String?,
        placeId: String?,
        eventId: String?,
        direction: Int,
        confirmed: Boolean,
        countInTotal: Boolean,
        type: Int,
        debtId: String? = null,
        savingId: String? = null
    ): TransactionEntity {
        return TransactionEntity(
            id = id,
            money = money,
            date = date,
            categoryId = categoryId,
            walletId = walletId,
            note = note?.takeIf { it.isNotEmpty() },
            description = description?.takeIf { it.isNotEmpty() },
            placeId = placeId,
            eventId = eventId,
            direction = direction,
            confirmed = confirmed,
            countInTotal = countInTotal,
            type = type,
            isDeleted = false,
            debtId = debtId,
            savingId = savingId,
            recurrenceId = null,
            tag = null,
            lastEdit = System.currentTimeMillis()
        )
    }

    val formattingSettings = settingsRepository.formattingSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FormattingSettings()
        )

    private data class EnrichedData(
        val place: PlaceEntity? = null,
        val event: EventEntity? = null
    )
    private data class TransactionData(
        val transaction: TransactionWithCategory?,
        val people: List<PersonEntity>,
        val attachments: List<AttachmentEntity>
    )
    private data class ListsWrapper(
        val wallets: List<com.sinxn.mymoney.core.data.local.model.WalletWithBalance>,
        val categories: List<CategoryEntity>,
        val places: List<PlaceEntity>,
        val events: List<EventEntity>,
        val people: List<PersonEntity>
    )
}
