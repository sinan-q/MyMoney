package com.sinxn.mymoney.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sinxn.mymoney.core.data.local.entity.CurrencyEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySelectionDialog(
    title: String = "Select Currency",
    currencies: List<CurrencyEntity>,
    selectedCurrencyIso: String?,
    onCurrencySelected: (CurrencyEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val filteredCurrencies = remember(currencies, searchQuery) {
        if (searchQuery.isBlank()) {
            currencies
        } else {
            currencies.filter { currency ->
                currency.iso.contains(searchQuery, ignoreCase = true) ||
                        currency.name.contains(searchQuery, ignoreCase = true) ||
                        (!currency.symbol.isNullOrBlank() && currency.symbol.contains(
                            searchQuery,
                            ignoreCase = true
                        ))
            }
        }
    }

    // Auto-scroll to selected currency on open
    LaunchedEffect(filteredCurrencies) {
        if (!selectedCurrencyIso.isNullOrBlank()) {
            val idx = filteredCurrencies.indexOfFirst {
                it.iso.equals(
                    selectedCurrencyIso,
                    ignoreCase = true
                )
            }
            if (idx >= 0) {
                listState.scrollToItem(idx)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.82f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                SearchBar(searchQuery) { searchQuery = it }

                Spacer(modifier = Modifier.height(8.dp))

                // Currency List
                if (filteredCurrencies.isEmpty()) {
                    EmptyListItem(isSearching = searchQuery.isNotBlank(), text = "currencies")
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(
                            items = filteredCurrencies,
                            key = { _, item -> item.iso }
                        ) { _, currency ->
                            val isSelected = currency.iso.equals(selectedCurrencyIso, ignoreCase = true)
                            FinanceListItem(
                                icon = {
                                    CategoryIcon(
                                        iconData = IconData(
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            text = currency.symbol ?: currency.iso.take(2)
                                        ),
                                        textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

                                    )
                                },
                                title = currency.name,
                                subtitle = currency.iso,
                                isSelected = isSelected,
                                onClick = {
                                    onCurrencySelected(currency)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}