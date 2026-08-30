package com.sinxn.mymoney.feature.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
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
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MoneyFormatter
import com.sinxn.mymoney.feature.overview.component.OverviewHeader
import com.sinxn.mymoney.feature.overview.component.OverviewSettingsSheet
import com.sinxn.mymoney.feature.overview.component.OverviewWalletPickerSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    onPeriodClick: (startDate: String, endDate: String) -> Unit = { _, _ -> },
    viewModel: OverviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val allWallets by viewModel.allWallets.collectAsState()

    if (uiState.showSettingsSheet && uiState.settings != null) {
        OverviewSettingsSheet(
            settings = uiState.settings!!,
            currentWalletId = uiState.currentWalletId,
            wallets = allWallets,
            formattingSettings = uiState.formattingSettings,
            onDismiss = { viewModel.dismissSettingsSheet() },
            onApply = { newSettings, newWalletId ->
                viewModel.applyOverviewConfig(newSettings, newWalletId)
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header: View-only active context status card + settings action
        item {
            OverviewHeader(
                walletName = uiState.walletName,
                walletIcon = uiState.walletIcon,
                settings = uiState.settings,
                selectedCategoryName = uiState.selectedCategoryName,
                onConfigureClick = { viewModel.toggleSettingsSheet() }
            )
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
                // Chart Section — Bar + Line in pager
                item {
                    ChartPager(
                        overviewData = overviewData,
                        currencyCode = uiState.currencyCode
                    )
                }

                // Total Summary Card
                item {
                    TotalSummaryCard(
                        totalNetIncomes = overviewData.totalNetIncomes,
                        currencyCode = uiState.currencyCode,
                        decimals = uiState.currencyDecimals,
                        formattingSettings = uiState.formattingSettings
                    )
                }

                // Period List — matching legacy OverviewItemAdapter
                item {
                    Text(
                        text = "Period Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 8.dp)
                    )
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

@Composable
private fun ChartPager(
    overviewData: OverviewData,
    currencyCode: String
) {
    val pagerState = rememberPagerState(pageCount = { 2 })

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Tab indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.BarChart,
                    contentDescription = null,
                    tint = if (pagerState.currentPage == 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(2) { index ->
                        Box(
                            modifier = Modifier
                                .size(if (pagerState.currentPage == index) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Icon(
                    Icons.Default.ShowChart,
                    contentDescription = null,
                    tint = if (pagerState.currentPage == 1) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Chart pages
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) { page ->
                val chartValues = overviewData.chartDataByCurrency[currencyCode]
                    ?: overviewData.chartDataByCurrency.values.firstOrNull()

                if (!chartValues.isNullOrEmpty()) {
                    when (page) {
                        0 -> BarChartView(chartValues)
                        1 -> LineChartView(chartValues)
                    }
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
private fun LineChartView(dataPoints: List<ChartDataPoint>) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val primaryColor = MaterialTheme.colorScheme.primary

    LaunchedEffect(dataPoints) {
        modelProducer.runTransaction {
            lineModel {
                series(dataPoints.map { it.value })
            }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(),
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
private fun TotalSummaryCard(
    totalNetIncomes: MultiCurrencyMoney,
    currencyCode: String,
    decimals: Int,
    formattingSettings: com.sinxn.mymoney.core.data.preferences.FormattingSettings
) {
    val config = remember(formattingSettings) {
        MoneyFormatter.Config(
            showCurrency = formattingSettings.showCurrency,
            groupDigits = formattingSettings.groupDigits,
            roundDecimals = formattingSettings.roundDecimals,
            showPlusMinus = true
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Total for Period",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Show all currencies
            for (currency in totalNetIncomes.getCurrencies()) {
                val amount = totalNetIncomes.getMoney(currency)
                val currDecimals = try {
                    java.util.Currency.getInstance(currency).defaultFractionDigits
                } catch (e: Exception) { decimals }

                val formatted = MoneyFormatter.format(
                    amount = amount,
                    currencyCode = currency,
                    decimals = currDecimals,
                    config = config
                )
                val color = when {
                    amount > 0 -> Color(0xFF2E7D32)
                    amount < 0 -> Color(0xFFC62828)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(
                    text = formatted,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            if (totalNetIncomes.getCurrencies().isEmpty()) {
                Text(
                    text = MoneyFormatter.format(0L, currencyCode, decimals, config),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private val PeriodCardShape = RoundedCornerShape(14.dp)

@Composable
private fun PeriodRow(
    item: OverviewPeriodUiModel,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = PeriodCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Period index badge
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${item.index}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Date range
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.dateRangeText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Show income/expense breakdown
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

            // Net amount for the period
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
}
