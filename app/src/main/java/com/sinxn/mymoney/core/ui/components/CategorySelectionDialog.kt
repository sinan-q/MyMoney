package com.sinxn.mymoney.core.ui.components

import android.graphics.drawable.Icon
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
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.util.CategoryType
import com.sinxn.mymoney.ui.theme.ExpenseColor
import com.sinxn.mymoney.ui.theme.IncomeColor
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
    title: String = "Category"
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
            // ── Header Row: Title + Tab Pill + Close ──
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Subtle pill toggle synced with pager state
                    aTabPill(
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
                val flatRows = if (page == 0) incomeFlatRows else expenseFlatRows
                val listState = if (page == 0) incomeListState else expenseListState

                if (flatRows.isEmpty() && !showNoneOption) {
                    EmptyState()
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
                                val isNoneSelected = selectedCategoryId.isNullOrBlank()
                                NoneCategoryRow(
                                    label = noneOptionLabel,
                                    isSelected = isNoneSelected,
                                    onClick = {
                                        onCategorySelected(null)
                                        scope.launch {
                                            sheetState.hide()
                                            onDismissRequest()
                                        }
                                    }
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
                                    ParentCategoryRow(
                                        cleanName = row.cleanName,
                                        iconData = row.iconData,
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
                                        cleanName = row.cleanName,
                                        iconData = row.iconData,
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
fun aTabPill(
    activeTab: Int,
    onTabChange: (Int) -> Unit,
    tabs: List<Pair<String, Color>> = listOf("Income" to IncomeColor, "Expense" to ExpenseColor)
) {

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

// ── None Category Row ──

@Composable
private fun NoneCategoryRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else Color.Transparent,
        label = "NoneBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = label,
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

    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

// ── Parent Category Row ──

@Composable
private fun ParentCategoryRow(
    cleanName: String,
    iconData: IconData,
    isSelected: Boolean,
    hasSubcategories: Boolean,
    isExpanded: Boolean,
    onRowClick: () -> Unit,
    onExpandToggle: () -> Unit
) {
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
            iconData = iconData,
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
    cleanName: String,
    iconData: IconData,
    isSelected: Boolean,
    onClick: () -> Unit
) {
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
            iconData = iconData,
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
