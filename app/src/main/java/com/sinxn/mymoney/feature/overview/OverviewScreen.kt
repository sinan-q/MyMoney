package com.sinxn.mymoney.feature.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
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
            item {

            }

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
                            settings = uiState.settings,
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
                            currencyCode = uiState.currencyCode
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
    currencyCode: String
) {
    val chartValues = overviewData.chartDataByCurrency[currencyCode]
        ?: overviewData.chartDataByCurrency.values.firstOrNull()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(horizontal = 16.dp)
    ) {
        if (!chartValues.isNullOrEmpty()) {
            BarChartView(chartValues)
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

@Composable
private fun BarChartView(dataPoints: List<ChartDataPoint>) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val primaryColor = MaterialTheme.colorScheme.primary

    LaunchedEffect(dataPoints) {
        modelProducer.runTransaction {
            columnModel { series(dataPoints.map { it.value }) }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(
                columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                    rememberLineComponent(
                        fill = Fill(primaryColor),
                        thickness = 12.dp
                    )
                )
            ),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = { value, x, _ ->
                    dataPoints.getOrNull(x.toInt())?.label ?: ""
                }
            )
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
