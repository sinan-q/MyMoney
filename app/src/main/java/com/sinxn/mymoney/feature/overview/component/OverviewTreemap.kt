package com.sinxn.mymoney.feature.overview.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class TreemapSubNode(
    val categoryId: String,
    val name: String,
    val value: Float,
    val formattedAmount: String,
    val percent: String
)

data class TreemapNode(
    val categoryId: String,
    val name: String,
    val value: Float,
    val color: Color,
    val formattedAmount: String,
    val percent: String,
    val subNodes: List<TreemapSubNode> = emptyList()
)

data class Rect(val x: Float, val y: Float, val width: Float, val height: Float)

data class SubRect(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val subNode: TreemapSubNode
)

data class DottedLine(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float
)

fun computeSubRects(
    subNodes: List<TreemapSubNode>,
    totalWidth: Float,
    totalHeight: Float
): Pair<List<SubRect>, List<DottedLine>> {
    val subRects = mutableListOf<SubRect>()
    val lines = mutableListOf<DottedLine>()
    var remaining = subNodes
    var curX = 0f
    var curY = 0f
    var curW = totalWidth
    var curH = totalHeight

    while (remaining.isNotEmpty()) {
        val totalRemaining = remaining.sumOf { it.value.toDouble() }.toFloat()
        if (totalRemaining <= 0f) break
        val isLast = remaining.size == 1
        val sub = remaining.first()

        if (isLast) {
            subRects.add(SubRect(curX, curY, curW, curH, sub))
            break
        }

        val ratio = (sub.value / totalRemaining).coerceIn(0f, 1f)
        if (curW >= curH) {
            val sliceW = curW * ratio
            subRects.add(SubRect(curX, curY, sliceW, curH, sub))
            curX += sliceW
            curW -= sliceW
            lines.add(DottedLine(curX, curY, curX, curY + curH))
        } else {
            val sliceH = curH * ratio
            subRects.add(SubRect(curX, curY, curW, sliceH, sub))
            curY += sliceH
            curH -= sliceH
            lines.add(DottedLine(curX, curY, curX + curW, curY))
        }
        remaining = remaining.drop(1)
    }
    return Pair(subRects, lines)
}

@Composable
fun OverviewTreemap(
    nodes: List<TreemapNode>,
    selectedCategoryId: String?,
    onNodeClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (nodes.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text(text = "No data available", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    Layout(
        content = {
            nodes.forEach { node ->
                TreemapBlock(
                    node = node,
                    isSelected = node.categoryId == selectedCategoryId,
                    isOtherSelected = selectedCategoryId != null && node.categoryId != selectedCategoryId,
                    onClick = { onNodeClick(node.categoryId) }
                )
            }
        },
        modifier = modifier
    ) { measurables, constraints ->
        // Calculate parent rects
        val rects = mutableListOf<Rect>()
        var remainingNodes = nodes
        var currentX = 0f
        var currentY = 0f
        var currentWidth = constraints.maxWidth.toFloat()
        var currentHeight = constraints.maxHeight.toFloat()
        
        while (remainingNodes.isNotEmpty()) {
            val totalRemaining = remainingNodes.sumOf { it.value.toDouble() }.toFloat()
            if (totalRemaining <= 0) break
            
            val isLast = remainingNodes.size == 1
            if (isLast) {
                rects.add(Rect(currentX, currentY, currentWidth, currentHeight))
                break
            }

            if (currentWidth >= currentHeight) {
                val node = remainingNodes.first()
                val ratio = node.value / totalRemaining
                val nodeWidth = currentWidth * ratio
                rects.add(Rect(currentX, currentY, nodeWidth, currentHeight))
                currentX += nodeWidth
                currentWidth -= nodeWidth
            } else {
                val node = remainingNodes.first()
                val ratio = node.value / totalRemaining
                val nodeHeight = currentHeight * ratio
                rects.add(Rect(currentX, currentY, currentWidth, nodeHeight))
                currentY += nodeHeight
                currentHeight -= nodeHeight
            }
            remainingNodes = remainingNodes.drop(1)
        }

        val placeables = measurables.mapIndexed { index, measurable ->
            val rect = rects.getOrNull(index) ?: Rect(0f, 0f, 0f, 0f)
            measurable.measure(
                Constraints.fixed(rect.width.toInt().coerceAtLeast(0), rect.height.toInt().coerceAtLeast(0))
            ) to rect
        }

        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEach { (placeable, rect) ->
                placeable.place(rect.x.toInt(), rect.y.toInt())
            }
        }
    }
}

@Composable
fun TreemapBlock(
    node: TreemapNode,
    isSelected: Boolean,
    isOtherSelected: Boolean,
    onClick: () -> Unit
) {
    val alpha by animateFloatAsState(targetValue = if (isOtherSelected) 0.3f else 1.0f, label = "alpha")

    Box(
        modifier = Modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(node.color.copy(alpha = alpha))
            .clickable { onClick() }
    ) {
        if (node.subNodes.size <= 1) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                Text(
                    text = node.name,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Column {
                    Text(
                        text = node.formattedAmount,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = node.percent,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                        maxLines = 1
                    )
                }
            }
        } else {
            // Multiple subcategories inside the parent block — partitioned and split by dotted line
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val (_, lines) = computeSubRects(
                            subNodes = node.subNodes,
                            totalWidth = size.width,
                            totalHeight = size.height
                        )
                        val stroke = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()), 0f)
                        )
                        lines.forEach { line ->
                            drawLine(
                                color = Color.White.copy(alpha = 0.55f),
                                start = Offset(line.startX, line.startY),
                                end = Offset(line.endX, line.endY),
                                strokeWidth = stroke.width,
                                pathEffect = stroke.pathEffect
                            )
                        }
                    }
            ) {
                Layout(
                    content = {
                        node.subNodes.forEachIndexed { index, sub ->
                            SubBlockCell(
                                parentName = node.name,
                                subNode = sub,
                                isPrimary = index == 0
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { measurables, constraints ->
                    val (subRects, _) = computeSubRects(
                        subNodes = node.subNodes,
                        totalWidth = constraints.maxWidth.toFloat(),
                        totalHeight = constraints.maxHeight.toFloat()
                    )

                    val placeables = measurables.mapIndexed { index, measurable ->
                        val rect = subRects.getOrNull(index) ?: SubRect(0f, 0f, 0f, 0f, node.subNodes[index])
                        val w = rect.width.toInt().coerceAtLeast(0)
                        val h = rect.height.toInt().coerceAtLeast(0)
                        measurable.measure(Constraints.fixed(w, h)) to rect
                    }

                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeables.forEach { (placeable, rect) ->
                            placeable.place(rect.x.toInt(), rect.y.toInt())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubBlockCell(
    parentName: String,
    subNode: TreemapSubNode,
    isPrimary: Boolean
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(6.dp)
    ) {
        val w = maxWidth
        val h = maxHeight
        if (w >= 36.dp && h >= 26.dp) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                Column {
                    if (isPrimary) {
                        Text(
                            text = parentName,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (h >= 52.dp) {
                            Text(
                                text = subNode.name,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Text(
                            text = subNode.name,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (h >= 40.dp) {
                    Column {
                        Text(
                            text = subNode.formattedAmount,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (h >= 58.dp) {
                            Text(
                                text = subNode.percent,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
