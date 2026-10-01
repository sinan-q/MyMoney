package com.sinxn.mymoney.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.sinxn.mymoney.core.data.repository.ExtractionPreviewMatch
import com.sinxn.mymoney.core.data.repository.ExtractionPreviewResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtractionPreviewDialog(
    preview: ExtractionPreviewResult,
    isApplying: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onTransactionClick: (String) -> Unit = {}
) {
    var showUnmatched by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { if (!isApplying) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // App Bar
                TopAppBar(
                    title = { Text("Extraction Preview") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss, enabled = !isApplying) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Summary stats
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            StatRow("Total transactions scanned", preview.totalTransactions.toString())
                            StatRow("Matched by rules", preview.matchedCount.toString(), MaterialTheme.colorScheme.primary)
                            StatRow("Unmatched", preview.unmatchedCount.toString())
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            StatRow("New values to create", preview.newValueCount.toString(), MaterialTheme.colorScheme.primary)
                            StatRow("Values to update", preview.updatedValueCount.toString(), MaterialTheme.colorScheme.tertiary)
                            if (preview.skippedManualCount > 0) {
                                StatRow(
                                    "Skipped (manual)",
                                    preview.skippedManualCount.toString(),
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    val totalChanges = preview.newValueCount + preview.updatedValueCount
                    if (totalChanges == 0 && preview.matchedCount == 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                "No transactions matched the extraction rules.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    // Toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            if (showUnmatched) "Unmatched Transactions (${preview.unmatchedTransactions.size})" 
                            else "Matched Transactions (${preview.allMatches.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Show Unmatched", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.width(8.dp))
                            Switch(
                                checked = showUnmatched,
                                onCheckedChange = { showUnmatched = it }
                            )
                        }
                    }

                    // Matches list
                    val listToShow = if (showUnmatched) preview.unmatchedTransactions else preview.allMatches
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(listToShow) { match ->
                            SampleMatchCard(
                                match = match,
                                onClick = { onTransactionClick(match.transactionId) }
                            )
                        }
                    }

                    // Bottom Action
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = totalChanges > 0 && !isApplying
                    ) {
                        if (isApplying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Applying…")
                        } else {
                            Text("Apply ${totalChanges} change${if (totalChanges != 1) "s" else ""}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
private fun SampleMatchCard(
    match: ExtractionPreviewMatch,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Date and badges / chevron
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = match.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (match.action == "unmatched") {
                        Text(
                            text = "Unmatched",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "View Transaction",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            
            val primaryText = when {
                !match.description.isNullOrBlank() -> match.description
                !match.note.isNullOrBlank() -> match.note
                else -> "(No description)"
            }
            Text(
                text = primaryText,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (!match.description.isNullOrBlank() && !match.note.isNullOrBlank()) {
                Text(
                    text = match.note,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Extracted values
            match.extractedValues.forEach { (key, value) ->
                val existing = match.existingValues[key]
                val icon = when {
                    existing == null -> Icons.Default.Check // New
                    existing.first == "manual" -> Icons.Default.Close // Skipped
                    else -> Icons.Default.Edit // Update
                }
                val color = when {
                    existing == null -> MaterialTheme.colorScheme.primary
                    existing.second == "manual" -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.tertiary
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = color
                    )
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = color)) {
                                append(key)
                            }
                            append(" → ")
                            append(value)
                            if (existing != null && existing.second == "manual") {
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))) {
                                    append(" (kept manual: ${existing.first})")
                                }
                            }
                        },
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
