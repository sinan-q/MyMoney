package com.sinxn.mymoney.feature.recurrence

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sinxn.mymoney.R
import com.sinxn.mymoney.core.data.local.model.RecurrentTransactionWithDetails
import com.sinxn.mymoney.core.data.local.model.RecurrentTransferWithDetails
import com.sinxn.mymoney.core.ui.components.AppExtendedFab
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.FinanceListItem
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MoneyFormatter
import com.sinxn.mymoney.ui.theme.TransferColor

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

    val txListState = rememberLazyListState()
    val transferListState = rememberLazyListState()
    val currentListState = if (selectedTab == 0) txListState else transferListState

    val isTx = selectedTab == 0
    val fabText = if (isTx) "New Recurrence" else "New Transfer"
    val fabColor = if (isTx) MaterialTheme.colorScheme.primary else TransferColor

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            AppExtendedFab(
                text = fabText,
                icon = Icons.Default.Add,
                onClick = {
                    if (selectedTab == 0) onAddRecurrentTransaction() else onAddRecurrentTransfer()
                },
                expanded = !currentListState.isScrollInProgress,
                containerColor = fabColor,
                contentColor = Color.White
            )
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
                        state = txListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        items(
                            items = uiState.recurrentTransactions,
                            key = { it.id },
                            contentType = { "recurrent_tx" }
                        ) { item ->
                            RecurrentTransactionCard(
                                item = item,
                                onClick = { onRecurrentTransactionClick(item.id) }
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
                        state = transferListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        items(
                            items = uiState.recurrentTransfers,
                            key = { it.id },
                            contentType = { "recurrent_transfer" }
                        ) { item ->
                            RecurrentTransferCard(
                                item = item,
                                onClick = { onRecurrentTransferClick(item.id) }
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
    item: RecurrentTxUiModel,
    onClick: () -> Unit
) {
    val amountColor = if (item.isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)

    FinanceListItem(
        icon = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CategoryIcon(
                    iconData = item.iconData,
                    modifier = Modifier.size(44.dp)
                )
            }
        },
        title = item.title,
        subtitle = item.subtitle,
        amountText = item.formattedAmount,
        amountColor = amountColor,
        subAmountText = item.nextOccurrenceText,
        onClick = onClick
    )
}

@Composable
fun RecurrentTransferCard(
    item: RecurrentTransferUiModel,
    onClick: () -> Unit
) {
    FinanceListItem(
        icon = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(TransferColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Repeat,
                    contentDescription = null,
                    tint = TransferColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        title = item.title,
        subtitle = item.subtitle,
        amountText = item.formattedAmount,
        amountColor = TransferColor,
        subAmountText = item.nextOccurrenceText,
        onClick = onClick
    )
}
