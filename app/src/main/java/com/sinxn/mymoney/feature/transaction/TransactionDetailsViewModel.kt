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
import com.sinxn.mymoney.core.data.local.entity.TransactionPeopleEntity
import com.sinxn.mymoney.core.data.local.entity.WalletEntity
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.repository.DebtRepository
import com.sinxn.mymoney.core.data.repository.SavingRepository
import com.sinxn.mymoney.core.util.CategoryType
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.Direction
import com.sinxn.mymoney.core.util.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import kotlin.math.pow

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
    val transferEntity: com.sinxn.mymoney.core.data.local.entity.TransferEntity? = null
)

@HiltViewModel
class TransactionDetailsViewModel @Inject constructor(
    private val moneyDao: MoneyDao,
    private val settingsRepository: SettingsRepository,
    private val savingRepository: SavingRepository,
    private val debtRepository: DebtRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val transactionId: String = checkNotNull(savedStateHandle["transactionId"])
    private val isNewTransaction = transactionId == "new"

    private val _savingId = MutableStateFlow<String?>(savedStateHandle.get<String>("savingId"))
    private val _savingCompletedOnSave = MutableStateFlow(false)
    private val _debtId = MutableStateFlow<String?>(savedStateHandle.get<String>("debtId"))

    private val _isEditMode = MutableStateFlow(isNewTransaction)
    private val _isSaving = MutableStateFlow(false)
    
    // Internal state for edits
    private val _editAmount = MutableStateFlow("")
    private val _editNote = MutableStateFlow("")
    private val _editDescription = MutableStateFlow("")
    private val _editDate = MutableStateFlow(
        if (isNewTransaction) DateUtils.getSQLDateTimeString(java.util.Date()) else ""
    )
    private val _editCategoryId = MutableStateFlow<String?>(null)
    private val _editWalletId = MutableStateFlow("")
    private val _editPlaceId = MutableStateFlow<String?>(null)
    private val _editEventId = MutableStateFlow<String?>(null)
    private val _editDirection = MutableStateFlow(Direction.EXPENSE)
    private val _editPeopleIds = MutableStateFlow<Set<String>>(emptySet())
    private val _editConfirmed = MutableStateFlow(true)
    private val _editCountInTotal = MutableStateFlow(true)

    // Transfer State Flows
    private val _isTransfer = MutableStateFlow(false)
    private val _targetWalletId = MutableStateFlow<String?>(null)
    private val _editTargetAmount = MutableStateFlow("")
    private val _editTransferFee = MutableStateFlow("")
    private val _transferEntity = MutableStateFlow<com.sinxn.mymoney.core.data.local.entity.TransferEntity?>(null)

    init {
        val savingIdArg: String? = savedStateHandle.get<String>("savingId")
        val savingActionArg: String? = savedStateHandle.get<String>("action")
        val debtIdArg: String? = savedStateHandle.get<String>("debtId")
        val debtActionArg: String? = savedStateHandle.get<String>("debtAction")

        if (isNewTransaction) {

            if (!savingIdArg.isNullOrBlank()) {
                _savingId.value = savingIdArg
                viewModelScope.launch {
                    savingRepository.getSavingDetails(savingIdArg).firstOrNull()?.let { savingDetails ->
                        _editWalletId.value = savingDetails.saving.walletId
                        _editDescription.value = savingDetails.saving.description ?: "Saving"

                        val isDeposit = savingActionArg == "deposit"
                        val tag = if (isDeposit) SavingRepository.TAG_SAVING_DEPOSIT else SavingRepository.TAG_SAVING_WITHDRAW
                        val cat = savingRepository.getOrCreateSystemCategory(tag)
                        _editCategoryId.value = cat.id
                        _editDirection.value = if (isDeposit) Direction.EXPENSE else Direction.INCOME

                        if (savingActionArg == "withdraw_everything") {
                            val targetOrCurrent = if (savingDetails.neededMoney == 0L) savingDetails.saving.endMoney else savingDetails.currentMoney
                            _editAmount.value = (targetOrCurrent / 100.0).toString()
                            _savingCompletedOnSave.value = true
                        }
                    }
                }
            } else if (!debtIdArg.isNullOrBlank()) {
                _debtId.value = debtIdArg
                viewModelScope.launch {
                    debtRepository.getDebtDetails(debtIdArg).firstOrNull()?.let { debtDetails ->
                        val debt = debtDetails.debt
                        if (_editWalletId.value.isBlank()) {
                            _editWalletId.value = debt.walletId
                        }
                        
                        val isPay = debtActionArg.equals("PAY", ignoreCase = true) || (debtActionArg == null && debt.type == 0)

                        val direction = if (isPay) Direction.EXPENSE else Direction.INCOME // Expense for paying debt, Income for collecting credit
                        val catTag = if (isPay) DebtRepository.TAG_PAID_DEBT else DebtRepository.TAG_PAID_CREDIT
                        val systemCat = debtRepository.getOrCreateSystemCategory(catTag)

                        _editDirection.value = direction
                        _editCategoryId.value = systemCat.id

                        if (_editDescription.value.isBlank() && !debt.description.isNullOrBlank()) {
                            _editDescription.value = debt.description
                        }

                        if (debtDetails.people.isNotEmpty()) {
                            _editPeopleIds.value = debtDetails.people.map { it.id }.toSet()
                        }
                    }
                }
            }
        }

        if (!isNewTransaction) {
            viewModelScope.launch {
                val tx = moneyDao.getTransactionById(transactionId)
                val transfer = moneyDao.getTransferByTransactionId(transactionId)
                if (tx != null && tx.debtId != null) {
                    _debtId.value = tx.debtId
                }
                if (transfer != null) {
                    _isTransfer.value = true
                    _transferEntity.value = transfer
                    val fromTx = moneyDao.getTransactionById(transfer.transactionFromId)
                    val toTx = moneyDao.getTransactionById(transfer.transactionToId)
                    if (fromTx != null && toTx != null) {
                        _editWalletId.value = fromTx.walletId
                        _targetWalletId.value = toTx.walletId
                    }
                } else if (tx != null && tx.debtId == null && (tx.type == 1 || tx.type == 2 || tx.direction == 2)) {
                    _isTransfer.value = true
                    _editDirection.value = Direction.TRANSFER
                    val siblingTx = moneyDao.findSiblingTransferTransaction(tx.money, tx.date, tx.id)
                    if (siblingTx != null) {
                        val fromTx = if (tx.direction == Direction.EXPENSE) tx else siblingTx
                        val toTx = if (tx.direction == Direction.INCOME) tx else siblingTx
                        _editWalletId.value = fromTx.walletId
                        _targetWalletId.value = toTx.walletId

                        val autoTransfer = com.sinxn.mymoney.core.data.local.entity.TransferEntity(
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
                        _transferEntity.value = autoTransfer
                    }
                }
            }
        }
    }

    private val editPart1 = combine(
        _editAmount,
        _editNote,
        _editDescription,
        _editDate,
        _editCategoryId
    ) { amt, note, desc, date, catId ->
        EditPart1(amt, note, desc, date, catId)
    }

    private val editPart2 = combine(
        _editWalletId,
        _editPlaceId,
        _editEventId,
        _editDirection,
        _editPeopleIds
    ) { walletId, placeId, eventId, dir, people ->
        EditPart2(walletId, placeId, eventId, dir, people)
    }

    private val editPart3 = combine(
        _editConfirmed,
        _editCountInTotal
    ) { confirmed, countTotal ->
        EditPart3(confirmed, countTotal)
    }

    private val fullEditState = combine(
        _isEditMode,
        _isSaving,
        editPart1,
        editPart2,
        editPart3
    ) { isEdit, isSaving, e1, e2, e3 ->
        FullEditState(isEdit, isSaving, e1, e2, e3)
    }

    private val transactionData = if (isNewTransaction) {
        MutableStateFlow(TransactionData(null, emptyList(), emptyList()))
    } else {
        combine(
            moneyDao.getTransactionWithCategory(transactionId),
            moneyDao.getPeopleForTransaction(transactionId),
            moneyDao.getAttachmentsForTransaction(transactionId)
        ) { transaction, people, attachments ->
            TransactionData(transaction, people, attachments)
        }
    }

    private data class TransferState(
        val isTransfer: Boolean,
        val targetWalletId: String?,
        val targetAmount: String,
        val transferFee: String,
        val transferEntity: com.sinxn.mymoney.core.data.local.entity.TransferEntity?
    )

    private val transferState = combine(
        _isTransfer,
        _targetWalletId,
        _editTargetAmount,
        _editTransferFee,
        _transferEntity
    ) { isTransfer, targetId, targetAmt, fee, entity ->
        TransferState(isTransfer, targetId, targetAmt, fee, entity)
    }

    val uiState: StateFlow<TransactionDetailsUiState> = combine(
        transactionData,
        fullEditState,
        settingsRepository.currentWalletId,
        transferState,
        combine(
            moneyDao.getWalletsWithBalance(DateUtils.getSQLDateTimeString(java.util.Date())),
            moneyDao.getCategories(),
            moneyDao.getPlaces(),
            moneyDao.getEvents(),
            moneyDao.getPeople()
        ) { w, c, p, e, pp -> ListsWrapper(w, flattenCategories(c), p, e, pp) }
    ) { data, edit, currentWalletId, transferInfo, lists ->
        val transaction = data.transaction
        val people = data.people
        val attachments = data.attachments
        val enriched = enrichTransaction(transaction)
        val isTransfer = transferInfo.isTransfer
        val targetWalletId = transferInfo.targetWalletId
        val transferEntity = transferInfo.transferEntity
        
        // For new transactions, prefer the saved current wallet id, fall back to first available active wallet
        if (isNewTransaction && _editWalletId.value.isEmpty() && lists.wallets.isNotEmpty()) {
            val preferredWallet = lists.wallets.find { it.wallet.id == currentWalletId }
                ?: lists.wallets.firstOrNull { !it.wallet.isArchived }
                ?: lists.wallets.firstOrNull()
            
            _editWalletId.value = preferredWallet?.wallet?.id ?: ""
        }

        val activeWalletId = if (_editWalletId.value.isNotEmpty()) _editWalletId.value else transaction?.transaction?.walletId
        val activeWallet = lists.wallets.find { it.wallet.id == activeWalletId }
        val targetWallet = lists.wallets.find { it.wallet.id == targetWalletId }
        
        val activeWalletIds = setOfNotNull(
            transaction?.transaction?.walletId,
            _editWalletId.value.takeIf { it.isNotEmpty() },
            edit.e2.walletId.takeIf { it.isNotEmpty() },
            targetWalletId
        )

        val selectedCatId = edit.e1.catId ?: transaction?.transaction?.categoryId
        val selectedCat = lists.categories.find { it.id == selectedCatId }
        val selectedParentId = selectedCat?.parentId
        val baseActiveCatIds = setOfNotNull(selectedCatId, selectedParentId)
        val activeCatIds = baseActiveCatIds + lists.categories.filter { !it.isArchived || it.id in baseActiveCatIds }.mapNotNull { it.parentId }

        val selectedPlaceId = edit.e2.placeId ?: transaction?.transaction?.placeId
        val selectedEventId = edit.e2.eventId ?: transaction?.transaction?.eventId
        val activePeopleIds = edit.e2.people + people.map { it.id }.toSet()

        val filteredWallets = lists.wallets
            .filter { !it.wallet.isArchived || it.wallet.id in activeWalletIds }
            .map { it.wallet }

        val filteredCategories = lists.categories.filter { !it.isArchived || it.id in activeCatIds }
        val filteredPlaces = lists.places.filter { !it.isArchived || it.id == selectedPlaceId }
        val filteredEvents = lists.events.filter { !it.isArchived || it.id == selectedEventId }
        val filteredPeople = lists.people.filter { !it.isArchived || it.id in activePeopleIds }

        // Final combine with edit states
        TransactionDetailsUiState(
            transaction = transaction,
            isEditMode = edit.isEdit,
            isNewTransaction = isNewTransaction,
            isLoading = !isNewTransaction && transaction == null,
            isSaving = edit.isSaving,
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
                java.util.Currency.getInstance(activeWallet?.wallet?.currency ?: "USD").getSymbol(java.util.Locale.getDefault())
            } catch (e: Exception) {
                activeWallet?.currencySymbol ?: activeWallet?.wallet?.currency ?: "$"
            },
            currencyDecimals = activeWallet?.decimals ?: 2,
            walletName = activeWallet?.wallet?.name ?: "",
            categoryColor = transaction?.categoryName?.let { generateColor(it) } ?: androidx.compose.ui.graphics.Color.Gray,
            
            editAmount = edit.e1.amt,
            editNote = edit.e1.note,
            editDescription = edit.e1.desc,
            editDate = edit.e1.date,
            editCategoryId = edit.e1.catId,
            editWalletId = edit.e2.walletId.ifEmpty { activeWalletId ?: "" },
            editPlaceId = edit.e2.placeId,
            editEventId = edit.e2.eventId,
            editDirection = edit.e2.dir,
            editPeopleIds = edit.e2.people,
            editConfirmed = edit.e3.confirmed,
            editCountInTotal = edit.e3.countTotal,

            isTransfer = isTransfer,
            targetWalletId = targetWalletId,
            targetWalletName = targetWallet?.wallet?.name ?: "",
            editTargetAmount = transferInfo.targetAmount,
            editTransferFee = transferInfo.transferFee,
            targetWalletCurrency = targetWallet?.wallet?.currency ?: "",
            targetWalletDecimals = targetWallet?.decimals ?: 2,
            transferEntity = transferEntity
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

    private data class EditPart1(val amt: String, val note: String, val desc: String, val date: String, val catId: String?)
    private data class EditPart2(val walletId: String, val placeId: String?, val eventId: String?, val dir: Int, val people: Set<String>)
    private data class EditPart3(val confirmed: Boolean, val countTotal: Boolean)
    private data class FullEditState(
        val isEdit: Boolean,
        val isSaving: Boolean,
        val e1: EditPart1,
        val e2: EditPart2,
        val e3: EditPart3
    )
    private data class ListsWrapper(
        val wallets: List<com.sinxn.mymoney.core.data.local.model.WalletWithBalance>,
        val categories: List<CategoryEntity>,
        val places: List<PlaceEntity>,
        val events: List<EventEntity>,
        val people: List<PersonEntity>
    )
    private fun flattenCategories(categories: List<com.sinxn.mymoney.core.data.local.entity.CategoryEntity>): List<com.sinxn.mymoney.core.data.local.entity.CategoryEntity> {
        // Filter out system categories (CategoryType.SYSTEM) - Only show Income and Expense
        val validCategories = categories.filter { it.type == CategoryType.INCOME || it.type == CategoryType.EXPENSE }
        
        val parents = validCategories.filter { it.parentId == null }.sortedBy { it.index }
        val result = mutableListOf<com.sinxn.mymoney.core.data.local.entity.CategoryEntity>()
        
        parents.forEach { parent ->
            result.add(parent)
            val children = validCategories.filter { it.parentId == parent.id }.sortedBy { it.index }
            children.forEach { child ->
                // Create a visual copy for dropdown without verifying ID in DB (UI only trick)
                // Or simply modify name if it's display only? 
                // Since Entity is data class, copy works. We'll modify name for display.
                // NOTE: This modifies the Entity in the list. SelectionDialog uses this name.
                result.add(child.copy(name = "  ↳ ${child.name}"))
            }
        }
        return result
    }

    private data class EnrichedData(
        val place: PlaceEntity? = null,
        val event: EventEntity? = null
    )
    private data class TransactionData(
        val transaction: TransactionWithCategory?,
        val people: List<PersonEntity>,
        val attachments: List<AttachmentEntity>
    )

    fun toggleEditMode() {
        val current = uiState.value.transaction
        val currentPeople = uiState.value.people
        val decimals = uiState.value.currencyDecimals
        
        if (!_isEditMode.value && current != null) {
            // Convert Long (base units) to decimal String for UI
            val divider = 10.0.pow(decimals.toDouble())
            val amount = current.transaction.money.toDouble() / divider
            _editAmount.value = "%.${decimals}f".format(java.util.Locale.US, amount)
            
            _editNote.value = current.transaction.note ?: ""
            _editDescription.value = current.transaction.description ?: ""
            _editDate.value = current.transaction.date
            _editCategoryId.value = current.transaction.categoryId
            _editPlaceId.value = current.transaction.placeId
            _editEventId.value = current.transaction.eventId
            _editPeopleIds.value = currentPeople.map { it.id }.toSet()
            _editConfirmed.value = current.transaction.confirmed
            _editCountInTotal.value = current.transaction.countInTotal

            val transfer = _transferEntity.value
            if (transfer != null || current.transaction.type == 1 || current.transaction.type == 2) {
                _isTransfer.value = true
                _editDirection.value = Direction.TRANSFER
            } else {
                _editWalletId.value = current.transaction.walletId
                _editDirection.value = current.transaction.direction
            }
        }
        _isEditMode.value = !_isEditMode.value
    }

    fun onAmountChange(value: String) { _editAmount.value = value }

    fun onNumpadKeyPress(key: String) {
        var current = _editAmount.value
        if (key == "BACKSPACE") {
            if (current.isNotEmpty()) {
                current = current.trimEnd()
                if (current.isNotEmpty()) {
                    current = current.dropLast(1).trimEnd()
                }
                _editAmount.value = current.ifEmpty { "0" }
            }
        } else if (key in listOf("+", "-", "×", "÷")) {
            val trimmed = current.trim()
            val base = if (trimmed.endsWith("+") || trimmed.endsWith("-") || trimmed.endsWith("×") || trimmed.endsWith("÷")) {
                trimmed.dropLast(1).trim()
            } else {
                trimmed
            }
            _editAmount.value = "$base $key "
        } else if (key == ".") {
            val lastToken = current.split(" ").lastOrNull() ?: ""
            if (!lastToken.contains(".")) {
                _editAmount.value = "$current."
            }
        } else {
            val trimmed = current.trim()
            if (trimmed == "0" || trimmed == "0.0" || trimmed == "0.00") {
                _editAmount.value = key
            } else {
                _editAmount.value = current + key
            }
        }
    }

    fun evaluateMathExpression() {
        val expr = _editAmount.value.replace("×", "*").replace("÷", "/")
        val result = evaluateSimpleMath(expr)
        if (result != null) {
            val decimals = uiState.value.currencyDecimals
            _editAmount.value = if (result % 1.0 == 0.0) {
                result.toLong().toString()
            } else {
                "%.${decimals}f".format(java.util.Locale.US, result)
            }
        }
    }

    private fun evaluateSimpleMath(expr: String): Double? {
        val cleanExpr = expr.replace("×", "*").replace("÷", "/").trim()
        if (cleanExpr.isEmpty()) return null

        val tokens = mutableListOf<String>()
        var sb = StringBuilder()

        for (i in cleanExpr.indices) {
            val ch = cleanExpr[i]
            if (ch in listOf('+', '-', '*', '/')) {
                val isUnaryMinus = ch == '-' && (
                    sb.isEmpty() && (tokens.isEmpty() || tokens.last() in listOf("+", "-", "*", "/"))
                )

                if (isUnaryMinus) {
                    sb.append(ch)
                } else {
                    if (sb.isNotEmpty()) {
                        tokens.add(sb.toString().trim())
                        sb = StringBuilder()
                    }
                    tokens.add(ch.toString())
                }
            } else if (ch != ' ') {
                sb.append(ch)
            }
        }
        if (sb.isNotEmpty()) {
            tokens.add(sb.toString().trim())
        }

        if (tokens.isEmpty()) return null
        var currentVal = tokens[0].toDoubleOrNull() ?: return null

        var idx = 1
        while (idx < tokens.size - 1) {
            val op = tokens[idx]
            val nextVal = tokens[idx + 1].toDoubleOrNull() ?: break
            when (op) {
                "+" -> currentVal += nextVal
                "-" -> currentVal -= nextVal
                "*" -> currentVal *= nextVal
                "/" -> if (nextVal != 0.0) currentVal /= nextVal
            }
            idx += 2
        }
        return currentVal
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
        _editDate.value = DateUtils.getSQLDateTimeString(cal.time)
    }

    fun deleteTransaction(onComplete: () -> Unit) {
        viewModelScope.launch {
            if (!isNewTransaction) {
                val current = uiState.value.transaction?.transaction ?: return@launch
                moneyDao.updateTransaction(current.copy(isDeleted = true, lastEdit = System.currentTimeMillis()))
                val transfer = _transferEntity.value ?: moneyDao.getTransferByTransactionId(transactionId)
                if (transfer != null) {
                    moneyDao.updateTransfer(transfer.copy(isDeleted = true, lastEdit = System.currentTimeMillis()))
                    val siblingTxId = if (transfer.transactionFromId == transactionId) transfer.transactionToId else transfer.transactionFromId
                    val siblingTx = moneyDao.getTransactionById(siblingTxId)
                    if (siblingTx != null) {
                        moneyDao.updateTransaction(siblingTx.copy(isDeleted = true, lastEdit = System.currentTimeMillis()))
                    }
                    val taxTxId = transfer.transactionTaxId
                    if (taxTxId != null) {
                        val taxTx = moneyDao.getTransactionById(taxTxId)
                        if (taxTx != null) {
                            moneyDao.updateTransaction(taxTx.copy(isDeleted = true, lastEdit = System.currentTimeMillis()))
                        }
                    }
                }
            }
            onComplete()
        }
    }

    fun onTransferToggle(isTransfer: Boolean) {
        _isTransfer.value = isTransfer
        if (isTransfer) {
            _editDirection.value = Direction.TRANSFER // Transfer Blue
            if (_targetWalletId.value.isNullOrEmpty()) {
                val altWallet = uiState.value.availableWallets.firstOrNull { it.id != _editWalletId.value }
                _targetWalletId.value = altWallet?.id
            }
        } else {
            _editDirection.value = Direction.EXPENSE // Expense
        }
    }

    fun onTargetWalletIdChange(walletId: String) {
        _targetWalletId.value = walletId
    }

    fun swapTransferWallets() {
        val from = _editWalletId.value
        val to = _targetWalletId.value
        if (!to.isNullOrEmpty()) {
            _editWalletId.value = to
            _targetWalletId.value = from
        }
    }

    fun onTargetAmountChange(value: String) { _editTargetAmount.value = value }
    fun onTransferFeeChange(value: String) { _editTransferFee.value = value }
    fun onNoteChange(value: String) { _editNote.value = value }
    fun onDescriptionChange(value: String) { _editDescription.value = value }
    fun onConfirmedChange(value: Boolean) { _editConfirmed.value = value }
    fun onCountInTotalChange(value: Boolean) { _editCountInTotal.value = value }
    fun onCategoryIdChange(value: String?) { 
        _editCategoryId.value = value
        // Update direction based on category if found
        value?.let { id ->
            uiState.value.availableCategories.find { it.id == id }?.let { category ->
                _editDirection.value = if (category.type == CategoryType.INCOME) Direction.INCOME else Direction.EXPENSE
            }
        }
    }
    fun onWalletIdChange(value: String) { 
        _editWalletId.value = value 
        if (_targetWalletId.value == value) {
            val alt = uiState.value.availableWallets.firstOrNull { it.id != value }
            _targetWalletId.value = alt?.id
        }
    }
    fun onPlaceIdChange(value: String?) { _editPlaceId.value = value }
    fun onEventIdChange(value: String?) { _editEventId.value = value }
    fun onDirectionChange(value: Int) { _editDirection.value = value }
    fun onDateChange(millis: Long) {
        val currentDate = DateUtils.parseDate(_editDate.value)
        val newDate = java.util.Date(millis)
        
        // Merge year/month/day from newDate with hour/minute/second from currentDate
        val calendar = Calendar.getInstance()
        calendar.time = currentDate
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        
        calendar.time = newDate
        calendar.set(Calendar.HOUR_OF_DAY, currentHour)
        calendar.set(Calendar.MINUTE, currentMinute)
        
        _editDate.value = DateUtils.getSQLDateTimeString(calendar.time)
    }

    fun onTimeChange(hour: Int, minute: Int) {
        val currentDate = DateUtils.parseDate(_editDate.value)
        val calendar = Calendar.getInstance()
        calendar.time = currentDate
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        
        _editDate.value = DateUtils.getSQLDateTimeString(calendar.time)
    }

    fun onPeopleToggle(personId: String) {
        val current = _editPeopleIds.value.toMutableSet()
        if (current.contains(personId)) current.remove(personId)
        else current.add(personId)
        _editPeopleIds.value = current
    }

    fun getImmediateResult(editAmount: String): String {
        val trimmed = editAmount.trim()
        val rest = if (trimmed.startsWith("-")) trimmed.substring(1) else trimmed
        if (!rest.any { it in listOf('+', '-', '×', '÷', '*', '/') }) {
            return if (trimmed.isEmpty()) "0" else trimmed
        }
        val expr = trimmed.replace("×", "*").replace("÷", "/")
        val result = evaluateSimpleMath(expr) ?: return if (trimmed.isEmpty()) "0" else trimmed
        val decimals = uiState.value.currencyDecimals
        return if (result % 1.0 == 0.0) {
            result.toLong().toString()
        } else {
            "%.${decimals}f".format(java.util.Locale.US, result)
        }
    }

    fun saveChanges() {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val decimals = uiState.value.currencyDecimals
                val updatedDirection = _editDirection.value
                
                // Parse decimal string back to Long base units
                val moneyValue = try {
                    val multiplier = 10.0.pow(decimals.toDouble())
                    (getImmediateResult(_editAmount.value).replace(",", ".").toDouble() * multiplier).toLong()
                } catch (e: Exception) {
                    0L
                }

                if (_isTransfer.value) {
                    val walletFromId = _editWalletId.value
                    val walletToId = _targetWalletId.value ?: _editWalletId.value
                    val walletFrom = moneyDao.getWalletById(walletFromId)
                    val walletTo = moneyDao.getWalletById(walletToId)
                    val fromDecimals = walletFrom?.currency?.let { moneyDao.getCurrencyByIso(it)?.decimals } ?: decimals
                    val toDecimals = walletTo?.currency?.let { moneyDao.getCurrencyByIso(it)?.decimals } ?: decimals

                    val fromMoneyValue = try {
                        val multiplier = 10.0.pow(fromDecimals.toDouble())
                        (getImmediateResult(_editAmount.value).replace(",", ".").toDouble() * multiplier).toLong()
                    } catch (e: Exception) { 0L }

                    val toMoneyValue = if (walletFrom?.currency != null && walletTo?.currency != null && !walletFrom.currency.equals(walletTo.currency, ignoreCase = true) && _editTargetAmount.value.isNotBlank()) {
                        try {
                            val multiplier = 10.0.pow(toDecimals.toDouble())
                            (getImmediateResult(_editTargetAmount.value).replace(",", ".").toDouble() * multiplier).toLong()
                        } catch (e: Exception) { fromMoneyValue }
                    } else {
                        fromMoneyValue
                    }

                    val feeValue = try {
                        if (_editTransferFee.value.isNotBlank()) {
                            val multiplier = 10.0.pow(fromDecimals.toDouble())
                            (getImmediateResult(_editTransferFee.value).replace(",", ".").toDouble() * multiplier).toLong()
                        } else 0L
                    } catch (e: Exception) { 0L }

                    val existingTransfer = _transferEntity.value
                    val transferId = existingTransfer?.id ?: UUID.randomUUID().toString()
                    val fromTxId = existingTransfer?.transactionFromId ?: if (isNewTransaction) UUID.randomUUID().toString() else transactionId
                    val toTxId = existingTransfer?.transactionToId ?: UUID.randomUUID().toString()

                    val transferCategory = uiState.value.availableCategories.find { 
                        it.tag == "transfer" || it.name.equals("Transfer", ignoreCase = true) 
                    }?.id ?: _editCategoryId.value

                    val fromTx = com.sinxn.mymoney.core.data.local.entity.TransactionEntity(
                        id = fromTxId,
                        money = fromMoneyValue,
                        date = _editDate.value,
                        categoryId = transferCategory,
                        walletId = walletFromId,
                        note = _editNote.value.takeIf { it.isNotEmpty() },
                        description = _editDescription.value.takeIf { it.isNotEmpty() },
                        placeId = _editPlaceId.value,
                        eventId = _editEventId.value,
                        direction = Direction.EXPENSE, // Expense from Source Wallet
                        confirmed = _editConfirmed.value,
                        countInTotal = _editCountInTotal.value,
                        type = TransactionType.TRANSFER, // Contract.TransactionType.TRANSFER = 1
                        isDeleted = false,
                        debtId = null,
                        savingId = null,
                        recurrenceId = null,
                        tag = null,
                        lastEdit = System.currentTimeMillis()
                    )

                    val toTx = com.sinxn.mymoney.core.data.local.entity.TransactionEntity(
                        id = toTxId,
                        money = toMoneyValue,
                        date = _editDate.value,
                        categoryId = transferCategory,
                        walletId = walletToId,
                        note = _editNote.value.takeIf { it.isNotEmpty() },
                        description = _editDescription.value.takeIf { it.isNotEmpty() },
                        placeId = _editPlaceId.value,
                        eventId = _editEventId.value,
                        direction = Direction.INCOME, // Income into Destination Wallet
                        confirmed = _editConfirmed.value,
                        countInTotal = _editCountInTotal.value,
                        type = TransactionType.TRANSFER, // Contract.TransactionType.TRANSFER = 1
                        isDeleted = false,
                        debtId = null,
                        savingId = null,
                        recurrenceId = null,
                        tag = null,
                        lastEdit = System.currentTimeMillis()
                    )

                    val taxTxId = if (feeValue > 0) {
                        val existingTaxId = existingTransfer?.transactionTaxId ?: UUID.randomUUID().toString()
                        val taxCategory = uiState.value.availableCategories.find {
                            it.tag == "system::transfer_tax" || it.tag == "transfer_tax" || it.name.contains("Tax", ignoreCase = true) || it.name.contains("Fee", ignoreCase = true)
                        }?.id ?: transferCategory

                        val taxTx = com.sinxn.mymoney.core.data.local.entity.TransactionEntity(
                            id = existingTaxId,
                            money = feeValue,
                            date = _editDate.value,
                            categoryId = taxCategory,
                            walletId = walletFromId,
                            note = "Transfer fee",
                            description = _editDescription.value.takeIf { it.isNotEmpty() } ?: "Transfer Fee",
                            placeId = _editPlaceId.value,
                            eventId = _editEventId.value,
                            direction = Direction.EXPENSE,
                            confirmed = _editConfirmed.value,
                            countInTotal = _editCountInTotal.value,
                            type = TransactionType.TRANSFER,
                            isDeleted = false,
                            debtId = null,
                            savingId = null,
                            recurrenceId = null,
                            tag = null,
                            lastEdit = System.currentTimeMillis()
                        )
                        moneyDao.insertTransaction(taxTx)
                        existingTaxId
                    } else {
                        val existingTaxId = existingTransfer?.transactionTaxId
                        if (existingTaxId != null) {
                            val oldTax = moneyDao.getTransactionById(existingTaxId)
                            if (oldTax != null) {
                                moneyDao.updateTransaction(oldTax.copy(isDeleted = true, lastEdit = System.currentTimeMillis()))
                            }
                        }
                        null
                    }

                    val newTransfer = com.sinxn.mymoney.core.data.local.entity.TransferEntity(
                        id = transferId,
                        description = _editDescription.value.takeIf { it.isNotEmpty() },
                        date = _editDate.value,
                        transactionFromId = fromTxId,
                        transactionToId = toTxId,
                        transactionTaxId = taxTxId,
                        note = _editNote.value.takeIf { it.isNotEmpty() },
                        placeId = _editPlaceId.value,
                        eventId = _editEventId.value,
                        recurrenceId = null,
                        confirmed = _editConfirmed.value,
                        countInTotal = _editCountInTotal.value,
                        isDeleted = false,
                        lastEdit = System.currentTimeMillis(),
                        tag = null
                    )

                    if (isNewTransaction && existingTransfer == null) {
                        moneyDao.insertTransaction(fromTx)
                        moneyDao.insertTransaction(toTx)
                        moneyDao.insertTransfer(newTransfer)
                    } else {
                        moneyDao.updateTransaction(fromTx)
                        moneyDao.updateTransaction(toTx)
                        moneyDao.updateTransfer(newTransfer)
                    }

                    // Save People for Transfer
                    moneyDao.deletePeopleForTransaction(fromTxId)
                    val newPeople = _editPeopleIds.value.map { personId ->
                        TransactionPeopleEntity(
                            transactionId = fromTxId,
                            personId = personId,
                            isDeleted = false,
                            lastEdit = System.currentTimeMillis(),
                            id = UUID.randomUUID().toString()
                        )
                    }
                    moneyDao.insertTransactionPeople(newPeople)
                } else {
                    val targetId = if (isNewTransaction) UUID.randomUUID().toString() else transactionId

                    if (isNewTransaction) {
                        val newTransaction = com.sinxn.mymoney.core.data.local.entity.TransactionEntity(
                            id = targetId,
                            money = moneyValue,
                            date = _editDate.value,
                            categoryId = _editCategoryId.value,
                            walletId = _editWalletId.value,
                            note = _editNote.value.takeIf { it.isNotEmpty() },
                            description = _editDescription.value.takeIf { it.isNotEmpty() },
                            placeId = _editPlaceId.value,
                            eventId = _editEventId.value,
                            direction = updatedDirection,
                            confirmed = _editConfirmed.value,
                            countInTotal = _editCountInTotal.value,
                            type = if (_debtId.value != null) 2 else 0,
                            isDeleted = false,
                            debtId = _debtId.value,
                            savingId = _savingId.value,
                            recurrenceId = null,
                            tag = null,
                            lastEdit = System.currentTimeMillis()
                        )
                        moneyDao.insertTransaction(newTransaction)

                        _savingId.value?.let { sid ->
                            if (_savingCompletedOnSave.value) {
                                savingRepository.setSavingComplete(sid, true)
                            }
                        }
                    } else {
                        val current = uiState.value.transaction?.transaction ?: return@launch
                        val updated = current.copy(
                            money = moneyValue,
                            note = _editNote.value.takeIf { it.isNotEmpty() },
                            description = _editDescription.value.takeIf { it.isNotEmpty() },
                            categoryId = _editCategoryId.value,
                            walletId = _editWalletId.value,
                            placeId = _editPlaceId.value,
                            eventId = _editEventId.value,
                            direction = updatedDirection,
                            type = if (_debtId.value != null) 2 else 0,
                            confirmed = _editConfirmed.value,
                            countInTotal = _editCountInTotal.value,
                            debtId = _debtId.value ?: current.debtId,
                            lastEdit = System.currentTimeMillis()
                        )
                        moneyDao.updateTransaction(updated)
                        
                        // If it WAS a transfer and now it's not, delete the sibling, transfer entity, and tax
                        val existingTransfer = _transferEntity.value
                        if (existingTransfer != null) {
                            moneyDao.updateTransfer(existingTransfer.copy(isDeleted = true, lastEdit = System.currentTimeMillis()))
                            val siblingTxId = if (existingTransfer.transactionFromId == transactionId) existingTransfer.transactionToId else existingTransfer.transactionFromId
                            val siblingTx = moneyDao.getTransactionById(siblingTxId)
                            if (siblingTx != null) {
                                moneyDao.updateTransaction(siblingTx.copy(isDeleted = true, lastEdit = System.currentTimeMillis()))
                            }
                            val taxTxId = existingTransfer.transactionTaxId
                            if (taxTxId != null) {
                                val taxTx = moneyDao.getTransactionById(taxTxId)
                                if (taxTx != null) {
                                    moneyDao.updateTransaction(taxTx.copy(isDeleted = true, lastEdit = System.currentTimeMillis()))
                                }
                            }
                            _transferEntity.value = null
                        }
                    }
                    
                    // Update People
                    moneyDao.deletePeopleForTransaction(targetId)
                    val newPeople = _editPeopleIds.value.map { personId ->
                        TransactionPeopleEntity(
                            transactionId = targetId,
                            personId = personId,
                            isDeleted = false,
                            lastEdit = System.currentTimeMillis(),
                            id = UUID.randomUUID().toString()
                        )
                    }
                    moneyDao.insertTransactionPeople(newPeople)
                }
                
                _isEditMode.value = false
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isSaving.value = false
            }
        }
    }
    
    val formattingSettings = settingsRepository.formattingSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FormattingSettings()
        )

    private fun generateColor(name: String): androidx.compose.ui.graphics.Color {
        val hash = name.hashCode()
        val hue = kotlin.math.abs(hash % 360).toFloat()
        return androidx.compose.ui.graphics.Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.6f, 0.8f)))
    }
}
