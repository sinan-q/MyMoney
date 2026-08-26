package com.sinxn.mymoney.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * A generic LazyColumn that supports drag-and-drop reordering with smooth animations,
 * haptic feedback, and custom item rendering.
 */
@Composable
fun <T> ReorderableLazyColumn(
    items: List<T>,
    key: (T) -> Any,
    onReorder: (List<T>) -> Unit,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    isReorderEnabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
    itemShape: Shape = RoundedCornerShape(12.dp),
    dragContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentType: ((T) -> Any?)? = null,
    itemContent: @Composable (item: T, isDragging: Boolean, handleModifier: Modifier) -> Unit
) {
    var localList by remember { mutableStateOf(items) }
    val hapticFeedback = LocalHapticFeedback.current
    val currentOnReorder by rememberUpdatedState(onReorder)

    LaunchedEffect(items) {
        val currentKeys = localList.map(key)
        val newKeys = items.map(key)
        if (currentKeys != newKeys || items != localList) {
            localList = items
        }
    }

    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState
    ) { from, to ->
        val fromIdx = from.index
        val toIdx = to.index
        if (fromIdx in localList.indices && toIdx in localList.indices && fromIdx != toIdx) {
            localList = localList.toMutableList().apply {
                add(toIdx, removeAt(fromIdx))
            }
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement
    ) {
        items(
            items = localList,
            key = key,
            contentType = contentType?.let { ct -> { ct(it) } } ?: { null }
        ) { item ->
            ReorderableItem(
                state = reorderableState,
                key = key(item)
            ) { isDragging ->
                val scale by animateFloatAsState(
                    targetValue = if (isDragging) 1.02f else 1.0f,
                    label = "reorderDragScale"
                )

                Surface(
                    shape = itemShape,
                    color = if (isDragging) dragContainerColor else Color.Transparent,
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
                            },
                            onDragStopped = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                currentOnReorder(localList)
                            }
                        )
                ) {
                    val handleModifier = Modifier.draggableHandle(
                        onDragStarted = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragStopped = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureEnd)
                            currentOnReorder(localList)
                        }
                    )

                    itemContent(item, isDragging, handleModifier)
                }
            }
        }
    }
}
