package com.sinxn.mymoney.feature.event

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sinxn.mymoney.core.data.local.entity.EventEntity
import com.sinxn.mymoney.core.ui.components.AppExtendedFab
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.FilterComponent
import com.sinxn.mymoney.core.ui.components.FinanceListItem
import com.sinxn.mymoney.core.ui.components.IconData
import com.sinxn.mymoney.core.ui.components.SearchBar
import com.sinxn.mymoney.core.ui.components.parseIconData
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MoneyFormatter
import com.sinxn.mymoney.feature.place.PlaceSortOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventListScreen(
    onNavigateBack: () -> Unit,
    onAddEventClick: () -> Unit = {},
    onEventClick: (String) -> Unit = {},
    viewModel: EventViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val filteredEvents = remember(uiState.events, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.events
        } else {
            uiState.events.filter { item ->
                item.event.name.contains(searchQuery, ignoreCase = true) ||
                (!item.event.note.isNullOrBlank() && item.event.note.contains(searchQuery, ignoreCase = true)) ||
                (!item.event.tag.isNullOrBlank() && item.event.tag.contains(searchQuery, ignoreCase = true))
            }
        }
    }

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
            // Search field (shown when > 5 events or actively searching)
            if (uiState.events.size > 5 || searchQuery.isNotEmpty()) {
                SearchBar(
                    searchQuery = searchQuery,
                    setSearchQuery = { searchQuery = it }
                )
            }

            // Sort / Count header bar (between Search Bar and List)
            if (uiState.events.isNotEmpty()) {
                FilterComponent(
                    countText = if (searchQuery.isNotBlank()) {
                        "${filteredEvents.size} found"
                    } else {
                        "${uiState.events.size} ${if (uiState.events.size == 1) "event" else "events"}"
                    },
                    activeSortOption = uiState.sortOption,
                    options = EventSortOption.entries,
                    setSortOption = viewModel::setSortOption
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (filteredEvents.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matching events found" else "No events created yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        items(
                            items = filteredEvents,
                            key = { it.event.id },
                            contentType = { "event_item" }
                        ) { item ->
                            EventListItem(
                                item = item,
                                formatterConfig = uiState.formatterConfig,
                                dateFormat = uiState.dateFormat,
                                onClick = { onEventClick(item.event.id) }
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
    formatterConfig: MoneyFormatter.Config,
    dateFormat: Int,
    onClick: () -> Unit
) {
    val event = item.event

    val formattedDateRange = remember(event.startDate, event.endDate, dateFormat) {
        val startDateObj = DateUtils.parseDate(event.startDate)
        val endDateObj = DateUtils.parseDate(event.endDate)
        val startStr = DateUtils.formatDate(startDateObj, dateFormat)
        val endStr = DateUtils.formatDate(endDateObj, dateFormat)
        if (startStr == endStr) startStr else "$startStr - $endStr"
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

    val iconData = remember(event.icon, event.name) {
        parseIconData(event.icon.ifBlank { "ic_event" }, event.name)
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
        title = event.name,
        subtitle = formattedDateRange,
        amountText = (if (isPositive && !formattedMoney.startsWith("+")) "+" else "") + formattedMoney,
        amountColor = amountColor,
        onClick = onClick
    )
}


