package com.sinxn.mymoney.feature.recurrence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.model.RecurrentTransactionWithDetails
import com.sinxn.mymoney.core.data.local.model.RecurrentTransferWithDetails
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.repository.RecurrenceRepository
import com.sinxn.mymoney.core.util.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecurrenceUiState(
    val recurrentTransactions: List<RecurrentTransactionWithDetails> = emptyList(),
    val recurrentTransfers: List<RecurrentTransferWithDetails> = emptyList(),
    val formatterConfig: MoneyFormatter.Config = MoneyFormatter.Config(),
    val dateFormat: Int = 3
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecurrenceViewModel @Inject constructor(
    private val recurrenceRepository: RecurrenceRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val recurrencesFlow = settingsRepository.currentWalletId.flatMapLatest { walletId ->
        combine(
            recurrenceRepository.getRecurrentTransactions(walletId),
            recurrenceRepository.getRecurrentTransfers(walletId)
        ) { txs, trs -> Pair(txs, trs) }
    }

    val uiState: StateFlow<RecurrenceUiState> = combine(
        recurrencesFlow,
        settingsRepository.formattingSettings
    ) { (transactions, transfers), formatting ->
        RecurrenceUiState(
            recurrentTransactions = transactions,
            recurrentTransfers = transfers,
            formatterConfig = MoneyFormatter.Config(
                showCurrency = formatting.showCurrency,
                groupDigits = formatting.groupDigits,
                roundDecimals = formatting.roundDecimals,
                showPlusMinus = formatting.showPlusMinus
            ),
            dateFormat = formatting.dateFormat
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        RecurrenceUiState()
    )

    val recurrentTransactions: StateFlow<List<RecurrentTransactionWithDetails>> =
        settingsRepository.currentWalletId.flatMapLatest { walletId ->
            recurrenceRepository.getRecurrentTransactions(walletId)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val recurrentTransfers: StateFlow<List<RecurrentTransferWithDetails>> =
        settingsRepository.currentWalletId.flatMapLatest { walletId ->
            recurrenceRepository.getRecurrentTransfers(walletId)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun deleteRecurrentTransaction(id: String) {
        viewModelScope.launch {
            recurrenceRepository.deleteRecurrentTransaction(id)
        }
    }

    fun deleteRecurrentTransfer(id: String) {
        viewModelScope.launch {
            recurrenceRepository.deleteRecurrentTransfer(id)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            recurrenceRepository.processPendingRecurrences()
        }
    }
}
