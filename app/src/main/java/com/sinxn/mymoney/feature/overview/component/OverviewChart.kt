package com.sinxn.mymoney.feature.overview.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ChartSegment(
    val value: Float,
    val color: Color,
    val categoryId: String? = null
)

data class ChartPeriodData(
    val label: String,
    val segments: List<ChartSegment>
)

val OverviewChartColumnWidth = 26.dp
val OverviewChartBarWidth = 20.dp
val OverviewChartSpacing = 6.dp

@Composable
fun OverviewChart(
    periods: List<ChartPeriodData>,
    maxValue: Float,
    yAxisFormatter: (Float) -> String,
    selectedCategoryId: String? = null,
    onCategoryClick: ((String) -> Unit)? = null,
    scrollState: ScrollState = rememberScrollState(),
    modifier: Modifier = Modifier
) {
    if (periods.isEmpty() || maxValue <= 0f) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No chart data", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LaunchedEffect(periods) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Row(modifier = modifier.fillMaxWidth()) {
        // Y Axis
        Column(
            modifier = Modifier.width(42.dp).fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            Text(yAxisFormatter(maxValue), style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(yAxisFormatter(maxValue / 2f), style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("0", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(20.dp)) // Space for X axis labels
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Chart area
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            // Horizontal grid lines
            Column(modifier = Modifier.fillMaxSize().padding(bottom = 24.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)))
            }

            // Columns
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(OverviewChartSpacing, Alignment.Start),
                verticalAlignment = Alignment.Bottom
            ) {
                periods.forEach { period ->
                    val periodTotal = period.segments.sumOf { it.value.toDouble() }.toFloat()
                    val totalBarHeightFraction = (periodTotal / maxValue).coerceIn(0f, 1f)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(OverviewChartColumnWidth)
                            .fillMaxHeight()
                    ) {
                        // Bar container
                        Box(
                            modifier = Modifier
                                .width(OverviewChartColumnWidth)
                                .weight(1f)
                                .padding(bottom = 8.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            if (totalBarHeightFraction > 0f) {
                                Box(
                                    modifier = Modifier
                                        .width(OverviewChartBarWidth)
                                        .fillMaxHeight(totalBarHeightFraction)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                ) {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        period.segments.reversed().forEach { segment ->
                                            if (segment.value > 0f) {
                                                val isSelected = selectedCategoryId != null && segment.categoryId == selectedCategoryId
                                                val isOtherSelected = selectedCategoryId != null && segment.categoryId != selectedCategoryId
                                                val alpha by animateFloatAsState(
                                                    targetValue = if (isOtherSelected) 0.25f else 1.0f,
                                                    label = "segmentAlpha"
                                                )

                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .weight(segment.value)
                                                        .background(segment.color.copy(alpha = alpha))
                                                        .then(
                                                            if (isSelected) {
                                                                Modifier.border(1.dp, Color.White.copy(alpha = 0.8f))
                                                            } else Modifier
                                                        )
                                                        .then(
                                                            if (segment.categoryId != null && onCategoryClick != null) {
                                                                Modifier.clickable { onCategoryClick(segment.categoryId) }
                                                            } else Modifier
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // X axis label
                        Text(
                            text = period.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(OverviewChartColumnWidth)
                        )
                    }
                }
            }
        }
    }
}
