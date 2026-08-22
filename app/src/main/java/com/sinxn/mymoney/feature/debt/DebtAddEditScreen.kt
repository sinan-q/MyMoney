package com.sinxn.mymoney.feature.debt

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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

private val DebtRoseColor = Color(0xFFE11D48)
private val CreditEmeraldColor = Color(0xFF10B981)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtAddEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: DebtAddEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDebt = uiState.editType == 0
    val accentColor = if (isDebt) DebtRoseColor else CreditEmeraldColor

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
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 16.dp)
                    ) {
                        if (uiState.isNewDebt) {
                            TabPill(
                                tabs = listOf("Debt" to DebtRoseColor, "Credit" to CreditEmeraldColor),
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
                            onFocusField = numpadState::onFocusField,
                            onDismissKeyboardAndNumpad = numpadState::dismiss
                        )
                    }

                    // Docked Bottom Bar (Numpad or Save Button)
                    Surface(
                        tonalElevation = 6.dp,
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    ) {
                        Column {
                            AnimatedVisibility(
                                visible = numpadState.isNumpadVisible,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
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
                        if (!numpadState.isNumpadVisible) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        numpadState.dismiss()
                                        viewModel.saveDebt { onNavigateBack() }
                                    },
                                    enabled = isSaveEnabled,
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = accentColor,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                ) {
                                    if (uiState.isSaving) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(
                                            text = actionBtnText,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
