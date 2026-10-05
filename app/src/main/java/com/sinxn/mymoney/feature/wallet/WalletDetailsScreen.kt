package com.sinxn.mymoney.feature.wallet

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sinxn.mymoney.core.data.local.model.TransactionListItem
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import com.sinxn.mymoney.core.ui.components.AppExtendedFab
import com.sinxn.mymoney.core.ui.components.TransactionHeader
import com.sinxn.mymoney.core.ui.components.TransactionItem
import com.sinxn.mymoney.core.ui.components.WalletDropdownList
import com.sinxn.mymoney.core.ui.components.WalletHeader
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MoneyFormatter
import com.sinxn.mymoney.feature.transaction.TransactionFilter
import com.sinxn.mymoney.feature.transaction.components.TransactionFilterSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletDetailsScreen(
    onTransactionClick: (String) -> Unit,
    onAddTransaction: () -> Unit,
    onNavigateToWallet: (String) -> Unit = {},
    onAddWallet: () -> Unit = {},
    onManageWallets: () -> Unit = {},
    viewModel: WalletDetailsViewModel = hiltViewModel()
) {
    val wallet by viewModel.wallet.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val pendingTransactions by viewModel.pendingTransactions.collectAsState()
    val settings by viewModel.formattingSettings.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val showFilterSheet by viewModel.showFilterSheet.collectAsState()
    val allWallets by viewModel.allWallets.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()

    val listState = rememberLazyListState()
    val isHeaderReduced by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 20
        }
    }

    val formatterConfig = remember(viewModel.formattingSettings) {
        MoneyFormatter.Config(
            showCurrency = settings.showCurrency,
            groupDigits = settings.groupDigits,
            roundDecimals = settings.roundDecimals,
            showPlusMinus = settings.showPlusMinus
        )
    }

    var isWalletListExpanded by remember { mutableStateOf(false) }

    BackHandler(enabled = isWalletListExpanded) {
        if (isWalletListExpanded) {
            isWalletListExpanded = false
        }
    }

    // Whether any filter is actively applied (badge indicator)
    val isFilterActive = filter.dateRangeEnabled || filter.categoryIds.isNotEmpty() || (filter.walletId.isNotEmpty() && filter.walletId != viewModel.walletId)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (!isWalletListExpanded && wallet != null) {
                AppExtendedFab(
                    text = "Add Transaction",
                    icon = Icons.Default.Add,
                    onClick = onAddTransaction,
                    expanded = !listState.isScrollInProgress
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (wallet == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                WalletHeader(
                    wallet = wallet!!,
                    formatterConfig = formatterConfig,
                    isExpanded = isWalletListExpanded,
                    onToggleExpand = { isWalletListExpanded = !isWalletListExpanded },
                    isReduced = isHeaderReduced && !isWalletListExpanded
                )

                if (isWalletListExpanded) {
                    WalletDropdownList(
                        onWalletSelect = { w ->
                            isWalletListExpanded = false
                            if (w.wallet.id != wallet?.wallet?.id) {
                                onNavigateToWallet(w.wallet.id)
                            }
                        },
                        onAddWallet = {
                            isWalletListExpanded = false
                            onAddWallet()
                        },
                        onManageWallets = {
                            isWalletListExpanded = false
                            onManageWallets()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    )
                } else {
                    Box(modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()) {
                        TransactionList(
                            items = transactions,
                            pendingTransactions = pendingTransactions,
                            decimals = wallet!!.decimals,
                            currencyCode = wallet!!.wallet.currency,
                            formatterConfig = formatterConfig,
                            dateFormat = settings.dateFormat,
                            listState = listState,
                            onTransactionClick = onTransactionClick,
                            onConfirmPending = { viewModel.confirmTransaction(it) },
                            onDismissPending = { viewModel.dismissTransaction(it) },
                            onPendingClick = onTransactionClick,
                            isFilterActive = isFilterActive,
                            onFilterClick = { viewModel.showFilterSheet() }
                        )
                    }
                }
            }
        }
    }

    // ── Filter Sheet ─────────────────────────────────────────────────────────
    if (showFilterSheet) {
        TransactionFilterSheet(
            filter = filter,
            currentWalletId = filter.walletId.ifEmpty { viewModel.walletId },
            wallets = allWallets,
            categories = allCategories,
            formattingSettings = settings,
            onDismiss = { viewModel.dismissFilterSheet() },
            onApply = { newFilter -> viewModel.applyFilter(newFilter) }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionList(
    items: List<TransactionListItem>,
    pendingTransactions: List<TransactionWithCategory> = emptyList(),
    decimals: Int,
    currencyCode: String,
    formatterConfig: MoneyFormatter.Config,
    dateFormat: Int,
    listState: LazyListState,
    onTransactionClick: (String) -> Unit,
    onConfirmPending: (String) -> Unit = {},
    onDismissPending: (String) -> Unit = {},
    onPendingClick: (String) -> Unit = {},
    isFilterActive: Boolean = false,
    onFilterClick: () -> Unit = {}
) {
    var collapsedGroups by remember { mutableStateOf(setOf<String>()) }

    val customGrouped = remember(items) {
        val grouped = mutableListOf<Pair<TransactionListItem.Header, MutableList<TransactionListItem>>>()
        var currentHeader: TransactionListItem.Header? = null
        items.forEach { item ->
            when (item) {
                is TransactionListItem.Header -> {
                    currentHeader = item
                    grouped.add(item to mutableListOf())
                }
                is TransactionListItem.Transaction -> {
                    currentHeader?.let {
                        grouped.lastOrNull()?.second?.add(item)
                    }
                }
            }
        }
        grouped
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = 80.dp),
    ) {
        if (pendingTransactions.isNotEmpty()) {
            item(key = "pending_header") {
                Text(
                    text = "Pending Confirmations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            items(
                items = pendingTransactions,
                key = { "pending_${it.transaction.id}" }
            ) { item ->
                PendingConfirmationItem(
                    item = item,
                    onConfirm = { onConfirmPending(item.transaction.id) },
                    onDismiss = { onDismissPending(item.transaction.id) },
                    onClick = { onPendingClick(item.transaction.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (items.isEmpty() && pendingTransactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .padding(top = 100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No transactions found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ── Toolbar row with filter button ────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    BadgedBox(
                        badge = {
                            if (isFilterActive) {
                                Badge()
                            }
                        }
                    ) {
                        FilledTonalIconButton(
                            onClick = onFilterClick,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (isFilterActive)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter Transactions",
                                tint = if (isFilterActive)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        customGrouped.forEach { (header, groupItems) ->
            val headerKey = DateUtils.formatMonthHeader(header.date)
            val isCollapsed = collapsedGroups.contains(headerKey)

            stickyHeader(key = headerKey) {
                TransactionHeader(
                    header = header,
                    decimals = decimals,
                    currencyCode = currencyCode,
                    formatterConfig = formatterConfig,
                    isCollapsed = isCollapsed,
                    onToggle = {
                        collapsedGroups = if (isCollapsed) {
                            collapsedGroups - headerKey
                        } else {
                            collapsedGroups + headerKey
                        }
                    }
                )
            }

            if (!isCollapsed) {
                itemsIndexed(
                    items = groupItems,
                    key = { _, item ->
                        when(item) {
                            is TransactionListItem.Transaction -> item.transaction.transaction.id
                            else -> "Unknown"
                        }
                    }
                ) { index, item ->
                    val isLastItem = index == groupItems.lastIndex

                    when (item) {
                        is TransactionListItem.Transaction -> {
                            val trans = item.transaction
                            TransactionItem(
                                item = trans,
                                decimals = trans.decimals,
                                currencyCode = trans.currencySymbol ?: trans.currencyCode ?: currencyCode,
                                formatterConfig = formatterConfig,
                                dateFormat = dateFormat,
                                isLastItem = isLastItem,
                                showDate = true,
                                onClick = { onTransactionClick(trans.transaction.id) }
                            )
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}



@Composable
fun PendingConfirmationItem(
    item: TransactionWithCategory,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.transaction.description.takeIf { !it.isNullOrBlank() } ?: item.categoryName ?: "Recurrence",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.transaction.date.take(10), // yyyy-MM-dd
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                val formattedMoney = MoneyFormatter.formatColored(
                    amount = item.transaction.money,
                    currencyCode = item.currencyCode ?: "USD",
                    decimals = item.decimals,
                    tintMode = if (item.transaction.direction == 1) MoneyFormatter.TintMode.INCOME else MoneyFormatter.TintMode.EXPENSE,
                    config = MoneyFormatter.Config()
                )
                Text(
                    text = formattedMoney,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Dismiss", color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Confirm")
                }
            }
        }
    }
}
