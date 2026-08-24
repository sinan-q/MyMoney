package com.sinxn.mymoney.feature.category

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sinxn.mymoney.core.ui.components.TabPill
import com.sinxn.mymoney.core.util.CategoryType
import com.sinxn.mymoney.feature.category.components.CategoryReorderableList
import com.sinxn.mymoney.feature.category.components.EmptyCategoryState
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
    val isLoading by viewModel.isLoading.collectAsState()
    val incomeCategories by viewModel.incomeCategories.collectAsState()
    val expenseCategories by viewModel.expenseCategories.collectAsState()

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    val incomeListState = rememberLazyListState()
    val expenseListState = rememberLazyListState()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val activeType = if (pagerState.currentPage == 0) CategoryType.INCOME else CategoryType.EXPENSE
                    onAddCategoryClick(activeType)
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Category")
            }
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

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    beyondViewportPageCount = 1
                ) { page ->
                    val isIncome = page == CategoryType.INCOME
                    val parentItems = if (isIncome) incomeCategories else expenseCategories
                    val listState = if (isIncome) incomeListState else expenseListState

                    CategoryTabPane(
                        isIncome = isIncome,
                        items = parentItems,
                        lazyListState = listState,
                        onCategoryClick = onCategoryClick,
                        onExpandToggle = viewModel::toggleParentExpanded,
                        onReorderCategories = viewModel::reorderCategories
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryTabPane(
    isIncome: Boolean,
    items: List<ParentCategoryItem>,
    lazyListState: LazyListState,
    onCategoryClick: (String) -> Unit,
    onExpandToggle: (String) -> Unit,
    onReorderCategories: (List<String>) -> Unit
) {
    if (items.isEmpty()) {
        EmptyCategoryState(tabName = if (isIncome) "Income" else "Expense")
    } else {
        CategoryReorderableList(
            items = items,
            lazyListState = lazyListState,
            onCategoryClick = onCategoryClick,
            onExpandToggle = onExpandToggle,
            onReorderParents = onReorderCategories,
            onReorderSubcategories = onReorderCategories
        )
    }
}