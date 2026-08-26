package com.sinxn.mymoney.feature.wallet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sinxn.mymoney.core.ui.components.AppExtendedFab
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.EmptyListItem
import com.sinxn.mymoney.core.ui.components.FilterComponent
import com.sinxn.mymoney.core.ui.components.FinanceListItem
import com.sinxn.mymoney.core.ui.components.ReorderDragHandle
import com.sinxn.mymoney.core.ui.components.ReorderableLazyColumn
import com.sinxn.mymoney.core.ui.components.SearchBar
import com.sinxn.mymoney.core.ui.components.TabPill
import com.sinxn.mymoney.core.util.MoneyFormatter
import com.sinxn.mymoney.ui.theme.ExpenseColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletListScreen(
    onWalletClick: (String) -> Unit = {},
    onAddWalletClick: () -> Unit = {},
    viewModel: WalletListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    val activeListState = rememberLazyListState()
    val archivedListState = rememberLazyListState()

    val isArchivedTab = pagerState.currentPage == 1
    val currentWallets = if (isArchivedTab) uiState.archivedWallets else uiState.activeWallets
    val currentListState = if (isArchivedTab) archivedListState else activeListState

    val formatterConfig = remember(uiState.formattingSettings) {
        MoneyFormatter.Config(
            showCurrency = uiState.formattingSettings.showCurrency,
            groupDigits = uiState.formattingSettings.groupDigits,
            roundDecimals = uiState.formattingSettings.roundDecimals,
            showPlusMinus = false
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            AppExtendedFab(
                text = "Add Wallet",
                icon = Icons.Default.Add,
                onClick = onAddWalletClick,
                expanded = !currentListState.isScrollInProgress
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // 1. Total Summary Header Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Total Balance",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val formattedTotal = MoneyFormatter.format(
                            amount = uiState.totalBalance,
                            currencyCode = uiState.globalCurrencySymbol,
                            decimals = uiState.globalCurrencyDecimals,
                            config = formatterConfig
                        )
                        Text(
                            text = if (uiState.isTotalValid) formattedTotal else "Multi-Currency",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (uiState.totalBreakdown != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = uiState.totalBreakdown!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // 2. Search Field (when > 5 wallets or search active)
                if (uiState.activeWallets.size > 10 || uiState.archivedWallets.size > 10 || uiState.searchQuery.isNotEmpty()) {
                    SearchBar(searchQuery = uiState.searchQuery, viewModel::onSearchQueryChange)
                }

                // 3. Tab Pill (Active / Archived)
                TabPill(
                    tabs = listOf(
                        "Active (${uiState.activeWallets.size})" to MaterialTheme.colorScheme.primary,
                        "Archived (${uiState.archivedWallets.size})" to MaterialTheme.colorScheme.error
                    ),
                    activeTab = pagerState.currentPage,
                    onTabChange = { index ->
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                )

                // 4. Filter Component
                if (currentWallets.isNotEmpty() || uiState.activeWallets.isNotEmpty() || uiState.archivedWallets.isNotEmpty()) {
                    FilterComponent(
                        countText = if (uiState.searchQuery.isNotBlank()) {
                            "${currentWallets.size} found"
                        } else {
                            "${currentWallets.size} ${if (currentWallets.size == 1) "wallet" else "wallets"}"
                        },
                        activeSortOption = uiState.sortOption,
                        options = WalletSortOption.entries,
                        setSortOption = viewModel::setSortOption
                    )
                }
                // 5. Horizontal Pager for Active & Archived tabs
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth(),
                    beyondViewportPageCount = 1
                ) { page ->
                    val isArchived = page == 1
                    val wallets = if (isArchived) uiState.archivedWallets else uiState.activeWallets
                    val listState = if (isArchived) archivedListState else activeListState

                    if (wallets.isEmpty()) {
                        EmptyListItem(
                            isSearching = uiState.searchQuery.isNotEmpty(),
                            text = if (isArchivedTab) "archived wallet" else "wallet"
                        )
                    } else {
                        ReorderableLazyColumn(
                            items = wallets,
                            key = { it.id },
                            lazyListState = listState,
                            isReorderEnabled = uiState.sortOption == WalletSortOption.CUSTOM,
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 88.dp),
                            contentType = { "wallet_card" },
                            onReorder = { reordered -> viewModel.reorderWallets(reordered.map { it.id }) }
                        ) { item, _, handleModifier ->
                            val balanceColor = if (item.isNegativeBalance) {
                                ExpenseColor
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                            FinanceListItem(
                                icon = {
                                    if (uiState.sortOption == WalletSortOption.CUSTOM) {
                                        ReorderDragHandle(
                                            modifier = handleModifier.padding(end = 10.dp),
                                            enabled = true
                                        )
                                    }
                                    CategoryIcon(iconData = item.iconData)
                                },
                                title = item.name,
                                subtitle = item.currency,
                                trailingContent = {
                                    Text(
                                        text = item.formattedBalance,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = balanceColor,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                },
                                onClick = { onWalletClick(item.id) },
                                horizontalPadding = 1.dp
                            )
                        }
                    }
                }
            }
        }
    }
}
