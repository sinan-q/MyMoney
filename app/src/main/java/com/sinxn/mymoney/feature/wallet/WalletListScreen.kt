package com.sinxn.mymoney.feature.wallet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sinxn.mymoney.core.data.local.model.WalletWithBalance
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.IconData
import com.sinxn.mymoney.core.ui.components.TabPill
import com.sinxn.mymoney.core.ui.components.parseIconData
import com.sinxn.mymoney.core.util.MoneyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletListScreen(
    onNavigateBack: () -> Unit,
    onWalletClick: (String) -> Unit,
    onAddWalletClick: () -> Unit,
    viewModel: WalletListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0: Active, 1: Archived

    val currentWallets = if (selectedTab == 0) uiState.activeWallets else uiState.archivedWallets

    val formatterConfig = remember(uiState.formattingSettings) {
        MoneyFormatter.Config(
            showCurrency = uiState.formattingSettings.showCurrency,
            groupDigits = uiState.formattingSettings.groupDigits,
            roundDecimals = uiState.formattingSettings.roundDecimals,
            showPlusMinus = false
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isSortMode) "Reorder Wallets" else "Wallets",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.isSortMode) {
                        IconButton(onClick = viewModel::toggleSortMode) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Done Sorting",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        IconButton(onClick = viewModel::toggleSortMode) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort Wallets")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!uiState.isSortMode) {
                FloatingActionButton(
                    onClick = onAddWalletClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Wallet")
                }
            }
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
                // 1. Total Summary Header Card (when not in sort mode)
                if (!uiState.isSortMode) {
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
                }

                // 2. Search Field (when not in sort mode and > 5 wallets or search active)
                if (!uiState.isSortMode && (uiState.activeWallets.size + uiState.archivedWallets.size > 5 || uiState.searchQuery.isNotEmpty())) {
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
                if (!uiState.isSortMode && uiState.archivedWallets.isNotEmpty()) {
                    TabPill(
                        tabs = listOf(
                            "Active (${uiState.activeWallets.size})" to MaterialTheme.colorScheme.primary,
                            "Archived (${uiState.archivedWallets.size})" to MaterialTheme.colorScheme.error
                        ),
                        activeTab = selectedTab,
                        onTabChange = { selectedTab = it }
                    )
                }

                // 4. Content List
                val currentWalletUiModels = remember(currentWallets, formatterConfig) {
                    currentWallets.map { it.toUiModel(formatterConfig) }
                }
                val reorderWalletUiModels = remember(uiState.sortedWalletsForReorder, formatterConfig) {
                    uiState.sortedWalletsForReorder.map { it.toUiModel(formatterConfig) }
                }

                if (uiState.isSortMode) {
                    // Reorder List View
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(
                            items = reorderWalletUiModels,
                            key = { _, item -> item.id },
                            contentType = { _, _ -> "reorder_wallet" }
                        ) { index, item ->
                            ReorderWalletRow(
                                wallet = item,
                                index = index,
                                totalCount = reorderWalletUiModels.size,
                                onMoveUp = { viewModel.moveWalletUp(index) },
                                onMoveDown = { viewModel.moveWalletDown(index) }
                            )
                        }
                    }
                } else if (currentWallets.isEmpty()) {
                    EmptyWalletState(isSearching = uiState.searchQuery.isNotEmpty(), isArchivedTab = selectedTab == 1)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(
                            items = currentWalletUiModels,
                            key = { _, item -> item.id },
                            contentType = { _, _ -> "wallet_card" }
                        ) { _, item ->
                            WalletCardRow(
                                item = item,
                                onClick = { onWalletClick(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private val WalletCardShape = RoundedCornerShape(16.dp)
private val WalletTagShape = RoundedCornerShape(6.dp)
private val ReorderCardShape = RoundedCornerShape(14.dp)

@Immutable
private data class WalletUiModel(
    val id: String,
    val name: String,
    val currency: String,
    val currentBalance: Long,
    val formattedBalance: String,
    val formattedStartMoney: String?,
    val isNegativeBalance: Boolean,
    val iconData: IconData,
    val isExcludedFromTotal: Boolean,
    val note: String?
)

private fun WalletWithBalance.toUiModel(formatterConfig: MoneyFormatter.Config): WalletUiModel {
    val currencyCode = currencySymbol ?: wallet.currency
    val formattedBalance = MoneyFormatter.format(
        amount = currentBalance,
        currencyCode = currencyCode,
        decimals = decimals,
        config = formatterConfig
    )
    val formattedStartMoney = if (wallet.countInTotal && wallet.note.isNullOrBlank()) {
        val startFormatted = MoneyFormatter.format(
            amount = wallet.startMoney,
            currencyCode = currencyCode,
            decimals = decimals,
            config = formatterConfig
        )
        "Start: $startFormatted"
    } else null
    val isNegativeBalance = currentBalance < 0
    val iconData = parseIconData(wallet.icon, wallet.name)
    val note = wallet.note?.takeIf { it.isNotBlank() }

    return WalletUiModel(
        id = wallet.id,
        name = wallet.name,
        currency = wallet.currency,
        currentBalance = currentBalance,
        formattedBalance = formattedBalance,
        formattedStartMoney = formattedStartMoney,
        isNegativeBalance = isNegativeBalance,
        iconData = iconData,
        isExcludedFromTotal = !wallet.countInTotal,
        note = note
    )
}

@Composable
private fun WalletCardRow(
    item: WalletUiModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                CategoryIcon(
                    iconData = item.iconData,
                    modifier = Modifier.size(44.dp)
                )

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

@Composable
private fun ReorderWalletRow(
    wallet: WalletUiModel,
    index: Int,
    totalCount: Int,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ReorderCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                CategoryIcon(
                    iconData = wallet.iconData,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = wallet.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = index > 0,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Move Up",
                        tint = if (index > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = index < totalCount - 1,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Move Down",
                        tint = if (index < totalCount - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyWalletState(isSearching: Boolean, isArchivedTab: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isArchivedTab) Icons.Default.Archive else Icons.Default.Wallet,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = when {
                    isSearching -> "No wallets found matching search"
                    isArchivedTab -> "No archived wallets"
                    else -> "No wallets created yet"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (isArchivedTab) "Archived wallets will appear here" else "Tap + button below to create your first wallet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
