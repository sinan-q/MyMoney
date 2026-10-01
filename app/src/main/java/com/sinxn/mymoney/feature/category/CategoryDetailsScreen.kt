package com.sinxn.mymoney.feature.category

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.groupTransactionsIntoMonthGroups
import com.sinxn.mymoney.core.ui.components.monthGroupedTransactionItems
import com.sinxn.mymoney.core.util.CategoryType
import com.sinxn.mymoney.core.util.MoneyFormatter
import kotlinx.coroutines.flow.collectLatest

private val SubcategoryChipShape = RoundedCornerShape(12.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailsScreen(
    onNavigateBack: () -> Unit,
    onTransactionClick: (String) -> Unit,
    onSubcategoryClick: (String) -> Unit = {},
    onEditCategoryClick: (String) -> Unit = {},
    onAddSubcategoryClick: (String, Int) -> Unit = { _, _ -> },
    onCustomFieldClick: (String) -> Unit = {},
    viewModel: CategoryDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var collapsedGroups by remember { mutableStateOf(setOf<String>()) }

    val groupedItems = remember(
        uiState.transactions,
        uiState.decimals,
        uiState.currencyCode,
        uiState.formatterConfig,
        uiState.dateFormat
    ) {
        groupTransactionsIntoMonthGroups(
            transactions = uiState.transactions,
            decimals = uiState.decimals,
            currencyCode = uiState.currencyCode,
            formatterConfig = uiState.formatterConfig,
            dateFormat = uiState.dateFormat
        )
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
                    val isSystem = cat?.type == CategoryType.SYSTEM || cat?.tag?.startsWith("system::") == true
                    if (cat != null && !isSystem) {
                        IconButton(onClick = { onEditCategoryClick(uiState.categoryId) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Category"
                            )
                        }
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
                                    uiState.categoryIconData?.let { iconData ->
                                        CategoryIcon(
                                            iconData = iconData,
                                            size = 48.dp
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


                                        val parentIcon = uiState.parentCategoryIconData
                                        if (parentCat != null && parentIcon != null) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "Subcategory of ",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Surface(
                                                    modifier = Modifier.clickable { onSubcategoryClick(parentCat.id) },
                                                    shape = SubcategoryChipShape,
                                                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        CategoryIcon(
                                                            iconData = parentIcon,
                                                            size = 28.dp
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

                    // Metrics Row
                    item {
                        CategoryMetricCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 12.dp),
                            income = uiState.totalIncome,
                            expense = uiState.totalExpense,
                            currencyCode = uiState.currencyCode,
                            decimals = uiState.decimals,
                            config = uiState.formatterConfig
                        )
                    }

                    // Subcategories Chips (if any)
                    if (uiState.subcategoriesUi.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end= 16.dp,  top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {

                                val chipBorder = CardDefaults.outlinedCardBorder()
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(
                                        items = uiState.subcategoriesUi,
                                        key = { it.id },
                                        contentType = { "subcat_chip" }
                                    ) { sub ->
                                        Surface(
                                            modifier = Modifier.clickable { onSubcategoryClick(sub.id) },
                                            shape = SubcategoryChipShape,
                                            border = chipBorder
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                CategoryIcon(
                                                    iconData = sub.iconData,
                                                    size = 28.dp
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

                    val allCustomFields = uiState.customFields + uiState.inheritedCustomFields
                    if (allCustomFields.isNotEmpty()) {
                        item {
                            Text(
                                text = "Custom Fields",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        items(
                            items = allCustomFields,
                            key = { it.id },
                            contentType = { "custom_field" }
                        ) { field ->
                            androidx.compose.material3.ListItem(
                                headlineContent = { Text(field.label) },
                                supportingContent = { Text(field.type.replaceFirstChar { it.uppercase() }) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onCustomFieldClick(field.id) },
                                trailingContent = {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null
                                    )
                                }
                            )
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
                        monthGroups = groupedItems,
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