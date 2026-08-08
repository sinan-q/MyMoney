package com.sinxn.mymoney.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sinxn.mymoney.core.data.local.model.WalletWithBalance
import com.sinxn.mymoney.core.util.MoneyFormatter
import com.sinxn.mymoney.core.util.Constants
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.filled.Archive
import androidx.compose.runtime.rememberCoroutineScope
import com.sinxn.mymoney.core.ui.components.NavigationMenuPage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToBackup: () -> Unit,
    onNavigateToWallet: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onAddTransaction: () -> Unit,
    onNavigateToDebts: (String?) -> Unit = {},
    onAddDebt: (type: Int) -> Unit = {},
    onDebtClick: (String) -> Unit = {},
    onNavigateToBudgets: () -> Unit = {},
    onNavigateToSavings: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    var showMenu by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()

    var activeSection by remember { mutableStateOf("transactions") }
    val debtListViewModel: com.sinxn.mymoney.feature.debt.DebtListViewModel = hiltViewModel()
    val debtUiState by debtListViewModel.uiState.collectAsState()

    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    BackHandler(enabled = pagerState.currentPage == 1 || activeSection != "transactions") {
        if (pagerState.currentPage == 1) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(0)
            }
        } else {
            activeSection = "transactions"
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "MY MONEY",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified
                    )
                },
                actions = {
                    if (activeSection == "debts") {
                        IconButton(onClick = { debtListViewModel.setIncludeArchived(!debtUiState.includeArchived) }) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Archive,
                                contentDescription = "Toggle Archived",
                                tint = if (debtUiState.includeArchived) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Import Backup") },
                                onClick = {
                                    showMenu = false
                                    onNavigateToBackup()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                onClick = {
                                    showMenu = false
                                    onNavigateToSettings()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            if (activeSection == "debts") {
                androidx.compose.material3.ExtendedFloatingActionButton(
                    onClick = { onAddDebt(debtUiState.selectedTab) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(if (debtUiState.selectedTab == 0) "Add Debt" else "Add Credit") },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                androidx.compose.material3.FloatingActionButton(
                    onClick = onAddTransaction,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Transaction")
                }
            }
        }
    ) { padding ->
        val totalWallet = uiState.activeWallets.find { it.wallet.id == Constants.TOTAL_WALLET_ID }
        val otherWallets = uiState.activeWallets.filter { it.wallet.id != Constants.TOTAL_WALLET_ID }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Constant Grand Total Header at top
            totalWallet?.let {
                GrandTotalHeader(
                    wallet = it,
                    isTotalValid = uiState.isTotalValid,
                    balanceBreakdown = uiState.balanceBreakdown,
                    activeSection = activeSection,
                    debtUiState = debtUiState,
                    onClick = { onNavigateToWallet(it.wallet.id) }
                )
            }

            // HorizontalPager for content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> {
                        if (activeSection == "debts") {
                            com.sinxn.mymoney.feature.debt.DebtListBodyContent(
                                uiState = debtUiState,
                                showSummaryCard = false,
                                onTabSelected = debtListViewModel::setSelectedTab,
                                onDebtClick = onDebtClick,
                                onToggleArchived = debtListViewModel::toggleArchived,
                                onDeleteDebt = debtListViewModel::deleteDebt
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 80.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (otherWallets.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Your Wallets",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                                        )
                                    }
                                }

                                items(otherWallets) { walletWithBalance ->
                                    WalletItem(
                                        item = walletWithBalance,
                                        onClick = { onNavigateToWallet(walletWithBalance.wallet.id) }
                                    )
                                }

                                if (uiState.archivedWallets.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Archived",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                                        )
                                    }
                                    items(uiState.archivedWallets) { walletWithBalance ->
                                        WalletItem(
                                            item = walletWithBalance,
                                            onClick = { onNavigateToWallet(walletWithBalance.wallet.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        com.sinxn.mymoney.core.ui.components.NavigationMenuContent(
                            wallets = uiState.activeWallets,
                            selectedItemId = activeSection,
                            onWalletSelect = { wallet ->
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(0)
                                }
                                onNavigateToWallet(wallet.wallet.id)
                            },
                            onAddWallet = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(0)
                                }
                                onAddTransaction()
                            },
                            onManageWallets = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(0)
                                }
                            },
                            onItemClick = { item ->
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(0)
                                }
                                when (item.id) {
                                    "debts" -> activeSection = "debts"
                                    "transactions" -> activeSection = "transactions"
                                    "budgets" -> onNavigateToBudgets()
                                    "savings" -> onNavigateToSavings()
                                    "settings" -> onNavigateToSettings()
                                    else -> {}
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GrandTotalHeader(
    wallet: WalletWithBalance,
    isTotalValid: Boolean,
    balanceBreakdown: String?,
    activeSection: String = "transactions",
    debtUiState: com.sinxn.mymoney.feature.debt.DebtListUiState? = null,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        enabled = activeSection != "debts" && isTotalValid,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (activeSection == "debts") {
                if (debtUiState?.selectedTab == 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                else MaterialTheme.colorScheme.primaryContainer
            } else if (isTotalValid) {
                MaterialTheme.colorScheme.primaryContainer 
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (activeSection == "debts") {
                    if (debtUiState?.selectedTab == 0) "Total Unpaid Debts" else "Total Pending Credits"
                } else {
                    "Global Balance"
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = if (isTotalValid) 0.7f else 0.4f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (activeSection == "debts" && debtUiState != null) {
                val summaryCurrency = if (debtUiState.debts.isNotEmpty()) debtUiState.debts.first().walletCurrency else debtUiState.currencyCode
                val summaryDecimals = if (debtUiState.debts.isNotEmpty()) debtUiState.debts.first().walletDecimals else 2
                val formattedBalance = MoneyFormatter.format(
                    amount = debtUiState.totalRemainingMoney,
                    currencyCode = summaryCurrency,
                    decimals = summaryDecimals
                )
                Text(
                    text = formattedBalance,
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Black
                )
            } else if (isTotalValid) {
                val formattedBalance = MoneyFormatter.format(
                    amount = wallet.currentBalance,
                    currencyCode = wallet.wallet.currency,
                    decimals = wallet.decimals
                )
                Text(
                    text = formattedBalance,
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Black
                )
            } else {
                Text(
                    text = "Multi-Currency",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold
                )
                if (!balanceBreakdown.isNullOrEmpty()) {
                    Text(
                        text = balanceBreakdown,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                Text(
                    text = "Conversion not supported yet",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun WalletItem(
    item: WalletWithBalance,
    onClick: () -> Unit
) {
    val walletColor = remember(item.wallet.name) { 
        // We'll use a color derived from the name just like in details
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(
            (0xFF000000.toInt() or item.wallet.name.hashCode()), 
            hsv
        )
        hsv[1] = 0.4f // Desaturate for item background
        hsv[2] = 0.95f // Lighten
        Color.hsv(hsv[0], hsv[1], hsv[2])
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = walletColor,
                        shape = androidx.compose.foundation.shape.CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                 Text(
                    text = item.wallet.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.wallet.name, 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (!item.wallet.note.isNullOrEmpty()) {
                    Text(
                        text = item.wallet.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            val formattedBalance = MoneyFormatter.format(
                amount = item.currentBalance,
                currencyCode = item.wallet.currency,
                decimals = item.decimals
            )
            Text(
                text = formattedBalance, 
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
