package com.sinxn.mymoney.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt

@Immutable
data class DonutSlice(
    val label: String,
    val value: Float,
    val color: Color,
    val formattedValue: String = ""
)

/**
 * A premium donut chart composable with sweep entrance animation,
 * tap-to-select interactivity, and a center information badge.
 *
 * @param slices Data slices to render. Each slice has a label, value, color,
 *               and optional formatted value string.
 * @param modifier Modifier for the chart container.
 * @param chartSize The overall size of the donut chart.
 * @param strokeWidth The width of the donut ring.
 * @param gapAngle Angular gap (degrees) between consecutive arcs.
 * @param centerTitle Default center text (shown when no slice is selected).
 * @param centerSubtitle Default center subtitle (shown when no slice is selected).
 * @param selectedExpandPx How many pixels a selected arc expands outward.
 * @param animationDurationMs Duration of the sweep entrance animation.
 */
@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    chartSize: Dp = 200.dp,
    strokeWidth: Dp = 32.dp,
    gapAngle: Float = 2.5f,
    centerTitle: String = "",
    centerSubtitle: String = "",
    selectedExpandPx: Float = 8f,
    animationDurationMs: Int = 800
) {
    if (slices.isEmpty()) return

    val totalValue = remember(slices) { slices.sumOf { it.value.toDouble() }.toFloat() }
    if (totalValue <= 0f) return

    // Sweep animation
    val sweepProgress = remember { Animatable(0f) }
    LaunchedEffect(slices) {
        sweepProgress.snapTo(0f)
        sweepProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = animationDurationMs,
                easing = FastOutSlowInEasing
            )
        )
    }

    // Selection state: -1 = none selected
    var selectedIndex by remember(slices) { mutableIntStateOf(-1) }

    // Pre-compute slice angles
    val sliceAngles = remember(slices, totalValue) {
        val totalGap = gapAngle * slices.size
        val availableSweep = 360f - totalGap
        slices.map { slice ->
            (slice.value / totalValue) * availableSweep
        }
    }

    // Determine center text
    val displayTitle: String
    val displaySubtitle: String
    if (selectedIndex in slices.indices) {
        val sel = slices[selectedIndex]
        displayTitle = sel.label
        val pct = if (totalValue > 0f) (sel.value / totalValue) * 100f else 0f
        displaySubtitle = if (sel.formattedValue.isNotEmpty()) {
            "${String.format("%.1f", pct)}% • ${sel.formattedValue}"
        } else {
            String.format("%.1f%%", pct)
        }
    } else {
        displayTitle = centerTitle
        displaySubtitle = centerSubtitle
    }

    Box(
        modifier = modifier.size(chartSize),
        contentAlignment = Alignment.Center
    ) {
        val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(slices) {
                    detectTapGestures { tapOffset ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val dx = tapOffset.x - centerX
                        val dy = tapOffset.y - centerY
                        val dist = sqrt(dx * dx + dy * dy)
                        val outerRadius = min(size.width, size.height) / 2f
                        val strokePx = strokeWidth.toPx()
                        val innerRadius = outerRadius - strokePx

                        // Only process taps within the donut ring
                        if (dist < innerRadius || dist > outerRadius + selectedExpandPx) {
                            selectedIndex = -1
                            return@detectTapGestures
                        }

                        // Calculate angle of tap (0° at 3 o'clock, clockwise)
                        var angle = Math
                            .toDegrees(
                                atan2(dy.toDouble(), dx.toDouble())
                            )
                            .toFloat()
                        // Adjust to start from top (12 o'clock = -90°)
                        angle = (angle + 90f + 360f) % 360f

                        // Find which slice the tap falls in
                        var cumAngle = 0f
                        for (i in sliceAngles.indices) {
                            val sliceSweep = sliceAngles[i]
                            val sliceStart = cumAngle
                            val sliceEnd = cumAngle + sliceSweep
                            if (angle >= sliceStart && angle <= sliceEnd) {
                                selectedIndex = if (selectedIndex == i) -1 else i
                                return@detectTapGestures
                            }
                            cumAngle += sliceSweep + gapAngle
                        }
                        selectedIndex = -1
                    }
                }
        ) {
            val strokePx = strokeWidth.toPx()
            val padding = selectedExpandPx + 2f
            val outerDiameter = min(size.width, size.height) - (padding * 2)
            val topLeft = Offset(
                (size.width - outerDiameter) / 2f,
                (size.height - outerDiameter) / 2f
            )
            val arcSize = Size(outerDiameter, outerDiameter)

            // Draw background track ring
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Butt)
            )

            val animatedSweep = sweepProgress.value * 360f
            var currentStartAngle = -90f // Start from top

            for (i in slices.indices) {
                val sliceSweep = sliceAngles[i]
                val isSelected = selectedIndex == i

                // Clamp to animated progress
                val drawSweep = (sliceSweep).coerceAtMost(
                    (animatedSweep - (currentStartAngle + 90f)).coerceAtLeast(0f)
                )

                if (drawSweep > 0f) {
                    val currentStrokeWidth = if (isSelected) strokePx + selectedExpandPx else strokePx
                    val expandOffset = if (isSelected) selectedExpandPx / 2f else 0f
                    val selTopLeft = Offset(
                        topLeft.x - expandOffset,
                        topLeft.y - expandOffset
                    )
                    val selArcSize = Size(
                        arcSize.width + expandOffset * 2,
                        arcSize.height + expandOffset * 2
                    )

                    drawArc(
                        color = slices[i].color,
                        startAngle = currentStartAngle,
                        sweepAngle = drawSweep,
                        useCenter = false,
                        topLeft = selTopLeft,
                        size = selArcSize,
                        style = Stroke(
                            width = currentStrokeWidth,
                            cap = StrokeCap.Butt
                        )
                    )
                }

                currentStartAngle += sliceSweep + gapAngle
            }
        }

        // Center information badge
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 36.dp)
        ) {
            if (displayTitle.isNotEmpty()) {
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (displaySubtitle.isNotEmpty()) {
                Text(
                    text = displaySubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
