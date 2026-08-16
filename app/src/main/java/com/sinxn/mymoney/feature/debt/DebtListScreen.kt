package com.sinxn.mymoney.feature.debt

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Payment
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
import com.sinxn.mymoney.core.data.local.model.DebtWithDetails
import com.sinxn.mymoney.core.ui.components.TabPill
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MoneyFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.collections.listOf

private val DebtRoseColor = Color(0xFFE11D48)
private val CreditEmeraldColor = Color(0xFF10B981)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtListScreen(
    onNavigateUp: () -> Unit,
    onQuickPayment: (String) -> Unit,
    onDebtClick: (String) -> Unit,
    onAddDebt: (type: Int) -> Unit,
    viewModel: DebtListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var debtToDelete by remember { mutableStateOf<String?>(null) }

    if (debtToDelete != null) {
        AlertDialog(
            onDismissRequest = { debtToDelete = null },
            title = { Text("Delete Debt") },
            text = { Text("Do you want to delete all associated transactions or keep them in history?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = debtToDelete
                        debtToDelete = null
                        if (id != null) {
                            viewModel.deleteDebt(id, deleteTransactions = true)
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        val id = debtToDelete
                        debtToDelete = null
                        if (id != null) {
                            viewModel.deleteDebt(id, deleteTransactions = false)
                        }
                    }) {
                        Text("Keep Transactions")
                    }
                    TextButton(onClick = { debtToDelete = null }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    DebtListContent(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onQuickPayment = onQuickPayment,
        onToggleIncludeArchived = { viewModel.setIncludeArchived(!uiState.includeArchived) },
        onDebtClick = onDebtClick,
        onAddDebt = onAddDebt,
        onToggleArchived = viewModel::toggleArchived,
        onDeleteDebt = { id -> debtToDelete = id }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebtListContent(
    uiState: DebtListUiState,
    onQuickPayment: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onToggleIncludeArchived: () -> Unit,
    onDebtClick: (String) -> Unit,
    onAddDebt: (type: Int) -> Unit,
    onToggleArchived: (String, Boolean) -> Unit,
    onDeleteDebt: (String) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val accentColor = if (pagerState.currentPage == 0) DebtRoseColor else CreditEmeraldColor

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAddDebt(pagerState.currentPage) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = {
                    Text(
                        text = if (pagerState.currentPage == 0) "Add Debt" else "Add Credit",
                        fontWeight = FontWeight.Bold
                    )
                },
                containerColor = accentColor,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp)
            )
        }
    ) { paddingValues ->
        DebtListBodyContent(
            uiState = uiState,
            pagerState = pagerState,
            modifier = Modifier.padding(paddingValues),
            showSummaryCard = true,
            onQuickPayment = onQuickPayment,
            onDebtClick = onDebtClick,
            onAddDebt = onAddDebt,
            onToggleArchived = onToggleArchived,
            onDeleteDebt = onDeleteDebt
        )
    }
}

@Composable
fun DebtListBodyContent(
    uiState: DebtListUiState,
    modifier: Modifier = Modifier,
    pagerState: PagerState = rememberPagerState(initialPage = 0, pageCount = { 2 }),
    showSummaryCard: Boolean = true,
    onTabSelected: (Int) -> Unit = {},
    onDebtClick: (String) -> Unit,
    onQuickPayment: (String) -> Unit,
    onAddDebt: (type: Int) -> Unit = {},
    onToggleArchived: (String, Boolean) -> Unit,
    onDeleteDebt: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val debtListState = rememberLazyListState()
    val creditListState = rememberLazyListState()

    val formatterConfig = remember(uiState.formattingSettings) {
        MoneyFormatter.Config(
            showCurrency = uiState.formattingSettings.showCurrency,
            groupDigits = uiState.formattingSettings.groupDigits,
            roundDecimals = uiState.formattingSettings.roundDecimals,
            showPlusMinus = false
        )
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Tab Pill Selector (Inspired by CategoryListScreen)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            TabPill(
                tabs = listOf(Pair("Debts",  DebtRoseColor), Pair("Credits",  CreditEmeraldColor)),
                activeTab = pagerState.currentPage,
                onTabChange = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                    onTabSelected(index)
                }
            )
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
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
                val isDebt = page == 0
                val currentDebts = if (isDebt) uiState.debts else uiState.credits
                val listState = if (isDebt) debtListState else creditListState
                val summary = if (isDebt) uiState.debtSummary else uiState.creditSummary

                val summaryCurrency = summary.currencyCode
                val summaryDecimals = summary.currencyDecimals

                Column(modifier = Modifier.fillMaxSize()) {
                    if (showSummaryCard) {
                        DebtSummaryCard(
                            selectedTab = page,
                            totalRemainingMoney = summary.totalRemainingMoney,
                            totalOriginalMoney = summary.totalOriginalMoney,
                            totalPaidMoney = summary.totalPaidMoney,
                            activeCount = summary.activeCount,
                            settledCount = summary.settledCount,
                            formatterConfig = formatterConfig,
                            currencyCode = summaryCurrency,
                            currencyDecimals = summaryDecimals,
                            filterWalletId = uiState.filterWalletId,
                            filterWalletName = uiState.filterWalletName,
                            includeArchived = uiState.includeArchived
                        )
                    }

                    if (currentDebts.isEmpty()) {
                        EmptyDebtState(
                            selectedTab = page
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(currentDebts, key = { it.debt.id }) { debtItem ->
                                DebtCardItem(
                                    debtWithDetails = debtItem,
                                    formatterConfig = formatterConfig,
                                    dateFormat = uiState.formattingSettings.dateFormat,
                                    onClick = { onDebtClick(debtItem.debt.id) },
                                    onQuickPayment = { onQuickPayment(debtItem.debt.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DebtSummaryCard(
    selectedTab: Int,
    totalRemainingMoney: Long,
    totalOriginalMoney: Long,
    totalPaidMoney: Long,
    activeCount: Int,
    settledCount: Int,
    formatterConfig: MoneyFormatter.Config,
    currencyCode: String,
    currencyDecimals: Int = 2,
    filterWalletId: String? = null,
    filterWalletName: String? = null,
    includeArchived: Boolean = false
) {
    val isDebt = selectedTab == 0
    val accentColor = if (isDebt) DebtRoseColor else CreditEmeraldColor

    val gradientColors = listOf(
        accentColor.copy(alpha = 0.16f),
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradientColors))
                .padding(20.dp)
        ) {
            Column {
                // Header Label + Filter Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isDebt) "Total Outstanding Debt" else "Total Pending Credit",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (includeArchived) {
                            BadgeChip(
                                icon = Icons.Default.Archive,
                                text = "Archived",
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!filterWalletId.isNullOrBlank() && filterWalletId != "total") {
                            BadgeChip(
                                icon = Icons.Default.Wallet,
                                text = filterWalletName ?: "Wallet",
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = accentColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Hero Money Amount
                Text(
                    text = MoneyFormatter.format(
                        amount = totalRemainingMoney,
                        currencyCode = currencyCode,
                        decimals = currencyDecimals,
                        config = formatterConfig
                    ),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = accentColor,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Bar
                if (totalOriginalMoney > 0) {
                    val progressFraction = (totalPaidMoney.toFloat() / totalOriginalMoney.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = accentColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Subtitle Info Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$activeCount active" + if (settledCount > 0) " • $settledCount settled" else "",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                    )

                    if (totalOriginalMoney > 0) {
                        Text(
                            text = "Total: ${MoneyFormatter.format(amount = totalOriginalMoney, currencyCode = currencyCode, decimals = currencyDecimals, config = formatterConfig)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DebtCardItem(
    debtWithDetails: DebtWithDetails,
    formatterConfig: MoneyFormatter.Config,
    dateFormat: Int = 2,
    onClick: () -> Unit,
    onQuickPayment: () -> Unit
) {
    val debt = debtWithDetails.debt
    val progress = debtWithDetails.progress
    val totalMoney = debt.money
    val remaining = debtWithDetails.remainingMoney

    val isDebt = debt.type == 0
    val accentColor = if (isDebt) DebtRoseColor else CreditEmeraldColor
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

    val formattedDate = remember(debt.date, dateFormat) {
        val dateObj = DateUtils.parseDate(debt.date)
        DateUtils.formatDate(dateObj, dateFormat)
    }

    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Row: Avatar, Title, Status Badges & Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Indicator Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isFullyPaid) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            else accentColor.copy(alpha = 0.12f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFullyPaid) Icons.Default.CheckCircle
                        else if (isDebt) Icons.Default.ArrowDownward
                        else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = if (isFullyPaid) MaterialTheme.colorScheme.primary else accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Subtitle Details Badges (Wallet, People, Due Date)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {

                        // Status Badge
                        if (isOverdue) {
                            BadgeChip(
                                icon = Icons.Default.Event,
                                text = "Overdue" ,
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        } else if (!debt.expirationDate.isNullOrBlank()) {
                            val formattedDueDate = remember(debt.expirationDate, dateFormat) {
                                val expDateObj = DateUtils.parseDate(debt.expirationDate)
                                DateUtils.formatDate(expDateObj, dateFormat)
                            }
                            BadgeChip(
                                icon = Icons.Default.Event,
                                text = "Due $formattedDueDate"
                            )
                        }
                    }
                    Text(
                        text = debt.description.ifBlank { if (isDebt) "Debt" else "Credit" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Money & Progress Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    if (!isFullyPaid) {
                        Text(
                            text = "Remaining",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = if (isFullyPaid) "Fully Settled"
                        else MoneyFormatter.format(
                            amount = remaining,
                            currencyCode = debtWithDetails.walletCurrency,
                            decimals = debtWithDetails.walletDecimals,
                            config = formatterConfig
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Black,
                        color = if (isFullyPaid) MaterialTheme.colorScheme.primary else accentColor
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total: ${MoneyFormatter.format(amount = totalMoney, currencyCode = debtWithDetails.walletCurrency, decimals = debtWithDetails.walletDecimals, config = formatterConfig)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
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
                        .height(6.dp)
                        .clip(CircleShape),
                    color = if (isFullyPaid) MaterialTheme.colorScheme.primary else accentColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            }

            // Note snippet preview if present
            if (!debt.note.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = debt.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Quick Payment Action Button
            if (!isFullyPaid) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onQuickPayment,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor.copy(alpha = 0.12f),
                        contentColor = accentColor
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Payment,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isDebt) "Record Repayment" else "Record Collection",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun BadgeChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Surface(
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
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
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmptyDebtState(
    selectedTab: Int,
) {
    val isDebt = selectedTab == 0
    val accentColor = if (isDebt) DebtRoseColor else CreditEmeraldColor

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDebt) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = accentColor
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = if (isDebt) "No Active Debts" else "No Pending Credits",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isDebt) "You have no outstanding debts to pay." else "You have no pending credits to collect.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
