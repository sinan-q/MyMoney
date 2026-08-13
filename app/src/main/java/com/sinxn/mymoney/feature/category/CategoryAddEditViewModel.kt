package com.sinxn.mymoney.feature.category

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.data.repository.CategoryRepository
import com.sinxn.mymoney.core.util.CategoryType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CategoryAddEditEvent {
    object Saved : CategoryAddEditEvent()
    object Deleted : CategoryAddEditEvent()
}

data class CategoryAddEditUiState(
    val categoryId: String? = null,
    val name: String = "",
    val icon: String = "ic_category",
    val type: Int = CategoryType.EXPENSE,
    val parentId: String? = null,
    val showReport: Boolean = true,
    val isSystemCategory: Boolean = false,
    val tag: String? = null,
    val allCategories: List<CategoryEntity> = emptyList(),
    val availableParentCategories: List<CategoryEntity> = emptyList(),
    val isLoading: Boolean = true,
    val isEditMode: Boolean = false
)

@HiltViewModel
class CategoryAddEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    val navCategoryId: String? = savedStateHandle.get<String>("categoryId")?.takeIf { it.isNotBlank() }
    private val navType: Int? = savedStateHandle.get<String>("type")?.toIntOrNull()
    private val navParentId: String? = savedStateHandle.get<String>("parentId")?.takeIf { it.isNotBlank() }

    private val _uiState = MutableStateFlow(
        CategoryAddEditUiState(
            categoryId = navCategoryId,
            isEditMode = navCategoryId != null
        )
    )
    val uiState: StateFlow<CategoryAddEditUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<CategoryAddEditEvent>()
    val eventFlow: SharedFlow<CategoryAddEditEvent> = _eventFlow.asSharedFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val allCategories = categoryRepository.getCategories().first()
            val isEditMode = navCategoryId != null
            
            if (isEditMode && navCategoryId != null) {
                val existingCategory = categoryRepository.getCategoryById(navCategoryId)
                if (existingCategory != null) {
                    val availableParents = filterParentCategories(
                        allCategories = allCategories,
                        currentCategoryId = navCategoryId,
                        currentType = existingCategory.type
                    )
                    _uiState.value = _uiState.value.copy(
                        categoryId = existingCategory.id,
                        name = existingCategory.name,
                        icon = existingCategory.icon,
                        type = existingCategory.type,
                        parentId = existingCategory.parentId,
                        showReport = existingCategory.showReport,
                        isSystemCategory = existingCategory.tag != null || existingCategory.type == CategoryType.SYSTEM,
                        tag = existingCategory.tag,
                        allCategories = allCategories,
                        availableParentCategories = availableParents,
                        isLoading = false,
                        isEditMode = true
                    )
                    return@launch
                }
            }

            // Creating a new category
            var selectedType = navType ?: CategoryType.EXPENSE
            var selectedParentId = navParentId

            if (selectedParentId != null) {
                val parentCat = allCategories.find { it.id == selectedParentId }
                if (parentCat != null) {
                    selectedType = parentCat.type
                } else {
                    selectedParentId = null
                }
            }

            val defaultIcon = if (selectedType == CategoryType.INCOME) "ic_income" else "ic_expense"
            val availableParents = filterParentCategories(
                allCategories = allCategories,
                currentCategoryId = null,
                currentType = selectedType
            )

            _uiState.value = _uiState.value.copy(
                name = "",
                icon = defaultIcon,
                type = selectedType,
                parentId = selectedParentId,
                showReport = true,
                isSystemCategory = false,
                tag = null,
                allCategories = allCategories,
                availableParentCategories = availableParents,
                isLoading = false,
                isEditMode = false
            )
        }
    }

    private fun filterParentCategories(
        allCategories: List<CategoryEntity>,
        currentCategoryId: String?,
        currentType: Int
    ): List<CategoryEntity> {
        val excludeIds = mutableSetOf<String>()
        if (currentCategoryId != null) {
            excludeIds.add(currentCategoryId)
            // Recursively collect all descendant category IDs to prevent circular parent references
            fun collectDescendants(parentId: String) {
                val children = allCategories.filter { it.parentId == parentId }
                for (child in children) {
                    if (excludeIds.add(child.id)) {
                        collectDescendants(child.id)
                    }
                }
            }
            collectDescendants(currentCategoryId)
        }

        return allCategories.filter { category ->
            category.parentId == null && // Only top-level categories can be parents
            category.id !in excludeIds &&
            category.type == currentType &&
            category.tag == null // Exclude system categories from parent selection
        }
    }

    fun onNameChange(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun onIconChange(icon: String) {
        _uiState.value = _uiState.value.copy(icon = icon)
    }

    fun onTypeChange(type: Int) {
        val current = _uiState.value
        if (current.isSystemCategory) return

        var newParentId = current.parentId
        if (newParentId != null) {
            val parentCat = current.allCategories.find { it.id == newParentId }
            if (parentCat?.type != type) {
                newParentId = null
            }
        }

        val availableParents = filterParentCategories(
            allCategories = current.allCategories,
            currentCategoryId = current.categoryId,
            currentType = type
        )

        // Adjust icon if it was default
        val newIcon = if (current.icon == "ic_expense" || current.icon == "ic_income" || current.icon == "ic_category") {
            if (type == CategoryType.INCOME) "ic_income" else "ic_expense"
        } else {
            current.icon
        }

        _uiState.value = current.copy(
            type = type,
            parentId = newParentId,
            icon = newIcon,
            availableParentCategories = availableParents
        )
    }

    fun onParentIdChange(parentId: String?) {
        val current = _uiState.value
        if (current.isSystemCategory) return

        var updatedType = current.type
        if (parentId != null) {
            val parentCat = current.allCategories.find { it.id == parentId }
            if (parentCat != null) {
                updatedType = parentCat.type
            }
        }

        val availableParents = filterParentCategories(
            allCategories = current.allCategories,
            currentCategoryId = current.categoryId,
            currentType = updatedType
        )

        _uiState.value = current.copy(
            parentId = parentId,
            type = updatedType,
            availableParentCategories = availableParents
        )
    }

    fun onShowReportChange(showReport: Boolean) {
        _uiState.value = _uiState.value.copy(showReport = showReport)
    }

    fun saveCategory() {
        val current = _uiState.value
        val cleanName = current.name.trim()
        if (cleanName.isEmpty()) return

        viewModelScope.launch {
            categoryRepository.saveCategory(
                id = current.categoryId,
                name = cleanName,
                icon = current.icon,
                type = current.type,
                parentId = current.parentId,
                showReport = current.showReport,
                tag = current.tag
            )
            _eventFlow.emit(CategoryAddEditEvent.Saved)
        }
    }

    fun deleteCategory() {
        val currentId = _uiState.value.categoryId ?: return
        if (_uiState.value.isSystemCategory) return

        viewModelScope.launch {
            categoryRepository.deleteCategory(currentId)
            _eventFlow.emit(CategoryAddEditEvent.Deleted)
        }
    }
}
