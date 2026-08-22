package com.sinxn.mymoney.feature.category

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.Immutable
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.IconData
import com.sinxn.mymoney.core.ui.components.TabPill
import com.sinxn.mymoney.core.ui.components.parseIconData
import com.sinxn.mymoney.core.util.CategoryType
import com.sinxn.mymoney.ui.theme.ExpenseColor
import com.sinxn.mymoney.ui.theme.IncomeColor
import kotlinx.coroutines.launch
import kotlin.collections.listOf
import kotlin.math.roundToInt

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
        val iconData: IconData,
        val parentCategoryId: String
    ) : CategoryRow()
}

/** Identifies the drag group a row belongs to. */
@Immutable
private sealed class DragGroup {
    /** All parent rows share one drag group. */
    data object Parents : DragGroup()
    /** Subcategories under [parentId] share one group and can only be reordered within it. */
    data class SubsOf(val parentId: String) : DragGroup()
}

private fun CategoryRow.dragGroup(): DragGroup = when (this) {
    is CategoryRow.Parent -> DragGroup.Parents
    is CategoryRow.Sub -> DragGroup.SubsOf(parentCategoryId)
}

private fun CategoryRow.categoryId(): String = when (this) {
    is CategoryRow.Parent -> category.id
    is CategoryRow.Sub -> category.id
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
                tabs = listOf("Income" to IncomeColor, "Expense" to ExpenseColor),
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
                        DraggableCategoryList(
                            flatRows = flatRows,
                            listState = listState,
                            onCategoryClick = onCategoryClick,
                            onExpandToggle = toggleParentExpanded,
                            onReorder = { orderedIds ->
                                viewModel.reorderCategories(orderedIds)
                            }
                        )
                    }
                }
            }
        }
    }
}

// ─── Drag-to-reorder LazyColumn ──────────────────────────────────────────────

@Composable
private fun DraggableCategoryList(
    flatRows: List<CategoryRow>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onCategoryClick: (String) -> Unit,
    onExpandToggle: (String) -> Unit,
    onReorder: (List<String>) -> Unit
) {
    // Mutable snapshot of the list that we reorder in real-time during drag
    var currentRows by remember(flatRows) { mutableStateOf(flatRows) }

    // Drag state
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    var draggedGroup by remember { mutableStateOf<DragGroup?>(null) }

    // Find the visible item info by its index
    fun findItemInfo(index: Int): LazyListItemInfo? {
        return listState.layoutInfo.visibleItemsInfo.find { it.index == index }
    }

    // Compute the valid index range for the current drag group
    fun groupIndexRange(group: DragGroup, rows: List<CategoryRow>): IntRange {
        var first = -1
        var last = -1
        for (i in rows.indices) {
            if (rows[i].dragGroup() == group) {
                if (first == -1) first = i
                last = i
            }
        }
        return if (first == -1) IntRange.EMPTY else first..last
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        // Find which item is under the finger
                        listState.layoutInfo.visibleItemsInfo
                            .firstOrNull { itemInfo ->
                                offset.y.roundToInt() in itemInfo.offset..(itemInfo.offset + itemInfo.size)
                            }
                            ?.let { itemInfo ->
                                val idx = itemInfo.index
                                if (idx in currentRows.indices) {
                                    draggedItemIndex = idx
                                    dragOffset = 0f
                                    draggedGroup = currentRows[idx].dragGroup()
                                }
                            }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount.y

                        val currentIndex = draggedItemIndex ?: return@detectDragGesturesAfterLongPress
                        val group = draggedGroup ?: return@detectDragGesturesAfterLongPress
                        val range = groupIndexRange(group, currentRows)

                        val draggedItemInfo = findItemInfo(currentIndex) ?: return@detectDragGesturesAfterLongPress
                        val draggedCenter = draggedItemInfo.offset + draggedItemInfo.size / 2 + dragOffset.roundToInt()

                        // Check if we should swap with neighbor
                        val targetIndex = if (dragOffset > 0) {
                            // Dragging down – check the item below
                            val nextIdx = currentIndex + 1
                            if (nextIdx <= range.last) {
                                val nextInfo = findItemInfo(nextIdx)
                                if (nextInfo != null && draggedCenter > nextInfo.offset + nextInfo.size / 2) nextIdx else null
                            } else null
                        } else {
                            // Dragging up – check the item above
                            val prevIdx = currentIndex - 1
                            if (prevIdx >= range.first) {
                                val prevInfo = findItemInfo(prevIdx)
                                if (prevInfo != null && draggedCenter < prevInfo.offset + prevInfo.size / 2) prevIdx else null
                            } else null
                        }

                        if (targetIndex != null) {
                            val mutable = currentRows.toMutableList()

                            if (group is DragGroup.Parents) {
                                // When moving a parent, move it with all its expanded subcategories
                                val draggedRow = mutable[currentIndex]
                                val parentId = (draggedRow as CategoryRow.Parent).category.id

                                // Collect the block: parent + its subs
                                val blockStart = currentIndex
                                var blockEnd = currentIndex
                                for (i in (currentIndex + 1) until mutable.size) {
                                    if (mutable[i] is CategoryRow.Sub && (mutable[i] as CategoryRow.Sub).parentCategoryId == parentId) {
                                        blockEnd = i
                                    } else break
                                }
                                val blockSize = blockEnd - blockStart + 1
                                val block = mutable.subList(blockStart, blockEnd + 1).toList()

                                if (targetIndex > currentIndex) {
                                    // Moving down: find next parent's block end
                                    val nextParentIdx = blockEnd + 1
                                    if (nextParentIdx < mutable.size) {
                                        var insertAfter = nextParentIdx
                                        val nextParentId = (mutable[nextParentIdx] as? CategoryRow.Parent)?.category?.id
                                        if (nextParentId != null) {
                                            for (i in (nextParentIdx + 1) until mutable.size) {
                                                if (mutable[i] is CategoryRow.Sub && (mutable[i] as CategoryRow.Sub).parentCategoryId == nextParentId) {
                                                    insertAfter = i
                                                } else break
                                            }
                                        }
                                        // Remove the block
                                        for (i in 0 until blockSize) mutable.removeAt(blockStart)
                                        // Insert after the next parent's block
                                        val newInsertIdx = (insertAfter - blockSize + 1).coerceAtLeast(0)
                                        mutable.addAll(newInsertIdx, block)
                                        val newDraggedIdx = newInsertIdx
                                        // Adjust drag offset to account for position change
                                        val oldInfo = findItemInfo(currentIndex)
                                        val newTargetInfo = findItemInfo(targetIndex)
                                        if (oldInfo != null && newTargetInfo != null) {
                                            dragOffset -= (newTargetInfo.offset - oldInfo.offset).toFloat()
                                        }
                                        draggedItemIndex = newDraggedIdx
                                        currentRows = mutable
                                    }
                                } else {
                                    // Moving up: find prev parent's block start
                                    val prevParentIdx = blockStart - 1
                                    if (prevParentIdx >= 0) {
                                        // Walk back to find the parent that owns prevParentIdx
                                        var targetParentStart = prevParentIdx
                                        while (targetParentStart > 0 && mutable[targetParentStart] is CategoryRow.Sub) {
                                            targetParentStart--
                                        }
                                        // Remove block first
                                        for (i in 0 until blockSize) mutable.removeAt(blockStart)
                                        // Insert at the target parent start
                                        mutable.addAll(targetParentStart, block)
                                        val newDraggedIdx = targetParentStart
                                        val oldInfo = findItemInfo(currentIndex)
                                        val newTargetInfo = findItemInfo(targetIndex)
                                        if (oldInfo != null && newTargetInfo != null) {
                                            dragOffset -= (newTargetInfo.offset - oldInfo.offset).toFloat()
                                        }
                                        draggedItemIndex = newDraggedIdx
                                        currentRows = mutable
                                    }
                                }
                            } else {
                                // Subcategory: simple swap within the group
                                val temp = mutable[currentIndex]
                                mutable[currentIndex] = mutable[targetIndex]
                                mutable[targetIndex] = temp
                                currentRows = mutable

                                // Adjust offset so the item stays under the finger
                                val targetInfo = findItemInfo(targetIndex)
                                val currentInfo = findItemInfo(currentIndex)
                                if (targetInfo != null && currentInfo != null) {
                                    dragOffset -= (targetInfo.offset - currentInfo.offset).toFloat()
                                }
                                draggedItemIndex = targetIndex
                            }
                        }
                    },
                    onDragEnd = {
                        // Persist: extract just parent IDs or sub IDs in order for the affected group
                        val group = draggedGroup
                        if (group != null) {
                            val orderedIds = currentRows
                                .filter { it.dragGroup() == group }
                                .map { it.categoryId() }
                            onReorder(orderedIds)
                        }
                        draggedItemIndex = null
                        dragOffset = 0f
                        draggedGroup = null
                    },
                    onDragCancel = {
                        draggedItemIndex = null
                        dragOffset = 0f
                        draggedGroup = null
                    }
                )
            },
        contentPadding = PaddingValues(bottom = 88.dp)
    ) {
        items(
            items = currentRows,
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
            val index = currentRows.indexOf(row)
            val isDragged = index == draggedItemIndex

            val elevationMod = if (isDragged) {
                Modifier
                    .zIndex(1f)
                    .graphicsLayer {
                        translationY = dragOffset
                        shadowElevation = 8.dp.toPx()
                        shape = RoundedCornerShape(8.dp)
                        clip = true
                    }
                    .background(
                        MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(8.dp)
                    )
            } else {
                Modifier.animateItem()
            }

            Box(modifier = elevationMod) {
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
                                onExpandToggle(row.category.id)
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
                result.add(CategoryRow.Sub(sub, subCleanName, subIconData, parent.id))
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
