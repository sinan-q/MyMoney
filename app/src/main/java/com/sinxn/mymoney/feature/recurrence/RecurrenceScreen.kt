package com.sinxn.mymoney.feature.recurrence

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sinxn.mymoney.core.ui.components.AppExtendedFab
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.EmptyListItem
import com.sinxn.mymoney.core.ui.components.FinanceListItem
import com.sinxn.mymoney.core.ui.components.TabPill
import com.sinxn.mymoney.ui.theme.ExpenseColor
import com.sinxn.mymoney.ui.theme.IncomeColor
import com.sinxn.mymoney.ui.theme.TransferColor
import kotlinx.coroutines.launch

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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    val txListState = rememberLazyListState()
    val transferListState = rememberLazyListState()
    val currentListState = if (pagerState.currentPage == 0) txListState else transferListState

    val isTx = pagerState.currentPage == 0
    val fabText = if (isTx) "New Recurrence" else "New Transfer"
    val fabColor = if (isTx) MaterialTheme.colorScheme.primary else TransferColor

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            AppExtendedFab(
                text = fabText,
                icon = Icons.Default.Add,
                onClick = {
                    if (pagerState.currentPage == 0) onAddRecurrentTransaction() else onAddRecurrentTransfer()
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
            TabPill(
                tabs = listOf(
                    "Transactions" to MaterialTheme.colorScheme.primary,
                    "Transfers" to TransferColor
                ),
                activeTab = pagerState.currentPage,
                onTabChange = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                }
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                key = { page -> page }
            ) { page ->
                val isTxPage = page == 0
                if (isTxPage) {
                    if (uiState.recurrentTransactions.isEmpty()) {
                        EmptyListItem(
                            text = "recurrent transaction",
                            isSearching = false
                        )
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
                        EmptyListItem(
                            text = "recurrent transfer",
                            isSearching = false
                        )
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
}

@Composable
fun RecurrentTransactionCard(
    item: RecurrentTxUiModel,
    onClick: () -> Unit
) {
    val amountColor = if (item.isIncome) IncomeColor else ExpenseColor

    FinanceListItem(
        icon = {
            CategoryIcon(iconData = item.iconData)
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
