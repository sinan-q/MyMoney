package com.sinxn.mymoney.feature.overview.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.data.local.model.WalletWithBalance
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.CategorySelectionDialog
import com.sinxn.mymoney.core.ui.components.FormCardContainer
import com.sinxn.mymoney.core.ui.components.TabPill
import com.sinxn.mymoney.core.ui.components.TransactionFormRowItem
import com.sinxn.mymoney.core.ui.components.parseIconData
import com.sinxn.mymoney.core.util.Constants
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.feature.overview.CashFlowFilter
import com.sinxn.mymoney.feature.overview.GroupType
import com.sinxn.mymoney.feature.overview.OverviewSettings
import com.sinxn.mymoney.feature.overview.OverviewType
import java.util.Date
import kotlin.collections.map

/**
 * Bottom sheet for configuring overview settings.
 * Includes Account/Wallet selection, Date Range, Group By, View Type (Cash Flow / Category),
 * and Category selection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewSettingsSheet(
    settings: OverviewSettings,
    currentWalletId: String,
    wallets: List<WalletWithBalance>,
    categories: List<CategoryEntity>,
    formattingSettings: FormattingSettings,
    onDismiss: () -> Unit,
    onApply: (OverviewSettings, String) -> Unit
) {
    var selectedWalletId by remember(currentWalletId) { mutableStateOf(currentWalletId) }
    var showWalletPicker by remember { mutableStateOf(false) }

    val selectedWallet = remember(selectedWalletId, wallets) {
        if (selectedWalletId == Constants.TOTAL_WALLET_ID) {
            null
        } else {
            wallets.find { it.wallet.id == selectedWalletId }
        }
    }
    val selectedWalletName = if (selectedWalletId == Constants.TOTAL_WALLET_ID) "Total (All Accounts)" else (selectedWallet?.wallet?.name ?: "Total")
    val selectedWalletIcon = if (selectedWalletId == Constants.TOTAL_WALLET_ID) "sigma" else selectedWallet?.wallet?.icon

    var groupType by remember(settings) { mutableStateOf(settings.groupType) }
    var overviewType by remember(settings) { mutableStateOf(settings.overviewType) }
    var cashFlowFilter by remember(settings) { mutableStateOf(settings.cashFlowFilter) }
    var selectedCategoryId by remember(settings.categoryId) { mutableStateOf(settings.categoryId) }
    var showCategoryPicker by remember { mutableStateOf(false) }

    val selectedCategory = remember(selectedCategoryId, categories) {
        categories.find { it.id == selectedCategoryId }
    }

    var startDate by remember(settings) { mutableStateOf(settings.startDate) }
    var endDate by remember(settings) { mutableStateOf(settings.endDate) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val walletIconData = remember(selectedWalletIcon, selectedWalletName) {
        parseIconData(selectedWalletIcon, selectedWalletName)
    }


    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                modifier = Modifier.padding(start = 16.dp),
                text = "Overview Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // ── Account / Wallet Selection ──
            CustomText("Wallet")
            FormCardContainer {
                TransactionFormRowItem(
                    icon = Icons.Default.AccountBalanceWallet,
                    active = true,
                    accentColor = MaterialTheme.colorScheme.primary,
                    label = "Wallet",
                    value = selectedWalletName,
                    trailingIconData = walletIconData,
                    onClick = { showWalletPicker = true }
                )
            }

            // ── Date Range Section ──
            CustomText("Date Range")
            FormCardContainer {
                TransactionFormRowItem(
                    icon = Icons.Default.CalendarMonth,
                    active = true,
                    accentColor = MaterialTheme.colorScheme.primary,
                    label = "From",
                    value = DateUtils.formatMonthDayYear(startDate),
                    onClick = { showStartDatePicker = true }

                )
                TransactionFormRowItem(
                    icon = Icons.Default.CalendarMonth,
                    active = true,
                    accentColor = MaterialTheme.colorScheme.primary,
                    label = "To",
                    value = DateUtils.formatMonthDayYear(endDate),
                    onClick = { showEndDatePicker = true }
                )
            }

            // ── Group Type — Daily, Weekly, Monthly, Yearly ──
            CustomText("Group By")
            TabPill(
                tabs = GroupType.entries.map { it.name.lowercase()
                    .replaceFirstChar { it.uppercase() } to MaterialTheme.colorScheme.primary },
                activeTab = groupType.ordinal
            ) { groupType = GroupType.entries[it] }


            // ── Overview Type — Cash Flow / Category ──
            CustomText("View Type")
            TabPill(
                tabs = listOf("Cash flow" to MaterialTheme.colorScheme.primary, "Category" to MaterialTheme.colorScheme.primary),
                activeTab = overviewType.ordinal
            ) {
                overviewType = OverviewType.entries[it]
                if (overviewType == OverviewType.CATEGORY) showCategoryPicker = true

            }


            // ── Cash Flow Sub-filter — Incomes, Expenses, Net Incomes ──
            if (overviewType == OverviewType.CASH_FLOW) {
                CustomText("Cash Flow Filter")
                TabPill(
                    tabs = CashFlowFilter.entries.map { when (it) {
                        CashFlowFilter.INCOMES -> "Incomes"
                        CashFlowFilter.EXPENSES -> "Expenses"
                        CashFlowFilter.NET_INCOMES -> "Net"
                    } to MaterialTheme.colorScheme.primary} ,
                    activeTab = cashFlowFilter.ordinal
                ) {
                     cashFlowFilter = CashFlowFilter.entries[it]
                }

            }

            // ── Category Selector — when overviewType is CATEGORY ──
            if (overviewType == OverviewType.CATEGORY) {
                val categoryIconData = remember(selectedCategory) {
                    parseIconData(selectedCategory?.icon, selectedCategory?.name?: "Category")
                }
                CustomText("Category Filter")
                FormCardContainer {
                    TransactionFormRowItem(
                        icon = Icons.Default.Category,
                        accentColor = MaterialTheme.colorScheme.primary,
                        label = "Category",
                        value = selectedCategory?.name ?: "Select Category",
                        trailingIconData = if (selectedCategory!= null) categoryIconData else null,
                        onClick = { showCategoryPicker = true }

                    )
                }
            }

            // ── Apply button ──
            val isApplyEnabled = overviewType != OverviewType.CATEGORY || selectedCategoryId != null
            Button(
                onClick = {
                    onApply(
                        OverviewSettings(
                            startDate = startDate,
                            endDate = endDate,
                            groupType = groupType,
                            overviewType = overviewType,
                            cashFlowFilter = cashFlowFilter,
                            categoryId = if (overviewType == OverviewType.CATEGORY) selectedCategoryId else null
                        ),
                        selectedWalletId
                    )
                },
                enabled = isApplyEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Apply", fontWeight = FontWeight.Bold)
            }
        }
    }

    // ── Wallet Picker Sub-Sheet ──
    if (showWalletPicker) {
        OverviewWalletPickerSheet(
            wallets = wallets,
            currentWalletId = selectedWalletId,
            formattingSettings = formattingSettings,
            onSelectWallet = {
                selectedWalletId = it
                showWalletPicker = false
            },
            onDismiss = { showWalletPicker = false }
        )
    }

    // ── Category Picker Dialog ──
    if (showCategoryPicker) {
        CategorySelectionDialog(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            showNoneOption = false,
            title = "Select Category",
            onCategorySelected = { cat ->
                if (cat != null) {
                    selectedCategoryId = cat.id
                }
                showCategoryPicker = false
            },
            onDismissRequest = { showCategoryPicker = false }
        )
    }

    // ── Date Pickers ──
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.dateToDatePickerMillis(startDate)
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            startDate = DateUtils.datePickerMillisToDate(millis, endOfDay = false)
                        }
                        showStartDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.dateToDatePickerMillis(endDate)
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            endDate = DateUtils.datePickerMillisToDate(millis, endOfDay = true)
                        }
                        showEndDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun CustomText(text: String) {
    Text(
        modifier = Modifier.padding(start = 16.dp),
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}