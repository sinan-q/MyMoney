package com.sinxn.mymoney.feature.category.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.feature.category.ParentCategoryItem
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun CategoryReorderableList(
    items: List<ParentCategoryItem>,
    lazyListState: LazyListState,
    onCategoryClick: (String) -> Unit,
    onExpandToggle: (String) -> Unit,
    onReorderParents: (List<String>) -> Unit,
    onReorderSubcategories: (List<String>) -> Unit
) {
    var parentList by remember { mutableStateOf(items) }
    val hapticFeedback = LocalHapticFeedback.current
    val currentOnReorderParents by rememberUpdatedState(onReorderParents)

    LaunchedEffect(items) {
        val currentIds = parentList.map { it.category.id }
        val newIds = items.map { it.category.id }
        if (currentIds != newIds || items != parentList) {
            parentList = items
        }
    }

    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState
    ) { from, to ->
        val fromIdx = from.index
        val toIdx = to.index
        if (fromIdx in parentList.indices && toIdx in parentList.indices && fromIdx != toIdx) {
            parentList = parentList.toMutableList().apply {
                add(toIdx, removeAt(fromIdx))
            }
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = parentList,
            key = { it.category.id },
            contentType = { "parent_category_card" }
        ) { parentItem ->
            ReorderableItem(
                state = reorderableState,
                key = parentItem.category.id
            ) { isDragging ->

                val scale by animateFloatAsState(
                    targetValue = if (isDragging) 1.02f else 1.0f,
                    label = "parentDragScale"
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDragging) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        Color.Transparent
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .longPressDraggableHandle(
                            onDragStarted = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDragStopped = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                currentOnReorderParents(parentList.map { it.category.id })
                            }
                        )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Parent Category Header Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCategoryClick(parentItem.category.id) }
                                .padding(horizontal = 6.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Drag Handle Icon (also supports immediate drag on handle)
                            Box(
                                modifier = Modifier
                                    .draggableHandle(
                                        onDragStarted = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onDragStopped = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                            currentOnReorderParents(parentList.map { it.category.id })
                                        }
                                    )
                                    .padding(end = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DragIndicator,
                                    contentDescription = "Reorder",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            CategoryIcon(iconData = parentItem.iconData)

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = parentItem.cleanName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (parentItem.subcategories.isNotEmpty()) {
                                    Text(
                                        text = "${parentItem.subcategories.size} subcategor${if (parentItem.subcategories.size == 1) "y" else "ies"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            if (parentItem.subcategories.isNotEmpty()) {
                                IconButton(
                                    onClick = { onExpandToggle(parentItem.category.id) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (parentItem.isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (parentItem.isExpanded) "Collapse" else "Expand",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        // Subcategories Expandable Section
                        AnimatedVisibility(
                            visible = parentItem.isExpanded && parentItem.subcategories.isNotEmpty(),
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            SubcategoriesSection(
                                subcategories = parentItem.subcategories,
                                onSubcategoryClick = onCategoryClick,
                                onReorderSubcategories = onReorderSubcategories
                            )
                        }
                    }
                }
            }
        }
    }
}
