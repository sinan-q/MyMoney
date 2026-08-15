package com.sinxn.mymoney.feature.debt

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.sinxn.mymoney.feature.debt.component.DebtViewContent

private val DebtRoseColor = Color(0xFFE11D48)
private val CreditEmeraldColor = Color(0xFF10B981)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtDetailsScreen(
    onNavigateBack: () -> Unit,
    onEditDebtClick: (String) -> Unit,
    onTransactionClick: (String) -> Unit = {},
    onRecordPayment: (debtId: String, walletId: String, debtAction: String) -> Unit = { _, _, _ -> },
    viewModel: DebtDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val formatterConfig = uiState.formatterConfig
    var showDeleteDialog by remember { mutableStateOf(false) }

    val isDebt = (uiState.debtDetails?.debt?.type ?: 0) == 0
    val accentColor = if (isDebt) DebtRoseColor else CreditEmeraldColor

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isDebt) "Debt Details" else "Credit Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val debt = uiState.debtDetails?.debt
                    if (debt != null) {
                        IconButton(onClick = { onEditDebtClick(debt.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = viewModel::toggleArchived) {
                            Icon(
                                imageVector = if (debt.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                contentDescription = "Archive",
                                tint = if (debt.isArchived) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
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
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Debt not found", style = MaterialTheme.typography.bodyLarge)
                    }
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
