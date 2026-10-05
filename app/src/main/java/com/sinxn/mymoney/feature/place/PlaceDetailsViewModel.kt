package com.sinxn.mymoney.feature.place

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.entity.PlaceEntity
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.preferences.toFormatterConfig
import com.sinxn.mymoney.core.data.repository.PlaceRepository
import com.sinxn.mymoney.core.util.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PlaceDetailsEvent {
    object Deleted : PlaceDetailsEvent()
}

data class PlaceDetailsUiState(
    val placeId: String = "",
    val place: PlaceEntity? = null,
    val transactions: List<TransactionWithCategory> = emptyList(),
    val totalExpense: Long = 0L,
    val totalIncome: Long = 0L,
    val currencyCode: String = "USD",
    val decimals: Int = 2,
    val formatterConfig: MoneyFormatter.Config = MoneyFormatter.Config(),
    val dateFormat: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class PlaceDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val placeRepository: PlaceRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val placeId: String = checkNotNull(savedStateHandle["placeId"])

    private val _uiState = MutableStateFlow(PlaceDetailsUiState(placeId = placeId))
    val uiState: StateFlow<PlaceDetailsUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<PlaceDetailsEvent>()
    val eventFlow: SharedFlow<PlaceDetailsEvent> = _eventFlow.asSharedFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                placeRepository.getPlaces(),
                placeRepository.getTransactionsForPlace(placeId),
                settingsRepository.formattingSettings
            ) { allPlaces, transactions, settings ->
                val currentPlace = allPlaces.find { it.id == placeId }

                var totalExpense = 0L
                var totalIncome = 0L

                transactions.forEach { item ->
                    if (item.transaction.direction == 1) {
                        totalIncome += item.transaction.money
                    } else {
                        totalExpense += item.transaction.money
                    }
                }

                val transactionCurrencies = transactions.mapNotNull { it.currencySymbol ?: it.currencyCode }.distinct()
                val displayCurrency = if (transactionCurrencies.size == 1) transactionCurrencies.first() else settings.globalCurrency
                val displayDecimals = transactions.firstOrNull()?.decimals ?: 2

                val formatterConfig = settings.toFormatterConfig()

                _uiState.value = _uiState.value.copy(
                    place = currentPlace,
                    transactions = transactions,
                    totalExpense = totalExpense,
                    totalIncome = totalIncome,
                    currencyCode = displayCurrency,
                    decimals = displayDecimals,
                    formatterConfig = formatterConfig,
                    dateFormat = settings.dateFormat,
                    isLoading = false
                )
            }.collect {}
        }
    }

    fun toggleArchive() {
        val currentPlace = _uiState.value.place ?: return
        viewModelScope.launch {
            placeRepository.updatePlaceArchived(placeId, !currentPlace.isArchived)
        }
    }

    fun deletePlace() {
        viewModelScope.launch {
            placeRepository.deletePlace(placeId)
            _eventFlow.emit(PlaceDetailsEvent.Deleted)
        }
    }
}
