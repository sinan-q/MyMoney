package com.sinxn.mymoney.feature.template

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.model.TransactionModelWithDetails
import com.sinxn.mymoney.core.data.local.model.TransferModelWithDetails
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.repository.TemplateRepository
import com.sinxn.mymoney.core.ui.components.IconData
import com.sinxn.mymoney.core.ui.components.parseIconData
import com.sinxn.mymoney.core.util.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionTemplateUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val formattedMoney: String,
    val isIncome: Boolean,
    val iconData: IconData,
    val rawItem: TransactionModelWithDetails
)

data class TransferTemplateUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val formattedMoney: String,
    val rawItem: TransferModelWithDetails
)

data class TemplateUiState(
    val transactionTemplates: List<TransactionTemplateUi> = emptyList(),
    val transferTemplates: List<TransferTemplateUi> = emptyList(),
    val totalTxCount: Int = 0,
    val totalTrCount: Int = 0,
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TemplateViewModel @Inject constructor(
    private val templateRepository: TemplateRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    private val templatesFlow = settingsRepository.currentWalletId.flatMapLatest { walletId ->
        combine(
            templateRepository.getTransactionModels(walletId),
            templateRepository.getTransferModels(walletId)
        ) { txModels, trModels ->
            Pair(txModels, trModels)
        }
    }

    val uiState: StateFlow<TemplateUiState> = combine(
        templatesFlow,
        _searchQuery
    ) { (txModels, trModels), searchQuery ->
        val txUiList = txModels.map { item ->
            val isIncome = item.model.direction == 1
            val formattedMoney = (if (isIncome) "+" else "-") + MoneyFormatter.format(
                amount = item.model.money,
                currencyCode = item.walletCurrency,
                decimals = item.walletDecimals
            )
            val title = item.model.description?.takeIf { it.isNotBlank() } ?: (item.categoryName ?: "Template")
            val subtitle = buildString {
                append(item.walletName)
                if (!item.model.tag.isNullOrBlank()) {
                    append(" • ")
                    append(item.model.tag)
                }
            }
            val iconData = parseIconData(item.categoryIcon ?: "ic_category_other", item.categoryName ?: "Category")

            TransactionTemplateUi(
                id = item.model.id,
                title = title,
                subtitle = subtitle,
                formattedMoney = formattedMoney,
                isIncome = isIncome,
                iconData = iconData,
                rawItem = item
            )
        }

        val trUiList = trModels.map { item ->
            val formattedMoney = MoneyFormatter.format(
                amount = item.model.moneyFrom,
                currencyCode = item.walletFromCurrency,
                decimals = item.walletFromDecimals
            )
            val title = item.model.description?.takeIf { it.isNotBlank() } ?: "Transfer"
            val subtitle = "${item.walletFromName} ➔ ${item.walletToName}"

            TransferTemplateUi(
                id = item.model.id,
                title = title,
                subtitle = subtitle,
                formattedMoney = formattedMoney,
                rawItem = item
            )
        }

        val filteredTx = if (searchQuery.isBlank()) txUiList else {
            txUiList.filter { item ->
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.subtitle.contains(searchQuery, ignoreCase = true) ||
                (item.rawItem.model.note?.contains(searchQuery, ignoreCase = true) == true)
            }
        }

        val filteredTr = if (searchQuery.isBlank()) trUiList else {
            trUiList.filter { item ->
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.subtitle.contains(searchQuery, ignoreCase = true) ||
                (item.rawItem.model.note?.contains(searchQuery, ignoreCase = true) == true)
            }
        }

        TemplateUiState(
            transactionTemplates = filteredTx,
            transferTemplates = filteredTr,
            totalTxCount = txUiList.size,
            totalTrCount = trUiList.size,
            searchQuery = searchQuery,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TemplateUiState()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun applyTransactionTemplate(item: TransactionModelWithDetails) {
        viewModelScope.launch {
            templateRepository.applyTransactionModel(item.model.id)
        }
    }

    fun applyTransferTemplate(item: TransferModelWithDetails) {
        viewModelScope.launch {
            templateRepository.applyTransferModel(item.model.id)
        }
    }

    fun deleteTransactionTemplate(item: TransactionModelWithDetails) {
        viewModelScope.launch {
            templateRepository.deleteTransactionModel(item.model.id)
        }
    }

    fun deleteTransferTemplate(item: TransferModelWithDetails) {
        viewModelScope.launch {
            templateRepository.deleteTransferModel(item.model.id)
        }
    }
}
