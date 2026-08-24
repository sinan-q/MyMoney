package com.sinxn.mymoney.feature.debt

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import com.sinxn.mymoney.core.ui.components.AppExtendedFab
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sinxn.mymoney.core.ui.components.EditAmountHeader
import com.sinxn.mymoney.core.ui.components.NumpadView
import com.sinxn.mymoney.core.ui.components.TabPill
import com.sinxn.mymoney.core.ui.components.rememberNumpadFormState
import com.sinxn.mymoney.core.util.MoneyFormatter
import com.sinxn.mymoney.feature.debt.component.DebtFormContent
import com.sinxn.mymoney.ui.theme.ExpenseColor
import com.sinxn.mymoney.ui.theme.IncomeColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtAddEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: DebtAddEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDebt = uiState.editType == 0
    val accentColor = if (isDebt) ExpenseColor else IncomeColor

    val numpadState = rememberNumpadFormState(initialNumpadVisible = uiState.isNewDebt)

    val hasOperatorInAmount = remember(uiState.editAmount) {
        numpadState.hasOperator(uiState.editAmount)
    }
    val selectedWallet = uiState.availableWallets.find { it.id == uiState.editWalletId }

    val formCurrency = remember(selectedWallet, uiState.currencyCode) {
        MoneyFormatter.getCurrencySymbol(selectedWallet?.currency ?: uiState.currencyCode)
    }
    val evaluatedAmountStr = remember(uiState.editAmount) {
        numpadState.getImmediateResult(uiState.editAmount, uiState.currencyDecimals)
    }
    val amountValue = remember(evaluatedAmountStr) {
        evaluatedAmountStr.toDoubleOrNull()
    }

    val isSaveEnabled = uiState.editDescription.isNotBlank() &&
            (amountValue != null && amountValue > 0.0) &&
            uiState.editWalletId.isNotBlank() &&
            !uiState.isSaving

    val actionBtnText = if (uiState.isNewDebt) {
        if (uiState.editType == 0) "Save Debt" else "Save Credit"
    } else {
        "Save Changes"
    }

    val titleText = if (uiState.isNewDebt) {
        if (uiState.editType == 0) "New Debt" else "New Credit"
    } else {
        if (uiState.editType == 0) "Edit Debt" else "Edit Credit"
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            AnimatedVisibility(
                visible = isSaveEnabled && !numpadState.isNumpadVisible,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                AppExtendedFab(
                    text = actionBtnText,
                    icon = if (uiState.isNewDebt) Icons.Default.Add else Icons.Default.Check,
                    onClick = {
                        numpadState.dismiss()
                        viewModel.saveDebt { onNavigateBack() }
                    },
                    containerColor = accentColor,
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
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 88.dp)
                ) {
                    if (uiState.isNewDebt) {
                        TabPill(
                            tabs = listOf("Debt" to ExpenseColor, "Credit" to IncomeColor),
                            activeTab = uiState.editType,
                            onTabChange = { viewModel.updateType(it) }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    EditAmountHeader(
                        amountText = uiState.editAmount,
                        currencySymbol = formCurrency,
                        evaluatedResult = evaluatedAmountStr,
                        hasOperatorInAmount = hasOperatorInAmount,
                        accentColor = accentColor,
                        isNumpadVisible = numpadState.isNumpadVisible,
                        onHeaderClick = { numpadState.showNumpad() }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    DebtFormContent(
                        accentColor = accentColor,
                        focusRequester = numpadState.focusRequester,
                        uiState = uiState,
                        viewModel = viewModel,
                        onDismissKeyboardAndNumpad = numpadState::dismiss
                    )
                }
            }
        }
    }

    if (numpadState.isNumpadVisible) {
        NumpadView(
            onKeyPress = viewModel::onNumpadKeyPress,
            onEvaluate = viewModel::evaluateMathExpression,
            onNext = { numpadState.onNext() },
            hasOperatorInAmount = hasOperatorInAmount,
            saveButtonText = actionBtnText,
            saveButtonColor = accentColor,
            onDismiss = { numpadState.dismiss() }
        )
    }
}
