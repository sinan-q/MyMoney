package com.sinxn.mymoney.feature.event

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sinxn.mymoney.core.ui.components.AppExtendedFab
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.EmptyListItem
import com.sinxn.mymoney.core.ui.components.FilterComponent
import com.sinxn.mymoney.core.ui.components.FinanceListItem
import com.sinxn.mymoney.core.ui.components.SearchBar
import com.sinxn.mymoney.core.ui.components.parseIconData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventListScreen(
    onAddEventClick: () -> Unit = {},
    onEventClick: (String) -> Unit = {},
    viewModel: EventViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            AppExtendedFab(
                text = "Add Event",
                icon = Icons.Default.Add,
                onClick = onAddEventClick,
                expanded = !listState.isScrollInProgress
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.totalEventsCount > 5 || uiState.searchQuery.isNotEmpty()) {
                SearchBar(
                    searchQuery = uiState.searchQuery,
                    setSearchQuery = viewModel::setSearchQuery
                )
            }
            if (uiState.totalEventsCount > 0) {
                FilterComponent(
                    countText = if (uiState.searchQuery.isNotBlank()) {
                        "${uiState.events.size} found"
                    } else {
                        "${uiState.totalEventsCount} ${if (uiState.totalEventsCount == 1) "event" else "events"}"
                    },
                    activeSortOption = uiState.sortOption,
                    options = EventSortOption.entries,
                    setSortOption = viewModel::setSortOption
                )
            }

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (uiState.events.isEmpty()) {
                    EmptyListItem(
                        isSearching = uiState.searchQuery.isNotEmpty(),
                        text = "events"
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        items(
                            items = uiState.events,
                            key = { it.id },
                            contentType = { "event_item" }
                        ) { item ->
                            EventListItem(
                                item = item,
                                onClick = { onEventClick(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventListItem(
    item: EventItemUi,
    onClick: () -> Unit
) {
    val amountColor = when {
        item.isPositive -> Color(0xFF2E7D32)
        item.isNegative -> Color(0xFFC62828)
        else -> MaterialTheme.colorScheme.onSurface
    }

    val iconData = remember(item.icon, item.name) {
        parseIconData(item.icon.ifBlank { "ic_event" }, item.name)
    }

    FinanceListItem(
        icon = {
            CategoryIcon(iconData = iconData)
        },
        title = item.name,
        subtitle = item.formattedDateRange,
        amountText = item.amountText,
        amountColor = amountColor,
        onClick = onClick
    )
}