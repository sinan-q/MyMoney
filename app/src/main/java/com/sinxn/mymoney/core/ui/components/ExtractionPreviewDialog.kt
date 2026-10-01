package com.sinxn.mymoney.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import com.sinxn.mymoney.core.data.repository.ExtractionPreviewMatch
import com.sinxn.mymoney.core.data.repository.ExtractionPreviewResult

@Composable
fun ExtractionPreviewDialog(
    preview: ExtractionPreviewResult,
    isApplying: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isApplying) onDismiss() },
        title = {
            Text("Extraction Preview", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Summary stats
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Summary",
                            style = MaterialTheme.typography.titleSmall,
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
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                // Sample matches
                if (preview.sampleMatches.isNotEmpty()) {
                    Text(
                        "Sample Matches (${preview.sampleMatches.size} of ${preview.matchedCount})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 250.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(preview.sampleMatches) { match ->
                            SampleMatchCard(match)
                        }
                    }
                }
            }
        },
        confirmButton = {
            val totalChanges = preview.newValueCount + preview.updatedValueCount
            TextButton(
                onClick = onConfirm,
                enabled = totalChanges > 0 && !isApplying
            ) {
                if (isApplying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Applying…")
                } else {
                    Text("Apply ${totalChanges} change${if (totalChanges != 1) "s" else ""}")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isApplying
            ) {
                Text("Cancel")
            }
        }
    )
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
private fun SampleMatchCard(match: ExtractionPreviewMatch) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Transaction text
            if (!match.description.isNullOrBlank()) {
                Text(
                    text = match.description,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
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
