package com.sinxn.mymoney.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.sinxn.mymoney.core.data.local.entity.EventEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventSelectionDialog(
    title: String = "Select Event",
    events: List<EventEntity>,
    selectedEventId: String?,
    onEventSelected: (EventEntity?) -> Unit,
    onDismissRequest: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()

    var searchQuery by remember { mutableStateOf("") }

    val filteredEvents = remember(events, searchQuery) {
        if (searchQuery.isBlank()) {
            events
        } else {
            events.filter { event ->
                event.name.contains(searchQuery, ignoreCase = true) ||
                (!event.note.isNullOrBlank() && event.note.contains(searchQuery, ignoreCase = true)) ||
                (!event.tag.isNullOrBlank() && event.tag.contains(searchQuery, ignoreCase = true))
            }
        }
    }

    // Auto-scroll to selected event on launch
    LaunchedEffect(Unit) {
        if (!selectedEventId.isNullOrBlank()) {
            val idx = filteredEvents.indexOfFirst { it.id == selectedEventId }
            if (idx >= 0) {
                // Offset +1 because item 0 is "None" option
                listState.scrollToItem(
                    index = maxOf(0, idx),
                    scrollOffset = 0
                )
            }
        }
    }

    // Stop bottom overscroll from bouncing sheet
    val stopBottomOverscrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                return if (available.y < 0f) Offset(0f, available.y) else Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                return if (available.y < 0f) Velocity(0f, available.y) else Velocity.Zero
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.4f),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            // ── 1. Header Row: Title + Close ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // ── 2. Compact Search Field (only shown when events count > 5 or user searching) ──
            if (events.size > 5 || searchQuery.isNotEmpty()) {
                SearchBar(searchQuery) { searchQuery = it }
            }

            // ── 3. Edge-to-Edge Events List ──
            val eventDialogItems = remember(filteredEvents) {
                filteredEvents.map { event ->
                    EventDialogItem(
                        event = event,
                        iconData = if (event.icon.isNotBlank()) parseIconData(event.icon, event.name) else null
                    )
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(stopBottomOverscrollConnection),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // "None" Clear Selection Option
                item(key = "event_none") {
                    FinanceListItem(
                        icon = {
                            CategoryIconExtended(color = MaterialTheme.colorScheme.primary, icon = Icons.Default.Block)
                        },
                        title = "None",
                        onClick = {
                            onEventSelected(null)
                            scope.launch {
                                sheetState.hide()
                                onDismissRequest()
                            }
                        },
                        isSelected = selectedEventId.isNullOrBlank()
                    )
                }
                if (filteredEvents.isEmpty() && searchQuery.isNotEmpty()) {
                    item(key = "empty_state") {
                        EmptyListItem(isSearching = true, text = "events")
                    }
                } else {
                    itemsIndexed(
                        items = eventDialogItems,
                        key = { idx, item -> "event_${item.event.id}_$idx" },
                        contentType = { _, _ -> "event_dialog_row" }
                    ) { _, item ->
                        FinanceListItem(
                            icon = {
                                if (item.iconData != null) {
                                    CategoryIcon(iconData = item.iconData)
                                } else {
                                    CategoryIconExtended(
                                        color = MaterialTheme.colorScheme.primary,
                                        icon = Icons.Default.Event
                                    )
                                } },
                            title = item.event.name,
                            onClick = {
                                onEventSelected(item.event)
                                scope.launch {
                                    sheetState.hide()
                                    onDismissRequest()
                                }
                            },
                            subtitle = item.event.note,
                            isSelected = item.event.id == selectedEventId,
                        )
                    }
                }
            }
        }
    }
}

@Immutable
private data class EventDialogItem(
    val event: EventEntity,
    val iconData: IconData?
)

