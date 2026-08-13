package com.sinxn.mymoney.feature.category

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.util.CategoryType

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
fun CategoryListScreen(
    onNavigateBack: () -> Unit,
    viewModel: CategoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Expense, 1: Income

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
                    val targetType = if (selectedTab == 0) CategoryType.EXPENSE else CategoryType.INCOME
                    viewModel.openCreateCategoryDialog(targetType)
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
            // Tab Pill Selector Row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                TabPill(
                    activeTab = selectedTab,
                    onTabChange = { index ->
                        selectedTab = index
                    }
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
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut())
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut())
                        }
                    },
                    modifier = Modifier.weight(1f),
                    label = "TabTransition"
                ) { page ->
                    val flatRows = if (page == 0) expenseFlatRows else incomeFlatRows
                    val listState = if (page == 0) expenseListState else incomeListState

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
                                            hasSubcategories = row.subcategoryCount > 0,
                                            isExpanded = row.isExpanded,
                                            onRowClick = {
                                                viewModel.openEditCategoryDialog(row.category)
                                            },
                                            onExpandToggle = {
                                                toggleParentExpanded(row.category.id)
                                            }
                                        )
                                    }
                                    is CategoryRow.Sub -> {
                                        SubcategoryCategoryRow(
                                            category = row.category,
                                            onClick = {
                                                viewModel.openEditCategoryDialog(row.category)
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

        if (uiState.isEditDialogOpen) {
            CategoryEditDialog(
                uiState = uiState,
                onNameChange = viewModel::onNameChange,
                onDismiss = viewModel::closeDialog,
                onSave = viewModel::saveCategory,
                onDelete = {
                    uiState.editingCategory?.let { viewModel.deleteCategory(it) }
                    viewModel.closeDialog()
                }
            )
        }
    }
}

@Composable
private fun TabPill(
    activeTab: Int,
    onTabChange: (Int) -> Unit
) {
    val tabs = listOf("Expense" to Color(0xFFE11D48), "Income" to Color(0xFF10B981))

    Row(
        modifier = Modifier
            .height(34.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
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
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun ParentCategoryRow(
    category: CategoryEntity,
    hasSubcategories: Boolean,
    isExpanded: Boolean,
    onRowClick: () -> Unit,
    onExpandToggle: () -> Unit
) {
    val cleanName = remember(category.name) {
        category.name.replace("  ↳ ", "").replace("↳", "").trim()
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onRowClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
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
    category: CategoryEntity,
    onClick: () -> Unit
) {
    val cleanName = remember(category.name) {
        category.name.replace("  ↳ ", "").replace("↳", "").trim()
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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

@Composable
private fun CategoryEditDialog(
    uiState: CategoryUiState,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    val isEditing = uiState.editingCategory != null
    val canDelete = isEditing && uiState.editingCategory?.tag == null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Category" else "New Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = uiState.editName,
                    onValueChange = onNameChange,
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = uiState.editName.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (canDelete) {
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

