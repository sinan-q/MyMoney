package com.sinxn.mymoney.feature.recurrence

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.model.RecurrentTransactionWithDetails
import com.sinxn.mymoney.core.data.local.model.RecurrentTransferWithDetails
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.repository.RecurrenceRepository
import com.sinxn.mymoney.core.ui.components.IconData
import com.sinxn.mymoney.core.ui.components.parseIconData
import com.sinxn.mymoney.core.util.DateUtils
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

@Immutable
data class RecurrentTxUiModel(
    val id: String,
    val title: String,
    val subtitle: String?,
    val formattedAmount: String,
    val isIncome: Boolean,
    val nextOccurrenceText: String,
    val iconData: IconData
)

@Immutable
data class RecurrentTransferUiModel(
    val id: String,
    val title: String,
    val subtitle: String?,
    val formattedAmount: String,
    val nextOccurrenceText: String
)

@Immutable
data class RecurrenceUiState(
    val recurrentTransactions: List<RecurrentTxUiModel> = emptyList(),
    val recurrentTransfers: List<RecurrentTransferUiModel> = emptyList(),
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
        val formatterConfig = MoneyFormatter.Config(
            showCurrency = formatting.showCurrency,
            groupDigits = formatting.groupDigits,
            roundDecimals = formatting.roundDecimals,
            showPlusMinus = formatting.showPlusMinus
        )

        val txUiModels = transactions.map { it.toUiModel(formatterConfig, formatting.dateFormat) }
        val trUiModels = transfers.map { it.toUiModel(formatterConfig, formatting.dateFormat) }

        RecurrenceUiState(
            recurrentTransactions = txUiModels,
            recurrentTransfers = trUiModels,
            formatterConfig = formatterConfig,
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

private fun RecurrentTransactionWithDetails.toUiModel(
    formatterConfig: MoneyFormatter.Config,
    dateFormat: Int
): RecurrentTxUiModel {
    val rt = recurrentTransaction
    val isIncome = rt.direction == 1
    val amount = if (isIncome) rt.money else -rt.money
    val decimals = MoneyFormatter.getCurrencyDecimals(wallet.currency)
    val formattedMoney = MoneyFormatter.format(
        amount = amount,
        currencyCode = wallet.currency,
        decimals = decimals,
        config = formatterConfig
    )
    val amountText = (if (isIncome && !formattedMoney.startsWith("+")) "+" else "") + formattedMoney

    val nextOccurrenceText = rt.nextOccurrence?.let { next ->
        val nextDate = DateUtils.parseDate(next)
        DateUtils.formatDate(nextDate, dateFormat)
    } ?: "Finished"

    val iconData = parseIconData(category.icon, category.name)

    return RecurrentTxUiModel(
        id = rt.id,
        title = category.name,
        subtitle = rt.description?.takeIf { it.isNotBlank() },
        formattedAmount = amountText,
        isIncome = isIncome,
        nextOccurrenceText = nextOccurrenceText,
        iconData = iconData
    )
}

private fun RecurrentTransferWithDetails.toUiModel(
    formatterConfig: MoneyFormatter.Config,
    dateFormat: Int
): RecurrentTransferUiModel {
    val rtf = recurrentTransfer
    val decimals = MoneyFormatter.getCurrencyDecimals(walletFrom.currency)
    val formattedMoney = MoneyFormatter.format(
        amount = rtf.moneyFrom,
        currencyCode = walletFrom.currency,
        decimals = decimals,
        config = formatterConfig
    )

    val nextOccurrenceText = rtf.nextOccurrence?.let { next ->
        val nextDate = DateUtils.parseDate(next)
        DateUtils.formatDate(nextDate, dateFormat)
    } ?: "Finished"

    return RecurrentTransferUiModel(
        id = rtf.id,
        title = "${walletFrom.name} → ${walletTo.name}",
        subtitle = rtf.description?.takeIf { it.isNotBlank() },
        formattedAmount = formattedMoney,
        nextOccurrenceText = nextOccurrenceText
    )
}
