package com.sinxn.mymoney.feature.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.data.repository.CategoryRepository
import com.sinxn.mymoney.core.util.CategoryType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryFormState(
    val isOpen: Boolean = false,
    val editingCategory: CategoryEntity? = null,
    val name: String = "",
    val icon: String = "ic_category",
    val type: Int = CategoryType.EXPENSE,
    val parentId: String? = null
)

data class CategoryUiState(
    val categories: List<CategoryEntity> = emptyList(),
    val expenseCategories: List<CategoryEntity> = emptyList(),
    val incomeCategories: List<CategoryEntity> = emptyList(),
    val isLoading: Boolean = false,
    val isEditDialogOpen: Boolean = false,
    val editingCategory: CategoryEntity? = null,
    val editName: String = "",
    val editIcon: String = "ic_category",
    val editType: Int = CategoryType.EXPENSE,
    val editParentId: String? = null
)

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _formState = MutableStateFlow(CategoryFormState())

    val uiState: StateFlow<CategoryUiState> = combine(
        categoryRepository.getCategories(),
        _formState
    ) { categories, form ->
        CategoryUiState(
            categories = categories,
            expenseCategories = categories.filter { it.type == CategoryType.EXPENSE },
            incomeCategories = categories.filter { it.type == CategoryType.INCOME },
            isLoading = false,
            isEditDialogOpen = form.isOpen,
            editingCategory = form.editingCategory,
            editName = form.name,
            editIcon = form.icon,
            editType = form.type,
            editParentId = form.parentId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CategoryUiState()
    )

    fun openCreateCategoryDialog(type: Int = CategoryType.EXPENSE) {
        _formState.value = CategoryFormState(
            isOpen = true,
            editingCategory = null,
            name = "",
            icon = if (type == CategoryType.INCOME) "ic_income" else "ic_expense",
            type = type,
            parentId = null
        )
    }

    fun openEditCategoryDialog(category: CategoryEntity) {
        _formState.value = CategoryFormState(
            isOpen = true,
            editingCategory = category,
            name = category.name,
            icon = category.icon,
            type = category.type,
            parentId = category.parentId
        )
    }

    fun closeDialog() {
        _formState.value = _formState.value.copy(isOpen = false)
    }

    fun onNameChange(name: String) {
        _formState.value = _formState.value.copy(name = name)
    }

    fun onIconChange(icon: String) {
        _formState.value = _formState.value.copy(icon = icon)
    }

    fun onTypeChange(type: Int) {
        _formState.value = _formState.value.copy(type = type)
    }

    fun onParentIdChange(parentId: String?) {
        _formState.value = _formState.value.copy(parentId = parentId)
    }

    fun saveCategory() {
        viewModelScope.launch {
            val form = _formState.value
            val name = form.name.trim()
            if (name.isEmpty()) return@launch

            categoryRepository.saveCategory(
                id = form.editingCategory?.id,
                name = name,
                icon = form.icon,
                type = form.type,
                parentId = form.parentId
            )
            closeDialog()
        }
    }

    fun reorderCategories(orderedCategoryIds: List<String>) {
        viewModelScope.launch {
            categoryRepository.reorderCategories(orderedCategoryIds)
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(category.id)
        }
    }
}
