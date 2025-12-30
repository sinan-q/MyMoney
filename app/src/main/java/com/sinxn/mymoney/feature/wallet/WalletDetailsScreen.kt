package com.sinxn.mymoney.feature.wallet

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sinxn.mymoney.core.data.local.entity.TransactionEntity
import com.sinxn.mymoney.core.data.local.model.WalletWithBalance
import com.sinxn.mymoney.core.util.MoneyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletDetailsScreen(
    onNavigateUp: () -> Unit,
    viewModel: WalletDetailsViewModel = hiltViewModel()
) {
    val wallet by viewModel.wallet.collectAsState(initial = null)
    val transactions by viewModel.transactions.collectAsState(initial = emptyList())
    val settings by viewModel.formattingSettings.collectAsState()

    // Map Settings to Formatter Config
    val formatterConfig = MoneyFormatter.Config(
        showCurrency = settings.showCurrency,
        groupDigits = settings.groupDigits,
        roundDecimals = settings.roundDecimals,
        showPlusMinus = settings.showPlusMinus
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = wallet?.wallet?.name ?: "Wallet Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Wallet Summary Header
            wallet?.let { walletData ->
                WalletHeader(walletData, formatterConfig)
            }

            // Transactions List
            wallet?.let { walletData ->
                TransactionList(
                    items = transactions,
                    decimals = walletData.decimals,
                    currencyCode = walletData.wallet.currency,
                    formatterConfig = formatterConfig
                )
            }
        }
    }
}

@Composable
fun WalletHeader(
    wallet: WalletWithBalance,
    formatterConfig: MoneyFormatter.Config
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Total Balance",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            val formattedBalance = MoneyFormatter.format(
                amount = wallet.currentBalance,
                currencyCode = wallet.wallet.currency,
                decimals = wallet.decimals,
                config = formatterConfig
            )
            Text(
                text = formattedBalance,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionList(
    items: List<TransactionListItem>,
    decimals: Int,
    currencyCode: String,
    formatterConfig: MoneyFormatter.Config
) {
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No transactions found", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            
            var currentHeader: TransactionListItem.Header? = null
            val customGrouped = mutableListOf<Pair<TransactionListItem.Header, MutableList<TransactionListItem.Transaction>>>()
            
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
                }
            }
            
            customGrouped.forEach { (header, transactions) ->
                stickyHeader {
                    TransactionHeader(header, decimals, currencyCode, formatterConfig)
                }
                
                items(transactions) { transactionItem ->
                     Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                         TransactionItem(transactionItem.transaction, decimals, currencyCode, formatterConfig)
                     }
                }
            }
        }
    }
}

@Composable
fun TransactionHeader(
    header: TransactionListItem.Header,
    decimals: Int,
    currencyCode: String,
    formatterConfig: MoneyFormatter.Config
) {
    val formattedDate = com.sinxn.mymoney.core.util.DateUtils.formatMonthHeader(header.date)
    val formattedTotal = MoneyFormatter.format(
        amount = header.totalAmount,
        currencyCode = currencyCode,
        decimals = decimals,
        config = formatterConfig
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface) // Opaque for sticky
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formattedDate,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = formattedTotal,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (header.totalAmount >= 0) Color(0xFF4CAF50) else Color(0xFFE53935)
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

@Composable
fun TransactionItem(
    transaction: TransactionEntity,
    decimals: Int,
    currencyCode: String,
    formatterConfig: MoneyFormatter.Config
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description ?: "No Description",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = transaction.date, // Note: This could also be formatted nicely now
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            val isIncome = transaction.direction == 1
            val amountColor = if (isIncome) Color(0xFF4CAF50) else Color(0xFFE53935)

            val amount = if (isIncome) transaction.money else -transaction.money
            val formattedMoney = MoneyFormatter.format(
                amount = amount, // Send signed amount
                currencyCode = currencyCode,
                decimals = decimals,
                config = formatterConfig
            )
            
            Text(
                text = formattedMoney,
                style = MaterialTheme.typography.titleMedium,
                color = amountColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
