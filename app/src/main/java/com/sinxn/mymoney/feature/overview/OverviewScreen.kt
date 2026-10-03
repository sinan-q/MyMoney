package com.sinxn.mymoney.feature.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.sinxn.mymoney.core.ui.components.CategoryIconExtended
import com.sinxn.mymoney.core.ui.components.ListRow
import com.sinxn.mymoney.core.util.MoneyFormatter
import com.sinxn.mymoney.feature.overview.component.CategoryDetailChart
import com.sinxn.mymoney.feature.overview.component.OverviewChart
import com.sinxn.mymoney.feature.overview.component.OverviewHeader
import com.sinxn.mymoney.feature.overview.component.OverviewSettingsSheet
import com.sinxn.mymoney.feature.overview.component.OverviewTreemap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    onPeriodClick: (startDate: String, endDate: String) -> Unit = { _, _ -> },
    viewModel: OverviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val allWallets by viewModel.allWallets.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    val mainChartScrollState = rememberScrollState()
    val detailChartScrollState = rememberScrollState()

    LaunchedEffect(mainChartScrollState.value) {
        if (detailChartScrollState.value != mainChartScrollState.value) {
            detailChartScrollState.scrollTo(mainChartScrollState.value)
        }
    }
    LaunchedEffect(detailChartScrollState.value) {
        if (mainChartScrollState.value != detailChartScrollState.value) {
            mainChartScrollState.scrollTo(detailChartScrollState.value)
        }
    }
    LaunchedEffect(uiState.showTreemap) {
        if (!uiState.showTreemap) {
            selectedCategoryId = null
        }
    }

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
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentPadding = paddingValues,
        ) {

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
                val data = uiState.overviewData
                if (data != null) {
                    item {
                        OverviewHeader(
                            walletName = uiState.walletName,
                            totalNetIncomes = data.totalNetIncomes,
                            settings = uiState.settings,
                            currencyCode = uiState.currencyCode,
                            decimals = uiState.currencyDecimals,
                            comparisonIsPostivie = uiState.comparisonIsPositive,
                            comparisonText = uiState.comparisonText,
                            formattingSettings = uiState.formattingSettings,
                            selectedCategoryName = uiState.selectedCategoryName,
                            insightsText = uiState.insightsText,
                            onConfigureClick = { viewModel.toggleSettingsSheet() }
                        )
                    }

                    if (uiState.showTreemap) {
                        item {
                            // Where it went (Treemap)
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
                                Text("Where it went", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                                Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f / 0.86f)) {
                                    OverviewTreemap(
                                        nodes = uiState.treemapNodes,
                                        selectedCategoryId = selectedCategoryId,
                                        onNodeClick = { id ->
                                            selectedCategoryId = if (selectedCategoryId == id) null else id
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                
                                if (selectedCategoryId != null) {
                                    val selectedNode = uiState.treemapNodes.find { it.categoryId == selectedCategoryId }
                                    if (selectedNode != null) {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(selectedNode.color))
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(selectedNode.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                        Text("${selectedNode.percent} of activity", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                    Text(selectedNode.formattedAmount, style = MaterialTheme.typography.titleLarge)
                                                }

                                                if (selectedNode.subNodes.size > 1) {
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(
                                                        text = "Subcategories",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    selectedNode.subNodes.forEach { sub ->
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(vertical = 4.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = "• ${sub.name}",
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Text(
                                                                    text = sub.percent,
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                    modifier = Modifier.padding(end = 8.dp)
                                                                )
                                                                Text(
                                                                    text = sub.formattedAmount,
                                                                    style = MaterialTheme.typography.bodyMedium,
                                                                    fontWeight = FontWeight.SemiBold
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        // When it went (Chart)
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
                            val selectedNode = uiState.treemapNodes.find { it.categoryId == selectedCategoryId }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("When it went", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                if (selectedNode != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(selectedNode.color.copy(alpha = 0.15f))
                                            .clickable { selectedCategoryId = null }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(selectedNode.color))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${selectedNode.name} ✕",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                            
                            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                                OverviewChart(
                                    periods = uiState.chartPeriods,
                                    maxValue = uiState.chartMaxValue,
                                    yAxisFormatter = { value -> 
                                        when {
                                            value >= 1000f -> "${(value / 1000f).toInt()}k"
                                            value >= 10f -> value.toInt().toString()
                                            value > 0f -> String.format(java.util.Locale.US, "%.1f", value)
                                            else -> "0"
                                        }
                                    },
                                    selectedCategoryId = selectedCategoryId,
                                    onCategoryClick = { id ->
                                        selectedCategoryId = if (selectedCategoryId == id) null else id
                                    },
                                    scrollState = mainChartScrollState,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            val categoryDetailData = if (selectedCategoryId != null) {
                                uiState.categoryCharts[selectedCategoryId]
                            } else null

                            androidx.compose.animation.AnimatedVisibility(
                                visible = categoryDetailData != null,
                                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
                                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
                            ) {
                                if (categoryDetailData != null) {
                                    Column(modifier = Modifier.padding(top = 16.dp)) {
                                        CategoryDetailChart(
                                            categoryChartData = categoryDetailData,
                                            yAxisFormatter = { value ->
                                                when {
                                                    value >= 1000f -> "${(value / 1000f).toInt()}k"
                                                    value >= 10f -> value.toInt().toString()
                                                    value > 0f -> String.format(java.util.Locale.US, "%.1f", value)
                                                    else -> "0"
                                                }
                                            },
                                            onClose = { selectedCategoryId = null },
                                            scrollState = detailChartScrollState,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
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
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(48.dp),
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
fun FilterPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.clip(CircleShape).clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
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
