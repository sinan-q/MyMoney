package com.sinxn.mymoney.feature.overview.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class CategoryChartData(
    val categoryId: String,
    val categoryName: String,
    val categoryColor: Color,
    val totalFormatted: String,
    val periods: List<ChartPeriodData>,
    val maxValue: Float,
    val subcategories: List<TreemapSubNode> = emptyList()
)

@Composable
fun CategoryDetailChart(
    categoryChartData: CategoryChartData,
    yAxisFormatter: (Float) -> String,
    onClose: () -> Unit,
    scrollState: ScrollState = rememberScrollState(),
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(categoryChartData.categoryColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = categoryChartData.categoryName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Period Total: ${categoryChartData.totalFormatted}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Chart area
            if (categoryChartData.periods.isNotEmpty() && categoryChartData.maxValue > 0f) {
                Row(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                    // Y Axis
                    Column(
                        modifier = Modifier.width(42.dp).fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            yAxisFormatter(categoryChartData.maxValue),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            yAxisFormatter(categoryChartData.maxValue / 2f),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "0",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Bars
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        // Grid lines
                        Column(
                            modifier = Modifier.fillMaxSize().padding(bottom = 24.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)))
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)))
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)))
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .horizontalScroll(scrollState),
                            horizontalArrangement = Arrangement.spacedBy(OverviewChartSpacing, Alignment.Start),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            categoryChartData.periods.forEach { period ->
                                val periodTotal = period.segments.sumOf { it.value.toDouble() }.toFloat()
                                val barHeightFraction = (periodTotal / categoryChartData.maxValue).coerceIn(0f, 1f)

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(OverviewChartColumnWidth)
                                        .fillMaxHeight()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(OverviewChartColumnWidth)
                                            .weight(1f)
                                            .padding(bottom = 8.dp),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        if (barHeightFraction > 0f) {
                                            Box(
                                                modifier = Modifier
                                                    .width(OverviewChartBarWidth)
                                                    .fillMaxHeight(barHeightFraction)
                                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            ) {
                                                Column(modifier = Modifier.fillMaxSize()) {
                                                    period.segments.reversed().forEach { segment ->
                                                        if (segment.value > 0f) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .weight(segment.value)
                                                                    .background(segment.color)
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
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No transactions in this period",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Subcategories legend if more than 1
            if (categoryChartData.subcategories.size > 1) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Subcategories Breakdown",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categoryChartData.subcategories.forEach { sub ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = sub.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = sub.formattedAmount,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
