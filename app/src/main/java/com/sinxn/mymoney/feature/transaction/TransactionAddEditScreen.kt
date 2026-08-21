package com.sinxn.mymoney.feature.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sinxn.mymoney.core.ui.components.TabPill
import com.sinxn.mymoney.core.ui.components.rememberNumpadFormState
import com.sinxn.mymoney.core.util.Direction
import com.sinxn.mymoney.feature.transaction.components.EditTransactionContent
import com.sinxn.mymoney.feature.transfer.TransferAddEditViewModel
import com.sinxn.mymoney.feature.transfer.components.EditTransferContent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionAddEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: TransactionAddEditViewModel = hiltViewModel(),
    transferViewModel: TransferAddEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.formattingSettings.collectAsState()

    val transferUiState by transferViewModel.uiState.collectAsState()
    val transferSettings by transferViewModel.formattingSettings.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })

    val txAccentColor = remember(uiState.editDirection) {
        if (uiState.editDirection == Direction.INCOME) Color(0xFF10B981) else Color(0xFFE11D48)
    }
    val transferAccentColor = Color(0xFF0284C7)

    val titleText = if (uiState.isNewTransaction) {
        if (pagerState.currentPage == 1) "New Transfer" else "New Transaction"
    } else {
        "Edit Transaction"
    }

    val fabText = if (uiState.isNewTransaction) {
        if (pagerState.currentPage == 1) "Add Transfer" else "Add Transaction"
    } else {
        "Save Transaction"
    }
    val numpadState = rememberNumpadFormState(initialNumpadVisible = uiState.isNewTransaction)
    val hasOperatorInAmount = remember(uiState.editAmount) {
        numpadState.hasOperator(uiState.editAmount)
    }
    val evaluatedAmountStr = remember(uiState.editAmount) {
        numpadState.getImmediateResult(uiState.editAmount, uiState.currencyDecimals)
    }
    val amountValue = remember(evaluatedAmountStr) {
        evaluatedAmountStr.toDoubleOrNull()
    }
    val isCategorySelected = !uiState.editCategoryId.isNullOrBlank()
    val isWalletSelected = uiState.editWalletId.isNotBlank()
    val isAmountNonNegative = amountValue != null && amountValue >= 0.0

    val isSaveEnabled = !uiState.isSaving && isCategorySelected && isWalletSelected && isAmountNonNegative



    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (isSaveEnabled && !numpadState.isNumpadVisible)
            ExtendedFloatingActionButton(
                onClick = {
                    numpadState.dismiss()
                    viewModel.saveChanges { onNavigateBack() } },
                icon = { Icon( if (uiState.isNewTransaction) Icons.Default.Add else Icons.Default.Check, null) },
                text = { Text(fabText) }
            )
        },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }

                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.width(48.dp))
            }
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
            } else if (!uiState.isNewTransaction) {
                // Editing existing transaction
                EditTransactionContent(
                    uiState = uiState,
                    settings = settings,
                    viewModel = viewModel,
                    onNavigateBack = onNavigateBack,
                    isNumpadVisible = numpadState.isNumpadVisible,
                    showNumpad = { numpadState.showNumpad() },
                    evaluatedAmountStr = evaluatedAmountStr,
                    hasOperatorInAmount = hasOperatorInAmount,
                    onFocusField = { numpadState.onFocusField() },
                    focusRequester = numpadState.focusRequester,
                    numpadDismiss = { numpadState.dismiss() },
                    numpadOnNext = { numpadState.onNext() }
                )
            } else {
                // Creating new item: Swipeable HorizontalPager between Transaction and Transfer
                Column(modifier = Modifier.fillMaxSize()) {
                    TabPill(
                        tabs = listOf(
                            "Transaction" to txAccentColor,
                            "Transfer" to transferAccentColor
                        ),
                        activeTab = pagerState.currentPage,
                        onTabChange = { index ->
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f)
                    ) { page ->
                        if (page == 0) {
                            EditTransactionContent(
                                uiState = uiState,
                                settings = settings,
                                viewModel = viewModel,
                                onNavigateBack = onNavigateBack,
                                isNumpadVisible = numpadState.isNumpadVisible,
                                showNumpad = { numpadState.showNumpad() },
                                evaluatedAmountStr = evaluatedAmountStr,
                                hasOperatorInAmount = hasOperatorInAmount,
                                onFocusField = { numpadState.onFocusField() },
                                focusRequester = numpadState.focusRequester,
                                numpadDismiss = { numpadState.dismiss() },
                                numpadOnNext = { numpadState.onNext() }
                            )
                        } else {
                            EditTransferContent(
                                uiState = transferUiState,
                                settings = transferSettings,
                                viewModel = transferViewModel,
                                onNavigateBack = onNavigateBack
                            )
                        }
                    }
                }
            }
        }
    }
}
