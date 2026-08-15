package com.sinxn.mymoney.feature.debt.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sinxn.mymoney.core.data.local.model.DebtWithDetails
import com.sinxn.mymoney.core.data.local.model.TransactionWithCategory
import com.sinxn.mymoney.core.ui.components.CleanListRow
import com.sinxn.mymoney.core.ui.components.FormCardContainer
import com.sinxn.mymoney.core.ui.components.groupTransactionsByMonth
import com.sinxn.mymoney.core.ui.components.monthGroupedTransactionItems
import com.sinxn.mymoney.core.util.MoneyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun DebtViewContent(
    debtDetails: DebtWithDetails,
    transactions: List<TransactionWithCategory>,
    formatterConfig: MoneyFormatter.Config,
    currencyCode: String,
    currencyDecimals: Int,
    dateFormat: Int,
    accentColor: Color,
    onTransactionClick: (String) -> Unit,
    onRecordPaymentClick: () -> Unit
) {
    val debt = debtDetails.debt
    val remaining = debtDetails.remainingMoney
    val totalMoney = debt.money
    val isDebt = debt.type == 0
    val isFullyPaid = remaining == 0L && totalMoney > 0

    var collapsedGroups by remember { mutableStateOf(setOf<String>()) }
    val groupedItems = remember(transactions) {
        groupTransactionsByMonth(transactions)
    }

    val isOverdue = remember(debt.expirationDate) {
        debt.expirationDate?.let { exp ->
            try {
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val expDate = format.parse(exp)
                expDate != null && expDate.before(Date()) && !isFullyPaid
            } catch (e: Exception) {
                false
            }
        } ?: false
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
    ) {
        // 1. Centered Hero Amount Section (Inspired by Transaction View Screen)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isFullyPaid) "Settled in Full" else "Remaining Balance",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                val formattedRemaining = MoneyFormatter.format(
                    amount = remaining,
                    currencyCode = debtDetails.walletCurrency,
                    decimals = debtDetails.walletDecimals,
                    config = formatterConfig
                )

                Text(
                    text = formattedRemaining,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isFullyPaid) MaterialTheme.colorScheme.primary else accentColor,
                    letterSpacing = (-1.2).sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Direction & Status Pill Chip
                Surface(
                    shape = CircleShape,
                    color = if (isFullyPaid) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else accentColor.copy(alpha = 0.12f),
                    border = BorderStroke(
                        1.dp,
                        if (isFullyPaid) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        else accentColor.copy(alpha = 0.25f)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (isFullyPaid) Icons.Default.CheckCircle
                            else if (isDebt) Icons.Default.ArrowDownward
                            else Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = if (isFullyPaid) MaterialTheme.colorScheme.primary else accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isFullyPaid) "Paid in Full"
                            else if (isDebt) "Debt"
                            else "Credit",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isFullyPaid) MaterialTheme.colorScheme.primary else accentColor
                        )
                    }
                }
            }
        }

        // 2. Progress Card
        if (totalMoney > 0) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    FormCardContainer(horizontalPadding = 0.dp) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Progress",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                val paidAmount = abs(debtDetails.progress)
                                val progressPercent = ((paidAmount.toFloat() / totalMoney.toFloat()) * 100).toInt().coerceIn(0, 100)
                                Text(
                                    text = "$progressPercent% Repaid",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            val progressFraction = (abs(debtDetails.progress).toFloat() / totalMoney.toFloat()).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = if (isFullyPaid) MaterialTheme.colorScheme.primary else accentColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Paid: ${MoneyFormatter.format(amount = abs(debtDetails.progress), currencyCode = debtDetails.walletCurrency, decimals = debtDetails.walletDecimals, config = formatterConfig)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Total: ${MoneyFormatter.format(amount = totalMoney, currencyCode = debtDetails.walletCurrency, decimals = debtDetails.walletDecimals, config = formatterConfig)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Description Block (if present)
        if (debt.description.isNotBlank()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    FormCardContainer(horizontalPadding = 0.dp) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Description",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = debt.description,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 4. Primary Details Card (Wallet, Dates, People, Place, Note)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                FormCardContainer(horizontalPadding = 0.dp) {
                    Column {
                        // Wallet Row
                        if (debtDetails.walletName.isNotBlank()) {
                            CleanListRow(
                                icon = { Icon(Icons.Default.Wallet, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp)) },
                                label = "Wallet",
                                value = debtDetails.walletName
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))
                        }

                        // Creation Date Row
                        CleanListRow(
                            icon = { Icon(Icons.Default.Event, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp)) },
                            label = "Date Created",
                            value = debt.date.take(10)
                        )

                        // Due Date / Expiration Date Row
                        if (!debt.expirationDate.isNullOrBlank()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))
                            CleanListRow(
                                icon = { Icon(Icons.Default.Event, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp)) },
                                label = "Due Date",
                                value = debt.expirationDate.take(10),
                                trailingBadge = if (isOverdue) {
                                    {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.errorContainer,
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                        ) {
                                            Text(
                                                text = "Overdue",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                } else null
                            )
                        }

                        // Linked People Row
                        if (debtDetails.people.isNotEmpty()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))
                            CleanListRow(
                                icon = { Icon(Icons.Default.Person, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp)) },
                                label = "People",
                                value = debtDetails.people.joinToString(", ") { it.name }
                            )
                        }

                        // Linked Place Row
                        if (!debtDetails.placeName.isNullOrBlank()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))
                            CleanListRow(
                                icon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp)) },
                                label = "Place",
                                value = debtDetails.placeName
                            )
                        }

                        // Notes Row
                        if (!debt.note.isNullOrBlank()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Notes, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Note",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = debt.note,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Quick Payment Button (if not fully paid)
        if (!isFullyPaid) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Button(
                        onClick = onRecordPaymentClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isDebt) "Record Repayment" else "Record Collection",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 6. Payment History Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Payment History (${transactions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // 7. Month-grouped Transactions List with Sticky Headers
        monthGroupedTransactionItems(
            items = groupedItems,
            collapsedGroups = collapsedGroups,
            onToggleGroup = { headerKey ->
                collapsedGroups = if (headerKey in collapsedGroups) {
                    collapsedGroups - headerKey
                } else {
                    collapsedGroups + headerKey
                }
            },
            onTransactionClick = onTransactionClick,
            decimals = currencyDecimals,
            currencyCode = currencyCode,
            formatterConfig = formatterConfig,
            dateFormat = dateFormat,
            emptyMessage = "No payments have been recorded yet."
        )
    }
}