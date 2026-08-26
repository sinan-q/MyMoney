package com.sinxn.mymoney.feature.template

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sinxn.mymoney.core.ui.components.AppExtendedFab
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.CategoryIconExtended
import com.sinxn.mymoney.core.ui.components.EmptyListItem
import com.sinxn.mymoney.core.ui.components.FinanceListItem
import com.sinxn.mymoney.core.ui.components.SearchBar
import com.sinxn.mymoney.core.ui.components.TabPill
import com.sinxn.mymoney.ui.theme.ExpenseColor
import com.sinxn.mymoney.ui.theme.IncomeColor
import com.sinxn.mymoney.ui.theme.TransferColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateListScreen(
    onAddTemplateClick: (Boolean) -> Unit = {},
    onTemplateClick: (String, Boolean) -> Unit = { _, _ -> },
    viewModel: TemplateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val txListState = rememberLazyListState()
    val transferListState = rememberLazyListState()
    val currentListState = if (pagerState.currentPage == 0) txListState else transferListState

    val isTx = pagerState.currentPage == 0
    val fabText = if (isTx) "New Template" else "New Transfer Template"
    val fabColor = if (isTx) MaterialTheme.colorScheme.primary else TransferColor

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            AppExtendedFab(
                text = fabText,
                icon = Icons.Default.Add,
                onClick = { onAddTemplateClick(pagerState.currentPage == 1) },
                expanded = !currentListState.isScrollInProgress,
                containerColor = fabColor,
                contentColor = Color.White
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search field
            val totalCount = uiState.totalTxCount + uiState.totalTrCount
            if (totalCount > 5 || uiState.searchQuery.isNotEmpty()) {
                SearchBar(searchQuery = uiState.searchQuery, setSearchQuery = viewModel::setSearchQuery)
            }

            // Tabs
            TabPill(
                tabs = listOf(
                    "Transactions" to MaterialTheme.colorScheme.primary,
                    "Transfers" to TransferColor
                ),
                activeTab = pagerState.currentPage,
                onTabChange = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                }
            )

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    key = { page -> page }
                ) { page ->
                    val isTxPage = page == 0
                    val countText = if (isTxPage) {
                        if (uiState.searchQuery.isNotEmpty()) "Transactions (${uiState.transactionTemplates.size})" else "Transactions (${uiState.totalTxCount})"
                    } else {
                        if (uiState.searchQuery.isNotEmpty()) "Transfers (${uiState.transferTemplates.size})" else "Transfers (${uiState.totalTrCount})"
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = countText,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isTxPage) {
                            if (uiState.transactionTemplates.isEmpty()) {
                                EmptyListItem(
                                    text = "transaction",
                                    isSearching = uiState.searchQuery.isNotEmpty()
                                )
                            } else {
                                LazyColumn(
                                    state = txListState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 88.dp)
                                ) {
                                    items(
                                        items = uiState.transactionTemplates,
                                        key = { it.id },
                                        contentType = { "tx_template" }
                                    ) { item ->
                                        FinanceListItem(
                                            icon = {
                                                CategoryIcon(iconData = item.iconData)
                                            },
                                            title = item.title,
                                            subtitle = item.subtitle,
                                            trailingContent = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = item.formattedMoney,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = if (item.isIncome) IncomeColor else ExpenseColor,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(Modifier.width(8.dp))
                                                    IconButton(
                                                        onClick = {
                                                            viewModel.applyTransactionTemplate(item.rawItem)
                                                            Toast.makeText(context, "Template applied!", Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Bolt,
                                                            contentDescription = "Apply Template",
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = { onTemplateClick(item.id, false) }
                                        )
                                    }
                                }
                            }
                        } else {
                            if (uiState.transferTemplates.isEmpty()) {
                                EmptyListItem(
                                    text = "transfer",
                                    isSearching = uiState.searchQuery.isNotEmpty()
                                )
                            } else {
                                LazyColumn(
                                    state = transferListState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 88.dp)
                                ) {
                                    items(
                                        items = uiState.transferTemplates,
                                        key = { it.id },
                                        contentType = { "transfer_template" }
                                    ) { item ->
                                        FinanceListItem(
                                            icon = {
                                                CategoryIconExtended(
                                                    color = TransferColor,
                                                    icon = Icons.Default.SwapHoriz
                                                )
                                            },
                                            title = item.title,
                                            subtitle = item.subtitle,
                                            trailingContent = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = item.formattedMoney,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = TransferColor,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(Modifier.width(8.dp))
                                                    IconButton(
                                                        onClick = {
                                                            viewModel.applyTransferTemplate(item.rawItem)
                                                            Toast.makeText(context, "Transfer applied!", Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Bolt,
                                                            contentDescription = "Apply Transfer Template",
                                                            tint = TransferColor
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = { onTemplateClick(item.id, true) }
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
