package com.sinxn.mymoney.feature.category.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.sinxn.mymoney.feature.category.SubcategoryItem
import sh.calvin.reorderable.ReorderableColumn

@Composable
fun SubcategoriesSection(
    subcategories: List<SubcategoryItem>,
    isReorderEnabled: Boolean = true,
    onSubcategoryClick: (String) -> Unit,
    onReorderSubcategories: (List<String>) -> Unit
) {
    var subList by remember { mutableStateOf(subcategories) }
    val hapticFeedback = LocalHapticFeedback.current
    val currentOnReorderSubcategories by rememberUpdatedState(onReorderSubcategories)

    LaunchedEffect(subcategories) {
        val currentIds = subList.map { it.category.id }
        val newIds = subcategories.map { it.category.id }
        if (currentIds != newIds || subcategories != subList) {
            subList = subcategories
        }
    }

    ReorderableColumn(
        list = subList,
        onSettle = { fromIndex, toIndex ->
            subList = subList.toMutableList().apply {
                add(toIndex, removeAt(fromIndex))
            }
            hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureEnd)
            currentOnReorderSubcategories(subList.map { it.category.id })
        },
        onMove = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 12.dp, bottom = 8.dp, top = 6.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(vertical = 4.dp)
    ) { index, subItem, isDragging ->
        key(subItem.category.id) {
            val scale by animateFloatAsState(
                targetValue = if (isDragging) 1.02f else 1.0f,
                label = "subDragScale"
            )

            ReorderableItem {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDragging) {
                        MaterialTheme.colorScheme.surfaceContainerHighest
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
                            enabled = isReorderEnabled,
                            onDragStarted = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        )
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (index > 0 && !isDragging) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSubcategoryClick(subItem.category.id) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isReorderEnabled) {
                                // Drag handle for subcategory
                                Box(
                                    modifier = Modifier
                                        .draggableHandle(
                                            onDragStarted = {
                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        )
                                        .padding(end = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DragIndicator,
                                        contentDescription = "Reorder subcategory",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            CategoryIcon(
                                iconData = subItem.iconData,
                                size = 38.dp
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = subItem.cleanName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}