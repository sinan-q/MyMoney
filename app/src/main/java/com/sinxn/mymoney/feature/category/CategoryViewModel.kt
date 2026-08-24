package com.sinxn.mymoney.feature.category

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.data.repository.CategoryRepository
import com.sinxn.mymoney.core.ui.components.IconData
import com.sinxn.mymoney.core.ui.components.parseIconData
import com.sinxn.mymoney.core.util.CategoryType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class ParentCategoryItem(
    val category: CategoryEntity,
    val cleanName: String,
    val iconData: IconData,
    val subcategories: List<SubcategoryItem>,
    val isExpanded: Boolean
)

@Immutable
data class SubcategoryItem(
    val category: CategoryEntity,
    val cleanName: String,
    val iconData: IconData,
    val parentCategoryId: String
)

data class CategoryUiState(
    val expenseCategories: List<ParentCategoryItem> = emptyList(),
    val incomeCategories: List<ParentCategoryItem> = emptyList(),
    val expandedParentIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
)

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _expandedParentIds = MutableStateFlow<Set<String>>(emptySet())

    val uiState: StateFlow<CategoryUiState> = combine(
        categoryRepository.getCategories(),
        _expandedParentIds
    ) { categories, expandedIds ->
        val incomeCats = categories.filter { it.type == CategoryType.INCOME }
        val expenseCats = categories.filter { it.type == CategoryType.EXPENSE }

        CategoryUiState(
            expenseCategories = buildParentCategoryItems(expenseCats, expandedIds),
            incomeCategories = buildParentCategoryItems(incomeCats, expandedIds),
            expandedParentIds = expandedIds,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CategoryUiState(isLoading = true)
    )

    fun toggleParentExpanded(parentId: String) {
        _expandedParentIds.update { current ->
            if (parentId in current) {
                current - parentId
            } else {
                current + parentId
            }
        }
    }

    fun reorderCategories(orderedCategoryIds: List<String>) {
        viewModelScope.launch {
            categoryRepository.reorderCategories(orderedCategoryIds)
        }
    }

    private fun buildParentCategoryItems(
        categories: List<CategoryEntity>,
        expandedParentIds: Set<String>
    ): List<ParentCategoryItem> {
        if (categories.isEmpty()) return emptyList()

        val parents = categories.filter { it.parentId == null }
        val subMap = categories.filter { it.parentId != null }.groupBy { it.parentId!! }

        return parents.map { parent ->
            val subs = (subMap[parent.id] ?: emptyList()).map { sub ->
                SubcategoryItem(
                    category = sub,
                    cleanName = sub.name,
                    iconData = parseIconData(sub.icon, sub.name),
                    parentCategoryId = parent.id
                )
            }
            ParentCategoryItem(
                category = parent,
                cleanName = parent.name,
                iconData = parseIconData(parent.icon, parent.name),
                subcategories = subs,
                isExpanded = parent.id in expandedParentIds
            )
        }
    }
}
