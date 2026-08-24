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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
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

@Immutable
data class CategoryUiState(
    val expenseCategories: List<ParentCategoryItem> = emptyList(),
    val incomeCategories: List<ParentCategoryItem> = emptyList(),
    val collapsedParentIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
)

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _collapsedParentIds = MutableStateFlow<Set<String>>(emptySet())
    private var reorderJob: Job? = null

    // Base tree flow: Only parses icons and groups categories when database categories change
    private val baseCategoryTreeFlow: Flow<Pair<List<ParentCategoryItem>, List<ParentCategoryItem>>> =
        categoryRepository.getCategories()
            .map { categories ->
                val income = buildBaseCategoryItems(categories.filter { it.type == CategoryType.INCOME })
                val expense = buildBaseCategoryItems(categories.filter { it.type == CategoryType.EXPENSE })
                income to expense
            }
            .flowOn(Dispatchers.Default)

    // UI state flow: Cheaply applies expansion states without re-parsing icons or re-grouping lists
    val uiState: StateFlow<CategoryUiState> = combine(
        baseCategoryTreeFlow,
        _collapsedParentIds
    ) { (incomeTree, expenseTree), collapsedIds ->
        CategoryUiState(
            incomeCategories = incomeTree.map { it.copy(isExpanded = it.category.id !in collapsedIds) },
            expenseCategories = expenseTree.map { it.copy(isExpanded = it.category.id !in collapsedIds) },
            collapsedParentIds = collapsedIds,
            isLoading = false,
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CategoryUiState(isLoading = true)
        )

    fun toggleParentExpanded(parentId: String) {
        _collapsedParentIds.update { current ->
            if (parentId in current) {
                current - parentId
            } else {
                current + parentId
            }
        }
    }

    fun reorderCategories(orderedCategoryIds: List<String>) {
        reorderJob?.cancel()
        reorderJob = viewModelScope.launch {
            categoryRepository.reorderCategories(orderedCategoryIds)
        }
    }

    private fun buildBaseCategoryItems(
        categories: List<CategoryEntity>
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
                isExpanded = true // Default state, mapped by combine
            )
        }
    }
}
