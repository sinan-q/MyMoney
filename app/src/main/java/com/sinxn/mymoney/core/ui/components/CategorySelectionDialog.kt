package com.sinxn.mymoney.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelectionDialog(
    showIncome: Boolean, // Initial tab
    incomeCategories: List<CategoryEntity>,
    expenseCategories: List<CategoryEntity>,
    selectedCategoryId: String?,
    onCategorySelected: (CategoryEntity) -> Unit,
    onDismissRequest: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = if (showIncome) 1 else 0,
        pageCount = { 2 }
    )
    val tabs = listOf("Expenses", "Income")

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Category", 
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                SecondaryTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = MaterialTheme.colorScheme.surface,
//                    edgePadding = 0.dp TODO
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            },
                            text = { 
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium
                                ) 
                            }
                        )
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                    userScrollEnabled = true,
                    beyondViewportPageCount = 1
                ) { page ->
                    val categories = if (page == 0) expenseCategories else incomeCategories
                    
                    if (categories.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No categories found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(categories, key = { it.id }) { category ->
                                val isSelected = category.id == selectedCategoryId
                                ListItem(
                                    headlineContent = { 
                                        Text(
                                            text = category.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        ) 
                                    },
                                    trailingContent = {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check, 
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    modifier = Modifier.clickable {
                                        onCategorySelected(category)
                                        onDismissRequest()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
