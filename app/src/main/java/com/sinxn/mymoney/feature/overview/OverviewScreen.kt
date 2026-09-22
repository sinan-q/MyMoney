package com.sinxn.mymoney.feature.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import com.patrykandpatrick.vico.compose.cartesian.decoration.HorizontalLine
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.LineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.sinxn.mymoney.core.ui.components.CategoryIconExtended
import com.sinxn.mymoney.core.ui.components.ListRow
import com.sinxn.mymoney.feature.overview.component.OverviewHeader
import com.sinxn.mymoney.feature.overview.component.OverviewSettingsSheet
import com.sinxn.mymoney.feature.overview.component.OverviewTotalSummaryCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    onPeriodClick: (startDate: String, endDate: String) -> Unit = { _, _ -> },
    viewModel: OverviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val allWallets by viewModel.allWallets.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()

    if (uiState.showSettingsSheet && uiState.settings != null) {
        OverviewSettingsSheet(
            settings = uiState.settings!!,
            currentWalletId = uiState.currentWalletId,
            wallets = allWallets,
            categories = allCategories,
            formattingSettings = uiState.formattingSettings,
            onDismiss = { viewModel.dismissSettingsSheet() },
            onApply = { newSettings, newWalletId ->
                viewModel.applyOverviewConfig(newSettings, newWalletId)
            }
        )
    }
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: View-only active context status card + settings action


            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                val overviewData = uiState.overviewData
                if (overviewData != null && overviewData.periods.isNotEmpty()) {
                    item {
                        OverviewHeader(
                            walletName = uiState.walletName,
                            overviewData = overviewData,
                            settings = uiState.settings,
                            currencyCode = uiState.currencyCode,
                            decimals = uiState.currencyDecimals,
                            formattingSettings = uiState.formattingSettings,
                            selectedCategoryName = uiState.selectedCategoryName,
                            onConfigureClick = { viewModel.toggleSettingsSheet() }
                        )
                    }

                    item {
                        OverviewTotalSummaryCard(
                            overviewData = overviewData,
                            settings = uiState.settings,
                            currencyCode = uiState.currencyCode,
                            decimals = uiState.currencyDecimals,
                            formattingSettings = uiState.formattingSettings,
                            selectedCategoryName = uiState.selectedCategoryName
                        )
                    }

                    // Chart Section — Bar chart

                    item {
                        OverviewBarChart(
                            overviewData = overviewData,
                            currencyCode = uiState.currencyCode,
                            isNetIncomeMode = uiState.settings?.cashFlowFilter == CashFlowFilter.NET_INCOMES,
                            isDiverging = uiState.isDivergingChart,
                            onToggleDiverging = { viewModel.setDivergingChart(it) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Period List — matching legacy OverviewItemAdapter
                    item {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Period Breakdown",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Text(
                                text = "${uiState.periodsUi.size} " + "periods",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                    }

                    items(
                        items = uiState.periodsUi,
                        key = { it.id },
                        contentType = { "period_row" }
                    ) { item ->
                        PeriodRow(
                            item = item,
                            onClick = { onPeriodClick(item.startDateTimeSql, item.endDateTimeSql) }
                        )
                    }
                } else {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No data for the selected period",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }


}

@Composable
private fun OverviewBarChart(
    overviewData: OverviewData,
    currencyCode: String,
    isNetIncomeMode: Boolean,
    isDiverging: Boolean,
    onToggleDiverging: (Boolean) -> Unit
) {
    val chartValues = overviewData.chartDataByCurrency[currencyCode]
        ?: overviewData.chartDataByCurrency.values.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isNetIncomeMode) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isDiverging) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2E7D32))
                            )
                            Text(
                                text = "Income",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFC62828))
                            )
                            Text(
                                text = "Expense",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Net Cash Flow",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = !isDiverging,
                        onClick = { onToggleDiverging(false) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Net", style = MaterialTheme.typography.labelSmall)
                    }
                    SegmentedButton(
                        selected = isDiverging,
                        onClick = { onToggleDiverging(true) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Split (±)", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            if (!chartValues.isNullOrEmpty()) {
                BarChartView(
                    dataPoints = chartValues,
                    isDiverging = isNetIncomeMode && isDiverging
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No chart data",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun BarChartView(
    dataPoints: List<ChartDataPoint>,
    isDiverging: Boolean
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val primaryColor = MaterialTheme.colorScheme.primary
    val incomeColor = Color(0xFF2E7D32)
    val expenseColor = Color(0xFFC62828)

    LaunchedEffect(dataPoints, isDiverging) {
        modelProducer.runTransaction {
            columnModel {
                if (isDiverging) {
                    series(dataPoints.map { it.income })
                    series(dataPoints.map { -it.expense })
                } else {
                    series(dataPoints.map { it.value })
                }
            }
        }
    }

    val columnLayer = if (isDiverging) {
        rememberColumnCartesianLayer(
            columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                rememberLineComponent(
                    fill = Fill(incomeColor),
                    thickness = 16.dp
                ),
                rememberLineComponent(
                    fill = Fill(expenseColor),
                    thickness = 16.dp
                )
            ),
            columnCollectionSpacing = 4.dp,
            mergeMode = { ColumnCartesianLayer.MergeMode.Stacked }
        )
    } else {
        rememberColumnCartesianLayer(
            columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                rememberLineComponent(
                    fill = Fill(primaryColor),
                    thickness = 16.dp
                )
            ),
            columnCollectionSpacing = 4.dp
        )
    }

    val startAxis = if (isDiverging) {
        VerticalAxis.rememberStart(
            valueFormatter = { _, value, _ ->
                val absVal = kotlin.math.abs(value)
                if (absVal >= 1_000_000) {
                    String.format(java.util.Locale.US, "%.1fM", absVal / 1_000_000)
                } else if (absVal >= 1000) {
                    String.format(java.util.Locale.US, "%.0fk", absVal / 1000)
                } else {
                    String.format(java.util.Locale.US, "%.0f", absVal)
                }
            }
        )
    } else {
        VerticalAxis.rememberStart()
    }

    val outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val decorations = if (isDiverging) {
        listOf(
            remember(outlineColor) {
                HorizontalLine(
                    y = { 0.0 },
                    line = LineComponent(fill = Fill(outlineColor), thickness = 1.dp)
                )
            }
        )
    } else {
        emptyList()
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            columnLayer,
            startAxis = startAxis,
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = { _, x, _ ->
                    dataPoints.getOrNull(x.toInt())?.label ?: ""
                }
            ),
            decorations = decorations
        ),
        modelProducer = modelProducer,
        modifier = Modifier.fillMaxSize()
    )
}


@Composable
private fun PeriodRow(
    item: OverviewPeriodUiModel,
    onClick: () -> Unit
) {
    ListRow(
        onClick = onClick,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryIconExtended(
                text = "${item.index}",
                color = MaterialTheme.colorScheme.primaryContainer
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column() {
                Text(
                    text = item.dateRangeText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
                if (item.formattedIncome != null || item.formattedExpense != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (item.formattedIncome != null) {
                            Text(
                                text = item.formattedIncome,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2E7D32)
                            )
                        }
                        if (item.formattedExpense != null) {
                            Text(
                                text = item.formattedExpense,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFC62828)
                            )
                        }
                    }
                }
            }
        }
        val color = when (item.netAmountColorType) {
            1 -> Color(0xFF2E7D32)
            -1 -> Color(0xFFC62828)
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        Text(
            text = item.formattedNetAmount,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
