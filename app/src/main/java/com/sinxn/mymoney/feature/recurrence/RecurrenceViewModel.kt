package com.sinxn.mymoney.feature.recurrence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.model.RecurrentTransactionWithDetails
import com.sinxn.mymoney.core.data.local.model.RecurrentTransferWithDetails
import com.sinxn.mymoney.core.data.repository.RecurrenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecurrenceViewModel @Inject constructor(
    private val recurrenceRepository: RecurrenceRepository
) : ViewModel() {

    val recurrentTransactions: StateFlow<List<RecurrentTransactionWithDetails>> =
        recurrenceRepository.recurrentTransactions.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val recurrentTransfers: StateFlow<List<RecurrentTransferWithDetails>> =
        recurrenceRepository.recurrentTransfers.stateIn(
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
