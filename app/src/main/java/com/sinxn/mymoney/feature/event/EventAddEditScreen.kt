package com.sinxn.mymoney.feature.event

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sinxn.mymoney.core.ui.components.AppExtendedFab
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.CleanListRow
import com.sinxn.mymoney.core.ui.components.FormCardContainer
import com.sinxn.mymoney.core.ui.components.FormDatePickerDialog
import com.sinxn.mymoney.core.ui.components.parseIconData
import com.sinxn.mymoney.core.util.DateUtils
import kotlinx.coroutines.flow.collectLatest
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventAddEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: EventAddEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                EventAddEditEvent.Saved -> onNavigateBack()
                EventAddEditEvent.Deleted -> onNavigateBack()
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Event") },
            text = { Text("Are you sure you want to delete '${uiState.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        viewModel.deleteEvent()
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
        EventIconColorPickerDialog(
            currentIcon = uiState.icon,
            eventName = uiState.name,
            onDismiss = { showIconPicker = false },
            onIconSelected = { newIcon ->
                viewModel.onIconChange(newIcon)
                showIconPicker = false
            }
        )
    }

    if (showStartDatePicker) {
        FormDatePickerDialog(
            initialDateString = uiState.startDate,
            onDateStringSelected = { dateString ->
                viewModel.onStartDateChange(dateString)
                showStartDatePicker = false
            },
            onDismissRequest = { showStartDatePicker = false }
        )
    }

    if (showEndDatePicker) {
        FormDatePickerDialog(
            initialDateString = uiState.endDate,
            onDateStringSelected = { dateString ->
                viewModel.onEndDateChange(dateString)
                showEndDatePicker = false
            },
            onDismissRequest = { showEndDatePicker = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditMode) "Edit Event" else "New Event",
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
                    if (uiState.isEditMode) {
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Event",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            val isSaveEnabled = uiState.name.isNotBlank()
            val text = if (uiState.isEditMode) "Save Changes" else "Create Event"
            val icon = if (uiState.isEditMode) Icons.Default.Check else Icons.Default.Add
            AppExtendedFab(
                text = text,
                icon = icon,
                onClick = {
                    if (isSaveEnabled) {
                        viewModel.saveEvent()
                    }
                },
                containerColor = if (isSaveEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isSaveEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Card: Icon and Name
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Clickable Avatar with edit badge
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clickable { showIconPicker = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    CategoryIcon(
                                        iconString = uiState.icon.ifBlank { "ic_event" },
                                        categoryName = uiState.name.ifBlank { "Event" },
                                        modifier = Modifier.size(76.dp)
                                    )

                                    // Small Edit Badge at bottom end
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .align(Alignment.BottomEnd)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Icon",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "Tap icon to change color or symbol",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )

                                OutlinedTextField(
                                    value = uiState.name,
                                    onValueChange = viewModel::onNameChange,
                                    label = { Text("Event Name") },
                                    placeholder = { Text("e.g. Summer Vacation, Wedding...") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    )
                                )
                            }
                        }
                    }

                    // Dates Card
                    item {
                        val formattedStartDate = remember(uiState.startDate, uiState.dateFormat) {
                            val d = DateUtils.parseDate(uiState.startDate)
                            DateUtils.formatDate(d, uiState.dateFormat)
                        }
                        val formattedEndDate = remember(uiState.endDate, uiState.dateFormat) {
                            val d = DateUtils.parseDate(uiState.endDate)
                            DateUtils.formatDate(d, uiState.dateFormat)
                        }

                        FormCardContainer(horizontalPadding = 0.dp) {
                            Column {
                                CleanListRow(
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Event,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    label = "Start Date",
                                    value = formattedStartDate,
                                    onClick = { showStartDatePicker = true }
                                )

                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )

                                CleanListRow(
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Event,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    label = "End Date",
                                    value = formattedEndDate,
                                    onClick = { showEndDatePicker = true }
                                )
                            }
                        }
                    }

                    // Additional Details Card: Note, Tag, and Archived status
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Additional Details",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedTextField(
                                    value = uiState.note,
                                    onValueChange = viewModel::onNoteChange,
                                    label = { Text("Note (Optional)") },
                                    minLines = 2,
                                    maxLines = 4,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    )
                                )

                                OutlinedTextField(
                                    value = uiState.tag,
                                    onValueChange = viewModel::onTagChange,
                                    label = { Text("Tag / Group (Optional)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    )
                                )

                                if (uiState.isEditMode) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Archive,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Archive Event",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Switch(
                                            checked = uiState.isArchived,
                                            onCheckedChange = viewModel::onArchivedChange
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bottom padding spacer for FAB
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EventIconColorPickerDialog(
    currentIcon: String,
    eventName: String,
    onDismiss: () -> Unit,
    onIconSelected: (String) -> Unit
) {
    val initialData = remember(currentIcon, eventName) {
        parseIconData(currentIcon, eventName.ifBlank { "Event" })
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
                        text = iconText.ifEmpty { eventName.firstOrNull()?.toString()?.uppercase() ?: "📅" },
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
                        .height(130.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(presetColors) { color ->
                        val isSelected = selectedColor == color
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = color }
                                .then(
                                    if (isSelected) {
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
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
                    val colorHex = String.format("#%06X", (0xFFFFFF and selectedColor.toArgb()))
                    val json = JSONObject().apply {
                        put("type", "custom")
                        put("text", iconText.trim())
                        put("color", colorHex)
                    }
                    onIconSelected(json.toString())
                }
            ) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
