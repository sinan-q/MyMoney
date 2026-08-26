package com.sinxn.mymoney.feature.place

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sinxn.mymoney.core.ui.components.AppExtendedFab
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.EmptyListItem
import com.sinxn.mymoney.core.ui.components.FilterComponent
import com.sinxn.mymoney.core.ui.components.FinanceListItem
import com.sinxn.mymoney.core.ui.components.SearchBar
import com.sinxn.mymoney.core.util.MoneyFormatter
import com.sinxn.mymoney.ui.theme.ExpenseColor
import com.sinxn.mymoney.ui.theme.IncomeColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceListScreen(
    onAddPlaceClick: () -> Unit = {},
    onPlaceClick: (String) -> Unit = {},
    viewModel: PlaceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

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
            AppExtendedFab(
                text = "Add Place",
                icon = Icons.Default.Add,
                onClick = onAddPlaceClick,
                expanded = !listState.isScrollInProgress
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search field (shown when > 5 places or actively searching)
            if (uiState.places.size > 5 || searchQuery.isNotEmpty()) {
                SearchBar(
                    searchQuery = searchQuery,
                    setSearchQuery = { searchQuery = it }
                )
            }

            // Sort / Count header bar (between Search Bar and List)
            if (uiState.places.isNotEmpty()) {
                FilterComponent(
                    countText = if (searchQuery.isNotBlank()) {
                        "${filteredPlaces.size} found"
                    } else {
                        "${uiState.places.size} ${if (uiState.places.size == 1) "place" else "places"}"
                    },
                    activeSortOption = uiState.sortOption,
                    options = PlaceSortOption.entries,
                    setSortOption = viewModel::setSortOption
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (filteredPlaces.isEmpty()) {
                    EmptyListItem(
                        isSearching = searchQuery.isNotEmpty(),
                        text = "places"
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        items(
                            items = filteredPlaces,
                            key = { it.place.id },
                            contentType = { "place_item" }
                        ) { item ->
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
        isPositive -> IncomeColor
        isNegative -> ExpenseColor
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
            CategoryIcon(iconData = item.iconData)
        },
        title = place.name,
        subtitle = subtitle,
        amountText = (if (isPositive && !formattedMoney.startsWith("+")) "+" else "") + formattedMoney,
        amountColor = amountColor,
        onClick = onClick
    )
}

