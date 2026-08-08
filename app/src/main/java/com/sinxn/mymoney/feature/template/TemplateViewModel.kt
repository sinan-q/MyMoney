package com.sinxn.mymoney.feature.template

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.data.local.entity.TransactionModelEntity
import com.sinxn.mymoney.core.data.local.entity.TransferModelEntity
import com.sinxn.mymoney.core.data.local.entity.WalletEntity
import com.sinxn.mymoney.core.data.local.model.TransactionModelWithDetails
import com.sinxn.mymoney.core.data.local.model.TransferModelWithDetails
import com.sinxn.mymoney.core.data.repository.CategoryRepository
import com.sinxn.mymoney.core.data.repository.TemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import kotlin.math.pow

data class TemplateFormState(
    val isOpen: Boolean = false,
    val isTransfer: Boolean = false,
    val editingTxModel: TransactionModelEntity? = null,
    val editingTransferModel: TransferModelEntity? = null,
    val amount: String = "",
    val description: String = "",
    val categoryId: String = "",
    val walletId: String = "",
    val targetWalletId: String = "",
    val direction: Int = 0
)

data class TemplateUiState(
    val transactionTemplates: List<TransactionModelWithDetails> = emptyList(),
    val transferTemplates: List<TransferModelWithDetails> = emptyList(),
    val availableWallets: List<WalletEntity> = emptyList(),
    val availableCategories: List<CategoryEntity> = emptyList(),
    val isLoading: Boolean = true,
    val isFormOpen: Boolean = false,
    val isTransferForm: Boolean = false,
    val editAmount: String = "",
    val editDescription: String = "",
    val editCategoryId: String = "",
    val editWalletId: String = "",
    val editTargetWalletId: String = "",
    val editDirection: Int = 0
)

@HiltViewModel
class TemplateViewModel @Inject constructor(
    private val templateRepository: TemplateRepository,
    private val moneyDao: MoneyDao,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _formState = MutableStateFlow(TemplateFormState())

    val uiState: StateFlow<TemplateUiState> = combine(
        templateRepository.getTransactionModels(),
        templateRepository.getTransferModels(),
        moneyDao.getWallets(),
        categoryRepository.getCategories(),
        _formState
    ) { txModels, trModels, wallets, categories, form ->
        TemplateUiState(
            transactionTemplates = txModels,
            transferTemplates = trModels,
            availableWallets = wallets,
            availableCategories = categories,
            isLoading = false,
            isFormOpen = form.isOpen,
            isTransferForm = form.isTransfer,
            editAmount = form.amount,
            editDescription = form.description,
            editCategoryId = form.categoryId,
            editWalletId = form.walletId,
            editTargetWalletId = form.targetWalletId,
            editDirection = form.direction
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TemplateUiState()
    )

    fun openCreateTxTemplateDialog(isTransfer: Boolean = false) {
        val firstWallet = uiState.value.availableWallets.firstOrNull()?.id ?: ""
        val secondWallet = uiState.value.availableWallets.getOrNull(1)?.id ?: firstWallet
        val firstCat = uiState.value.availableCategories.firstOrNull()?.id ?: ""

        _formState.value = TemplateFormState(
            isOpen = true,
            isTransfer = isTransfer,
            amount = "",
            description = "",
            categoryId = firstCat,
            walletId = firstWallet,
            targetWalletId = secondWallet,
            direction = if (isTransfer) 2 else 0
        )
    }

    fun openEditTxTemplateDialog(item: TransactionModelWithDetails) {
        val decimals = item.walletDecimals
        val amountStr = (item.model.money.toDouble() / 10.0.pow(decimals.toDouble())).toString()

        _formState.value = TemplateFormState(
            isOpen = true,
            isTransfer = false,
            editingTxModel = item.model,
            amount = amountStr,
            description = item.model.description ?: "",
            categoryId = item.model.categoryId,
            walletId = item.model.walletId,
            direction = item.model.direction
        )
    }

    fun openEditTransferTemplateDialog(item: TransferModelWithDetails) {
        val amountStr = (item.model.moneyFrom.toDouble() / 100.0).toString()

        _formState.value = TemplateFormState(
            isOpen = true,
            isTransfer = true,
            editingTransferModel = item.model,
            amount = amountStr,
            description = item.model.description ?: "",
            walletId = item.model.walletFromId,
            targetWalletId = item.model.walletToId,
            direction = 2
        )
    }

    fun closeDialog() {
        _formState.value = _formState.value.copy(isOpen = false)
    }

    fun onAmountChange(v: String) { _formState.value = _formState.value.copy(amount = v) }
    fun onDescriptionChange(v: String) { _formState.value = _formState.value.copy(description = v) }
    fun onCategoryChange(v: String) { _formState.value = _formState.value.copy(categoryId = v) }
    fun onWalletChange(v: String) { _formState.value = _formState.value.copy(walletId = v) }
    fun onTargetWalletChange(v: String) { _formState.value = _formState.value.copy(targetWalletId = v) }
    fun onDirectionChange(v: Int) { _formState.value = _formState.value.copy(direction = v) }

    fun saveTemplate() {
        viewModelScope.launch {
            val form = _formState.value
            val now = System.currentTimeMillis()
            val money = (form.amount.toDoubleOrNull() ?: 0.0) * 100.0

            if (form.isTransfer) {
                val model = TransferModelEntity(
                    id = form.editingTransferModel?.id ?: UUID.randomUUID().toString(),
                    description = form.description.takeIf { it.isNotBlank() },
                    walletFromId = form.walletId,
                    walletToId = form.targetWalletId,
                    moneyFrom = money.toLong(),
                    moneyTo = money.toLong(),
                    moneyTax = null,
                    note = null,
                    eventId = null,
                    placeId = null,
                    confirmed = true,
                    countInTotal = true,
                    isDeleted = false,
                    lastEdit = now,
                    tag = null
                )
                templateRepository.saveTransferModel(model)
            } else {
                val model = TransactionModelEntity(
                    id = form.editingTxModel?.id ?: UUID.randomUUID().toString(),
                    money = money.toLong(),
                    description = form.description.takeIf { it.isNotBlank() },
                    categoryId = form.categoryId,
                    direction = form.direction,
                    walletId = form.walletId,
                    placeId = null,
                    note = null,
                    eventId = null,
                    confirmed = true,
                    countInTotal = true,
                    isDeleted = false,
                    lastEdit = now,
                    tag = null
                )
                templateRepository.saveTransactionModel(model)
            }
            closeDialog()
        }
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
