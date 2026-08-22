package com.sinxn.mymoney.feature.transaction.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sinxn.mymoney.core.data.preferences.FormattingSettings
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.CleanListRow
import com.sinxn.mymoney.core.ui.components.CreditEmeraldColor
import com.sinxn.mymoney.core.ui.components.DebtRoseColor
import com.sinxn.mymoney.core.ui.components.FormCardContainer
import com.sinxn.mymoney.core.ui.components.TransactionFormRowItem
import com.sinxn.mymoney.core.ui.components.parseIconData
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MoneyFormatter
import com.sinxn.mymoney.feature.transaction.TransactionDetailsUiState
import com.sinxn.mymoney.feature.transaction.util.resolveCategoryHierarchy
import com.sinxn.mymoney.feature.transfer.components.CentreSwapButton
import java.util.Locale

@Composable
fun ViewTransactionContent(
    uiState: TransactionDetailsUiState,
    settings: FormattingSettings
) {
    val transaction = uiState.transaction?.transaction ?: return
    val scrollState = rememberScrollState()

    val isTransfer = uiState.isTransfer || transaction.type == 1
    val (categoryName, parentCategoryName) = if (isTransfer) {
        "Transfer" to null
    } else {
        resolveCategoryHierarchy(
            transaction.categoryId,
            uiState.availableCategories,
            fallbackName = uiState.transaction.categoryName
        )
    }
    val categoryIcon =  uiState.transaction.categoryIcon
        ?: uiState.availableCategories.find { it.id == transaction.categoryId }?.icon

    val directionColor = if (isTransfer) Color(0xFF0284C7) else when (transaction.direction) {
        1 -> Color(0xFF10B981)
        else -> Color(0xFFE11D48)
    }
    val activeWallet = uiState.wallet
    val walletIconData = remember(activeWallet?.icon, activeWallet?.name) {
        if (activeWallet != null) parseIconData(activeWallet.icon, activeWallet.name) else null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 100.dp)
    )  {
        // 1. Centered Hero Amount Display
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val formattedAmountValue = MoneyFormatter.format(
                amount = transaction.money,
                currencyCode = "",
                decimals = uiState.currencyDecimals,
                config = settings
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = uiState.currencySymbol,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    color = directionColor
                )
                Text(
                    text = MoneyFormatter.formatBalanceWithNonBoldDecimals(formattedAmountValue, baseWeight = FontWeight.Bold) ,
                    fontSize = 58.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-1.5).sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Category Icon & Name Chip
            Surface(
                shape = CircleShape,
                color = directionColor.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, directionColor.copy(alpha = 0.25f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    if (isTransfer) {
                        val isIncomeTransfer = transaction.direction == 1
                        val transferAccentColor = if (isIncomeTransfer) CreditEmeraldColor else DebtRoseColor
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(transferAccentColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isIncomeTransfer) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = transferAccentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                    } else {
                        CategoryIcon(
                            iconString = categoryIcon,
                            categoryName = categoryName,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    if (parentCategoryName != null) {
                        Text(
                            text = "$parentCategoryName • ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.65f)
                        )
                    }
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        // 2. Description Card (Reused)
        if (!transaction.description.isNullOrEmpty()) {
            FormCardContainer {
                TransactionFormRowItem(
                    icon = Icons.Default.Description,
                    accentColor = directionColor,
                    label = "Description",
                    value = transaction.description
                )
            }
        }

        // 3. Primary Details Card (Wallet, Date & Time, People)
        if (uiState.isTransfer) {
            FormCardContainer {
                TransactionFormRowItem(
                    icon = Icons.Default.AccountBalanceWallet,
                    accentColor = directionColor,
                    label = "From Wallet",
                    value = activeWallet?.name ?: "Source Wallet",
                    trailingIconData = walletIconData
                )
            }
            CentreSwapButton(
                icon = Icons.Default.ArrowDownward,
                accentColor = directionColor,
            )
            FormCardContainer {
                val activeTargetWallet = uiState.targetWallet
                val targetWalletIconData = remember(activeTargetWallet?.icon, activeTargetWallet?.name) {
                    if (activeTargetWallet != null) parseIconData(activeTargetWallet.icon, activeTargetWallet.name) else null
                }
                TransactionFormRowItem(
                    icon = Icons.Default.AccountBalanceWallet,
                    accentColor = directionColor,
                    label = "To Wallet",
                    value = activeTargetWallet?.name ?: "Target Wallet",
                    trailingIconData = targetWalletIconData
                )
            }
        }
        FormCardContainer {
            if (!isTransfer) {
                TransactionFormRowItem(
                    icon = Icons.Default.AccountBalanceWallet,
                    accentColor = directionColor,
                    label = "Wallet",
                    value = activeWallet?.name ?: "Select Wallet",
                    trailingIconData = walletIconData
                )
            }

            val parsedDate = remember(transaction.date) { DateUtils.parseDate(transaction.date) }
            val formattedDate = DateUtils.formatDate(parsedDate, settings.dateFormat)
            val formattedTime = DateUtils.formatTime(parsedDate)

            TransactionFormRowItem(
                icon = Icons.Default.CalendarToday,
                accentColor = directionColor,
                label = if (settings.hideTime) "Date" else "Date & Time",
                value = if (settings.hideTime) formattedDate else "$formattedDate at $formattedTime"
            )

            if (uiState.people.isNotEmpty()) {
                TransactionFormRowItem(
                    icon = Icons.Default.People,
                    accentColor = directionColor,
                    label = "People",
                    value = uiState.people.joinToString { it.name },
                )
            }
            uiState.place?.let { place ->
                TransactionFormRowItem(
                    icon = Icons.Default.LocationOn,
                    accentColor = directionColor,
                    label = "Place",
                    value = place.name
                )
            }

            uiState.event?.let { event ->
                TransactionFormRowItem(
                    icon = Icons.Default.Flag,
                    accentColor = directionColor,
                    label = "Event",
                    value = event.name
                )
            }
            if (!transaction.note.isNullOrEmpty()) {
                ViewStackedDetailBlock(
                    icon = Icons.AutoMirrored.Filled.Notes,
                    iconTint = directionColor,
                    label = "Note",
                    value = transaction.note
                )
            }
            if (uiState.attachments.isNotEmpty()) {
                CleanListRow(
                    icon = {
                        Icon(
                            Icons.Default.AttachFile,
                            contentDescription = null,
                            tint = directionColor,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = "Attachments",
                    value = "${uiState.attachments.size} files"
                )
            }
        }

        if (!transaction.recurrenceId.isNullOrEmpty()) {
            FormCardContainer {
                TransactionFormRowItem(
                    icon = Icons.Default.Repeat,
                    accentColor = directionColor,
                    label = "Created by Recurrence",
                )
            }
        }
        if (!settings.hideStatusAndImpact) {
            FormCardContainer(containerAlpha = 0.25f) {
                CleanListRow(
                    label = "Confirmed",
                    trailingIcon = {
                        Icon( if (transaction.confirmed) Icons.Default.Check else Icons.Default.Close, contentDescription = null)
                    }
                )
                CleanListRow(
                    label = "Count In Total",
                    trailingIcon = {
                        Icon(Icons.Default.Check, contentDescription = null)
                    }
                )
            }
        }
    }
}

@Composable
private fun ViewStackedDetailBlock(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}
