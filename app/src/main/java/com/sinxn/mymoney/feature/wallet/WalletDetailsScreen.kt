package com.sinxn.mymoney.feature.wallet

import android.graphics.Color.HSVToColor
import android.graphics.Color.colorToHSV
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.sinxn.mymoney.core.data.local.entity.TransactionEntity
import com.sinxn.mymoney.core.data.local.model.TransactionListItem
import com.sinxn.mymoney.core.data.local.model.WalletWithBalance
import com.sinxn.mymoney.core.util.MoneyFormatter
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.TransactionHeader
import com.sinxn.mymoney.core.ui.components.TransactionItem
import com.sinxn.mymoney.core.ui.components.WalletHeader
import com.sinxn.mymoney.core.ui.components.generateColor
import com.sinxn.mymoney.feature.recap.YearRecapScreen

import androidx.activity.compose.BackHandler
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletDetailsScreen(
    onNavigateUp: () -> Unit,
    onTransactionClick: (String) -> Unit,
    onNavigateToRecap: () -> Unit,
    onAddTransaction: () -> Unit,
    onNavigateToDebts: (String?) -> Unit = {},
    onNavigateToBudgets: (String?) -> Unit = {},
    onNavigateToSavings: (String?) -> Unit = {},
    onAddDebt: (walletId: String?, type: Int) -> Unit = { _, _ -> },
    onDebtClick: (String) -> Unit = {},
    onNavigateToWallet: (String) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateMenuItem: (String) -> Unit = {},
    viewModel: WalletDetailsViewModel = hiltViewModel()
) {
    val wallet by viewModel.wallet.collectAsState(initial = null)
    val allWallets by viewModel.allWallets.collectAsState(initial = emptyList())
    val transactions by viewModel.transactions.collectAsState(initial = emptyList())
    val settings by viewModel.formattingSettings.collectAsState()
    
    var showSettings by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val showTopBarTitle by remember {
        androidx.compose.runtime.derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 300
        }
    }

    // Map Settings to Formatter Config
    val formatterConfig = MoneyFormatter.Config(
        showCurrency = settings.showCurrency,
        groupDigits = settings.groupDigits,
        roundDecimals = settings.roundDecimals,
        showPlusMinus = settings.showPlusMinus
    )

    var isWalletListExpanded by remember { mutableStateOf(false) }

    BackHandler(enabled = isWalletListExpanded) {
        if (isWalletListExpanded) {
            isWalletListExpanded = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        if (wallet == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            WalletHeader(
                wallet = wallet!!,
                formatterConfig = formatterConfig,
                isExpanded = isWalletListExpanded,
                onToggleExpand = { isWalletListExpanded = !isWalletListExpanded }
            )

            if (isWalletListExpanded) {
                com.sinxn.mymoney.core.ui.components.WalletDropdownList(
                    onWalletSelect = { w ->
                        isWalletListExpanded = false
                        if (w.wallet.id != wallet?.wallet?.id) {
                            onNavigateToWallet(w.wallet.id)
                        }
                    },
                    onAddWallet = {
                        isWalletListExpanded = false
                        onNavigateToWallet("new")
                    },
                    onManageWallets = {
                        isWalletListExpanded = false
                        onNavigateToSettings()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                )
            } else {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    TransactionList(
                        wallet = wallet!!,
                        items = transactions,
                        decimals = wallet!!.decimals,
                        currencyCode = wallet!!.wallet.currency,
                        formatterConfig = formatterConfig,
                        dateFormat = settings.dateFormat,
                        listState = listState,
                        onTransactionClick = onTransactionClick,
                        onRecapClick = onNavigateToRecap
                    )

                    FloatingActionButton(
                        onClick = onAddTransaction,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Transaction")
                    }
                }
            }
        }
        if (showSettings && wallet != null) {
            WalletSettingsBottomSheet(
                wallet = wallet!!,
                onDismiss = { showSettings = false },
                onToggleCountInTotal = { viewModel.toggleCountInTotal() },
                onToggleArchived = { viewModel.toggleArchived() },
                sheetState = sheetState
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletSettingsBottomSheet(
    wallet: WalletWithBalance,
    onDismiss: () -> Unit,
    onToggleCountInTotal: () -> Unit,
    onToggleArchived: () -> Unit,
    sheetState: SheetState
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Wallet Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Show in Total Toggle
            Surface(
                onClick = onToggleCountInTotal,
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, 
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        Column {
                            Text(
                                text = "Show in Total",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Include this wallet in global balance",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Switch(
                        checked = wallet.wallet.countInTotal,
                        onCheckedChange = { onToggleCountInTotal() }
                    )
                }
            }
            
            // Archive Toggle
            Surface(
                onClick = onToggleArchived,
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, 
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (wallet.wallet.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        Column {
                            Text(
                                text = if (wallet.wallet.isArchived) "Unarchive Wallet" else "Archive Wallet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (wallet.wallet.isArchived) "Restore wallet to active list" else "Hide wallet from active list",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Switch(
                        checked = wallet.wallet.isArchived,
                        onCheckedChange = { onToggleArchived() }
                    )
                }
            }
            
            // Helpful note
            Text(
                text = "Settings changed here will immediately affect your budget and global totals.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionList(
    wallet: WalletWithBalance,
    items: List<TransactionListItem>,
    decimals: Int,
    currencyCode: String,
    formatterConfig: MoneyFormatter.Config,
    dateFormat: Int,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onTransactionClick: (String) -> Unit,
    onRecapClick: () -> Unit
) {
    var collapsedGroups by remember { mutableStateOf(setOf<String>()) }
    
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = 80.dp),
    ) {
        if (items.isEmpty()) {
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
        var currentHeader: TransactionListItem.Header? = null
        val customGrouped = mutableListOf<Pair<TransactionListItem.Header, MutableList<TransactionListItem>>>()

        items.forEach { item ->
            when (item) {
                is TransactionListItem.Header -> {
                    currentHeader = item
                    customGrouped.add(item to mutableListOf())
                }
                is TransactionListItem.Transaction -> {
                    currentHeader?.let {
                         customGrouped.lastOrNull()?.second?.add(item)
                    }
                }
                else -> {}
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

                    Box(
                        modifier = Modifier.animateItem()
                    ) {
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
}

@Preview(showBackground = true)
@Composable
fun PreviewRecap() {
    RecapBanner {  }
}

@Composable
fun RecapBanner(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF1A237E), Color(0xFF0D47A1)) // Deep Blue premium gradient
                )
            )
            .clickable(onClick = onClick)
    ) {
         Canvas(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.1f)
        ) {
             drawCircle(color = Color.White, radius = size.minDimension, center = Offset(x = size.width, y = 0f))
        }

        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                 Icon(
                     imageVector = Icons.Rounded.CardGiftcard,
                     contentDescription = "Recap",
                     tint = Color.White
                 )
            }
            
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(
                    text = "Your 2025 Recap",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "See your spending highlights!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}
