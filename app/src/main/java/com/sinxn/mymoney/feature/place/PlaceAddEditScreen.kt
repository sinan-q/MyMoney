package com.sinxn.mymoney.feature.place

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.parseIconData
import kotlinx.coroutines.flow.collectLatest
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceAddEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: PlaceAddEditViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                PlaceAddEditEvent.Saved -> onNavigateBack()
                PlaceAddEditEvent.Deleted -> onNavigateBack()
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Place") },
            text = { Text("Are you sure you want to delete '${uiState.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        viewModel.deletePlace()
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
        PlaceIconColorPickerDialog(
            currentIcon = uiState.icon,
            placeName = uiState.name,
            onDismiss = { showIconPicker = false },
            onIconSelected = { newIcon ->
                viewModel.onIconChange(newIcon)
                showIconPicker = false
            }
        )
    }

    fun previewInMaps() {
        val lat = uiState.latitude.trim().toDoubleOrNull()
        val lng = uiState.longitude.trim().toDoubleOrNull()
        val address = uiState.address.trim()

        try {
            val uri = if (lat != null && lng != null) {
                Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(uiState.name.ifBlank { "Place" })})")
            } else if (address.isNotBlank()) {
                Uri.parse("geo:0,0?q=${Uri.encode(address)}")
            } else null

            if (uri != null) {
                val intent = Intent(Intent.ACTION_VIEW, uri)
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "Please enter coordinates or address first", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ActivityNotFoundException) {
            try {
                val webUri = if (lat != null && lng != null) {
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
                } else if (address.isNotBlank()) {
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(address)}")
                } else null

                if (webUri != null) {
                    val webIntent = Intent(Intent.ACTION_VIEW, webUri)
                    context.startActivity(webIntent)
                }
            } catch (ex: Exception) {
                Toast.makeText(context, "No application found to open map", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditMode) "Edit Place" else "New Place",
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
                                contentDescription = "Delete Place",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(
                        onClick = viewModel::savePlace,
                        enabled = uiState.name.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save Place",
                            tint = if (uiState.name.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::savePlace,
                containerColor = if (uiState.name.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (uiState.name.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(18.dp),
                icon = { Icon(Icons.Default.Check, contentDescription = "Save") },
                text = { Text(if (uiState.isEditMode) "Save Changes" else "Create Place", fontWeight = FontWeight.Bold) }
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
                                        iconString = uiState.icon.ifBlank { "ic_place" },
                                        categoryName = uiState.name.ifBlank { "Place" },
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
                                    label = { Text("Place Name") },
                                    placeholder = { Text("e.g. Supermarket, Office, Coffee Shop...") },
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

                    // Location & Address Card
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Location & Address",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                OutlinedTextField(
                                    value = uiState.address,
                                    onValueChange = viewModel::onAddressChange,
                                    label = { Text("Address (Optional)") },
                                    placeholder = { Text("e.g. 123 Main St, Springfield") },
                                    minLines = 1,
                                    maxLines = 3,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    )
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = uiState.latitude,
                                        onValueChange = viewModel::onLatitudeChange,
                                        label = { Text("Latitude") },
                                        placeholder = { Text("e.g. 37.7749") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                        )
                                    )

                                    OutlinedTextField(
                                        value = uiState.longitude,
                                        onValueChange = viewModel::onLongitudeChange,
                                        label = { Text("Longitude") },
                                        placeholder = { Text("e.g. -122.4194") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                        )
                                    )
                                }

                                // Quick actions for location
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (uiState.latitude.isNotBlank() || uiState.longitude.isNotBlank()) {
                                        TextButton(
                                            onClick = viewModel::clearCoordinates,
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Clear Coordinates")
                                        }
                                    }

                                    Spacer(modifier = Modifier.weight(1f))

                                    if (uiState.latitude.isNotBlank() || uiState.longitude.isNotBlank() || uiState.address.isNotBlank()) {
                                        FilledTonalButton(
                                            onClick = { previewInMaps() },
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Preview in Map")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Additional Details Card: Tag and Archived switch
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
                                    value = uiState.tag,
                                    onValueChange = viewModel::onTagChange,
                                    label = { Text("Tag / Group (Optional)") },
                                    placeholder = { Text("e.g. Shopping, Work, Travel") },
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
                                                text = "Archive Place",
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
private fun PlaceIconColorPickerDialog(
    currentIcon: String,
    placeName: String,
    onDismiss: () -> Unit,
    onIconSelected: (String) -> Unit
) {
    val initialData = remember(currentIcon, placeName) {
        parseIconData(currentIcon, placeName.ifBlank { "Place" })
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
                        text = iconText.ifEmpty { placeName.firstOrNull()?.toString()?.uppercase() ?: "📍" },
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
