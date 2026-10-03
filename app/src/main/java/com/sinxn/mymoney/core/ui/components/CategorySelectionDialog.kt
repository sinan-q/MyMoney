package com.sinxn.mymoney.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.util.CategoryType
import kotlinx.coroutines.launch

/**
 * Sealed class representing a flattened row in the category list.
 */
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

/**
 * Category selection bottom sheet.
 *
 * **Single-select mode** (default): pass [selectedCategoryId] and [onCategorySelected].
 * Selecting a category closes the sheet immediately.
 *
 * **Multi-select mode**: pass [multiSelectIds] (non-null) and [onMultiSelectConfirmed].
 * Items are toggled with checkmarks; an Apply button confirms the selection.
 * An empty [multiSelectIds] means "All categories selected".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelectionDialog(
    categories: List<CategoryEntity>,
    selectedCategoryId: String?,
    onCategorySelected: (CategoryEntity?) -> Unit,
    onDismissRequest: () -> Unit,
    showIncome: Boolean? = null,
    showNoneOption: Boolean = false,
    noneOptionLabel: String = "None (Top Level Category)",
    title: String = "Category",
    // ── Multi-select params (null = single-select mode) ──────────────────
    multiSelectIds: Set<String>? = null,
    onMultiSelectConfirmed: ((Set<String>) -> Unit)? = null
) {
    val isMultiSelect = multiSelectIds != null

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Multi-select local state (only used when multiSelectIds != null)
    var localSelected by remember(multiSelectIds) {
        mutableStateOf(multiSelectIds ?: emptySet())
    }

    val incomeCategories = remember(categories) {
        categories.filter { it.type == CategoryType.INCOME }
    }
    val expenseCategories = remember(categories) {
        categories.filter { it.type == CategoryType.EXPENSE }
    }

    val isInitialIncome = remember(categories, selectedCategoryId, showIncome) {
        when {
            showIncome != null -> showIncome
            !selectedCategoryId.isNullOrBlank() -> {
                categories.find { it.id == selectedCategoryId }?.type == CategoryType.INCOME
            }
            else -> false
        }
    }

    val pagerState = rememberPagerState(
        initialPage = if (isInitialIncome) 0 else 1,
        pageCount = { 2 }
    )

    val expenseListState = rememberLazyListState()
    val incomeListState = rememberLazyListState()

    // Expanded parent category IDs (hidden by default, unless currently selected category is a subcategory)
    var expandedParentIds by remember(selectedCategoryId) {
        mutableStateOf(getInitialExpandedParentIds(categories, selectedCategoryId))
    }

    val toggleParentExpanded: (String) -> Unit = { parentId ->
        expandedParentIds = if (parentId in expandedParentIds) {
            expandedParentIds - parentId
        } else {
            expandedParentIds + parentId
        }
    }

    // Build flat rows for both lists taking expandedParentIds into account
    val expenseFlatRows = remember(expenseCategories, expandedParentIds) {
        buildFlatRows(expenseCategories, expandedParentIds)
    }
    val incomeFlatRows = remember(incomeCategories, expandedParentIds) {
        buildFlatRows(incomeCategories, expandedParentIds)
    }

    // Auto-scroll to selected item ONCE on initial launch
    LaunchedEffect(Unit) {
        if (!selectedCategoryId.isNullOrBlank()) {
            val expenseIdx = expenseFlatRows.indexOfFirst { row ->
                when (row) {
                    is CategoryRow.Parent -> row.category.id == selectedCategoryId
                    is CategoryRow.Sub -> row.category.id == selectedCategoryId
                }
            }
            if (expenseIdx >= 0) {
                expenseListState.scrollToItem(
                    index = maxOf(0, expenseIdx - 1),
                    scrollOffset = 0
                )
            }

            val incomeIdx = incomeFlatRows.indexOfFirst { row ->
                when (row) {
                    is CategoryRow.Parent -> row.category.id == selectedCategoryId
                    is CategoryRow.Sub -> row.category.id == selectedCategoryId
                }
            }
            if (incomeIdx >= 0) {
                incomeListState.scrollToItem(
                    index = maxOf(0, incomeIdx - 1),
                    scrollOffset = 0
                )
            }
        }
    }

    // Stop bottom overscroll from bouncing the sheet
    val stopBottomOverscrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                return if (available.y < 0f) Offset(0f, available.y) else Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                return if (available.y < 0f) Velocity(0f, available.y) else Velocity.Zero
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.4f),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
        ) {
            // Header row — title + optional "Select All / Clear" toggle in multi-select mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isMultiSelect) {
                    if (localSelected.isEmpty()) {
                        TextButton(onClick = {
                            localSelected = categories.map { it.id }.toSet()
                        }) { Text("Select All") }
                    } else {
                        TextButton(onClick = { localSelected = emptySet() }) { Text("Clear") }
                    }
                }
            }

            TabPill(
                activeTab = pagerState.currentPage,
                onTabChange = { index ->
                    scope.launch { pagerState.animateScrollToPage(index) }
                }
            )

            // ── Horizontal Pager for Swiping Between Expense / Income ──
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = true,
                beyondViewportPageCount = 1
            ) { page ->
                val flatRows = if (page == 0) incomeFlatRows else expenseFlatRows
                val listState = if (page == 0) incomeListState else expenseListState

                if (flatRows.isEmpty() && !showNoneOption) {
                    EmptyListItem(text = "categories", isSearching = false)
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(stopBottomOverscrollConnection),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        if (showNoneOption) {
                            item(
                                key = "none_option",
                                contentType = "none_option"
                            ) {
                                FinanceListItem(
                                    icon = {
                                        CategoryIconExtended(color = MaterialTheme.colorScheme.primary, icon = Icons.Default.Block)
                                    },
                                    title = noneOptionLabel,
                                    onClick = {
                                        onCategorySelected(null)
                                        scope.launch {
                                            sheetState.hide()
                                            onDismissRequest()
                                        }
                                    },
                                    isSelected = selectedCategoryId.isNullOrBlank()
                                )
                            }
                        }

                        itemsIndexed(
                            items = flatRows,
                            key = { idx, row ->
                                when (row) {
                                    is CategoryRow.Parent -> "p_${row.category.id}_$idx"
                                    is CategoryRow.Sub -> "s_${row.category.id}_$idx"
                                }
                            },
                            contentType = { _, row ->
                                when (row) {
                                    is CategoryRow.Parent -> "parent_cat"
                                    is CategoryRow.Sub -> "sub_cat"
                                }
                            }
                        ) { _, row ->
                            when (row) {
                                is CategoryRow.Parent -> {
                                    val isSelected = if (isMultiSelect) {
                                        row.category.id in localSelected
                                    } else {
                                        row.category.id == selectedCategoryId
                                    }
                                    FinanceListItem(
                                        trailingContent = {
                                            if (isMultiSelect && isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            } else if (!isMultiSelect && row.subcategoryCount > 0) {
                                                IconButton(
                                                    onClick = { toggleParentExpanded(row.category.id) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (row.isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                        contentDescription = if (row.isExpanded) "Collapse" else "Expand",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                            }
                                        },
                                        icon = { CategoryIcon(iconData = row.iconData) },
                                        title = row.cleanName,
                                        onClick = {
                                            if (isMultiSelect) {
                                                localSelected = if (isSelected) {
                                                    localSelected - row.category.id
                                                } else {
                                                    localSelected + row.category.id
                                                }
                                            } else {
                                                onCategorySelected(row.category)
                                                scope.launch {
                                                    sheetState.hide()
                                                    onDismissRequest()
                                                }
                                            }
                                        },
                                        isSelected = isSelected
                                    )
                                }
                                is CategoryRow.Sub -> {
                                    val isSelected = if (isMultiSelect) {
                                        row.category.id in localSelected
                                    } else {
                                        row.category.id == selectedCategoryId
                                    }
                                    FinanceListItem(
                                        modifier = Modifier.padding(start = 32.dp),
                                        icon = { CategoryIcon(iconData = row.iconData) },
                                        title = row.cleanName,
                                        trailingContent = if (isMultiSelect && isSelected) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        } else null,
                                        onClick = {
                                            if (isMultiSelect) {
                                                localSelected = if (isSelected) {
                                                    localSelected - row.category.id
                                                } else {
                                                    localSelected + row.category.id
                                                }
                                            } else {
                                                onCategorySelected(row.category)
                                                scope.launch {
                                                    sheetState.hide()
                                                    onDismissRequest()
                                                }
                                            }
                                        },
                                        isSelected = isSelected,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Apply button (multi-select only) ──────────────────────────
            if (isMultiSelect) {
                val applyLabel = when {
                    localSelected.isEmpty() -> "Apply — All categories"
                    localSelected.size == 1 -> "Apply — 1 category"
                    else -> "Apply — ${localSelected.size} categories"
                }
                Button(
                    onClick = {
                        onMultiSelectConfirmed?.invoke(localSelected)
                        scope.launch {
                            sheetState.hide()
                            onDismissRequest()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                ) {
                    Text(applyLabel, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "No categories available",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

// ── Initial Expanded State Helper ──

private fun getInitialExpandedParentIds(
    categories: List<CategoryEntity>,
    selectedCategoryId: String?
): Set<String> {
    if (selectedCategoryId.isNullOrBlank()) return emptySet()
    val selectedCat = categories.find { it.id == selectedCategoryId } ?: return emptySet()
    return if (selectedCat.parentId != null) setOf(selectedCat.parentId) else emptySet()
}

// ── Flat Row Builder ──

/**
 * Flattens a hierarchical category list into a list of [CategoryRow] items based on [expandedParentIds].
 */
private fun buildFlatRows(
    categories: List<CategoryEntity>,
    expandedParentIds: Set<String>
): List<CategoryRow> {
    if (categories.isEmpty()) return emptyList()

    val parents = categories.filter { it.parentId == null }
    val subMap = categories.filter { it.parentId != null }.groupBy { it.parentId!! }

    val result = mutableListOf<CategoryRow>()

    for (parent in parents) {
        val subs = subMap[parent.id] ?: emptyList()
        val isExpanded = parent.id in expandedParentIds
        val iconData = parseIconData(parent.icon, parent.name)
        result.add(CategoryRow.Parent(parent, parent.name, iconData, subs.size, isExpanded))
        if (isExpanded) {
            for (sub in subs) {
                val subIconData = parseIconData(sub.icon, sub.name)
                result.add(CategoryRow.Sub(sub, sub.name, subIconData))
            }
        }
    }

    return result
}
