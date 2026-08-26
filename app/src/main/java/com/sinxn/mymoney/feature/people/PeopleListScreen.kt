package com.sinxn.mymoney.feature.people

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sinxn.mymoney.core.ui.components.AppExtendedFab
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.EmptyListItem
import com.sinxn.mymoney.core.ui.components.FilterComponent
import com.sinxn.mymoney.core.ui.components.FinanceListItem
import com.sinxn.mymoney.core.ui.components.SearchBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleListScreen(
    onPersonClick: (String) -> Unit = {},
    onAddPersonClick: () -> Unit = {},
    viewModel: PeopleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

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
            if (uiState.totalPeopleCount > 5 || uiState.searchQuery.isNotEmpty()) {
                SearchBar(
                    searchQuery = uiState.searchQuery,
                    setSearchQuery = viewModel::setSearchQuery
                )
            }
            if (uiState.totalPeopleCount > 0) {
                FilterComponent(
                    countText = if (uiState.searchQuery.isNotBlank()) {
                        "${uiState.people.size} found"
                    } else {
                        "${uiState.totalPeopleCount} ${if (uiState.totalPeopleCount == 1) "person" else "people"}"
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
            } else if (uiState.people.isEmpty()) {
                EmptyListItem(
                    isSearching = uiState.searchQuery.isNotEmpty(),
                    text = "people"
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(
                        items = uiState.people,
                        key = { it.id },
                        contentType = { "person_item" }
                    ) { item ->
                        FinanceListItem(
                            icon = {
                                CategoryIcon(
                                    iconData = item.iconData,
                                    modifier = Modifier.size(44.dp)
                                )
                            },
                            title = item.name,
                            onClick = { onPersonClick(item.id) }
                        )
                    }
                }
            }
        }
    }
}