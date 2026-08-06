package com.sinxn.mymoney.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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

private data class CategoryGroupData(
    val parent: CategoryEntity,
    val subcategories: List<CategoryEntity>
)

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

    // Dedicated LazyListStates per page to prevent scroll index jumps
    val expenseListState = rememberLazyListState()
    val incomeListState = rememberLazyListState()
    val searchListState = rememberLazyListState()
    
    var searchQuery by remember { mutableStateOf("") }

    // Pre-calculate stable grouped categories at top level to avoid recomposition jitter
    val expenseGrouped = remember(expenseCategories) { groupCategories(expenseCategories) }
    val incomeGrouped = remember(incomeCategories) { groupCategories(incomeCategories) }

    // Custom NestedScrollConnection to stop ModalBottomSheet bottom overscroll velocity bounce
    val stopBottomOverscrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // Consume downward scroll delta when reaching bottom of list so sheet drag doesn't glitch
                return if (available.y < 0f) Offset(0f, available.y) else Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                // Consume downward fling velocity at bottom edge
                return if (available.y < 0f) Velocity(0f, available.y) else Velocity.Zero
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.45f),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            // 1. Merged Header Row (Title + Segmented Tabs + Close Button)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 12.dp, top = 2.dp, bottom = 6.dp),
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
                    // Segmented Tab Switcher
                    Row(
                        modifier = Modifier
                            .height(34.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                shape = CircleShape
                            )
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val tabs = listOf("Expenses" to Color(0xFFE11D48), "Income" to Color(0xFF10B981))
                        tabs.forEachIndexed { index, (title, activeAccent) ->
                            val isSelected = pagerState.currentPage == index
                            val backgroundColor by animateColorAsState(
                                targetValue = if (isSelected) activeAccent else Color.Transparent,
                                label = "MicroTabBg"
                            )
                            val contentColor by animateColorAsState(
                                targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                label = "MicroTabContent"
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(backgroundColor)
                                    .clickable {
                                        scope.launch { pagerState.animateScrollToPage(index) }
                                    }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = contentColor
                                )
                            }
                        }
                    }

                    // Close Button
                    IconButton(
                        onClick = {
                            scope.launch {
                                sheetState.hide()
                                onDismissRequest()
                            }
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close, 
                            contentDescription = "Close",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 2. Search Field
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { 
                        Text(
                            "Search categories...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        ) 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { 
                        Icon(
                            Icons.Default.Search, 
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        ) 
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    Icons.Default.Close, 
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else null,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 3. Pager Content with Intercepted Bottom Overscroll
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = true,
                beyondViewportPageCount = 1
            ) { page ->
                val rawCategories = if (page == 0) expenseCategories else incomeCategories
                val groupedCategories = if (page == 0) expenseGrouped else incomeGrouped
                val listState = if (page == 0) expenseListState else incomeListState

                if (searchQuery.isNotEmpty()) {
                    // Search Mode
                    val matchingCategories = remember(rawCategories, searchQuery) {
                        rawCategories.filter {
                            it.name.replace("  ↳ ", "").replace("↳", "").contains(searchQuery, ignoreCase = true)
                        }
                    }

                    if (matchingCategories.isEmpty()) {
                        EmptyCategoryState()
                    } else {
                        LazyColumn(
                            state = searchListState,
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(stopBottomOverscrollConnection),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 64.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            itemsIndexed(
                                items = matchingCategories,
                                key = { idx, item -> "search_${item.id}_$idx" }
                            ) { _, category ->
                                UltraCompactCategoryListItem(
                                    category = category,
                                    isSelected = category.id == selectedCategoryId,
                                    onClick = {
                                        onCategorySelected(category)
                                        scope.launch {
                                            sheetState.hide()
                                            onDismissRequest()
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Standard View with Intercepted Bottom Overscroll
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(stopBottomOverscrollConnection),
                        contentPadding = PaddingValues( top = 4.dp, bottom = 64.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(
                            items = groupedCategories,
                            key = { idx, group -> "group_${group.parent.id}_$idx" }
                        ) { _, group ->
                            DenseCategoryGroupCard(
                                group = group,
                                selectedCategoryId = selectedCategoryId,
                                onCategorySelected = { category ->
                                    onCategorySelected(category)
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

/**
 * Groups parent categories with their respective subcategories cleanly & deterministically
 */
private fun groupCategories(categories: List<CategoryEntity>): List<CategoryGroupData> {
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

    val result = parents.map { parent ->
        CategoryGroupData(
            parent = parent,
            subcategories = subMap[parent.id] ?: emptyList()
        )
    }.toMutableList()

    for (orphan in orphaned) {
        result.add(CategoryGroupData(parent = orphan, subcategories = emptyList()))
    }

    return result
}

@Composable
private fun DenseCategoryGroupCard(
    group: CategoryGroupData,
    selectedCategoryId: String?,
    onCategorySelected: (CategoryEntity) -> Unit
) {
    val parent = group.parent
    val isParentSelected = parent.id == selectedCategoryId
    val cleanParentName = remember(parent.name) {
        parent.name.replace("  ↳ ", "").replace("↳", "").trim()
    }

    val isAnySubSelected = remember(group.subcategories, selectedCategoryId) {
        group.subcategories.any { it.id == selectedCategoryId }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isParentSelected || isAnySubSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
               else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        border = BorderStroke(
            width = 1.dp,
            color = if (isAnySubSelected && !isParentSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                   else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // Parent Header Row - Single Crisp Highlight Border when Selected
            Surface(
                onClick = { onCategorySelected(parent) },
                shape = RoundedCornerShape(10.dp),
                color = if (isParentSelected) MaterialTheme.colorScheme.primaryContainer 
                       else Color.Transparent,
                border = if (isParentSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        CategoryIcon(
                            iconString = parent.icon,
                            categoryName = cleanParentName,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = cleanParentName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (isParentSelected) FontWeight.ExtraBold else FontWeight.Bold,
                            color = if (isParentSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                        if (group.subcategories.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${group.subcategories.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isParentSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            // Dense 3-Column Subcategory Grid
            if (group.subcategories.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                
                val subChunks = remember(group.subcategories) {
                    group.subcategories.chunked(3)
                }
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    subChunks.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            rowItems.forEach { subCat ->
                                val isSubSelected = subCat.id == selectedCategoryId
                                val cleanSubName = subCat.name.replace("  ↳ ", "").replace("↳", "").trim()

                                Surface(
                                    onClick = { onCategorySelected(subCat) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSubSelected) MaterialTheme.colorScheme.primaryContainer 
                                           else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                    border = BorderStroke(
                                        width = if (isSubSelected) 1.5.dp else 1.dp,
                                        color = if (isSubSelected) MaterialTheme.colorScheme.primary 
                                               else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CategoryIcon(
                                            iconString = subCat.icon,
                                            categoryName = cleanSubName,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = cleanSubName,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSubSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                            color = if (isSubSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            // Fill empty spaces in 3-item row chunk
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UltraCompactCategoryListItem(
    category: CategoryEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val cleanName = remember(category.name) {
        category.name.replace("  ↳ ", "").replace("↳", "").trim()
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
               else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary 
                   else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIcon(
                iconString = category.icon,
                categoryName = cleanName,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = cleanName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmptyCategoryState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Block, 
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "No matching categories", 
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
