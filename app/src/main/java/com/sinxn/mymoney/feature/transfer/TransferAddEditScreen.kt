package com.sinxn.mymoney.feature.transfer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sinxn.mymoney.core.ui.components.NumpadView
import com.sinxn.mymoney.core.ui.components.rememberNumpadFormState
import com.sinxn.mymoney.feature.transfer.components.EditTransferContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferAddEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: TransferAddEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.formattingSettings.collectAsState()
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val numpadState = rememberNumpadFormState(initialNumpadVisible = uiState.isNewTransfer)
    val transferAccentColor = Color(0xFF0284C7)

    val hasOperatorInAmount = remember(uiState.editAmount) {
        numpadState.hasOperator(uiState.editAmount)
    }
    val evaluatedAmountStr = remember(uiState.editAmount) {
        numpadState.getImmediateResult(uiState.editAmount, uiState.currencyDecimals)
    }
    val amountValue = remember(evaluatedAmountStr) {
        evaluatedAmountStr.toDoubleOrNull()
    }
    val isWalletValid = uiState.editWalletId.isNotBlank() && 
            !uiState.targetWalletId.isNullOrBlank() && 
            uiState.editWalletId != uiState.targetWalletId
    val isAmountPositive = amountValue != null && amountValue > 0.0
    val isSaveEnabled = !uiState.isSaving && isWalletValid && isAmountPositive

    val titleText = if (uiState.isNewTransfer) "New Transfer" else "Edit Transfer"
    val fabText = if (uiState.isNewTransfer) "Add Transfer" else "Save Changes"
    val fabIcon = if (uiState.isNewTransfer) Icons.Default.Add else Icons.Default.Check

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            AnimatedVisibility(
                visible = isSaveEnabled && !numpadState.isNumpadVisible,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        numpadState.dismiss()
                        viewModel.saveTransfer { onNavigateBack() }
                    },
                    icon = { Icon(fabIcon, contentDescription = fabText) },
                    text = { Text(fabText, fontWeight = FontWeight.Bold) },
                    containerColor = transferAccentColor,
                    contentColor = Color.White
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (!uiState.isNewTransfer) {
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                EditTransferContent(
                    uiState = uiState,
                    settings = settings,
                    viewModel = viewModel,
                    onNavigateBack = onNavigateBack,
                    evaluatedAmountStr = evaluatedAmountStr,
                    hasOperatorInAmount = hasOperatorInAmount,
                    isNumpadVisible = numpadState.isNumpadVisible,
                    showNumpad = { numpadState.showNumpad() },
                    focusRequester = numpadState.focusRequester,
                    onFocusField = { numpadState.onFocusField() },
                    numpadDismiss = { numpadState.dismiss() }
                )
            }
        }
    }

    // Numpad ModalBottomSheet Overlay over NavigationBar
    if (numpadState.isNumpadVisible) {
        NumpadView(
            onKeyPress = viewModel::onNumpadKeyPress,
            onEvaluate = viewModel::evaluateMathExpression,
            onNext = { numpadState.onNext() },
            hasOperatorInAmount = hasOperatorInAmount,
            saveButtonText = fabText,
            saveButtonColor = transferAccentColor,
            onDismiss = { numpadState.dismiss() }
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Transfer") },
            text = { Text("Are you sure you want to delete this transfer?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        viewModel.deleteTransfer { onNavigateBack() }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}


