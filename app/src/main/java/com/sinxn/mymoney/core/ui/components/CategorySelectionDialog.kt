package com.sinxn.mymoney.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import kotlinx.coroutines.launch

/**
 * Sealed class representing a flattened row in the category list.
 */
private sealed class CategoryRow {
    data class Parent(
        val category: CategoryEntity,
        val subcategoryCount: Int,
        val isExpanded: Boolean
    ) : CategoryRow()
    data class Sub(val category: CategoryEntity) : CategoryRow()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelectionDialog(
    showIncome: Boolean,
    incomeCategories: List<CategoryEntity>,
    expenseCategories: List<CategoryEntity>,
    selectedCategoryId: String?,
    onCategorySelected: (CategoryEntity) -> Unit,
    onDismissRequest: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val pagerState = rememberPagerState(
        initialPage = if (showIncome) 1 else 0,
        pageCount = { 2 }
    )

    val expenseListState = rememberLazyListState()
    val incomeListState = rememberLazyListState()

    // Expanded parent category IDs (hidden by default, unless currently selected category is a subcategory)
    var expandedParentIds by remember(selectedCategoryId) {
        mutableStateOf(getInitialExpandedParentIds(expenseCategories, incomeCategories, selectedCategoryId))
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
            // ── Header Row: Title + Tab Pill + Close ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Subtle pill toggle synced with pager state
                    TabPill(
                        activeTab = pagerState.currentPage,
                        onTabChange = { index ->
                            scope.launch { pagerState.animateScrollToPage(index) }
                        }
                    )

                    // Close button
                    IconButton(
                        onClick = {
                            scope.launch {
                                sheetState.hide()
                                onDismissRequest()
                            }
                        },
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Horizontal Pager for Swiping Between Expense / Income ──
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = true,
                beyondViewportPageCount = 1
            ) { page ->
                val flatRows = if (page == 0) expenseFlatRows else incomeFlatRows
                val listState = if (page == 0) expenseListState else incomeListState

                if (flatRows.isEmpty()) {
                    EmptyState()
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(stopBottomOverscrollConnection),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        itemsIndexed(
                            items = flatRows,
                            key = { idx, row ->
                                when (row) {
                                    is CategoryRow.Parent -> "p_${row.category.id}_$idx"
                                    is CategoryRow.Sub -> "s_${row.category.id}_$idx"
                                }
                            }
                        ) { _, row ->
                            when (row) {
                                is CategoryRow.Parent -> {
                                    ParentCategoryRow(
                                        category = row.category,
                                        isSelected = row.category.id == selectedCategoryId,
                                        hasSubcategories = row.subcategoryCount > 0,
                                        isExpanded = row.isExpanded,
                                        onRowClick = {
                                            onCategorySelected(row.category)
                                            scope.launch {
                                                sheetState.hide()
                                                onDismissRequest()
                                            }
                                        },
                                        onExpandToggle = {
                                            toggleParentExpanded(row.category.id)
                                        }
                                    )
                                }
                                is CategoryRow.Sub -> {
                                    SubcategoryCategoryRow(
                                        category = row.category,
                                        isSelected = row.category.id == selectedCategoryId,
                                        onClick = {
                                            onCategorySelected(row.category)
                                            scope.launch {
                                                sheetState.hide()
                                                onDismissRequest()
                                            }
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

// ── Tab Pill ──

@Composable
private fun TabPill(
    activeTab: Int,
    onTabChange: (Int) -> Unit
) {
    val tabs = listOf("Expense" to Color(0xFFE11D48), "Income" to Color(0xFF10B981))

    Row(
        modifier = Modifier
            .height(30.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = CircleShape
            )
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        tabs.forEachIndexed { index, (title, accent) ->
            val isSelected = activeTab == index
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) accent else Color.Transparent,
                label = "TabBg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "TabText"
            )

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(bgColor)
                    .clickable { onTabChange(index) }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = textColor
                )
            }
        }
    }
}

// ── Parent Category Row ──

@Composable
private fun ParentCategoryRow(
    category: CategoryEntity,
    isSelected: Boolean,
    hasSubcategories: Boolean,
    isExpanded: Boolean,
    onRowClick: () -> Unit,
    onExpandToggle: () -> Unit
) {
    val cleanName = remember(category.name) {
        category.name.replace("  ↳ ", "").replace("↳", "").trim()
    }

    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else Color.Transparent,
        label = "ParentBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(onClick = onRowClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIcon(
            iconString = category.icon,
            categoryName = cleanName,
            modifier = Modifier.size(42.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = cleanName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }

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

// ── Subcategory Row ──

@Composable
private fun SubcategoryCategoryRow(
    category: CategoryEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val cleanName = remember(category.name) {
        category.name.replace("  ↳ ", "").replace("↳", "").trim()
    }

    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        else Color.Transparent,
        label = "SubBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(start = 48.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIcon(
            iconString = category.icon,
            categoryName = cleanName,
            modifier = Modifier.size(42.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = cleanName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ── Empty State ──

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
    expenseCategories: List<CategoryEntity>,
    incomeCategories: List<CategoryEntity>,
    selectedCategoryId: String?
): Set<String> {
    if (selectedCategoryId.isNullOrBlank()) return emptySet()

    val allCategories = expenseCategories + incomeCategories
    val selectedCat = allCategories.find { it.id == selectedCategoryId } ?: return emptySet()

    if (selectedCat.parentId != null) {
        return setOf(selectedCat.parentId)
    }

    val cleanName = selectedCat.name.trim()
    val isSubByName = cleanName.startsWith("↳") || cleanName.startsWith("  ↳ ")
    if (isSubByName) {
        var currentParentId: String? = null
        for (cat in allCategories) {
            val name = cat.name.trim()
            val isSub = name.startsWith("↳") || name.startsWith("  ↳ ")
            if (!isSub && cat.parentId == null) {
                currentParentId = cat.id
            } else if (cat.id == selectedCategoryId) {
                if (currentParentId != null) return setOf(currentParentId)
            }
        }
    }

    return emptySet()
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

    // Handle name-based subcategory assignment
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
        result.add(CategoryRow.Parent(parent, subs.size, isExpanded))
        if (isExpanded) {
            for (sub in subs) {
                result.add(CategoryRow.Sub(sub))
            }
        }
    }

    for (orphan in orphaned) {
        result.add(CategoryRow.Parent(orphan, 0, false))
    }

    return result
}
