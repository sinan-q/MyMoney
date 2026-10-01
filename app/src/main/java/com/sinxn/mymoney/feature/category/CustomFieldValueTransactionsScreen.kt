package com.sinxn.mymoney.feature.category

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sinxn.mymoney.core.ui.components.groupTransactionsIntoMonthGroups
import com.sinxn.mymoney.core.ui.components.monthGroupedTransactionItems

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomFieldValueTransactionsScreen(
    onNavigateBack: () -> Unit,
    onTransactionClick: (String) -> Unit,
    viewModel: CustomFieldValueTransactionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var collapsedGroups by remember { mutableStateOf(setOf<String>()) }

    val groupedItems = remember(
        uiState.transactions,
        uiState.decimals,
        uiState.currencyCode,
        uiState.formatterConfig,
        uiState.dateFormat
    ) {
        groupTransactionsIntoMonthGroups(
            transactions = uiState.transactions,
            decimals = uiState.decimals,
            currencyCode = uiState.currencyCode,
            formatterConfig = uiState.formatterConfig,
            dateFormat = uiState.dateFormat
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = uiState.fieldDefinition?.let { "${it.label}: ${uiState.normalizedValue}" } ?: uiState.normalizedValue
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0,0,0,0)
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
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    monthGroupedTransactionItems(
                        monthGroups = groupedItems,
                        collapsedGroups = collapsedGroups,
                        onToggleGroup = { headerKey ->
                            collapsedGroups = if (headerKey in collapsedGroups) {
                                collapsedGroups - headerKey
                            } else {
                                collapsedGroups + headerKey
                            }
                        },
                        onTransactionClick = onTransactionClick,
                        decimals = uiState.decimals,
                        currencyCode = uiState.currencyCode,
                        formatterConfig = uiState.formatterConfig,
                        dateFormat = uiState.dateFormat,
                        emptyMessage = "No transactions found for this value."
                    )
                }
            }
        }
    }
}
