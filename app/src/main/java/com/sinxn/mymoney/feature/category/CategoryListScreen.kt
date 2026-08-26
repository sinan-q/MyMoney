package com.sinxn.mymoney.feature.category

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sinxn.mymoney.core.ui.components.AppExtendedFab
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.FilterComponent
import com.sinxn.mymoney.core.ui.components.FinanceListItem
import com.sinxn.mymoney.core.ui.components.ReorderDragHandle
import com.sinxn.mymoney.core.ui.components.ReorderableLazyColumn
import com.sinxn.mymoney.core.ui.components.TabPill
import com.sinxn.mymoney.core.util.CategoryType
import com.sinxn.mymoney.feature.category.components.EmptyCategoryState
import com.sinxn.mymoney.feature.category.components.SubcategoriesSection
import com.sinxn.mymoney.ui.theme.ExpenseColor
import com.sinxn.mymoney.ui.theme.IncomeColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(
    onCategoryClick: (String) -> Unit = {},
    onAddCategoryClick: (Int) -> Unit = {},
    viewModel: CategoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    val incomeListState = rememberLazyListState()
    val expenseListState = rememberLazyListState()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            val isIncome = pagerState.currentPage == 0
            val currentListState = if (isIncome) incomeListState else expenseListState
            val activeColor = if (isIncome) IncomeColor else ExpenseColor
            val fabText = if (isIncome) "Add Income" else "Add Expense"

            AppExtendedFab(
                text = fabText,
                icon = Icons.Default.Add,
                onClick = {
                    val activeType = if (isIncome) CategoryType.INCOME else CategoryType.EXPENSE
                    onAddCategoryClick(activeType)
                },
                expanded = !currentListState.isScrollInProgress,
                containerColor = activeColor,
                contentColor = Color.White
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabPill(
                tabs = listOf("Income" to IncomeColor, "Expense" to ExpenseColor),
                activeTab = pagerState.currentPage,
                onTabChange = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                }
            )

            val isIncomeTab = pagerState.currentPage == 0
            val currentCategories = if (isIncomeTab) uiState.incomeCategories else uiState.expenseCategories
            val totalCategoriesCount = currentCategories.size

            if (totalCategoriesCount > 0 || uiState.incomeCategories.isNotEmpty() || uiState.expenseCategories.isNotEmpty()) {
                FilterComponent(
                    countText = "$totalCategoriesCount ${if (totalCategoriesCount == 1) "category" else "categories"}",
                    activeSortOption = uiState.sortOption,
                    options = CategorySortOption.entries,
                    setSortOption = viewModel::setSortOption
                )
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth(),
                    beyondViewportPageCount = 1
                ) { page ->
                    val isIncome = page == CategoryType.INCOME
                    val parentItems = if (isIncome) uiState.incomeCategories else uiState.expenseCategories
                    val listState = if (isIncome) incomeListState else expenseListState

                    if (parentItems.isEmpty()) {
                        EmptyCategoryState(tabName = if (isIncome) "Income" else "Expense")
                    } else {
                        ReorderableLazyColumn(
                            items = parentItems,
                            key = { it.category.id },
                            lazyListState = listState,
                            isReorderEnabled = uiState.sortOption == CategorySortOption.CUSTOM,
                            itemShape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 88.dp),
                            contentType = { "parent_category_card" },
                            modifier = Modifier.fillMaxSize(),
                            onReorder = { reordered -> viewModel.reorderCategories(reordered.map { it.category.id }) }
                        ) { parentItem, _, handleModifier ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                FinanceListItem(
                                    icon = {
                                        if (uiState.sortOption == CategorySortOption.CUSTOM) {
                                            ReorderDragHandle(
                                                modifier = handleModifier.padding(end = 10.dp),
                                                enabled = true
                                            )
                                        }
                                        CategoryIcon(iconData = parentItem.iconData)
                                    },
                                    onClick = { onCategoryClick(parentItem.category.id) },
                                    title = parentItem.cleanName,
                                    subtitle = if (parentItem.subcategories.isNotEmpty() ) {
                                        "${parentItem.subcategories.size} subcategor${if (parentItem.subcategories.size == 1) "y" else "ies"}"
                                    } else null,
                                    trailingContent = {
                                        if (parentItem.subcategories.isNotEmpty()) {
                                            IconButton(
                                                onClick = { viewModel.toggleParentExpanded(parentItem.category.id) },
                                            ) {
                                                Icon(
                                                    imageVector = if (parentItem.isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                    contentDescription = if (parentItem.isExpanded) "Collapse" else "Expand",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    },
                                    horizontalPadding = 1.dp
                                )

                                // Subcategories Expandable Section
                                AnimatedVisibility(
                                    visible = parentItem.isExpanded && parentItem.subcategories.isNotEmpty(),
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    SubcategoriesSection(
                                        subcategories = parentItem.subcategories,
                                        isReorderEnabled = uiState.sortOption == CategorySortOption.CUSTOM,
                                        onSubcategoryClick = onCategoryClick,
                                        onReorderSubcategories = viewModel::reorderCategories
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}