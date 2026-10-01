package com.sinxn.mymoney.feature.category

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.entity.CustomFieldDefinitionEntity
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.repository.CustomFieldRepository
import com.sinxn.mymoney.core.util.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomFieldValueTransactionsUiState(
    val fieldId: String = "",
    val normalizedValue: String = "",
    val fieldDefinition: CustomFieldDefinitionEntity? = null,
    val transactions: List<TransactionWithCategory> = emptyList(),
    val currencyCode: String = "USD",
    val decimals: Int = 2,
    val formatterConfig: MoneyFormatter.Config = MoneyFormatter.Config(),
    val dateFormat: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class CustomFieldValueTransactionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val customFieldRepository: CustomFieldRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val fieldId: String = checkNotNull(savedStateHandle["fieldId"])
    val normalizedValue: String = checkNotNull(savedStateHandle["normalizedValue"])

    private val _uiState = MutableStateFlow(CustomFieldValueTransactionsUiState(fieldId = fieldId, normalizedValue = normalizedValue))
    val uiState: StateFlow<CustomFieldValueTransactionsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val definition = customFieldRepository.getDefinitionById(fieldId)
            
            combine(
                customFieldRepository.getTransactionsForCustomFieldValue(fieldId, normalizedValue),
                settingsRepository.formattingSettings
            ) { transactions, settings ->

                val transactionCurrencies = transactions.mapNotNull { it.currencySymbol ?: it.currencyCode }.distinct()
                val displayCurrency = if (transactionCurrencies.size == 1) transactionCurrencies.first() else settings.globalCurrency
                val displayDecimals = transactions.firstOrNull()?.decimals ?: 2

                val formatterConfig = MoneyFormatter.Config(
                    showCurrency = settings.showCurrency,
                    groupDigits = settings.groupDigits,
                    roundDecimals = settings.roundDecimals,
                    showPlusMinus = settings.showPlusMinus
                )

                _uiState.value = _uiState.value.copy(
                    fieldDefinition = definition,
                    transactions = transactions,
                    currencyCode = displayCurrency,
                    decimals = displayDecimals,
                    formatterConfig = formatterConfig,
                    dateFormat = settings.dateFormat,
                    isLoading = false
                )
            }.collect {}
        }
    }
}
