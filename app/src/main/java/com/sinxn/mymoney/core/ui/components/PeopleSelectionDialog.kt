package com.sinxn.mymoney.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.sinxn.mymoney.core.data.local.entity.PersonEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleSelectionDialog(
    title: String = "Select People",
    people: List<PersonEntity>,
    selectedPeopleIds: Set<String>,
    onPersonToggle: (PersonEntity) -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()

    var searchQuery by remember { mutableStateOf("") }

    val filteredPeople = remember(people, searchQuery) {
        if (searchQuery.isBlank()) {
            people
        } else {
            people.filter { person ->
                person.name.contains(searchQuery, ignoreCase = true) ||
                (!person.tag.isNullOrBlank() && person.tag.contains(searchQuery, ignoreCase = true)) ||
                (!person.note.isNullOrBlank() && person.note.contains(searchQuery, ignoreCase = true))
            }
        }
    }

    val selectedCount = remember(selectedPeopleIds) { selectedPeopleIds.size }

    // Auto-scroll to first selected person on launch
    LaunchedEffect(Unit) {
        if (selectedPeopleIds.isNotEmpty()) {
            val idx = filteredPeople.indexOfFirst { it.id in selectedPeopleIds }
            if (idx >= 0) {
                listState.scrollToItem(
                    index = maxOf(0, idx - 1),
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
            // ── 1. Header Row: Title + Selected Count Pill + Close ──
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selectedCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "$selectedCount selected",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
            if (people.size > 5 || searchQuery.isNotEmpty()) {
                SearchBar(searchQuery) { searchQuery = it}
            }

            // ── 3. Edge-to-Edge People List ──
            val peopleDialogItems = remember(filteredPeople) {
                filteredPeople.map { person ->
                    PeopleDialogItem(
                        person = person,
                        iconData = parseIconData(person.icon, person.name)
                    )
                }
            }

            if (filteredPeople.isEmpty()) {
                EmptyListItem(isSearching = true, text = "people")
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(stopBottomOverscrollConnection),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    itemsIndexed(
                        items = peopleDialogItems,
                        key = { idx, item -> "person_${item.person.id}_$idx" },
                        contentType = { _, _ -> "person_dialog_row" }
                    ) { _, item ->
                        FinanceListItem(
                            icon = {
                                CategoryIcon(iconData = item.iconData)
                            },
                            title = item.person.name,
                            onClick = { onPersonToggle(item.person) },
                            subtitle =  item.person.note,
                            isSelected = item.person.id in selectedPeopleIds,
                        )
                    }
                }
            }
        }
    }
}

@Immutable
private data class PeopleDialogItem(
    val person: PersonEntity,
    val iconData: IconData
)
