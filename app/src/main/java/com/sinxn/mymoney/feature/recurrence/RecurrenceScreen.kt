package com.sinxn.mymoney.feature.recurrence

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sinxn.mymoney.R
import com.sinxn.mymoney.core.data.local.model.RecurrentTransactionWithDetails
import com.sinxn.mymoney.core.data.local.model.RecurrentTransferWithDetails
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.FinanceListItem
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MoneyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurrenceScreen(
    onNavigateUp: () -> Unit,
    onAddRecurrentTransaction: () -> Unit,
    onAddRecurrentTransfer: () -> Unit,
    onRecurrentTransactionClick: (String) -> Unit,
    onRecurrentTransferClick: (String) -> Unit,
    viewModel: RecurrenceViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) onAddRecurrentTransaction() else onAddRecurrentTransfer()
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Recurrent Item")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Transactions") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Transfers") }
                )
            }

            if (selectedTab == 0) {
                if (uiState.recurrentTransactions.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.message_no_recurrence_found),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        items(uiState.recurrentTransactions, key = { it.recurrentTransaction.id }) { item ->
                            RecurrentTransactionCard(
                                item = item,
                                formatterConfig = uiState.formatterConfig,
                                dateFormat = uiState.dateFormat,
                                onClick = { onRecurrentTransactionClick(item.recurrentTransaction.id) }
                            )
                        }
                    }
                }
            } else {
                if (uiState.recurrentTransfers.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.message_no_recurrence_found),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        items(uiState.recurrentTransfers, key = { it.recurrentTransfer.id }) { item ->
                            RecurrentTransferCard(
                                item = item,
                                formatterConfig = uiState.formatterConfig,
                                dateFormat = uiState.dateFormat,
                                onClick = { onRecurrentTransferClick(item.recurrentTransfer.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecurrentTransactionCard(
    item: RecurrentTransactionWithDetails,
    formatterConfig: MoneyFormatter.Config,
    dateFormat: Int,
    onClick: () -> Unit
) {
    val rt = item.recurrentTransaction
    val isIncome = rt.direction == 1
    val amountColor = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)
    val amount = if (isIncome) rt.money else -rt.money

    val formattedMoney = remember(amount, item.wallet.currency, formatterConfig) {
        MoneyFormatter.format(
            amount = amount,
            currencyCode = item.wallet.currency,
            decimals = 2,
            config = formatterConfig
        )
    }

    val formattedNextOccurrence = remember(rt.nextOccurrence, dateFormat) {
        rt.nextOccurrence?.let { next ->
            val nextDate = DateUtils.parseDate(next)
            DateUtils.formatDate(nextDate, dateFormat)
        }
    }

    FinanceListItem(
        icon = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CategoryIcon(
                    iconString = item.category.icon,
                    categoryName = item.category.name,
                    modifier = Modifier.size(44.dp)
                )
            }
        },
        title = item.category.name,
        subtitle = rt.description?.takeIf { it.isNotBlank() },
        amountText = (if (isIncome && !formattedMoney.startsWith("+")) "+" else "") + formattedMoney,
        amountColor = amountColor,
        subAmountText = formattedNextOccurrence,
        onClick = onClick
    )
}

@Composable
fun RecurrentTransferCard(
    item: RecurrentTransferWithDetails,
    formatterConfig: MoneyFormatter.Config,
    dateFormat: Int,
    onClick: () -> Unit
) {
    val rtf = item.recurrentTransfer
    val formattedMoney = remember(rtf.moneyFrom, item.walletFrom.currency, formatterConfig) {
        MoneyFormatter.format(
            amount = rtf.moneyFrom,
            currencyCode = item.walletFrom.currency,
            decimals = 2,
            config = formatterConfig
        )
    }

    val formattedNextOccurrence = remember(rtf.nextOccurrence, dateFormat) {
        rtf.nextOccurrence?.let { next ->
            val nextDate = DateUtils.parseDate(next)
            DateUtils.formatDate(nextDate, dateFormat)
        }
    }

    FinanceListItem(
        icon = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0284C7).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Repeat,
                    contentDescription = null,
                    tint = Color(0xFF0284C7),
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        title = "${item.walletFrom.name} → ${item.walletTo.name}",
        subtitle = rtf.description?.takeIf { it.isNotBlank() },
        amountText = formattedMoney,
        amountColor = Color(0xFF0284C7),
        subAmountText = formattedNextOccurrence,
        onClick = onClick
    )
}
