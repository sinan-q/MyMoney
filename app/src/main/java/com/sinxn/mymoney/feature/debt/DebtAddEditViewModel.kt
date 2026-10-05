package com.sinxn.mymoney.feature.debt

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.entity.PersonEntity
import com.sinxn.mymoney.core.data.local.entity.PlaceEntity
import com.sinxn.mymoney.core.data.local.entity.WalletEntity
import com.sinxn.mymoney.core.data.local.model.DebtWithDetails
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.preferences.toFormatterConfig
import com.sinxn.mymoney.core.data.repository.DebtRepository
import com.sinxn.mymoney.core.util.Constants
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MathExpressionEvaluator
import com.sinxn.mymoney.core.util.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import kotlin.math.pow

data class DebtAddEditUiState(
    val debtId: String? = null,
    val isNewDebt: Boolean = true,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    // Form fields
    val editType: Int = 0, // 0: DEBT, 1: CREDIT
    val editDescription: String = "",
    val editAmount: String = "",
    val editWalletId: String = "",
    val editPlaceId: String? = null,
    val editDate: String = "",
    val editExpirationDate: String? = null,
    val editIcon: String = "ic_debt",
    val editNote: String = "",
    val editPeopleIds: Set<String> = emptySet(),
    val editInsertMasterTransaction: Boolean = true,
    // Available selectors
    val availableWallets: List<WalletEntity> = emptyList(),
    val availablePlaces: List<PlaceEntity> = emptyList(),
    val availablePeople: List<PersonEntity> = emptyList(),
    // Currency info
    val currencyCode: String = "USD",
    val currencySymbol: String = "$",
    val currencyDecimals: Int = 2,
    val decimals: Int = 2,
    val formatterConfig: MoneyFormatter.Config = MoneyFormatter.Config(),
    val dateFormat: Int = 0,
    val formattingSettings: FormattingSettings = FormattingSettings()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DebtAddEditViewModel @Inject constructor(
    private val debtRepository: DebtRepository,
    private val moneyDao: MoneyDao,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val debtIdArg: String? = savedStateHandle.get<String>("debtId")?.takeIf { it.isNotBlank() && it != "new" }
    private val initialType: Int = savedStateHandle.get<Int>("type") ?: 0
    val isNewDebt = debtIdArg == null

    private val _isSaving = MutableStateFlow(false)

    // Form states
    private val _editType = MutableStateFlow(initialType)
    private val _editDescription = MutableStateFlow("")
    private val _editAmount = MutableStateFlow("")
    private val _editWalletId = MutableStateFlow("")
    private val _editPlaceId = MutableStateFlow<String?>(null)
    private val _editDate = MutableStateFlow(DateUtils.getSQLDateTimeString(Date()))
    private val _editExpirationDate = MutableStateFlow<String?>(null)
    private val _editIcon = MutableStateFlow(if (initialType == 1) "ic_credit" else "ic_debt")
    private val _editNote = MutableStateFlow("")
    private val _editPeopleIds = MutableStateFlow<Set<String>>(emptySet())
    private val _editInsertMasterTransaction = MutableStateFlow(true)

    init {
        if (isNewDebt) {
            viewModelScope.launch {
                val currentWId = settingsRepository.currentWalletId.first()
                if (currentWId != Constants.TOTAL_WALLET_ID) {
                    _editWalletId.value = currentWId
                }
            }
        }
    }

    private val existingDebtFlow = if (debtIdArg != null) {
        debtRepository.getDebtDetails(debtIdArg)
    } else {
        flowOf(null)
    }

    fun getImmediateResult(editAmount: String): String {
        return MathExpressionEvaluator.getImmediateResult(editAmount, uiState.value.currencyDecimals)
    }

    private val formStateFlow = combine(
        combine(_editType, _editDescription, _editAmount, _editWalletId, _editPlaceId) { type, desc, amt, wId, pId ->
            Tuple5(type, desc, amt, wId, pId)
        },
        combine(_editDate, _editExpirationDate, _editIcon, _editNote, _editPeopleIds) { date, expDate, icon, note, pIds ->
            Tuple5(date, expDate, icon, note, pIds)
        },
        _editInsertMasterTransaction
    ) { part1, part2, insertMaster ->
        FormTuple(
            type = part1.t1,
            description = part1.t2,
            amount = part1.t3,
            walletId = part1.t4,
            placeId = part1.t5,
            date = part2.t1,
            expirationDate = part2.t2,
            icon = part2.t3,
            note = part2.t4,
            peopleIds = part2.t5,
            insertMasterTransaction = insertMaster
        )
    }

    private val selectorsFlow = combine(
        moneyDao.getWallets(),
        moneyDao.getPlaces(),
        moneyDao.getPeople(),
        settingsRepository.formattingSettings
    ) { wallets, places, people, formatting ->
        SelectorsTuple(wallets, places, people, formatting)
    }

    val uiState: StateFlow<DebtAddEditUiState> = combine(
        existingDebtFlow,
        _isSaving,
        formStateFlow,
        selectorsFlow
    ) { flows: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val existingDebt = flows[0] as DebtWithDetails?
        val isSaving = flows[1] as Boolean
        val form = flows[2] as FormTuple
        val selectors = flows[3] as SelectorsTuple

        // Auto-populate form once existing debt is loaded
        if (debtIdArg != null && existingDebt != null && _editWalletId.value.isEmpty()) {
            val decimals = existingDebt.walletDecimals ?: 2
            val amountStr = (existingDebt.debt.money / 10.0.pow(decimals)).toString()
            _editType.value = existingDebt.debt.type
            _editDescription.value = existingDebt.debt.description
            _editAmount.value = amountStr
            _editWalletId.value = existingDebt.debt.walletId
            _editPlaceId.value = existingDebt.debt.placeId
            _editDate.value = existingDebt.debt.date
            _editExpirationDate.value = existingDebt.debt.expirationDate
            _editIcon.value = existingDebt.debt.icon
            _editNote.value = existingDebt.debt.note ?: ""
            _editPeopleIds.value = existingDebt.people.map { it.id }.toSet()
        }

        val effectiveWalletId = if (form.walletId.isEmpty() && selectors.wallets.isNotEmpty()) {
            val firstWallet = selectors.wallets.first().id
            _editWalletId.value = firstWallet
            firstWallet
        } else {
            form.walletId
        }

        val selectedWallet = selectors.wallets.firstOrNull { it.id == effectiveWalletId }
        val displayCurrency = selectedWallet?.currency ?: existingDebt?.walletCurrency ?: selectors.formatting.globalCurrency
        val displayDecimals = existingDebt?.walletDecimals ?: 2

        val formatterConfig = selectors.formatting.toFormatterConfig()

        DebtAddEditUiState(
            debtId = debtIdArg,
            isNewDebt = isNewDebt,
            isLoading = debtIdArg != null && existingDebt == null,
            isSaving = isSaving,
            editType = form.type,
            editDescription = form.description,
            editAmount = form.amount,
            editWalletId = effectiveWalletId,
            editPlaceId = form.placeId,
            editDate = form.date,
            editExpirationDate = form.expirationDate,
            editIcon = form.icon,
            editNote = form.note,
            editPeopleIds = form.peopleIds,
            editInsertMasterTransaction = form.insertMasterTransaction,
            availableWallets = selectors.wallets,
            availablePlaces = selectors.places,
            availablePeople = selectors.people,
            currencyCode = displayCurrency,
            currencySymbol = MoneyFormatter.getCurrencySymbol(displayCurrency),
            currencyDecimals = displayDecimals,
            decimals = displayDecimals,
            formatterConfig = formatterConfig,
            dateFormat = selectors.formatting.dateFormat,
            formattingSettings = selectors.formatting
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DebtAddEditUiState()
    )

    fun updateType(type: Int) {
        _editType.value = type
        if (_editIcon.value == "ic_debt" || _editIcon.value == "ic_credit") {
            _editIcon.value = if (type == 1) "ic_credit" else "ic_debt"
        }
    }

    fun updateDescription(desc: String) { _editDescription.value = desc }
    fun updateAmount(amount: String) { _editAmount.value = amount }
    fun updateWalletId(walletId: String) { _editWalletId.value = walletId }
    fun updatePlaceId(placeId: String?) { _editPlaceId.value = placeId }
    fun updateDate(date: String) { _editDate.value = date }
    fun updateExpirationDate(expDate: String?) { _editExpirationDate.value = expDate }
    fun updateIcon(icon: String) { _editIcon.value = icon }
    fun updateNote(note: String) { _editNote.value = note }
    fun updateInsertMasterTransaction(insert: Boolean) { _editInsertMasterTransaction.value = insert }

    fun togglePersonSelection(personId: String) {
        val current = _editPeopleIds.value.toMutableSet()
        if (current.contains(personId)) {
            current.remove(personId)
        } else {
            current.add(personId)
        }
        _editPeopleIds.value = current
    }

    fun onNumpadKeyPress(key: String) {
        _editAmount.value = MathExpressionEvaluator.processNumpadKeyPress(_editAmount.value, key)
    }

    fun evaluateMathExpression() {
        val decimals = uiState.value.currencyDecimals
        _editAmount.value = MathExpressionEvaluator.evaluateMathExpression(_editAmount.value, decimals)
    }

    fun saveDebt(onSuccess: (String) -> Unit) {
        val currentState = uiState.value
        val evaluatedStr = MathExpressionEvaluator.getImmediateResult(currentState.editAmount, currentState.currencyDecimals)
        val amountDouble = evaluatedStr.toDoubleOrNull() ?: currentState.editAmount.toDoubleOrNull() ?: 0.0
        val moneyCents = (amountDouble * 10.0.pow(currentState.currencyDecimals)).toLong()

        viewModelScope.launch {
            _isSaving.value = true
            try {
                if (isNewDebt) {
                    val newId = debtRepository.createDebt(
                        type = currentState.editType,
                        icon = currentState.editIcon,
                        description = currentState.editDescription,
                        date = currentState.editDate,
                        expirationDate = currentState.editExpirationDate,
                        walletId = currentState.editWalletId,
                        placeId = currentState.editPlaceId,
                        money = moneyCents,
                        note = currentState.editNote.ifBlank { null },
                        peopleIds = currentState.editPeopleIds,
                        insertMasterTransaction = currentState.editInsertMasterTransaction
                    )
                    onSuccess(newId)
                } else {
                    val currentDebtId = debtIdArg ?: return@launch
                    debtRepository.updateDebt(
                        debtId = currentDebtId,
                        type = currentState.editType,
                        icon = currentState.editIcon,
                        description = currentState.editDescription,
                        date = currentState.editDate,
                        expirationDate = currentState.editExpirationDate,
                        walletId = currentState.editWalletId,
                        placeId = currentState.editPlaceId,
                        money = moneyCents,
                        note = currentState.editNote.ifBlank { null },
                        peopleIds = currentState.editPeopleIds
                    )
                    onSuccess(currentDebtId)
                }
            } finally {
                _isSaving.value = false
            }
        }
    }
}

private data class Tuple5<T1, T2, T3, T4, T5>(
    val t1: T1, val t2: T2, val t3: T3, val t4: T4, val t5: T5
)

private data class FormTuple(
    val type: Int,
    val description: String,
    val amount: String,
    val walletId: String,
    val placeId: String?,
    val date: String,
    val expirationDate: String?,
    val icon: String,
    val note: String,
    val peopleIds: Set<String>,
    val insertMasterTransaction: Boolean
)

private data class SelectorsTuple(
    val wallets: List<WalletEntity>,
    val places: List<PlaceEntity>,
    val people: List<PersonEntity>,
    val formatting: FormattingSettings
)
