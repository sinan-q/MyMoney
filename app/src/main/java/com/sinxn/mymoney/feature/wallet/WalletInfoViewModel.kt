package com.sinxn.mymoney.feature.wallet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import com.sinxn.mymoney.core.data.local.model.WalletWithBalance
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.repository.WalletInUseInTransferException
import com.sinxn.mymoney.core.data.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WalletInfoUiState(
    val wallet: WalletWithBalance? = null,
    val transactions: List<TransactionWithCategory> = emptyList(),
    val formattingSettings: FormattingSettings = FormattingSettings(),
    val isLoading: Boolean = true
)

sealed interface WalletInfoEvent {
    object Deleted : WalletInfoEvent
    object DeleteErrorTransferInUse : WalletInfoEvent
}

@HiltViewModel
class WalletInfoViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val walletId: String = checkNotNull(savedStateHandle["walletId"])

    private val _eventFlow = MutableSharedFlow<WalletInfoEvent>()
    val eventFlow: SharedFlow<WalletInfoEvent> = _eventFlow.asSharedFlow()

    val uiState: StateFlow<WalletInfoUiState> = combine(
        walletRepository.getWalletWithBalance(walletId),
        walletRepository.getTransactionsForWallet(walletId),
        settingsRepository.formattingSettings
    ) { wallet, transactions, settings ->
        WalletInfoUiState(
            wallet = wallet,
            transactions = transactions,
            formattingSettings = settings,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WalletInfoUiState()
    )

    fun toggleArchive() {
        val currentWallet = uiState.value.wallet?.wallet ?: return
        viewModelScope.launch {
            walletRepository.updateWalletArchived(walletId, !currentWallet.isArchived)
        }
    }

    fun toggleCountInTotal() {
        val currentWallet = uiState.value.wallet?.wallet ?: return
        viewModelScope.launch {
            walletRepository.updateWalletCountInTotal(walletId, !currentWallet.countInTotal)
        }
    }

    fun deleteWallet() {
        viewModelScope.launch {
            val result = walletRepository.deleteWallet(walletId)
            if (result.isSuccess) {
                _eventFlow.emit(WalletInfoEvent.Deleted)
            } else {
                val ex = result.exceptionOrNull()
                if (ex is WalletInUseInTransferException) {
                    _eventFlow.emit(WalletInfoEvent.DeleteErrorTransferInUse)
                }
            }
        }
    }
}
