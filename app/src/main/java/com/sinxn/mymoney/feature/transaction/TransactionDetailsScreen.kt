package com.sinxn.mymoney.feature.transaction

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sinxn.mymoney.feature.transaction.components.EditTransactionContent
import com.sinxn.mymoney.feature.transaction.components.ViewTransactionContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailsScreen(
    onNavigateBack: () -> Unit,
    viewModel: TransactionDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.formattingSettings.collectAsState()
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val isEditing = uiState.isEditMode || uiState.isNewTransaction

    val handleBack: () -> Unit = {
        if (!uiState.isNewTransaction && uiState.isEditMode) {
            viewModel.toggleEditMode()
        } else {
            onNavigateBack()
        }
    }

    BackHandler(enabled = !uiState.isNewTransaction && uiState.isEditMode) {
        viewModel.toggleEditMode()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0,0,0,0),
        topBar = {
            MinimalTopBar(
                isNewTransaction = uiState.isNewTransaction,
                isEditMode = uiState.isEditMode,
                onBackClick = handleBack,
                onDeleteClick = { showDeleteConfirmation = true }
            )
        },
        floatingActionButton = {
            if (!isEditing) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.toggleEditMode() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(18.dp),
                    icon = { Icon(Icons.Default.Edit, contentDescription = "Edit") },
                    text = { Text("Edit Transaction", fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding )
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Crossfade(targetState = isEditing, label = "TransactionScreenMode") { editing ->
                    if (editing) {
                        EditTransactionContent(
                            uiState = uiState,
                            settings = settings,
                            viewModel = viewModel,
                            onNavigateBack = handleBack
                        )
                    } else {
                        ViewTransactionContent(
                            uiState = uiState,
                            settings = settings,
                            onEditClick = { viewModel.toggleEditMode() }
                        )
                    }
                }
            }
        }

        if (showDeleteConfirmation) {
            DeleteConfirmationDialogue(
                onDismiss = { showDeleteConfirmation = false },
                onDelete = { viewModel.deleteTransaction { onNavigateBack() }},
            )
        }
    }
}

@Composable
fun MinimalTopBar(
    isNewTransaction: Boolean,
    isEditMode: Boolean,
    onBackClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back"
            )
        }

        Text(
            text = when {
                isNewTransaction -> "New Transaction"
                isEditMode -> "Edit Transaction"
                else -> "Transaction Details"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!isNewTransaction && !isEditMode) {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialogue(
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Delete Transaction") },
        text = { Text("Are you sure you want to delete this transaction?") },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    onDelete()
                },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("Cancel")
            }
        }
    )
}

