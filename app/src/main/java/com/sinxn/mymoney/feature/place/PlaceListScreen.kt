package com.sinxn.mymoney.feature.place

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sinxn.mymoney.core.ui.components.AppExtendedFab
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.EmptyListItem
import com.sinxn.mymoney.core.ui.components.FilterComponent
import com.sinxn.mymoney.core.ui.components.FinanceListItem
import com.sinxn.mymoney.core.ui.components.SearchBar
import com.sinxn.mymoney.ui.theme.ExpenseColor
import com.sinxn.mymoney.ui.theme.IncomeColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceListScreen(
    onAddPlaceClick: () -> Unit = {},
    onPlaceClick: (String) -> Unit = {},
    viewModel: PlaceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

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
            if (uiState.totalPlacesCount > 5 || uiState.searchQuery.isNotEmpty()) {
                SearchBar(
                    searchQuery = uiState.searchQuery,
                    setSearchQuery = viewModel::setSearchQuery
                )
            }

            // Sort / Count header bar (between Search Bar and List)
            if (uiState.totalPlacesCount > 0) {
                FilterComponent(
                    countText = if (uiState.searchQuery.isNotBlank()) {
                        "${uiState.places.size} found"
                    } else {
                        "${uiState.totalPlacesCount} ${if (uiState.totalPlacesCount == 1) "place" else "places"}"
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
                } else if (uiState.places.isEmpty()) {
                    EmptyListItem(
                        isSearching = uiState.searchQuery.isNotEmpty(),
                        text = "places"
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        items(
                            items = uiState.places,
                            key = { it.id },
                            contentType = { "place_item" }
                        ) { item ->
                            PlaceListItem(
                                item = item,
                                onClick = { onPlaceClick(item.id) }
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
    onClick: () -> Unit
) {
    val amountColor = when {
        item.isPositive -> IncomeColor
        item.isNegative -> ExpenseColor
        else -> MaterialTheme.colorScheme.onSurface
    }

    FinanceListItem(
        icon = {
            CategoryIcon(iconData = item.iconData)
        },
        title = item.name,
        subtitle = item.subtitle,
        amountText = item.amountText,
        amountColor = amountColor,
        onClick = onClick
    )
}

