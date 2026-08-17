package com.sinxn.mymoney.feature.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.data.local.entity.EventEntity
import com.sinxn.mymoney.core.data.local.entity.PersonEntity
import com.sinxn.mymoney.core.data.local.entity.PlaceEntity
import com.sinxn.mymoney.core.data.local.entity.TransactionEntity
import com.sinxn.mymoney.core.data.local.entity.WalletEntity
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.repository.DebtRepository
import com.sinxn.mymoney.core.data.repository.SavingRepository
import com.sinxn.mymoney.core.data.repository.TemplateRepository
import com.sinxn.mymoney.core.data.repository.TransactionRepository
import com.sinxn.mymoney.core.util.AmountUtils.parseAmountToLong
import com.sinxn.mymoney.core.util.AmountUtils.toDecimalString
import com.sinxn.mymoney.core.util.CategoryType
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.Direction
import com.sinxn.mymoney.core.util.MathExpressionEvaluator
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

data class TransactionAddEditUiState(
    val isNewTransaction: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
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
    val availableWallets: List<WalletEntity> = emptyList(),
    val availableCategories: List<CategoryEntity> = emptyList(),
    val availableIncomeCategories: List<CategoryEntity> = emptyList(),
    val availableExpenseCategories: List<CategoryEntity> = emptyList(),
    val availablePlaces: List<PlaceEntity> = emptyList(),
    val availableEvents: List<EventEntity> = emptyList(),
    val availablePeople: List<PersonEntity> = emptyList(),
    val currencyCode: String = "USD",
    val currencySymbol: String = "$",
    val currencyDecimals: Int = 2,
    val walletName: String = "",
    val categoryColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Gray
)

@HiltViewModel
class TransactionAddEditViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val moneyDao: MoneyDao,
    private val settingsRepository: SettingsRepository,
    private val savingRepository: SavingRepository,
    private val debtRepository: DebtRepository,
    private val templateRepository: TemplateRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val transactionIdArg: String? = savedStateHandle.get<String>("transactionId")?.takeIf { it.isNotBlank() && it != "new" }
    val isNewTransaction = transactionIdArg == null

    private val _isSaving = MutableStateFlow(false)

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
        val templateIdArg: String? = savedStateHandle.get<String>("templateId")

        if (isNewTransaction) {
            initNewTransactionDefaults(savingIdArg, savingActionArg, debtIdArg, debtActionArg, templateIdArg)
        } else {
            initExistingTransaction()
        }
    }

    private fun initNewTransactionDefaults(
        savingIdArg: String?,
        savingActionArg: String?,
        debtIdArg: String?,
        debtActionArg: String?,
        templateIdArg: String?
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
        } else if (!templateIdArg.isNullOrBlank()) {
            viewModelScope.launch {
                val template = templateRepository.getTransactionModelById(templateIdArg)
                if (template != null) {
                    val wallet = moneyDao.getWalletsList().find { it.id == template.walletId }
                    val curr = wallet?.let { moneyDao.getCurrencyByIso(it.currency) }
                    val decimals = curr?.decimals ?: 2
                    val amountStr = template.money.toDecimalString(decimals)

                    _formState.update { current ->
                        current.copy(
                            amount = amountStr,
                            description = template.description ?: "",
                            categoryId = template.categoryId,
                            direction = template.direction,
                            walletId = template.walletId,
                            placeId = template.placeId,
                            eventId = template.eventId,
                            note = template.note ?: "",
                            confirmed = template.confirmed,
                            countInTotal = template.countInTotal
                        )
                    }
                }
            }
        }
    }

    private fun initExistingTransaction() {
        val txId = transactionIdArg ?: return
        viewModelScope.launch {
            val tx = transactionRepository.getTransactionById(txId)
            val people = transactionRepository.getPeopleForTransaction(txId).firstOrNull() ?: emptyList()

            if (tx != null) {
                val wallet = moneyDao.getWalletsList().find { it.id == tx.walletId }
                val curr = wallet?.let { moneyDao.getCurrencyByIso(it.currency) }
                val decimals = curr?.decimals ?: 2

                _formState.update { form ->
                    form.copy(
                        amount = tx.money.toDecimalString(decimals),
                        note = tx.note ?: "",
                        description = tx.description ?: "",
                        date = tx.date,
                        categoryId = tx.categoryId,
                        placeId = tx.placeId,
                        eventId = tx.eventId,
                        peopleIds = people.map { it.id }.toSet(),
                        confirmed = tx.confirmed,
                        countInTotal = tx.countInTotal,
                        direction = tx.direction,
                        walletId = tx.walletId,
                        debtId = tx.debtId,
                        savingId = tx.savingId
                    )
                }
            }
        }
    }

    private val listsFlow = combine(
        moneyDao.getWalletsWithBalance(DateUtils.getSQLDateTimeString(Date())),
        moneyDao.getCategories(),
        moneyDao.getPlaces(),
        moneyDao.getEvents(),
        moneyDao.getPeople()
    ) { w, c, p, e, pp -> ListsWrapper(w, TransactionDetailsUiMapper.flattenCategories(c), p, e, pp) }

    val uiState: StateFlow<TransactionAddEditUiState> = combine(
        _isSaving,
        _formState,
        settingsRepository.currentWalletId,
        listsFlow
    ) { isSaving, form, currentWalletId, lists ->
        if (isNewTransaction && form.walletId.isEmpty() && lists.wallets.isNotEmpty()) {
            if (currentWalletId.isNotEmpty() && currentWalletId != "total" && currentWalletId != com.sinxn.mymoney.core.util.Constants.TOTAL_WALLET_ID) {
                val preferredWallet = lists.wallets.find { it.wallet.id == currentWalletId && !it.wallet.isArchived }
                    ?: lists.wallets.find { it.wallet.id == currentWalletId }

                if (preferredWallet != null) {
                    _formState.update { it.copy(walletId = preferredWallet.wallet.id) }
                }
            }
        }

        val activeWalletId = form.walletId
        val activeWallet = lists.wallets.find { it.wallet.id == activeWalletId }

        val activeWalletIds = setOfNotNull(form.walletId.takeIf { it.isNotEmpty() })

        val selectedCatId = form.categoryId
        val selectedCat = lists.categories.find { it.id == selectedCatId }
        val selectedParentId = selectedCat?.parentId
        val baseActiveCatIds = setOfNotNull(selectedCatId, selectedParentId)
        val activeCatIds = baseActiveCatIds + lists.categories.filter { !it.isArchived || it.id in baseActiveCatIds }.mapNotNull { it.parentId }

        val selectedPlaceId = form.placeId
        val selectedEventId = form.eventId
        val activePeopleIds = form.peopleIds

        val filteredWallets = lists.wallets.filter { !it.wallet.isArchived || it.wallet.id in activeWalletIds }.map { it.wallet }
        val filteredCategories = lists.categories.filter { !it.isArchived || it.id in activeCatIds }
        val filteredPlaces = lists.places.filter { !it.isArchived || it.id == selectedPlaceId }
        val filteredEvents = lists.events.filter { !it.isArchived || it.id == selectedEventId }
        val filteredPeople = lists.people.filter { !it.isArchived || it.id in activePeopleIds }

        TransactionAddEditUiState(
            isNewTransaction = isNewTransaction,
            isLoading = false,
            isSaving = isSaving,
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
            categoryColor = selectedCat?.name?.let { TransactionDetailsUiMapper.generateCategoryColor(it) } ?: androidx.compose.ui.graphics.Color.Gray,
            editAmount = form.amount,
            editNote = form.note,
            editDescription = form.description,
            editDate = form.date,
            editCategoryId = form.categoryId,
            editWalletId = form.walletId,
            editPlaceId = form.placeId,
            editEventId = form.eventId,
            editDirection = form.direction,
            editPeopleIds = form.peopleIds,
            editConfirmed = form.confirmed,
            editCountInTotal = form.countInTotal
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionAddEditUiState()
    )

    val formattingSettings = settingsRepository.formattingSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FormattingSettings()
        )

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
        _formState.update { current -> current.copy(walletId = value) }
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

    fun saveChanges(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val decimals = uiState.value.currencyDecimals
                val form = _formState.value
                saveSingleTransaction(form, decimals)
                onSuccess()
            } catch (_: Exception) {
                // Handle error
            } finally {
                _isSaving.value = false
            }
        }
    }

    private suspend fun saveSingleTransaction(form: TransactionFormState, decimals: Int) {
        val targetId = if (isNewTransaction) UUID.randomUUID().toString() else (transactionIdArg ?: UUID.randomUUID().toString())
        val moneyValue = parseAmountToLong(getImmediateResult(form.amount), decimals)

        val txEntity = TransactionEntity(
            id = targetId,
            money = moneyValue,
            date = form.date,
            categoryId = form.categoryId,
            walletId = form.walletId,
            note = form.note.takeIf { it.isNotEmpty() },
            description = form.description.takeIf { it.isNotEmpty() },
            placeId = form.placeId,
            eventId = form.eventId,
            direction = form.direction,
            confirmed = form.confirmed,
            countInTotal = form.countInTotal,
            type = if (form.debtId != null) 2 else 0,
            isDeleted = false,
            debtId = form.debtId,
            savingId = form.savingId,
            recurrenceId = null,
            tag = null,
            lastEdit = System.currentTimeMillis()
        )

        transactionRepository.saveSingleTransaction(
            transaction = txEntity,
            isNewTransaction = isNewTransaction,
            savingId = form.savingId,
            isSavingCompleted = form.savingCompletedOnSave,
            peopleIds = form.peopleIds,
            previousTransfer = null
        )
    }

    private data class ListsWrapper(
        val wallets: List<com.sinxn.mymoney.core.data.local.model.WalletWithBalance>,
        val categories: List<CategoryEntity>,
        val places: List<PlaceEntity>,
        val events: List<EventEntity>,
        val people: List<PersonEntity>
    )
}
