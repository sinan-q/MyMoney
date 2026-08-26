package com.sinxn.mymoney.feature.wallet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sinxn.mymoney.core.ui.components.AppExtendedFab
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.EmptyListItem
import com.sinxn.mymoney.core.ui.components.FilterComponent
import com.sinxn.mymoney.core.ui.components.ReorderDragHandle
import com.sinxn.mymoney.core.ui.components.ReorderableLazyColumn
import com.sinxn.mymoney.core.ui.components.TabPill
import com.sinxn.mymoney.core.util.MoneyFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletListScreen(
    onNavigateBack: () -> Unit,
    onWalletClick: (String) -> Unit,
    onAddWalletClick: () -> Unit,
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
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Wallets",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
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
                if (uiState.activeWallets.size + uiState.archivedWallets.size > 5 || uiState.searchQuery.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = viewModel::onSearchQueryChange,
                            placeholder = {
                                Text(
                                    "Search wallets...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = if (uiState.searchQuery.isNotEmpty()) {
                                {
                                    IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Clear",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            } else null,
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            )
                        )
                    }
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
                        .fillMaxWidth()
                        .weight(1f),
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
                        WalletReorderableList(
                            items = wallets,
                            lazyListState = listState,
                            isReorderEnabled = uiState.sortOption == WalletSortOption.CUSTOM,
                            onWalletClick = onWalletClick,
                            onReorderWallets = viewModel::reorderWallets
                        )
                    }
                }
            }
        }
    }
}

private val WalletCardShape = RoundedCornerShape(16.dp)
private val WalletTagShape = RoundedCornerShape(6.dp)

@Composable
private fun WalletReorderableList(
    items: List<WalletUiModel>,
    lazyListState: LazyListState,
    isReorderEnabled: Boolean,
    onWalletClick: (String) -> Unit,
    onReorderWallets: (List<String>) -> Unit
) {
    ReorderableLazyColumn(
        items = items,
        key = { it.id },
        lazyListState = lazyListState,
        isReorderEnabled = isReorderEnabled,
        itemShape = WalletCardShape,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentType = { "wallet_card" },
        onReorder = { reordered -> onReorderWallets(reordered.map { it.id }) }
    ) { item, _, handleModifier ->
        WalletListItem(
            item = item,
            isReorderEnabled = isReorderEnabled,
            handleModifier = handleModifier,
            onWalletClick = onWalletClick
        )
    }
}

@Composable
private fun WalletListItem(
    item: WalletUiModel,
    isReorderEnabled: Boolean,
    handleModifier: Modifier,
    onWalletClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onWalletClick(item.id) },
        shape = WalletCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (isReorderEnabled) {
                    ReorderDragHandle(
                        modifier = handleModifier.padding(end = 10.dp),
                        enabled = true
                    )
                }

                CategoryIcon(iconData = item.iconData)

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = WalletTagShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = item.currency,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.isExcludedFromTotal) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = "Excluded from total",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Excluded from total",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        } else if (!item.note.isNullOrBlank()) {
                            Text(
                                text = item.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else if (item.formattedStartMoney != null) {
                            Text(
                                text = item.formattedStartMoney,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // Balance
            val balanceColor = if (item.isNegativeBalance) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            }

            Text(
                text = item.formattedBalance,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = balanceColor,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
