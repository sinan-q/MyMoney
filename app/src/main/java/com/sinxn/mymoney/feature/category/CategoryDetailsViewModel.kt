package com.sinxn.mymoney.feature.category

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import com.sinxn.mymoney.core.data.preferences.SettingsRepository
import com.sinxn.mymoney.core.data.preferences.toFormatterConfig
import com.sinxn.mymoney.core.data.repository.CategoryRepository
import com.sinxn.mymoney.core.ui.components.IconData
import com.sinxn.mymoney.core.ui.components.parseIconData
import com.sinxn.mymoney.core.util.CategoryType
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

sealed class CategoryDetailsEvent {
    object Deleted : CategoryDetailsEvent()
}

@Immutable
data class SubcategoryChipUi(
    val id: String,
    val name: String,
    val iconData: IconData
)

@Immutable
data class CategoryDetailsUiState(
    val categoryId: String = "",
    val category: CategoryEntity? = null,
    val categoryIconData: IconData? = null,
    val parentCategory: CategoryEntity? = null,
    val parentCategoryIconData: IconData? = null,
    val subcategories: List<CategoryEntity> = emptyList(),
    val subcategoriesUi: List<SubcategoryChipUi> = emptyList(),
    val customFields: List<com.sinxn.mymoney.core.data.local.entity.CustomFieldDefinitionEntity> = emptyList(),
    val inheritedCustomFields: List<com.sinxn.mymoney.core.data.local.entity.CustomFieldDefinitionEntity> = emptyList(),
    val allCategories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionWithCategory> = emptyList(),
    val totalExpense: Long = 0L,
    val totalIncome: Long = 0L,
    val currencyCode: String = "USD",
    val decimals: Int = 2,
    val formatterConfig: MoneyFormatter.Config = MoneyFormatter.Config(),
    val dateFormat: Int = 0,
    val isLoading: Boolean = true,
    val isEditDialogOpen: Boolean = false,
    val editName: String = "",
    val editIcon: String = "ic_category",
    val editType: Int = CategoryType.EXPENSE,
    val editParentId: String? = null
)

@HiltViewModel
class CategoryDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
    private val customFieldRepository: com.sinxn.mymoney.core.data.repository.CustomFieldRepository
) : ViewModel() {

    val categoryId: String = checkNotNull(savedStateHandle["categoryId"])

    private val _uiState = MutableStateFlow(CategoryDetailsUiState(categoryId = categoryId))
    val uiState: StateFlow<CategoryDetailsUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<CategoryDetailsEvent>()
    val eventFlow: SharedFlow<CategoryDetailsEvent> = _eventFlow.asSharedFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                categoryRepository.getCategories(),
                categoryRepository.getTransactionsForCategory(categoryId),
                settingsRepository.formattingSettings
            ) { allCategories, transactions, settings ->
                val currentCat = allCategories.find { it.id == categoryId }
                val parentCat = currentCat?.parentId?.let { pId -> allCategories.find { it.id == pId } }
                val subCats = allCategories.filter { it.parentId == categoryId }

                val currentCatIcon = currentCat?.let { parseIconData(it.icon, it.name) }
                val parentCatIcon = parentCat?.let { parseIconData(it.icon, it.name) }
                val subCatsUi = subCats.map { SubcategoryChipUi(it.id, it.name, parseIconData(it.icon, it.name)) }

                val allEffectiveFields = customFieldRepository.getEffectiveFieldsForCategory(categoryId)
                val categoryCustomFields = allEffectiveFields.filter { it.categoryId == categoryId }
                val inheritedFields = allEffectiveFields.filter { it.categoryId != categoryId }

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
                    category = currentCat,
                    categoryIconData = currentCatIcon,
                    parentCategory = parentCat,
                    parentCategoryIconData = parentCatIcon,
                    subcategories = subCats,
                    subcategoriesUi = subCatsUi,
                    customFields = categoryCustomFields,
                    inheritedCustomFields = inheritedFields,
                    allCategories = allCategories,
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

    fun openEditDialog() {
        val cat = _uiState.value.category ?: return
        _uiState.value = _uiState.value.copy(
            isEditDialogOpen = true,
            editName = cat.name,
            editIcon = cat.icon,
            editType = cat.type,
            editParentId = cat.parentId
        )
    }

    fun closeEditDialog() {
        _uiState.value = _uiState.value.copy(isEditDialogOpen = false)
    }

    fun onNameChange(name: String) {
        _uiState.value = _uiState.value.copy(editName = name)
    }

    fun onIconChange(icon: String) {
        _uiState.value = _uiState.value.copy(editIcon = icon)
    }

    fun onTypeChange(type: Int) {
        _uiState.value = _uiState.value.copy(editType = type)
    }

    fun onParentIdChange(parentId: String?) {
        _uiState.value = _uiState.value.copy(editParentId = parentId)
    }

    fun saveCategory() {
        viewModelScope.launch {
            val state = _uiState.value
            val name = state.editName.trim()
            if (name.isEmpty()) return@launch

            categoryRepository.saveCategory(
                id = categoryId,
                name = name,
                icon = state.editIcon,
                type = state.editType,
                parentId = state.editParentId
            )
            closeEditDialog()
        }
    }

    fun toggleArchive() {
        val cat = _uiState.value.category ?: return
        viewModelScope.launch {
            categoryRepository.updateCategoryArchived(categoryId, !cat.isArchived)
        }
    }

    fun deleteCategory() {
        viewModelScope.launch {
            categoryRepository.deleteCategory(categoryId)
            _eventFlow.emit(CategoryDetailsEvent.Deleted)
        }
    }
}
