package com.sinxn.mymoney.feature.people

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sinxn.mymoney.core.data.local.entity.PersonEntity
import com.sinxn.mymoney.core.ui.components.AppExtendedFab
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.EmptyListItem
import com.sinxn.mymoney.core.ui.components.FilterComponent
import com.sinxn.mymoney.core.ui.components.IconData
import com.sinxn.mymoney.core.ui.components.SearchBar
import com.sinxn.mymoney.core.ui.components.parseIconData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleListScreen(
    onPersonClick: (String) -> Unit = {},
    onAddPersonClick: () -> Unit = {},
    viewModel: PeopleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val filteredPeople = remember(uiState.people, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.people
        } else {
            uiState.people.filter { person ->
                person.name.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val peopleUiModels = remember(filteredPeople) {
        filteredPeople.map { person ->
            PersonUiModel(
                person = person,
                iconData = parseIconData(person.icon, person.name)
            )
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            AppExtendedFab(
                text = "Add Person",
                icon = Icons.Default.Add,
                onClick = onAddPersonClick,
                expanded = !listState.isScrollInProgress
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.people.size > 5 || searchQuery.isNotEmpty()) {
                SearchBar(
                    searchQuery = searchQuery,
                    setSearchQuery = { searchQuery = it }
                )
            }
            if (uiState.people.isNotEmpty()) {
                FilterComponent(
                    countText = if (searchQuery.isNotBlank()) {
                        "${peopleUiModels.size} found"
                    } else {
                        "${uiState.people.size} ${if (uiState.people.size == 1) "person" else "people"}"
                    },
                    activeSortOption = uiState.sortOption,
                    options = PersonSortOption.entries,
                    setSortOption = viewModel::setSortOption
                )
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (peopleUiModels.isEmpty()) {
                EmptyListItem(
                    isSearching = searchQuery.isNotEmpty(),
                    text = "people"
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    itemsIndexed(
                        items = peopleUiModels,
                        key = { idx, item -> "person_${item.person.id}_$idx" },
                        contentType = { _, _ -> "person_item" }
                    ) { _, item ->
                        PersonRow(
                            item = item,
                            onClick = { onPersonClick(item.person.id) }
                        )
                    }
                }
            }
        }
    }
}

@Immutable
private data class PersonUiModel(
    val person: PersonEntity,
    val iconData: IconData
)

@Composable
private fun PersonRow(
    item: PersonUiModel,
    onClick: () -> Unit
) {
    val person = item.person
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIcon(iconData = item.iconData)

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = person.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!person.tag.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = person.tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                if (!person.note.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = person.note,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}