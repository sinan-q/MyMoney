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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToBackup: () -> Unit,
    onNavigateToWallet: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onAddTransaction: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    var showMenu by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "My Money",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
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
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            androidx.compose.material3.FloatingActionButton(
                onClick = onAddTransaction,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { padding ->
        val totalWallet = uiState.activeWallets.find { it.wallet.id == com.sinxn.mymoney.core.util.Constants.TOTAL_WALLET_ID }
        val otherWallets = uiState.activeWallets.filter { it.wallet.id != com.sinxn.mymoney.core.util.Constants.TOTAL_WALLET_ID }

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp), // More bottom padding for FAB
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Grand Total Header
            totalWallet?.let {
                item {
                    GrandTotalHeader(
                        wallet = it,
                        onClick = { onNavigateToWallet(it.wallet.id) }
                    )
                }
            }

            // Section Label
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

            // Active Wallets
            items(otherWallets) { walletWithBalance ->
                WalletItem(
                    item = walletWithBalance,
                    onClick = { onNavigateToWallet(walletWithBalance.wallet.id) }
                )
            }

            // Archived Wallets
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
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
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
                text = "Global Balance",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            val formattedBalance = com.sinxn.mymoney.core.util.MoneyFormatter.format(
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

            val formattedBalance = com.sinxn.mymoney.core.util.MoneyFormatter.format(
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
