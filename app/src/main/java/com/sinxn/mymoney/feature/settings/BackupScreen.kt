package com.sinxn.mymoney.feature.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sinxn.mymoney.core.data.importBackup.model.EntityStats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onNavigateUp: () -> Unit = {},
    viewModel: BackupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val analysisReport by viewModel.analysisReport.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importBackup(it) }
    }

    val analysisLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.analyzeBackup(it) }
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is BackupUiState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.resetState()
            }
            is BackupUiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }

    if (analysisReport != null) {
        val report = analysisReport!!
        AlertDialog(
            onDismissRequest = { viewModel.clearAnalysis() },
            title = { Text("Backup Analysis") },
            text = {
                Column {
                    Text("Total Items / Deleted / Zombies", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    @Composable
                    fun itemRow(label: String, stats: EntityStats) {
                         Text("$label: ${stats.total} / ${stats.softDeleted} / ${stats.zombies}")
                    }

                    itemRow("Wallets", report.wallets)
                    itemRow("Categories", report.categories)
                    itemRow("Transactions", report.transactions)
                    itemRow("Transfers", report.transfers)
                    itemRow("Transaction Models", report.transactionModels)
                    itemRow("Transfer Models", report.transferModels)
                    itemRow("Recurrent Trans", report.recurrentTransactions)
                    itemRow("Recurrent Transfers", report.recurrentTransfers)
                    itemRow("Debts", report.debts)
                    itemRow("Savings", report.savings)
                    itemRow("Budgets", report.budgets)
                    itemRow("Currencies", report.currencies)
                    itemRow("Places", report.places)
                    itemRow("People", report.people)
                    itemRow("Events", report.events)
                    itemRow("Attachments", report.attachments)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Debug Info:", style = MaterialTheme.typography.labelSmall)
                    Text(report.debugInfo, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.clearAnalysis() }) {
                    Text("Close")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Import & Backup") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Backup & Restore",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            if (uiState is BackupUiState.Loading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = { /* Export Disabled */ },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false // Disabled as requested
                ) {
                    Icon(Icons.Default.Backup, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Export Backup (Coming Soon)")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        // Launch file picker for .mwbx or .zip (OpenDocument handles mime types, we can filter names later)
                        importLauncher.launch(arrayOf("*/*")) 
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Import Backup")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        analysisLauncher.launch(arrayOf("*/*")) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.Analytics, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Analyze Backup")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Note: Importing will clear existing data and replace it with backup contents.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
