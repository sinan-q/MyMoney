package com.sinxn.mymoney.feature.category

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import com.sinxn.mymoney.core.data.repository.CategoryRepository
import com.sinxn.mymoney.core.util.CategoryType
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

data class CategoryDetailsUiState(
    val categoryId: String = "",
    val category: CategoryEntity? = null,
    val parentCategory: CategoryEntity? = null,
    val subcategories: List<CategoryEntity> = emptyList(),
    val allCategories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionWithCategory> = emptyList(),
    val totalExpense: Long = 0L,
    val totalIncome: Long = 0L,
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
    private val categoryRepository: CategoryRepository
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
                categoryRepository.getTransactionsForCategory(categoryId)
            ) { allCategories, transactions ->
                val currentCat = allCategories.find { it.id == categoryId }
                val parentCat = currentCat?.parentId?.let { pId -> allCategories.find { it.id == pId } }
                val subCats = allCategories.filter { it.parentId == categoryId }

                var totalExpense = 0L
                var totalIncome = 0L

                transactions.forEach { item ->
                    if (item.transaction.direction == 0 || item.transaction.type == 0) {
                        totalExpense += item.transaction.money
                    } else if (item.transaction.direction == 1 || item.transaction.type == 1) {
                        totalIncome += item.transaction.money
                    }
                }

                _uiState.value = _uiState.value.copy(
                    category = currentCat,
                    parentCategory = parentCat,
                    subcategories = subCats,
                    allCategories = allCategories,
                    transactions = transactions,
                    totalExpense = totalExpense,
                    totalIncome = totalIncome,
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

    fun deleteCategory() {
        viewModelScope.launch {
            categoryRepository.deleteCategory(categoryId)
            _eventFlow.emit(CategoryDetailsEvent.Deleted)
        }
    }
}
