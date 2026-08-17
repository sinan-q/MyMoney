package com.sinxn.mymoney.feature.place

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.FinanceListItem
import com.sinxn.mymoney.core.util.MoneyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceListScreen(
    onNavigateBack: () -> Unit,
    onAddPlaceClick: () -> Unit = {},
    onPlaceClick: (String) -> Unit = {},
    viewModel: PlaceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filteredPlaces = remember(uiState.places, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.places
        } else {
            uiState.places.filter { item ->
                item.place.name.contains(searchQuery, ignoreCase = true) ||
                (!item.place.address.isNullOrBlank() && item.place.address.contains(searchQuery, ignoreCase = true)) ||
                (!item.place.tag.isNullOrBlank() && item.place.tag.contains(searchQuery, ignoreCase = true))
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPlaceClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Place")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search field (shown when > 5 places or actively searching)
            if (uiState.places.size > 5 || searchQuery.isNotEmpty()) {
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
                                "Search places...",
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

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (filteredPlaces.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matching places found" else "No places created yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        items(filteredPlaces, key = { it.place.id }) { item ->
                            PlaceListItem(
                                item = item,
                                formatterConfig = uiState.formatterConfig,
                                onClick = { onPlaceClick(item.place.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceListItem(
    item: PlaceItemUi,
    formatterConfig: MoneyFormatter.Config,
    onClick: () -> Unit
) {
    val place = item.place

    val subtitle = remember(place.address, place.tag) {
        when {
            !place.address.isNullOrBlank() && !place.tag.isNullOrBlank() -> "${place.address} • ${place.tag}"
            !place.address.isNullOrBlank() -> place.address
            !place.tag.isNullOrBlank() -> place.tag
            place.latitude != null && place.longitude != null -> String.format("%.4f, %.4f", place.latitude, place.longitude)
            else -> "No address specified"
        }
    }

    val isPositive = item.totalAmount > 0
    val isNegative = item.totalAmount < 0
    val amountColor = when {
        isPositive -> Color(0xFF2E7D32)
        isNegative -> Color(0xFFC62828)
        else -> MaterialTheme.colorScheme.onSurface
    }

    val formattedMoney = remember(item.totalAmount, item.currencyCode, item.decimals, formatterConfig) {
        MoneyFormatter.format(
            amount = item.totalAmount,
            currencyCode = item.currencyCode,
            decimals = item.decimals,
            config = formatterConfig
        )
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
                    iconString = place.icon.ifBlank { "ic_place" },
                    categoryName = place.name,
                    modifier = Modifier.size(44.dp)
                )
            }
        },
        title = place.name,
        subtitle = subtitle,
        amountText = (if (isPositive && !formattedMoney.startsWith("+")) "+" else "") + formattedMoney,
        amountColor = amountColor,
        onClick = onClick
    )
}

