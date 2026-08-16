package com.sinxn.mymoney.feature.recurrence

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sinxn.mymoney.R
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.CleanListRow
import com.sinxn.mymoney.core.ui.components.FormCardContainer
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MoneyFormatter
import com.sinxn.mymoney.core.util.RecurrenceSetting

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurrentTransactionDetailsScreen(
    onNavigateBack: () -> Unit,
    onEditClick: (String) -> Unit,
    viewModel: RecurrentTransactionDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.formattingSettings.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                    text = "Recurrence Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(onClick = { showDeleteConfirmation = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        floatingActionButton = {
            if (!uiState.isLoading && uiState.item != null) {
                ExtendedFloatingActionButton(
                    onClick = { onEditClick(viewModel.recurrenceId) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(18.dp),
                    icon = { Icon(Icons.Default.Edit, contentDescription = "Edit") },
                    text = { Text("Edit Recurrence", fontWeight = FontWeight.Bold) }
                )
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
            } else {
                val item = uiState.item
                if (item == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Recurrence not found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val rt = item.recurrentTransaction
                    val isIncome = rt.direction == 1
                    val directionColor = if (isIncome) Color(0xFF10B981) else Color(0xFFE11D48)

                    val startDate = remember(rt.startDate) { DateUtils.parseDate(rt.startDate) }
                    val setting = remember(rt.startDate, rt.rule) {
                        RecurrenceSetting.fromStringOrFallback(startDate, rt.rule)
                    }

                    val formattedAmountValue = remember(rt.money, settings) {
                        MoneyFormatter.format(
                            amount = rt.money,
                            currencyCode = "",
                            decimals = 2,
                            config = MoneyFormatter.Config(
                                showCurrency = false,
                                groupDigits = settings.groupDigits,
                                roundDecimals = settings.roundDecimals,
                                showPlusMinus = false
                            )
                        )
                    }

                    val formattedStartDate = remember(rt.startDate, settings.dateFormat) {
                        DateUtils.formatDate(startDate, settings.dateFormat)
                    }

                    val finishedHint = stringResource(R.string.hint_recurrence_finished)
                    val formattedNextOccurrence = remember(rt.nextOccurrence, settings.dateFormat, finishedHint) {
                        rt.nextOccurrence?.let { next ->
                            val nextDate = DateUtils.parseDate(next)
                            DateUtils.formatDate(nextDate, settings.dateFormat)
                        } ?: finishedHint
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(bottom = 100.dp)
                    ) {
                        // 1. Centered Hero Amount Display
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = item.wallet.currency,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = directionColor
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = (if (isIncome) "+" else "-") + formattedAmountValue,
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    letterSpacing = (-1.5).sp
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isIncome) "Recurrent Income" else "Recurrent Expense",
                                style = MaterialTheme.typography.bodyMedium,
                                color = directionColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // 2. Schedule Card Container
                        FormCardContainer {
                            CleanListRow(
                                icon = {
                                    Icon(
                                        Icons.Default.Repeat,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                label = "Frequency",
                                value = setting.getUserReadableString(context)
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            CleanListRow(
                                icon = {
                                    Icon(
                                        Icons.Default.Event,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                label = "Next Occurrence",
                                value = formattedNextOccurrence
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            CleanListRow(
                                icon = {
                                    Icon(
                                        Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                label = "Start Date",
                                value = formattedStartDate
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 3. Core Attributes Container
                        FormCardContainer {
                            CleanListRow(
                                icon = {
                                    CategoryIcon(
                                        iconString = item.category.icon,
                                        categoryName = item.category.name,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                label = "Category",
                                value = item.category.name
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            CleanListRow(
                                icon = {
                                    Icon(
                                        Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                label = "Wallet",
                                value = item.wallet.name
                            )
                        }

                        // 4. Details Container (if description, note, place, event present)
                        val hasDetails = !rt.description.isNullOrBlank() || !rt.note.isNullOrBlank() || item.place != null || item.event != null
                        if (hasDetails) {
                            Spacer(modifier = Modifier.height(12.dp))
                            FormCardContainer {
                                var isFirst = true

                                if (!rt.description.isNullOrBlank()) {
                                    CleanListRow(
                                        icon = {
                                            Icon(
                                                Icons.Default.Description,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        label = "Description",
                                        value = rt.description
                                    )
                                    isFirst = false
                                }

                                if (!rt.note.isNullOrBlank()) {
                                    if (!isFirst) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    CleanListRow(
                                        icon = {
                                            Icon(
                                                Icons.Default.Notes,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        label = "Note",
                                        value = rt.note
                                    )
                                    isFirst = false
                                }

                                if (item.place != null) {
                                    if (!isFirst) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    CleanListRow(
                                        icon = {
                                            Icon(
                                                Icons.Default.Place,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        label = "Place",
                                        value = item.place.name
                                    )
                                    isFirst = false
                                }

                                if (item.event != null) {
                                    if (!isFirst) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    CleanListRow(
                                        icon = {
                                            Icon(
                                                Icons.Default.Event,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        label = "Event",
                                        value = item.event.name
                                    )
                                }
                            }
                        }

                        // 5. Impact & Status Container
                        Spacer(modifier = Modifier.height(12.dp))
                        FormCardContainer {
                            CleanListRow(
                                icon = {
                                    Icon(
                                        if (rt.confirmed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (rt.confirmed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                label = "Auto Confirmed",
                                value = if (rt.confirmed) "Confirmed automatically" else "Requires manual confirmation"
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            CleanListRow(
                                icon = {
                                    Icon(
                                        Icons.Default.QueryStats,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                label = "Count in Total",
                                value = if (rt.countInTotal) "Included in reports" else "Excluded from reports"
                            )
                        }
                    }
                }
            }
        }

        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Delete Recurrence?") },
                text = { Text("Historical transactions will be preserved without recurrence link.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmation = false
                            viewModel.delete { onNavigateBack() }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
