package com.sinxn.mymoney.feature.debt

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.model.DebtWithDetails
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.repository.DebtRepository
import com.sinxn.mymoney.core.util.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.pow

data class DebtDetailsUiState(
    val debtDetails: DebtWithDetails? = null,
    val transactions: List<TransactionWithCategory> = emptyList(),
    val isLoading: Boolean = true,
    val currencyCode: String = "USD",
    val currencySymbol: String = "$",
    val currencyDecimals: Int = 2,
    val decimals: Int = 2,
    val formatterConfig: MoneyFormatter.Config = MoneyFormatter.Config(),
    val dateFormat: Int = 0
)

@HiltViewModel
class DebtDetailsViewModel @Inject constructor(
    private val debtRepository: DebtRepository,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val debtId: String = checkNotNull(savedStateHandle.get<String>("debtId"))

    private val debtDetailsFlow = debtRepository.getDebtDetails(debtId)
    private val transactionsFlow = debtRepository.getTransactionsForDebt(debtId)
    private val formattingFlow = settingsRepository.formattingSettings

    val uiState: StateFlow<DebtDetailsUiState> = combine(
        debtDetailsFlow,
        transactionsFlow,
        formattingFlow
    ) { debtDetails, txList, formatting ->
        val transactionCurrencies = txList.mapNotNull { it.currencySymbol ?: it.currencyCode }.distinct()
        val displayCurrency = debtDetails?.walletCurrency
            ?: if (transactionCurrencies.size == 1) transactionCurrencies.first() else formatting.globalCurrency
        val displayDecimals = debtDetails?.walletDecimals ?: txList.firstOrNull()?.decimals ?: 2

        val formatterConfig = MoneyFormatter.Config(
            showCurrency = formatting.showCurrency,
            groupDigits = formatting.groupDigits,
            roundDecimals = formatting.roundDecimals,
            showPlusMinus = formatting.showPlusMinus
        )

        DebtDetailsUiState(
            debtDetails = debtDetails,
            transactions = txList,
            isLoading = debtDetails == null,
            currencyCode = displayCurrency,
            currencySymbol = "$",
            currencyDecimals = displayDecimals,
            decimals = displayDecimals,
            formatterConfig = formatterConfig,
            dateFormat = formatting.dateFormat
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DebtDetailsUiState()
    )

    fun toggleArchived() {
        val current = uiState.value.debtDetails?.debt?.isArchived ?: return
        viewModelScope.launch {
            debtRepository.setDebtArchived(debtId, !current)
        }
    }

    fun deleteDebt(deleteTransactions: Boolean = true, onSuccess: () -> Unit) {
        viewModelScope.launch {
            debtRepository.deleteDebt(debtId, deleteTransactions)
            onSuccess()
        }
    }

    fun addPayment(
        amount: Double,
        walletId: String,
        description: String? = null,
        note: String? = null,
        onSuccess: () -> Unit
    ) {
        val decimals = uiState.value.currencyDecimals
        val moneyCents = (amount * 10.0.pow(decimals)).toLong()

        viewModelScope.launch {
            debtRepository.addDebtPayment(
                debtId = debtId,
                amount = moneyCents,
                walletId = walletId,
                description = description,
                note = note
            )
            onSuccess()
        }
    }
}
