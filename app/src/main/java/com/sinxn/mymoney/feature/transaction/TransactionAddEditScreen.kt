package com.sinxn.mymoney.feature.transaction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.sinxn.mymoney.core.ui.components.NumpadView
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

    // 1. Transaction Form State & Validation
    val txNumpadState = rememberNumpadFormState(initialNumpadVisible = uiState.isNewTransaction)
    val txHasOperator = remember(uiState.editAmount) {
        txNumpadState.hasOperator(uiState.editAmount)
    }
    val txEvaluatedAmountStr = remember(uiState.editAmount) {
        txNumpadState.getImmediateResult(uiState.editAmount, uiState.currencyDecimals)
    }
    val txAmountValue = remember(txEvaluatedAmountStr) {
        txEvaluatedAmountStr.toDoubleOrNull()
    }
    val isTxCategorySelected = !uiState.editCategoryId.isNullOrBlank()
    val isTxWalletSelected = uiState.editWalletId.isNotBlank()
    val isTxAmountPositive = txAmountValue != null && txAmountValue > 0.0
    val isTxSaveEnabled = !uiState.isSaving && isTxCategorySelected && isTxWalletSelected && isTxAmountPositive

    val txFabText = if (uiState.isNewTransaction) {
        if (uiState.editDirection == Direction.INCOME) "Add Income" else "Add Expense"
    } else {
        "Save Changes"
    }
    val txFabIcon = if (uiState.isNewTransaction) Icons.Default.Add else Icons.Default.Check

    // 2. Transfer Form State & Validation
    val transferNumpadState = rememberNumpadFormState(initialNumpadVisible = true)
    val transferHasOperator = remember(transferUiState.editAmount) {
        transferNumpadState.hasOperator(transferUiState.editAmount)
    }
    val transferEvaluatedAmountStr = remember(transferUiState.editAmount) {
        transferNumpadState.getImmediateResult(transferUiState.editAmount, transferUiState.currencyDecimals)
    }
    val transferAmountValue = remember(transferEvaluatedAmountStr) {
        transferEvaluatedAmountStr.toDoubleOrNull()
    }
    val isTransferWalletValid = transferUiState.editWalletId.isNotBlank() &&
            !transferUiState.targetWalletId.isNullOrBlank() &&
            transferUiState.editWalletId != transferUiState.targetWalletId
    val isTransferAmountPositive = transferAmountValue != null && transferAmountValue > 0.0
    val isTransferSaveEnabled = !transferUiState.isSaving && isTransferWalletValid && isTransferAmountPositive

    val transferFabText = if (transferUiState.isNewTransfer) "Add Transfer" else "Save Changes"
    val transferFabIcon = if (transferUiState.isNewTransfer) Icons.Default.Add else Icons.Default.Check

    // Active page state
    val isCurrentPageTx = pagerState.currentPage == 0 || !uiState.isNewTransaction
    val activeNumpadState = if (isCurrentPageTx) txNumpadState else transferNumpadState
    val isSaveEnabled = if (isCurrentPageTx) isTxSaveEnabled else isTransferSaveEnabled
    val isNumpadVisible = activeNumpadState.isNumpadVisible
    val activeFabText = if (isCurrentPageTx) txFabText else transferFabText
    val activeFabIcon = if (isCurrentPageTx) txFabIcon else transferFabIcon
    val activeFabColor = if (isCurrentPageTx) txAccentColor else transferAccentColor

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            AnimatedVisibility(
                visible = isSaveEnabled && !isNumpadVisible,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        activeNumpadState.dismiss()
                        if (isCurrentPageTx) {
                            viewModel.saveChanges { onNavigateBack() }
                        } else {
                            transferViewModel.saveTransfer { onNavigateBack() }
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = activeFabIcon,
                            contentDescription = activeFabText
                        )
                    },
                    text = {
                        Text(
                            text = activeFabText,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    containerColor = activeFabColor,
                    contentColor = Color.White
                )
            }
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
                    isNumpadVisible = txNumpadState.isNumpadVisible,
                    showNumpad = { txNumpadState.showNumpad() },
                    evaluatedAmountStr = txEvaluatedAmountStr,
                    hasOperatorInAmount = txHasOperator,
                    onFocusField = { txNumpadState.onFocusField() },
                    focusRequester = txNumpadState.focusRequester,
                    numpadDismiss = { txNumpadState.dismiss() }
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
                                isNumpadVisible = txNumpadState.isNumpadVisible,
                                showNumpad = { txNumpadState.showNumpad() },
                                evaluatedAmountStr = txEvaluatedAmountStr,
                                hasOperatorInAmount = txHasOperator,
                                onFocusField = { txNumpadState.onFocusField() },
                                focusRequester = txNumpadState.focusRequester,
                                numpadDismiss = { txNumpadState.dismiss() }
                            )
                        } else {
                            EditTransferContent(
                                uiState = transferUiState,
                                settings = transferSettings,
                                viewModel = transferViewModel,
                                onNavigateBack = onNavigateBack,
                                evaluatedAmountStr = transferEvaluatedAmountStr,
                                hasOperatorInAmount = transferHasOperator,
                                isNumpadVisible = transferNumpadState.isNumpadVisible,
                                showNumpad = { transferNumpadState.showNumpad() },
                                focusRequester = transferNumpadState.focusRequester,
                                onFocusField = { transferNumpadState.onFocusField() },
                                numpadDismiss = { transferNumpadState.dismiss() }
                            )
                        }
                    }
                }
            }
        }
    }

    // Numpad ModalBottomSheet Overlay over NavigationBar
    if (isNumpadVisible) {
        NumpadView(
            onKeyPress = if (isCurrentPageTx) viewModel::onNumpadKeyPress else transferViewModel::onNumpadKeyPress,
            onEvaluate = if (isCurrentPageTx) viewModel::evaluateMathExpression else transferViewModel::evaluateMathExpression,
            onNext = { activeNumpadState.onNext() },
            hasOperatorInAmount = if (isCurrentPageTx) txHasOperator else transferHasOperator,
            saveButtonText = activeFabText,
            saveButtonColor = activeFabColor,
            onDismiss = { activeNumpadState.dismiss() }
        )
    }
}

