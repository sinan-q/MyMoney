package com.sinxn.mymoney.feature.debt

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sinxn.mymoney.core.ui.components.NumpadView
import com.sinxn.mymoney.core.ui.components.TabPill
import com.sinxn.mymoney.core.ui.components.rememberNumpadFormState
import com.sinxn.mymoney.core.util.MoneyFormatter
import com.sinxn.mymoney.core.ui.components.EditAmountHeader
import com.sinxn.mymoney.feature.debt.component.DebtAddContent
import com.sinxn.mymoney.feature.debt.component.DebtFormContent
import com.sinxn.mymoney.feature.debt.component.DebtViewContent

private val DebtRoseColor = Color(0xFFE11D48)
private val CreditEmeraldColor = Color(0xFF10B981)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtScreen(
    onNavigateBack: () -> Unit,
    onTransactionClick: (String) -> Unit = {},
    onRecordPayment: (debtId: String, walletId: String, debtAction: String) -> Unit = { _, _, _ -> },
    viewModel: DebtDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val formatterConfig = uiState.formatterConfig
    var showDeleteDialog by remember { mutableStateOf(false) }

    val isDebt = if (uiState.isEditMode || uiState.isNewDebt) uiState.editType == 0
    else (uiState.debtDetails?.debt?.type ?: 0) == 0

    val accentColor = if (isDebt) DebtRoseColor else CreditEmeraldColor

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isNewDebt) {
                            if (uiState.editType == 0) "New Debt" else "New Credit"
                        } else if (uiState.isEditMode) {
                            if (uiState.editType == 0) "Edit Debt" else "Edit Credit"
                        } else {
                            if (isDebt) "Debt Details" else "Credit Details"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.isEditMode && !uiState.isNewDebt) {
                            viewModel.setEditMode(false)
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!uiState.isNewDebt) {
                        if (!uiState.isEditMode) {
                            IconButton(onClick = { viewModel.setEditMode(true) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = viewModel::toggleArchived) {
                                Icon(
                                    imageVector = if (uiState.debtDetails?.debt?.isArchived == true) Icons.Default.Unarchive else Icons.Default.Archive,
                                    contentDescription = "Archive",
                                    tint = if (uiState.debtDetails?.debt?.isArchived == true) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.isEditMode || uiState.isNewDebt) {
                DebtAddContent(
                    uiState = uiState,
                    viewModel = viewModel,
                    accentColor = accentColor,
                    onNavigateBack = onNavigateBack,
                )
            } else {
                val debtDetails = uiState.debtDetails
                if (debtDetails != null) {
                    DebtViewContent(
                        debtDetails = debtDetails,
                        transactions = uiState.transactions,
                        formatterConfig = formatterConfig,
                        currencyCode = uiState.currencyCode,
                        currencyDecimals = uiState.currencyDecimals,
                        dateFormat = uiState.dateFormat,
                        accentColor = accentColor,
                        onTransactionClick = onTransactionClick,
                        onRecordPaymentClick = {
                            onRecordPayment(
                                debtDetails.debt.id,
                                debtDetails.debt.walletId,
                                if (debtDetails.debt.type == 0) "PAY" else "COLLECT"
                            )
                        }
                    )
                }
            }
        }
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Debt") },
                text = { Text("Do you want to delete all associated transactions or keep them in history?") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        viewModel.deleteDebt(deleteTransactions = true, onSuccess = onNavigateBack)
                    }) {
                        Text("Delete All", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = {
                            showDeleteDialog = false
                            viewModel.deleteDebt(deleteTransactions = false, onSuccess = onNavigateBack)
                        }) {
                            Text("Keep Transactions")
                        }
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }
    }
}

