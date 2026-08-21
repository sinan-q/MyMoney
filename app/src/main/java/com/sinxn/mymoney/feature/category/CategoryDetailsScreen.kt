package com.sinxn.mymoney.feature.category

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Unarchive
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
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
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
    onEditCategoryClick: (String) -> Unit = {},
    onAddSubcategoryClick: (String, Int) -> Unit = { _, _ -> },
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
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val cat = uiState.category
                    if (cat != null && cat.tag == null) {
                        IconButton(onClick = viewModel::toggleArchive) {
                            Icon(
                                imageVector = if (cat.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                contentDescription = if (cat.isArchived) "Unarchive Category" else "Archive Category"
                            )
                        }
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
                    onClick = { onEditCategoryClick(uiState.categoryId) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Category")
                }
            }
        },
        contentWindowInsets = WindowInsets(0,0,0,0)
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
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ),
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
                                            .clip(CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CategoryIcon(
                                            iconString = category.icon,
                                            categoryName = category.name,
                                            modifier = Modifier.size(44.dp)
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                        Text(
                                            text = category.name,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )


                                        if (parentCat != null) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "Subcategory of ",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Surface(
                                                    modifier = Modifier.clickable { onSubcategoryClick(parentCat.id) },
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                                                    // border = CardDefaults.outlinedCardBorder()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        CategoryIcon(
                                                            iconString = parentCat.icon,
                                                            categoryName = parentCat.name,
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                        Text(
                                                            text = parentCat.name,
                                                            style = MaterialTheme.typography.labelSmall,
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
                                income = uiState.totalIncome,
                                expense = uiState.totalExpense,
                                currencyCode = uiState.currencyCode,
                                decimals = uiState.decimals,
                                config = uiState.formatterConfig
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

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(subcategories, key = { it.id }) { sub ->
                                        Surface(
                                            modifier = Modifier.clickable { onSubcategoryClick(sub.id) },
                                            shape = RoundedCornerShape(12.dp),
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
                        decimals = uiState.decimals,
                        currencyCode = uiState.currencyCode,
                        formatterConfig = uiState.formatterConfig,
                        dateFormat = uiState.dateFormat,
                        emptyMessage = "No transactions in this category yet."
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryMetricCard(
    modifier: Modifier = Modifier,
    config: MoneyFormatter.Config,
    decimals: Int,
    currencyCode: String,
    income: Long,
    expense: Long
) {
    val formattedIncome = MoneyFormatter.format(
        amount = income,
        currencyCode = currencyCode,
        decimals = decimals,
        config = config
    )
    val formattedExpense = MoneyFormatter.format(
        amount = expense,
        currencyCode = currencyCode,
        decimals = decimals,
        config = config
    )
    val formattedTotal = MoneyFormatter.format(
        amount = income - expense,
        currencyCode = currencyCode,
        decimals = decimals,
        config = config
    )
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Summary",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column() {
                Text(
                    text = formattedTotal,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (income > 0 && expense > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF43A047).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "+$formattedIncome",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFE53935).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "-$formattedExpense",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFC62828),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

        }
    }
}