package com.sinxn.mymoney.feature.transaction.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.data.local.model.WalletWithBalance
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
import com.sinxn.mymoney.core.ui.components.CategorySelectionDialog
import com.sinxn.mymoney.core.ui.components.FormCardContainer
import com.sinxn.mymoney.core.ui.components.TabPill
import com.sinxn.mymoney.core.ui.components.TransactionFormRowItem
import com.sinxn.mymoney.core.ui.components.parseIconData
import com.sinxn.mymoney.core.util.Constants
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.feature.overview.GroupType
import com.sinxn.mymoney.feature.overview.component.OverviewWalletPickerSheet
import com.sinxn.mymoney.feature.transaction.TransactionFilter

/**
 * Bottom sheet for filtering the transaction list on the Wallet Details screen.
 *
 * Filters:
 *  - Wallet selection
 *  - Date range (off by default, toggled by a Switch)
 *  - Group by (Daily / Weekly / Monthly / Yearly — Monthly default)
 *  - Categories (multi-select via [CategorySelectionDialog]; empty = All)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFilterSheet(
    filter: TransactionFilter,
    currentWalletId: String,
    wallets: List<WalletWithBalance>,
    categories: List<CategoryEntity>,
    formattingSettings: FormattingSettings,
    onDismiss: () -> Unit,
    onApply: (TransactionFilter) -> Unit
) {
    // ── Local state ──────────────────────────────────────────────────────────
    var selectedWalletId by remember(currentWalletId) { mutableStateOf(currentWalletId) }
    var showWalletPicker by remember { mutableStateOf(false) }

    var dateRangeEnabled by remember(filter) { mutableStateOf(filter.dateRangeEnabled) }
    var startDate by remember(filter) { mutableStateOf(filter.startDate) }
    var endDate by remember(filter) { mutableStateOf(filter.endDate) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    var groupType by remember(filter) { mutableStateOf(filter.groupType) }

    // Multi-select category IDs; empty = All
    var selectedCategoryIds by remember(filter) { mutableStateOf(filter.categoryIds) }
    var showCategoryPicker by remember { mutableStateOf(false) }

    // ── Derived display helpers ──────────────────────────────────────────────
    val selectedWallet = remember(selectedWalletId, wallets) {
        if (selectedWalletId == Constants.TOTAL_WALLET_ID) null
        else wallets.find { it.wallet.id == selectedWalletId }
    }
    val selectedWalletName =
        if (selectedWalletId == Constants.TOTAL_WALLET_ID) "Total (All Accounts)"
        else selectedWallet?.wallet?.name ?: "Total"
    val selectedWalletIcon =
        if (selectedWalletId == Constants.TOTAL_WALLET_ID) "sigma"
        else selectedWallet?.wallet?.icon
    val walletIconData = remember(selectedWalletIcon, selectedWalletName) {
        parseIconData(selectedWalletIcon, selectedWalletName)
    }

    val categorySummary = when {
        selectedCategoryIds.isEmpty() -> "All categories"
        selectedCategoryIds.size == 1 ->
            categories.find { it.id == selectedCategoryIds.first() }?.name ?: "1 category"
        else -> "${selectedCategoryIds.size} categories"
    }

    // ── Sheet ────────────────────────────────────────────────────────────────
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
                text = "Filter Transactions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // ── Wallet ──
            FilterLabel("Wallet")
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

            // ── Date Range (with toggle) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterLabel("Date Range")
                Switch(
                    checked = dateRangeEnabled,
                    onCheckedChange = { dateRangeEnabled = it }
                )
            }

            AnimatedVisibility(
                visible = dateRangeEnabled,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
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
            }

            // ── Group By ──
            FilterLabel("Group By")
            TabPill(
                tabs = GroupType.entries.map {
                    it.name.lowercase().replaceFirstChar { c -> c.uppercase() } to MaterialTheme.colorScheme.primary
                },
                activeTab = groupType.ordinal
            ) { groupType = GroupType.entries[it] }

            // ── Categories (multi-select) ──
            FilterLabel("Categories")
            FormCardContainer {
                TransactionFormRowItem(
                    icon = Icons.Default.Category,
                    active = true,
                    accentColor = MaterialTheme.colorScheme.primary,
                    label = "Categories",
                    value = categorySummary,
                    onClick = { showCategoryPicker = true }
                )
            }

            // ── Apply ──
            Button(
                onClick = {
                    onApply(
                        TransactionFilter(
                            walletId = selectedWalletId,
                            dateRangeEnabled = dateRangeEnabled,
                            startDate = startDate,
                            endDate = endDate,
                            groupType = groupType,
                            categoryIds = selectedCategoryIds
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Apply", fontWeight = FontWeight.Bold)
            }
        }
    }

    // ── Wallet Picker Sub-Sheet ──────────────────────────────────────────────
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

    // ── Multi-Category Picker (reuses CategorySelectionDialog in multi-select mode) ──
    if (showCategoryPicker) {
        CategorySelectionDialog(
            categories = categories,
            selectedCategoryId = null,
            onCategorySelected = {},
            onDismissRequest = { showCategoryPicker = false },
            title = "Select Categories",
            multiSelectIds = selectedCategoryIds,
            onMultiSelectConfirmed = { ids ->
                selectedCategoryIds = ids
                showCategoryPicker = false
            }
        )
    }

    // ── Start Date Picker ────────────────────────────────────────────────────
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.dateToDatePickerMillis(startDate)
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        startDate = DateUtils.datePickerMillisToDate(millis, endOfDay = false)
                    }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    // ── End Date Picker ──────────────────────────────────────────────────────
    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.dateToDatePickerMillis(endDate)
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        endDate = DateUtils.datePickerMillisToDate(millis, endOfDay = true)
                    }
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }
}

// ── Helper label ─────────────────────────────────────────────────────────────

@Composable
private fun FilterLabel(text: String) {
    Text(
        modifier = Modifier.padding(start = 16.dp),
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}