package com.sinxn.mymoney.feature.category

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
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
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.TransactionItem
import com.sinxn.mymoney.core.ui.components.groupTransactionsByMonth
import com.sinxn.mymoney.core.ui.components.monthGroupedTransactionItems
import com.sinxn.mymoney.core.util.CategoryType
import com.sinxn.mymoney.core.util.MoneyFormatter
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailsScreen(
    onNavigateBack: () -> Unit,
    onTransactionClick: (String) -> Unit,
    onSubcategoryClick: (String) -> Unit = {},
    viewModel: CategoryDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var collapsedGroups by remember { mutableStateOf(setOf<String>()) }

    val groupedItems = remember(uiState.transactions) {
        groupTransactionsByMonth(uiState.transactions)
    }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                CategoryDetailsEvent.Deleted -> onNavigateBack()
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Category") },
            text = { Text("Are you sure you want to delete '${uiState.category?.name ?: "this category"}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteCategory()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.category?.name ?: "Category Details",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.category != null && uiState.category?.tag == null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Category",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.category != null) {
                FloatingActionButton(
                    onClick = viewModel::openEditDialog,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Category")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.category == null) {
                Text(
                    text = "Category not found",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                val category = uiState.category!!
                val parentCat = uiState.parentCategory
                val subcategories = uiState.subcategories

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp, top = 8.dp)
                ) {
                    // Header Card
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CategoryIcon(
                                            iconString = category.icon,
                                            categoryName = category.name,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = category.name,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (category.type == CategoryType.INCOME) {
                                                    Color(0xFF43A047).copy(alpha = 0.15f)
                                                } else {
                                                    Color(0xFFE53935).copy(alpha = 0.15f)
                                                }
                                            ) {
                                                Text(
                                                    text = if (category.type == CategoryType.INCOME) "Income" else "Expense",
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (category.type == CategoryType.INCOME) Color(0xFF2E7D32) else Color(0xFFC62828)
                                                )
                                            }

                                            if (parentCat != null) {
                                                Text(
                                                    text = "Subcategory of ${parentCat.name}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            } else if (subcategories.isNotEmpty()) {
                                                Text(
                                                    text = "${subcategories.size} subcategories",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Metrics Row (Expense Total, Income Total, Transaction Count)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end= 16.dp, top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CategoryMetricCard(
                                modifier = Modifier.weight(1f),
                                title = "Total Expense",
                                value = MoneyFormatter.format(uiState.totalExpense, "USD"),
                                icon = Icons.AutoMirrored.Filled.TrendingDown,
                                color = Color(0xFFE53935)
                            )

                            CategoryMetricCard(
                                modifier = Modifier.weight(1f),
                                title = "Total Income",
                                value = MoneyFormatter.format(uiState.totalIncome, "USD"),
                                icon = Icons.Default.TrendingUp,
                                color = Color(0xFF43A047)
                            )
                        }
                    }

                    // Subcategories List (if any)
                    if (subcategories.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end= 16.dp,  top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Subcategories (${subcategories.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(subcategories, key = { it.id }) { sub ->
                                        Surface(
                                            modifier = Modifier.clickable { onSubcategoryClick(sub.id) },
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            border = CardDefaults.outlinedCardBorder()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                CategoryIcon(
                                                    iconString = sub.icon,
                                                    categoryName = sub.name,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Text(
                                                    text = sub.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Transactions Section Header
                    item {
                        Text(
                            text = "Transactions (${uiState.transactions.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Month-grouped Transactions List with Sticky Headers
                    monthGroupedTransactionItems(
                        items = groupedItems,
                        collapsedGroups = collapsedGroups,
                        onToggleGroup = { headerKey ->
                            collapsedGroups = if (headerKey in collapsedGroups) {
                                collapsedGroups - headerKey
                            } else {
                                collapsedGroups + headerKey
                            }
                        },
                        onTransactionClick = onTransactionClick,
                        emptyMessage = "No transactions in this category yet."
                    )
                }
            }
        }

        if (uiState.isEditDialogOpen) {
            CategoryDetailEditDialog(
                uiState = uiState,
                onNameChange = viewModel::onNameChange,
                onDismiss = viewModel::closeEditDialog,
                onSave = viewModel::saveCategory,
                onDelete = {
                    viewModel.closeEditDialog()
                    showDeleteDialog = true
                }
            )
        }
    }
}

@Composable
private fun CategoryMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CategoryTransactionItemCard(
    item: TransactionWithCategory,
    onClick: () -> Unit
) {
    val isIncome = item.transaction.direction == 1 || item.transaction.type == 1
    val currency = item.currencySymbol ?: item.currencyCode ?: "$"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CategoryIcon(
                iconString = item.categoryIcon ?: "ic_category",
                categoryName = item.categoryName ?: "Uncategorized",
                modifier = Modifier.size(40.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = item.transaction.note.takeIf { !it.isNullOrEmpty() }
                        ?: item.transaction.description.takeIf { !it.isNullOrEmpty() }
                        ?: item.categoryName ?: "Transaction",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.transaction.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!item.categoryName.isNullOrEmpty()) {
                        Text(
                            text = "• ${item.categoryName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Text(
                text = (if (isIncome) "+" else "-") + MoneyFormatter.format(item.transaction.money, currency),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isIncome) Color(0xFF43A047) else Color(0xFFE53935)
            )
        }
    }
}

@Composable
private fun CategoryDetailEditDialog(
    uiState: CategoryDetailsUiState,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    val canDelete = uiState.category?.tag == null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = uiState.editName,
                    onValueChange = onNameChange,
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = uiState.editName.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (canDelete) {
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

private fun String?.isNullOrEmpty(): Boolean = this == null || this.trim().isEmpty()
