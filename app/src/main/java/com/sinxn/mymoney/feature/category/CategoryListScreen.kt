package com.sinxn.mymoney.feature.category

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.Immutable
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.IconData
import com.sinxn.mymoney.core.ui.components.TabPill
import com.sinxn.mymoney.core.ui.components.parseIconData
import com.sinxn.mymoney.core.util.CategoryType
import kotlinx.coroutines.launch
import kotlin.collections.listOf

@Immutable
private sealed class CategoryRow {
    @Immutable
    data class Parent(
        val category: CategoryEntity,
        val cleanName: String,
        val iconData: IconData,
        val subcategoryCount: Int,
        val isExpanded: Boolean
    ) : CategoryRow()
    @Immutable
    data class Sub(
        val category: CategoryEntity,
        val cleanName: String,
        val iconData: IconData
    ) : CategoryRow()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(
    onNavigateBack: () -> Unit,
    onCategoryClick: (String) -> Unit = {},
    onAddCategoryClick: (Int) -> Unit = {},
    viewModel: CategoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    val expenseListState = rememberLazyListState()
    val incomeListState = rememberLazyListState()

    var expandedParentIds by remember { mutableStateOf(setOf<String>()) }

    val toggleParentExpanded: (String) -> Unit = { parentId ->
        expandedParentIds = if (parentId in expandedParentIds) {
            expandedParentIds - parentId
        } else {
            expandedParentIds + parentId
        }
    }

    val expenseFlatRows = remember(uiState.expenseCategories, expandedParentIds) {
        buildFlatRows(uiState.expenseCategories, expandedParentIds)
    }
    val incomeFlatRows = remember(uiState.incomeCategories, expandedParentIds) {
        buildFlatRows(uiState.incomeCategories, expandedParentIds)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val targetType = if (pagerState.currentPage == 0) CategoryType.INCOME else CategoryType.EXPENSE
                    onAddCategoryClick(targetType)
                }
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
                tabs = listOf("Income" to Color(0xFF10B981), "Expense" to Color(0xFFE11D48)),
                activeTab = pagerState.currentPage,
                onTabChange = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                }
            )


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
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    key = { page -> page }
                ) { page ->
                    val flatRows = if (page == 0) incomeFlatRows else expenseFlatRows
                    val listState = if (page == 0) incomeListState else expenseListState

                    if (flatRows.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No categories found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 88.dp)
                        ) {
                            items(
                                items = flatRows,
                                key = { row ->
                                    when (row) {
                                        is CategoryRow.Parent -> "p_${row.category.id}"
                                        is CategoryRow.Sub -> "s_${row.category.id}"
                                    }
                                },
                                contentType = { row ->
                                    when (row) {
                                        is CategoryRow.Parent -> "parent_cat"
                                        is CategoryRow.Sub -> "sub_cat"
                                    }
                                }
                            ) { row ->
                                when (row) {
                                    is CategoryRow.Parent -> {
                                        ParentCategoryRow(
                                            cleanName = row.cleanName,
                                            iconData = row.iconData,
                                            hasSubcategories = row.subcategoryCount > 0,
                                            isExpanded = row.isExpanded,
                                            onRowClick = {
                                                onCategoryClick(row.category.id)
                                            },
                                            onExpandToggle = {
                                                toggleParentExpanded(row.category.id)
                                            }
                                        )
                                    }
                                    is CategoryRow.Sub -> {
                                        SubcategoryCategoryRow(
                                            cleanName = row.cleanName,
                                            iconData = row.iconData,
                                            onClick = {
                                                onCategoryClick(row.category.id)
                                            }
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
}

@Composable
private fun ParentCategoryRow(
    cleanName: String,
    iconData: IconData,
    hasSubcategories: Boolean,
    isExpanded: Boolean,
    onRowClick: () -> Unit,
    onExpandToggle: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onRowClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIcon(
                iconData = iconData,
                modifier = Modifier.size(42.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = cleanName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (hasSubcategories) {
                IconButton(
                    onClick = onExpandToggle,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun SubcategoryCategoryRow(
    cleanName: String,
    iconData: IconData,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = 48.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIcon(
                iconData = iconData,
                modifier = Modifier.size(42.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = cleanName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
            modifier = Modifier.padding(start = 48.dp, end = 16.dp)
        )
    }
}

private fun buildFlatRows(
    categories: List<CategoryEntity>,
    expandedParentIds: Set<String>
): List<CategoryRow> {
    if (categories.isEmpty()) return emptyList()

    val parents = mutableListOf<CategoryEntity>()
    val subMap = mutableMapOf<String, MutableList<CategoryEntity>>()
    val orphaned = mutableListOf<CategoryEntity>()

    for (cat in categories) {
        val cleanName = cat.name.trim()
        val isSubByName = cleanName.startsWith("↳") || cleanName.startsWith("  ↳ ")

        if (cat.parentId == null && !isSubByName) {
            parents.add(cat)
        } else if (cat.parentId != null) {
            subMap.getOrPut(cat.parentId) { mutableListOf() }.add(cat)
        } else {
            orphaned.add(cat)
        }
    }

    var currentParent: CategoryEntity? = null
    for (cat in categories) {
        val cleanName = cat.name.trim()
        val isSubByName = cleanName.startsWith("↳") || cleanName.startsWith("  ↳ ")
        if (!isSubByName && cat.parentId == null) {
            currentParent = cat
        } else if (isSubByName && cat.parentId == null && currentParent != null) {
            subMap.getOrPut(currentParent.id) { mutableListOf() }.add(cat)
            orphaned.remove(cat)
        }
    }

    val result = mutableListOf<CategoryRow>()

    for (parent in parents) {
        val subs = subMap[parent.id] ?: emptyList()
        val isExpanded = parent.id in expandedParentIds
        val cleanName = parent.name.replace("  ↳ ", "").replace("↳", "").trim()
        val iconData = parseIconData(parent.icon, cleanName)
        result.add(CategoryRow.Parent(parent, cleanName, iconData, subs.size, isExpanded))
        if (isExpanded) {
            for (sub in subs) {
                val subCleanName = sub.name.replace("  ↳ ", "").replace("↳", "").trim()
                val subIconData = parseIconData(sub.icon, subCleanName)
                result.add(CategoryRow.Sub(sub, subCleanName, subIconData))
            }
        }
    }

    for (orphan in orphaned) {
        val cleanName = orphan.name.replace("  ↳ ", "").replace("↳", "").trim()
        val iconData = parseIconData(orphan.icon, cleanName)
        result.add(CategoryRow.Parent(orphan, cleanName, iconData, 0, false))
    }

    return result
}

