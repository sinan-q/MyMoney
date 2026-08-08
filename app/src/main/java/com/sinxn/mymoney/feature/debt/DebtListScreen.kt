package com.sinxn.mymoney.feature.debt

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sinxn.mymoney.core.data.local.entity.PersonEntity
import com.sinxn.mymoney.core.data.local.entity.WalletEntity
import com.sinxn.mymoney.core.data.local.model.DebtWithDetails
import com.sinxn.mymoney.core.ui.components.NavigationMenuPage
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MoneyFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtListScreen(
    onNavigateUp: () -> Unit,
    onDebtClick: (String) -> Unit,
    onAddDebt: (type: Int) -> Unit,
    onNavigateMenuItem: (String) -> Unit = {},
    onNavigateToWallet: (String) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: DebtListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    // Handle back button to close drawer
    BackHandler(enabled = pagerState.currentPage == 1) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(0)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = true
    ) { page ->
        if (page == 1) {
            NavigationMenuPage(
                selectedItemId = "debts",
                onReturnToMain = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(0)
                    }
                },
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
                    onNavigateToWallet("new")
                },
                onManageWallets = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(0)
                    }
                    onNavigateToSettings()
                },
                onItemClick = { item ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(0)
                    }
                    onNavigateMenuItem(item.id)
                }
            )
        } else {
            DebtListContent(
                uiState = uiState,
                onNavigateUp = onNavigateUp,
                onOpenDrawer = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(1)
                    }
                },
                onTabSelected = viewModel::setSelectedTab,
                onToggleIncludeArchived = { viewModel.setIncludeArchived(!uiState.includeArchived) },
                onDebtClick = onDebtClick,
                onAddDebt = { onAddDebt(uiState.selectedTab) },
                onToggleArchived = viewModel::toggleArchived,
                onDeleteDebt = viewModel::deleteDebt
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtListBodyContent(
    uiState: DebtListUiState,
    modifier: Modifier = Modifier,
    showSummaryCard: Boolean = true,
    onTabSelected: (Int) -> Unit,
    onDebtClick: (String) -> Unit,
    onToggleArchived: (String, Boolean) -> Unit,
    onDeleteDebt: (String) -> Unit
) {
    val formatterConfig = remember(uiState) {
        MoneyFormatter.Config(
            showCurrency = true,
            groupDigits = true,
            roundDecimals = false,
            showPlusMinus = false
        )
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Segmented Tab Switcher (Debts vs Credits)
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            SegmentedButton(
                selected = uiState.selectedTab == 0,
                onClick = { onTabSelected(0) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                icon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            ) {
                Text("I Owe (Debts)")
            }
            SegmentedButton(
                selected = uiState.selectedTab == 1,
                onClick = { onTabSelected(1) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                icon = {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            ) {
                Text("Owed to Me (Credits)")
            }
        }

        val summaryCurrency = if (uiState.debts.isNotEmpty()) uiState.debts.first().walletCurrency else uiState.currencyCode
        val summaryDecimals = if (uiState.debts.isNotEmpty()) uiState.debts.first().walletDecimals else 2

        if (showSummaryCard) {
            // Summary Header Card
            DebtSummaryCard(
                selectedTab = uiState.selectedTab,
                totalRemainingMoney = uiState.totalRemainingMoney,
                itemCount = uiState.debts.size,
                formatterConfig = formatterConfig,
                currencyCode = summaryCurrency,
                currencyDecimals = summaryDecimals,
                filterWalletId = uiState.filterWalletId
            )
        }

        // Debts List
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.debts.isEmpty()) {
            EmptyDebtState(selectedTab = uiState.selectedTab)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.debts, key = { it.debt.id }) { debtItem ->
                    DebtCardItem(
                        debtWithDetails = debtItem,
                        formatterConfig = formatterConfig,
                        currencyCode = uiState.currencyCode,
                        onClick = { onDebtClick(debtItem.debt.id) },
                        onQuickPayment = { onDebtClick(debtItem.debt.id) },
                        onToggleArchive = { onToggleArchived(debtItem.debt.id, debtItem.debt.isArchived) },
                        onDelete = { onDeleteDebt(debtItem.debt.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebtListContent(
    uiState: DebtListUiState,
    onNavigateUp: () -> Unit,
    onOpenDrawer: () -> Unit,
    onTabSelected: (Int) -> Unit,
    onToggleIncludeArchived: () -> Unit,
    onDebtClick: (String) -> Unit,
    onAddDebt: () -> Unit,
    onToggleArchived: (String, Boolean) -> Unit,
    onDeleteDebt: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Debts & Credits",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open Drawer"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onToggleIncludeArchived) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = "Toggle Archived",
                            tint = if (uiState.includeArchived) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddDebt,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(if (uiState.selectedTab == 0) "Add Debt" else "Add Credit") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    ) { paddingValues ->
        DebtListBodyContent(
            uiState = uiState,
            modifier = Modifier.padding(paddingValues),
            onTabSelected = onTabSelected,
            onDebtClick = onDebtClick,
            onToggleArchived = onToggleArchived,
            onDeleteDebt = onDeleteDebt
        )
    }
}

@Composable
private fun DebtSummaryCard(
    selectedTab: Int,
    totalRemainingMoney: Long,
    itemCount: Int,
    formatterConfig: MoneyFormatter.Config,
    currencyCode: String,
    currencyDecimals: Int = 2,
    filterWalletId: String? = null
) {
    val gradientColors = if (selectedTab == 0) {
        listOf(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.surfaceContainerHigh
        )
    } else {
        listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradientColors))
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedTab == 0) "Total Unpaid Debts" else "Total Pending Credits",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!filterWalletId.isNullOrBlank() && filterWalletId != "total") {
                        BadgeChip(
                            icon = Icons.Default.Wallet,
                            text = "Current Wallet",
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = MoneyFormatter.format(
                        amount = totalRemainingMoney,
                        currencyCode = currencyCode,
                        decimals = currencyDecimals,
                        config = formatterConfig
                    ),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = if (selectedTab == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$itemCount active ${if (selectedTab == 0) "debt" else "credit"}${if (itemCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun DebtCardItem(
    debtWithDetails: DebtWithDetails,
    formatterConfig: MoneyFormatter.Config,
    currencyCode: String,
    onClick: () -> Unit,
    onQuickPayment: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit
) {
    val debt = debtWithDetails.debt
    val progress = debtWithDetails.progress
    val totalMoney = debt.money
    val remaining = debtWithDetails.remainingMoney

    val isFullyPaid = remaining == 0L && totalMoney > 0
    val isOverdue = remember(debt.expirationDate) {
        debt.expirationDate?.let { exp ->
            try {
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val expDate = format.parse(exp)
                expDate != null && expDate.before(Date()) && !isFullyPaid
            } catch (e: Exception) {
                false
            }
        } ?: false
    }

    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Row: Icon, Title, Status Badges & Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (debt.type == 0) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (debt.type == 0) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = if (debt.type == 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = debt.description.ifBlank { if (debt.type == 0) "Debt" else "Credit" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Subtitle Details Row (Wallet, Place, Expiration)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        // Wallet Badge
                        if (debtWithDetails.walletName.isNotBlank()) {
                            BadgeChip(
                                icon = Icons.Default.Wallet,
                                text = debtWithDetails.walletName
                            )
                        }

                        // Expiration / Status Badge
                        if (isFullyPaid) {
                            BadgeChip(
                                icon = Icons.Default.CheckCircle,
                                text = "Paid",
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else if (isOverdue) {
                            BadgeChip(
                                icon = Icons.Default.Event,
                                text = "Overdue",
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        } else if (!debt.expirationDate.isNullOrBlank()) {
                            BadgeChip(
                                icon = Icons.Default.Event,
                                text = "Due ${debt.expirationDate}"
                            )
                        }
                    }
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                showMenu = false
                                onClick()
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(if (debt.isArchived) "Unarchive" else "Archive") },
                            onClick = {
                                showMenu = false
                                onToggleArchive()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (debt.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                    contentDescription = null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Money & Progress Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = MoneyFormatter.format(amount = remaining, currencyCode = debtWithDetails.walletCurrency, decimals = debtWithDetails.walletDecimals, config = formatterConfig),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isFullyPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total: ${MoneyFormatter.format(amount = totalMoney, currencyCode = debtWithDetails.walletCurrency, decimals = debtWithDetails.walletDecimals, config = formatterConfig)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Progress Bar
            if (totalMoney > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                val progressFraction = (kotlin.math.abs(progress).toFloat() / totalMoney.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = if (isFullyPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // Linked People Row (if any)
            if (debtWithDetails.people.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = debtWithDetails.people.joinToString(", ") { it.name },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Quick Payment Action Button
            if (!isFullyPaid) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onQuickPayment,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Payment,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (debt.type == 0) "Record Repayment" else "Record Collection")
                }
            }
        }
    }
}

@Composable
private fun BadgeChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Surface(
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun EmptyDebtState(selectedTab: Int) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = if (selectedTab == 0) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (selectedTab == 0) "No Active Debts" else "No Pending Credits",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (selectedTab == 0) "You have no recorded debts to pay." else "You have no active credits to collect.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
