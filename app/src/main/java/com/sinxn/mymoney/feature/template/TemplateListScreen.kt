package com.sinxn.mymoney.feature.template

import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sinxn.mymoney.core.data.local.model.TransactionModelWithDetails
import com.sinxn.mymoney.core.data.local.model.TransferModelWithDetails
import com.sinxn.mymoney.core.ui.components.AppExtendedFab
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.FinanceListItem
import com.sinxn.mymoney.core.ui.components.IconData
import com.sinxn.mymoney.core.ui.components.parseIconData
import com.sinxn.mymoney.core.util.MoneyFormatter
import com.sinxn.mymoney.ui.theme.TransferColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateListScreen(
    onNavigateBack: () -> Unit,
    onAddTemplateClick: (Boolean) -> Unit = {},
    onTemplateClick: (String, Boolean) -> Unit = { _, _ -> },
    viewModel: TemplateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Transactions, 1: Transfers
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current

    val txListState = rememberLazyListState()
    val transferListState = rememberLazyListState()
    val currentListState = if (selectedTab == 0) txListState else transferListState

    val filteredTxTemplates = remember(uiState.transactionTemplates, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.transactionTemplates
        } else {
            uiState.transactionTemplates.filter { item ->
                (item.model.description?.contains(searchQuery, ignoreCase = true) == true) ||
                (item.categoryName?.contains(searchQuery, ignoreCase = true) == true) ||
                (item.walletName.contains(searchQuery, ignoreCase = true)) ||
                (item.model.tag?.contains(searchQuery, ignoreCase = true) == true) ||
                (item.model.note?.contains(searchQuery, ignoreCase = true) == true)
            }
        }
    }

    val filteredTransferTemplates = remember(uiState.transferTemplates, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.transferTemplates
        } else {
            uiState.transferTemplates.filter { item ->
                (item.model.description?.contains(searchQuery, ignoreCase = true) == true) ||
                (item.walletFromName.contains(searchQuery, ignoreCase = true)) ||
                (item.walletToName.contains(searchQuery, ignoreCase = true)) ||
                (item.model.tag?.contains(searchQuery, ignoreCase = true) == true) ||
                (item.model.note?.contains(searchQuery, ignoreCase = true) == true)
            }
        }
    }

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
            val totalCount = uiState.transactionTemplates.size + uiState.transferTemplates.size
            if (totalCount > 5 || searchQuery.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                "Search templates...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        } else null,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        )
                    )
                }
            }

            // Tabs
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "Transactions (${uiState.transactionTemplates.size})",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "Transfers (${uiState.transferTemplates.size})",
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
                    if (filteredTxTemplates.isEmpty()) {
                        EmptyTemplateState(
                            message = if (searchQuery.isNotEmpty()) "No matching transaction templates" else "No transaction templates created yet",
                            isSearching = searchQuery.isNotEmpty()
                        )
                    } else {
                        LazyColumn(
                            state = txListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 88.dp)
                        ) {
                            items(
                                items = filteredTxTemplates,
                                key = { it.model.id },
                                contentType = { "tx_template" }
                            ) { item ->
                                TransactionTemplateListItem(
                                    item = item,
                                    onClick = { onTemplateClick(item.model.id, false) },
                                    onApply = {
                                        viewModel.applyTransactionTemplate(item)
                                        Toast.makeText(context, "Template applied!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                } else {
                    if (filteredTransferTemplates.isEmpty()) {
                        EmptyTemplateState(
                            message = if (searchQuery.isNotEmpty()) "No matching transfer templates" else "No transfer templates created yet",
                            isSearching = searchQuery.isNotEmpty()
                        )
                    } else {
                        LazyColumn(
                            state = transferListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 88.dp)
                        ) {
                            items(
                                items = filteredTransferTemplates,
                                key = { it.model.id },
                                contentType = { "transfer_template" }
                            ) { item ->
                                TransferTemplateListItem(
                                    item = item,
                                    onClick = { onTemplateClick(item.model.id, true) },
                                    onApply = {
                                        viewModel.applyTransferTemplate(item)
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
    item: TransactionModelWithDetails,
    onClick: () -> Unit,
    onApply: () -> Unit
) {
    val isIncome = item.model.direction == 1
    val formattedMoney = (if (isIncome) "+" else "-") + MoneyFormatter.format(
        amount = item.model.money,
        currencyCode = item.walletCurrency,
        decimals = item.walletDecimals
    )
    val amountColor = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)
    val title = item.model.description?.takeIf { it.isNotBlank() } ?: (item.categoryName ?: "Template")
    val subtitle = buildString {
        append(item.walletName)
        if (!item.model.tag.isNullOrBlank()) {
            append(" • ")
            append(item.model.tag)
        }
    }

    val iconData = remember(item.categoryIcon, item.categoryName) {
        parseIconData(item.categoryIcon ?: "ic_category_other", item.categoryName ?: "Category")
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
                    iconData = iconData,
                    modifier = Modifier.size(44.dp)
                )
            }
        },
        title = title,
        subtitle = subtitle,
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formattedMoney,
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
    item: TransferModelWithDetails,
    onClick: () -> Unit,
    onApply: () -> Unit
) {
    val formattedMoney = MoneyFormatter.format(
        amount = item.model.moneyFrom,
        currencyCode = item.walletFromCurrency,
        decimals = item.walletFromDecimals
    )
    val title = item.model.description?.takeIf { it.isNotBlank() } ?: "Transfer"
    val subtitle = "${item.walletFromName} ➔ ${item.walletToName}"

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
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        title = title,
        subtitle = subtitle,
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formattedMoney,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF1565C0),
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = onApply,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Apply Transfer",
                        tint = MaterialTheme.colorScheme.primary
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
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isSearching) Icons.Default.SearchOff else Icons.Default.Bolt,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
