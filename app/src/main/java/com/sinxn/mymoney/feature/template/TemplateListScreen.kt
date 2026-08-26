package com.sinxn.mymoney.feature.template

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sinxn.mymoney.core.ui.components.AppExtendedFab
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.FinanceListItem
import com.sinxn.mymoney.core.ui.components.SearchBar
import com.sinxn.mymoney.ui.theme.TransferColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateListScreen(
    onAddTemplateClick: (Boolean) -> Unit = {},
    onTemplateClick: (String, Boolean) -> Unit = { _, _ -> },
    viewModel: TemplateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Transactions, 1: Transfers
    val context = LocalContext.current

    val txListState = rememberLazyListState()
    val transferListState = rememberLazyListState()
    val currentListState = if (selectedTab == 0) txListState else transferListState

    val isTx = selectedTab == 0
    val fabText = if (isTx) "New Template" else "New Transfer Template"
    val fabColor = if (isTx) MaterialTheme.colorScheme.primary else TransferColor

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            AppExtendedFab(
                text = fabText,
                icon = Icons.Default.Add,
                onClick = { onAddTemplateClick(selectedTab == 1) },
                expanded = !currentListState.isScrollInProgress,
                containerColor = fabColor,
                contentColor = Color.White
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search field
            val totalCount = uiState.totalTxCount + uiState.totalTrCount
            if (totalCount > 5 || uiState.searchQuery.isNotEmpty()) {
                SearchBar(searchQuery = uiState.searchQuery, setSearchQuery = viewModel::setSearchQuery)
            }

            // Tabs
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = if (uiState.searchQuery.isNotEmpty()) "Transactions (${uiState.transactionTemplates.size})" else "Transactions (${uiState.totalTxCount})",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = if (uiState.searchQuery.isNotEmpty()) "Transfers (${uiState.transferTemplates.size})" else "Transfers (${uiState.totalTrCount})",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (selectedTab == 0) {
                    if (uiState.transactionTemplates.isEmpty()) {
                        EmptyTemplateState(
                            message = if (uiState.searchQuery.isNotEmpty()) "No matching transaction templates" else "No transaction templates created yet",
                            isSearching = uiState.searchQuery.isNotEmpty()
                        )
                    } else {
                        LazyColumn(
                            state = txListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 88.dp)
                        ) {
                            items(
                                items = uiState.transactionTemplates,
                                key = { it.id },
                                contentType = { "tx_template" }
                            ) { item ->
                                TransactionTemplateListItem(
                                    item = item,
                                    onClick = { onTemplateClick(item.id, false) },
                                    onApply = {
                                        viewModel.applyTransactionTemplate(item.rawItem)
                                        Toast.makeText(context, "Template applied!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                } else {
                    if (uiState.transferTemplates.isEmpty()) {
                        EmptyTemplateState(
                            message = if (uiState.searchQuery.isNotEmpty()) "No matching transfer templates" else "No transfer templates created yet",
                            isSearching = uiState.searchQuery.isNotEmpty()
                        )
                    } else {
                        LazyColumn(
                            state = transferListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 88.dp)
                        ) {
                            items(
                                items = uiState.transferTemplates,
                                key = { it.id },
                                contentType = { "transfer_template" }
                            ) { item ->
                                TransferTemplateListItem(
                                    item = item,
                                    onClick = { onTemplateClick(item.id, true) },
                                    onApply = {
                                        viewModel.applyTransferTemplate(item.rawItem)
                                        Toast.makeText(context, "Transfer applied!", Toast.LENGTH_SHORT).show()
                                    }
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
private fun TransactionTemplateListItem(
    item: TransactionTemplateUi,
    onClick: () -> Unit,
    onApply: () -> Unit
) {
    val amountColor = if (item.isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)

    FinanceListItem(
        icon = {
            CategoryIcon(iconData = item.iconData)
        },
        title = item.title,
        subtitle = item.subtitle,
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.formattedMoney,
                    style = MaterialTheme.typography.titleMedium,
                    color = amountColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = onApply,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Apply Template",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        onClick = onClick
    )
}

@Composable
private fun TransferTemplateListItem(
    item: TransferTemplateUi,
    onClick: () -> Unit,
    onApply: () -> Unit
) {
    FinanceListItem(
        icon = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = null,
                    tint = TransferColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = item.title,
        subtitle = item.subtitle,
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.formattedMoney,
                    style = MaterialTheme.typography.titleMedium,
                    color = TransferColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = onApply,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Apply Transfer Template",
                        tint = TransferColor
                    )
                }
            }
        },
        onClick = onClick
    )
}

@Composable
private fun EmptyTemplateState(
    message: String,
    isSearching: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isSearching) Icons.Default.SearchOff else Icons.Default.BookmarkBorder,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        if (!isSearching) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Use the + button below to create reusable transaction or transfer templates.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
