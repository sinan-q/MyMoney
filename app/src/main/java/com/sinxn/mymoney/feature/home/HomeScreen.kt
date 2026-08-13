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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.Archive

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
    onNavigateToRecurrences: () -> Unit = {},
    onNavigateMenuItem: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    var showMenu by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()

    val totalWallet = uiState.activeWallets.find { it.wallet.id == Constants.TOTAL_WALLET_ID }
    val otherWallets = uiState.activeWallets.filter { it.wallet.id != Constants.TOTAL_WALLET_ID }

    Column(
        modifier = Modifier.fillMaxSize()
        ) {
            if (totalWallet != null) {
                GrandTotalHeader(
                    wallet = totalWallet,
                    isTotalValid = uiState.isTotalValid,
                    balanceBreakdown = uiState.balanceBreakdown,
                    activeSection = "transactions", // Only used for animation states or specific UI, safe to hardcode
                    debtUiState = null, // Debt logic is handled in DebtListScreen
                    onClick = { onNavigateToWallet(totalWallet.wallet.id) }
                )
            }
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.pendingTransactions.isNotEmpty()) {
                    item {
                        Text(
                            text = "Pending Confirmation",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                    items(uiState.pendingTransactions, key = { "pending_${it.transaction.id}" }) { txWithCat ->
                        PendingConfirmationItem(
                            item = txWithCat,
                            onConfirm = { viewModel.confirmTransaction(txWithCat.transaction.id) },
                            onDismiss = { viewModel.dismissTransaction(txWithCat.transaction.id) }
                        )
                    }
                }

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

@Composable
fun PendingConfirmationItem(
    item: com.sinxn.mymoney.core.data.local.model.TransactionWithCategory,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
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
                    decimals = item.decimals ?: 2,
                    tintMode = if (item.transaction.direction == 1) MoneyFormatter.TintMode.INCOME else MoneyFormatter.TintMode.EXPENSE
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
                androidx.compose.material3.TextButton(onClick = onDismiss) {
                    Text("Dismiss", color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.material3.Button(
                    onClick = onConfirm,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Confirm")
                }
            }
        }
    }
}
