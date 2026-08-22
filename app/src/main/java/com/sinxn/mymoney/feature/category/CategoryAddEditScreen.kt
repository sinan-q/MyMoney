package com.sinxn.mymoney.feature.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.CategorySelectionDialog
import com.sinxn.mymoney.core.ui.components.DescriptionEditForm
import com.sinxn.mymoney.core.ui.components.FormCardContainer
import com.sinxn.mymoney.core.ui.components.TabPill
import com.sinxn.mymoney.core.ui.components.TransactionFormRowItem
import com.sinxn.mymoney.core.ui.components.parseIconData
import com.sinxn.mymoney.core.util.CategoryType
import com.sinxn.mymoney.feature.settings.SettingsSwitchItem
import com.sinxn.mymoney.ui.theme.ExpenseColor
import com.sinxn.mymoney.ui.theme.IncomeColor
import kotlinx.coroutines.flow.collectLatest
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryAddEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: CategoryAddEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    var showParentPicker by remember { mutableStateOf(false) }
    var iconPreviewName by remember(uiState.isLoading) { mutableStateOf(uiState.name) }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val accentColor = remember(uiState.type) {
        if (uiState.type == CategoryType.INCOME) IncomeColor else ExpenseColor
    }
    val scrollState = rememberScrollState()


    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                CategoryAddEditEvent.Saved -> onNavigateBack()
                CategoryAddEditEvent.Deleted -> onNavigateBack()
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Category") },
            text = { Text("Are you sure you want to delete '${uiState.name}'? All subcategories and associated transactions will remain, but category link will be removed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        viewModel.deleteCategory()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showIconPicker) {
        IconColorPickerDialog(
            currentIcon = uiState.icon,
            categoryName = iconPreviewName.ifBlank { "Category" },
            onDismiss = { showIconPicker = false },
            onIconSelected = { newIcon ->
                viewModel.onIconChange(newIcon)
                showIconPicker = false
            }
        )
    }

    if (showParentPicker) {
        CategorySelectionDialog(
            title = "Parent Category",
            categories = uiState.availableParentCategories,
            selectedCategoryId = uiState.parentId,
            showIncome = uiState.type == CategoryType.INCOME,
            showNoneOption = true,
            noneOptionLabel = "None (Top Level Category)",
            onCategorySelected = { category ->
                viewModel.onParentIdChange(category?.id)
                showParentPicker = false
            },
            onDismissRequest = { showParentPicker = false }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditMode) "Edit Category" else "New Category",
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
                    if (uiState.isEditMode && !uiState.isSystemCategory) {
                        IconButton(onClick = {
                            focusManager.clearFocus()
                            showDeleteConfirmation = true
                        }) {
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
            ExtendedFloatingActionButton(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.saveCategory()
                },
                containerColor = if (uiState.name.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (uiState.name.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(18.dp),
                icon = { if (uiState.isEditMode) Icon(Icons.Default.Check, contentDescription = "Save") else Icon(Icons.Default.Add, contentDescription = "Save") },
                text = { Text(if (uiState.isEditMode) "Save Changes" else "Create Category", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                }
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(bottom = 80.dp)
                    ) {
                    // Header Card: Icon preview & Name input
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                            // Interactive Icon Avatar
                            Box(
                                modifier = Modifier
                                    .clickable {
                                        focusManager.clearFocus()
                                        showIconPicker = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                CategoryIcon(
                                    iconString = uiState.icon,
                                    categoryName = uiState.name.ifBlank { "Category" },
                                    modifier = Modifier.size(76.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Icon",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Tap icon to change color or symbol",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                    }

                    FormCardContainer {
                        DescriptionEditForm(
                            icon = Icons.Default.Edit,
                            accentColor = accentColor,
                            label = "Category Name",
                            placeHolder = "Enter Category name",
                            value = uiState.name,
                            onValueChange = viewModel::onNameChange,
                            onFocusChange = { isFocused ->
                                if (!isFocused) {
                                    iconPreviewName = uiState.name
                                }
                            }
                        )
                        if (!uiState.isSystemCategory) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Category Type",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                )

                                TabPill(
                                    tabs = listOf(
                                        "Income" to IncomeColor,
                                        "Expense" to ExpenseColor
                                    ),
                                    activeTab = uiState.type,
                                    onTabChange = { newType ->
                                        focusManager.clearFocus()
                                        viewModel.onTypeChange(newType)
                                    }
                                )
                            }

                        }

                        // Parent Category Selector Card
                        if (!uiState.isSystemCategory && !uiState.hasSubcategories) {
                            val selectedParent = uiState.availableParentCategories.find { it.id == uiState.parentId }
                            val iconData = remember(selectedParent) {
                                if (selectedParent != null) parseIconData(
                                    selectedParent.icon,
                                    selectedParent.name
                                ) else null
                            }
                            TransactionFormRowItem(
                                active = selectedParent != null,
                                icon = Icons.Default.Folder,
                                accentColor = accentColor,
                                value = selectedParent?.name?: "None",
                                label = "Parent Category",
                                trailingIconData = iconData,
                                onClick = {
                                    focusManager.clearFocus()
                                    showParentPicker = true
                                }
                            )
                        }

                    }
                    FormCardContainer {
                        SettingsSwitchItem(
                            title = "Show in Reports",
                            checked = uiState.showReport,
                            onCheckedChange = { showReport ->
                                focusManager.clearFocus()
                                viewModel.onShowReportChange(showReport)
                            },
                            accentColor = accentColor,
                            horizontalPadding = 16.dp,
                        )
                    }
                    // Bottom padding spacer for FAB
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }
}


@Composable
private fun IconColorPickerDialog(
    currentIcon: String,
    categoryName: String,
    onDismiss: () -> Unit,
    onIconSelected: (String) -> Unit
) {
    val initialData = remember(currentIcon, categoryName) {
        parseIconData(currentIcon, categoryName.ifBlank { "Category" })
    }

    var selectedColor by remember { mutableStateOf(initialData.color) }
    var iconText by remember { mutableStateOf(initialData.text) }

    val presetColors = listOf(
        Color(0xFFE53935), Color(0xFFD81B60), Color(0xFF8E24AA),
        Color(0xFF5E35B1), Color(0xFF3949AB), Color(0xFF1E88E5),
        Color(0xFF00ACC1), Color(0xFF00897B), Color(0xFF43A047),
        Color(0xFF7CB342), Color(0xFFFDD835), Color(0xFFFB8C00),
        Color(0xFFF4511E), Color(0xFF6D4C41), Color(0xFF546E7A)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Customize Icon & Color", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Live preview
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(selectedColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = iconText.ifEmpty { categoryName.firstOrNull()?.toString()?.uppercase() ?: "?" },
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Black
                    )
                }

                // Text / Symbol input
                OutlinedTextField(
                    value = iconText,
                    onValueChange = { if (it.length <= 2) iconText = it },
                    label = { Text("Icon Text / Emoji (1-2 chars)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Select Color",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )

                // Color Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(presetColors) { color ->
                        val isSelected = selectedColor == color
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = color },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val hexColor = String.format("#%06X", (0xFFFFFF and selectedColor.toArgb()))
                    val jsonIcon = JSONObject().apply {
                        put("type", "color")
                        put("color", hexColor)
                        put("name", iconText.ifBlank { categoryName.firstOrNull()?.toString()?.uppercase() ?: "?" })
                    }.toString()
                    onIconSelected(jsonIcon)
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
