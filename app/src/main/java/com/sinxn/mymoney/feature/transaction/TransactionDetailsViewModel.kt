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
import com.sinxn.mymoney.core.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import kotlin.math.pow

data class TransactionDetailsUiState(
    val transaction: TransactionWithCategory? = null,
    val isEditMode: Boolean = false,
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
    // Redesign Fields
    val walletName: String = "",
    val categoryColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Gray
)

@HiltViewModel
class TransactionDetailsViewModel @Inject constructor(
    private val moneyDao: MoneyDao,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val transactionId: String = checkNotNull(savedStateHandle["transactionId"])

    private val _isEditMode = MutableStateFlow(false)
    private val _isSaving = MutableStateFlow(false)
    
    // Internal state for edits
    private val _editAmount = MutableStateFlow("")
    private val _editNote = MutableStateFlow("")
    private val _editDescription = MutableStateFlow("")
    private val _editDate = MutableStateFlow("")
    private val _editCategoryId = MutableStateFlow<String?>(null)
    private val _editWalletId = MutableStateFlow("")
    private val _editPlaceId = MutableStateFlow<String?>(null)
    private val _editEventId = MutableStateFlow<String?>(null)
    private val _editDirection = MutableStateFlow(0)
    private val _editPeopleIds = MutableStateFlow<Set<String>>(emptySet())
    private val _editConfirmed = MutableStateFlow(true)
    private val _editCountInTotal = MutableStateFlow(true)

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

    private val transactionData = combine(
        moneyDao.getTransactionWithCategory(transactionId),
        moneyDao.getPeopleForTransaction(transactionId),
        moneyDao.getAttachmentsForTransaction(transactionId)
    ) { transaction, people, attachments ->
        TransactionData(transaction, people, attachments)
    }

    val uiState: StateFlow<TransactionDetailsUiState> = combine(
        transactionData,
        fullEditState,
        combine(
            moneyDao.getWalletsWithBalance(),
            moneyDao.getCategories(),
            moneyDao.getPlaces(),
            moneyDao.getEvents(),
            moneyDao.getPeople()
        ) { w, c, p, e, pp -> ListsWrapper(w, flattenCategories(c), p, e, pp) }
    ) { data, edit, lists ->
        val transaction = data.transaction
        val people = data.people
        val attachments = data.attachments
        val enriched = enrichTransaction(transaction)
        
        val activeWalletId = if (edit.isEdit && edit.e2.walletId.isNotEmpty()) edit.e2.walletId else transaction?.transaction?.walletId
        val activeWallet = lists.wallets.find { it.wallet.id == activeWalletId }
        
        // Final combine with edit states
        TransactionDetailsUiState(
            transaction = transaction,
            isEditMode = edit.isEdit,
            isLoading = transaction == null,
            isSaving = edit.isSaving,
            place = enriched.place,
            event = enriched.event,
            people = people,
            attachments = attachments,
            // These will be updated via the `stateIn` logic if we use nested combines correctly
            // Filter archived wallets, but keep the current transaction's wallet even if archived
            availableWallets = lists.wallets
                .filter { !it.wallet.isArchived || it.wallet.id == transaction?.transaction?.walletId }
                .map { it.wallet },
            availableCategories = lists.categories,
            availableIncomeCategories = lists.categories.filter { it.type == 1 },
            availableExpenseCategories = lists.categories.filter { it.type == 0 },
            availablePlaces = lists.places,
            availableEvents = lists.events,
            availablePeople = lists.people,
            
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
            editWalletId = edit.e2.walletId,
            editPlaceId = edit.e2.placeId,
            editEventId = edit.e2.eventId,
            editDirection = edit.e2.dir,
            editPeopleIds = edit.e2.people,
            editConfirmed = edit.e3.confirmed,
            editCountInTotal = edit.e3.countTotal
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
        // Filter out system categories (Type 2+) - Only show Expense (0) and Income (1)
        val validCategories = categories.filter { it.type == 0 || it.type == 1 }
        
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
            _editWalletId.value = current.transaction.walletId
            _editPlaceId.value = current.transaction.placeId
            _editEventId.value = current.transaction.eventId
            _editDirection.value = current.transaction.direction
            _editPeopleIds.value = currentPeople.map { it.id }.toSet()
            _editConfirmed.value = current.transaction.confirmed
            _editCountInTotal.value = current.transaction.countInTotal
        }
        _isEditMode.value = !_isEditMode.value
    }

    fun onAmountChange(value: String) { _editAmount.value = value }
    fun onNoteChange(value: String) { _editNote.value = value }
    fun onDescriptionChange(value: String) { _editDescription.value = value }
    fun onConfirmedChange(value: Boolean) { _editConfirmed.value = value }
    fun onCountInTotalChange(value: Boolean) { _editCountInTotal.value = value }
    fun onCategoryIdChange(value: String?) { 
        _editCategoryId.value = value
        // Update direction based on category if found
        value?.let { id ->
            uiState.value.availableCategories.find { it.id == id }?.let { category ->
                _editDirection.value = category.type
            }
        }
    }
    fun onWalletIdChange(value: String) { _editWalletId.value = value }
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

    fun saveChanges() {
        val current = uiState.value.transaction?.transaction ?: return
        viewModelScope.launch {
            _isSaving.value = true
            try {
                // Determine direction: explicitly use editDirection which might have been updated by category
                val updatedDirection = _editDirection.value
                val decimals = uiState.value.currencyDecimals
                
                // Parse decimal string back to Long base units
                val moneyValue = try {
                    val multiplier = 10.0.pow(decimals.toDouble())
                    (_editAmount.value.replace(",", ".").toDouble() * multiplier).toLong()
                } catch (e: Exception) {
                    current.money
                }

                val updated = current.copy(
                    money = moneyValue,
                    note = _editNote.value.takeIf { it.isNotEmpty() },
                    description = _editDescription.value.takeIf { it.isNotEmpty() },
                    categoryId = _editCategoryId.value,
                    walletId = _editWalletId.value,
                    placeId = _editPlaceId.value,
                    eventId = _editEventId.value,
                    direction = updatedDirection,
                    confirmed = _editConfirmed.value,
                    countInTotal = _editCountInTotal.value,
                    lastEdit = System.currentTimeMillis()
                )
                moneyDao.insertTransaction(updated)
                
                // Update People
                moneyDao.deletePeopleForTransaction(transactionId)
                val newPeople = _editPeopleIds.value.map { personId ->
                    TransactionPeopleEntity(
                        transactionId = transactionId,
                        personId = personId,
                        isDeleted = false,
                        lastEdit = System.currentTimeMillis(),
                        id = UUID.randomUUID().toString()
                    )
                }
                moneyDao.insertTransactionPeople(newPeople)
                
                // Note: Attachments saving is not implemented here yet as we don't support editing attachments in this phase.
                
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
