package com.sinxn.mymoney.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SqlConsoleScreen(
    onNavigateUp: () -> Unit,
    viewModel: SqlConsoleViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    var showHistorySheet by remember { mutableStateOf(false) }
    var showSchemaSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Storage,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("SQL Console")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.queryHistory.isNotEmpty()) {
                        IconButton(onClick = { showHistorySheet = true }) {
                            Icon(Icons.Default.History, contentDescription = "Query History")
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Query Input Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Read-only badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Read-only mode • SELECT queries only",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::updateQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 160.dp),
                    placeholder = {
                        Text(
                            "SELECT * FROM wallets LIMIT 10",
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        keyboardController?.hide()
                        viewModel.executeQuery()
                    }),
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = {
                                viewModel.updateQuery("")
                                viewModel.clearResults()
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    }
                )

                Spacer(Modifier.height(8.dp))

                // Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Table chips
                    AssistChip(
                        onClick = { showSchemaSheet = true },
                        label = { Text("Tables", style = MaterialTheme.typography.labelMedium) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.TableChart,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )

                    // Run button
                    FilledTonalButton(
                        onClick = {
                            keyboardController?.hide()
                            viewModel.executeQuery()
                        },
                        enabled = state.query.isNotBlank() && !state.isLoading
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("Run")
                    }
                }
            }

            HorizontalDivider()

            // Results Section
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    state.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    state.error != null -> {
                        ErrorCard(error = state.error!!)
                    }

                    state.result != null -> {
                        ResultsTable(result = state.result!!)
                    }

                    else -> {
                        // Empty state
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Storage,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outlineVariant
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Write a SQL query above\nand tap Run to see results",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    // History Bottom Sheet
    if (showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    "Query History",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                state.queryHistory.forEach { query ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                viewModel.selectHistoryItem(query)
                                showHistorySheet = false
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Text(
                            text = query,
                            modifier = Modifier.padding(12.dp),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Schema Bottom Sheet
    if (showSchemaSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showSchemaSheet = false
                viewModel.clearSchemaSelection()
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                if (state.selectedTableSchema != null && state.selectedTableName != null) {
                    // Show schema details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Schema: ${state.selectedTableName}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        IconButton(onClick = { viewModel.clearSchemaSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Back to tables")
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    FilledTonalButton(
                        onClick = {
                            viewModel.insertTableQuery(state.selectedTableName!!)
                            showSchemaSheet = false
                            viewModel.clearSchemaSelection()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("SELECT * FROM ${state.selectedTableName} LIMIT 50")
                    }

                    Spacer(Modifier.height(12.dp))

                    state.selectedTableSchema!!.forEach { col ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerLow,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = col,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                } else {
                    // Show table list
                    Text(
                        "Database Tables",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        state.tables.forEach { table ->
                            AssistChip(
                                onClick = { viewModel.loadTableSchema(table) },
                                label = { Text(table, style = MaterialTheme.typography.labelMedium) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.TableChart,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ErrorCard(error: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Error",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun ResultsTable(result: com.sinxn.mymoney.core.data.repository.SqlResult) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Stats bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${result.rowCount} row${if (result.rowCount != 1) "s" else ""}" +
                        if (result.rowCount > result.rows.size) " (showing ${result.rows.size})" else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${result.executionTimeMs}ms",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (result.columns.isEmpty()) {
            Text(
                text = "Query returned no columns.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
            return
        }

        // Scrollable table
        val horizontalScrollState = rememberScrollState()

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header row
            item {
                Row(
                    modifier = Modifier
                        .horizontalScroll(horizontalScrollState)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(vertical = 2.dp)
                ) {
                    result.columns.forEachIndexed { index, column ->
                        Text(
                            text = column,
                            modifier = Modifier
                                .width(150.dp)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (index < result.columns.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(36.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            )
                        }
                    }
                }
                HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            }

            // Data rows
            itemsIndexed(result.rows) { rowIndex, row ->
                val bgColor = if (rowIndex % 2 == 0) {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                }

                Row(
                    modifier = Modifier
                        .horizontalScroll(horizontalScrollState)
                        .background(bgColor)
                ) {
                    row.forEachIndexed { colIndex, cell ->
                        Text(
                            text = cell,
                            modifier = Modifier
                                .width(150.dp)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = if (cell == "NULL") {
                                MaterialTheme.colorScheme.outline
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        if (colIndex < row.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(40.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                            )
                        }
                    }
                }
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                )
            }
        }
    }
}
