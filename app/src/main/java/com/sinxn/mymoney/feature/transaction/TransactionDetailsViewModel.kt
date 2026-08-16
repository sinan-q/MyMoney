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
import com.sinxn.mymoney.core.data.local.entity.TransferEntity
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.repository.TransactionRepository
import com.sinxn.mymoney.core.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Currency
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class TransactionDetailsUiState(
    val transaction: TransactionWithCategory? = null,
    val isLoading: Boolean = true,
    val place: PlaceEntity? = null,
    val event: EventEntity? = null,
    val people: List<PersonEntity> = emptyList(),
    val attachments: List<AttachmentEntity> = emptyList(),
    val availableCategories: List<CategoryEntity> = emptyList(),
    val currencyCode: String = "USD",
    val currencySymbol: String = "$",
    val currencyDecimals: Int = 2,
    val walletName: String = "",
    val targetWalletName: String = "",
    val categoryColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Gray,
    val isTransfer: Boolean = false,
    val transferEntity: TransferEntity? = null
)

@HiltViewModel
class TransactionDetailsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val moneyDao: MoneyDao,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val transactionId: String = checkNotNull(savedStateHandle.get<String>("transactionId"))

    private val transactionDataFlow = combine(
        transactionRepository.getTransactionWithCategory(transactionId),
        transactionRepository.getPeopleForTransaction(transactionId),
        transactionRepository.getAttachmentsForTransaction(transactionId)
    ) { transaction, people, attachments ->
        TransactionData(transaction, people, attachments)
    }

    private val transferFlow = flow {
        emit(transactionRepository.getTransferByTransactionId(transactionId))
    }

    private val listsFlow = combine(
        moneyDao.getWalletsWithBalance(DateUtils.getSQLDateTimeString(Date())),
        moneyDao.getCategories()
    ) { wallets, categories ->
        Pair(wallets, TransactionDetailsUiMapper.flattenCategories(categories))
    }

    val uiState: StateFlow<TransactionDetailsUiState> = combine(
        transactionDataFlow,
        transferFlow,
        listsFlow
    ) { data, transfer, (wallets, categories) ->
        val transaction = data.transaction
        val people = data.people
        val attachments = data.attachments

        val enriched = enrichTransaction(transaction)

        val activeWalletId = transaction?.transaction?.walletId
        val activeWallet = wallets.find { it.wallet.id == activeWalletId }

        val toTx = if (transfer != null) transactionRepository.getTransactionById(transfer.transactionToId) else null
        val targetWallet = if (toTx != null) wallets.find { it.wallet.id == toTx.walletId } else null

        val isTransfer = transfer != null || transaction?.transaction?.direction == 2 || transaction?.transaction?.type == 1 || transaction?.transaction?.type == 2

        TransactionDetailsUiState(
            transaction = transaction,
            isLoading = transaction == null,
            place = enriched.place,
            event = enriched.event,
            people = people,
            attachments = attachments,
            availableCategories = categories,
            currencyCode = activeWallet?.wallet?.currency ?: "USD",
            currencySymbol = try {
                Currency.getInstance(activeWallet?.wallet?.currency ?: "USD").getSymbol(Locale.getDefault())
            } catch (e: Exception) {
                activeWallet?.currencySymbol ?: activeWallet?.wallet?.currency ?: "$"
            },
            currencyDecimals = activeWallet?.decimals ?: 2,
            walletName = activeWallet?.wallet?.name ?: "",
            targetWalletName = targetWallet?.wallet?.name ?: "",
            categoryColor = transaction?.categoryName?.let { TransactionDetailsUiMapper.generateCategoryColor(it) } ?: androidx.compose.ui.graphics.Color.Gray,
            isTransfer = isTransfer,
            transferEntity = transfer
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionDetailsUiState()
    )

    val formattingSettings: StateFlow<FormattingSettings> = settingsRepository.formattingSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FormattingSettings()
        )

    private suspend fun enrichTransaction(transaction: TransactionWithCategory?): EnrichedData {
        if (transaction == null) return EnrichedData()
        val place = transaction.transaction.placeId?.let { moneyDao.getPlaceById(it) }
        val event = transaction.transaction.eventId?.let { moneyDao.getEventById(it) }
        return EnrichedData(place, event)
    }

    fun deleteTransaction(onComplete: () -> Unit) {
        viewModelScope.launch {
            val current = uiState.value.transaction?.transaction ?: return@launch
            val transfer = uiState.value.transferEntity ?: transactionRepository.getTransferByTransactionId(transactionId)
            transactionRepository.deleteTransaction(current, transfer)
            onComplete()
        }
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
}
