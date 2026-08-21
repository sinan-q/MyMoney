package com.sinxn.mymoney.feature.transaction.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MoneyFormatter
import com.sinxn.mymoney.feature.transaction.TransactionDetailsUiState
import com.sinxn.mymoney.feature.transaction.util.resolveCategoryHierarchy
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale

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
                            style = MaterialTheme.typography.labelMedium,
                            color = directionColor.copy(alpha = 0.8f)
                        )
                    }
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = directionColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Description Card (Reused)
        if (!transaction.description.isNullOrEmpty()) {
            TransactionDescriptionCard(
                description = transaction.description,
                accentColor = directionColor,
                isEditable = false
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 3. Primary Details Card (Wallet, Date & Time, People)
        FormCardContainer {
            Column {
                if (uiState.isTransfer) {
                    CleanListRow(
                        icon = {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = directionColor,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = "From Wallet",
                        value = uiState.walletName.ifEmpty { "Source Wallet" }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    CleanListRow(
                        icon = {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = directionColor,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = "To Wallet",
                        value = uiState.targetWalletName.ifEmpty { "Target Wallet" }
                    )
                } else {
                    CleanListRow(
                        icon = {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = directionColor,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = "Wallet",
                        value = uiState.walletName.ifEmpty { "Default Wallet" }
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                val parsedDate = DateUtils.parseDate(transaction.date)
                val formattedDate = DateUtils.formatDate(parsedDate, settings.dateFormat)
                val formattedTime =
                    SimpleDateFormat("HH:mm", LocalLocale.current.platformLocale).format(parsedDate)

                CleanListRow(
                    icon = {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = directionColor,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = if (settings.hideTime) "Date" else "Date & Time",
                    value = if (settings.hideTime) formattedDate else "$formattedDate at $formattedTime"
                )

                if (uiState.people.isNotEmpty()) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    CleanListRow(
                        icon = {
                            Icon(
                                Icons.Default.People,
                                contentDescription = null,
                                tint = directionColor,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = "People",
                        value = uiState.people.joinToString { it.name }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4. Secondary Details Card (Place, Event, Note, Attachments, Recurrence, Status, Impact)
        val hasSecondaryContent = uiState.place != null ||
                uiState.event != null ||
                !transaction.note.isNullOrEmpty() ||
                uiState.attachments.isNotEmpty() ||
                !transaction.recurrenceId.isNullOrEmpty() ||
                !settings.hideStatusAndImpact

        if (hasSecondaryContent) {
            FormCardContainer(containerAlpha = 0.25f) {
                Column {
                    var hasPreviousRow = false

                    uiState.place?.let { place ->
                        CleanListRow(
                            icon = {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = directionColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = "Place",
                            value = place.name
                        )
                        hasPreviousRow = true
                    }

                    uiState.event?.let { event ->
                        if (hasPreviousRow) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(
                                    alpha = 0.2f
                                ), modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        CleanListRow(
                            icon = {
                                Icon(
                                    Icons.Default.Event,
                                    contentDescription = null,
                                    tint = directionColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = "Event",
                            value = event.name
                        )
                        hasPreviousRow = true
                    }

                    if (!transaction.note.isNullOrEmpty()) {
                        if (hasPreviousRow) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(
                                    alpha = 0.2f
                                ), modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        ViewStackedDetailBlock(
                            icon = Icons.Default.Notes,
                            iconTint = directionColor,
                            label = "Note",
                            value = transaction.note
                        )
                        hasPreviousRow = true
                    }

                    if (uiState.attachments.isNotEmpty()) {
                        if (hasPreviousRow) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(
                                    alpha = 0.2f
                                ), modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
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
                        hasPreviousRow = true
                    }

                    if (!transaction.recurrenceId.isNullOrEmpty()) {
                        if (hasPreviousRow) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(
                                    alpha = 0.2f
                                ), modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        CleanListRow(
                            icon = {
                                Icon(
                                    Icons.Default.Repeat,
                                    contentDescription = null,
                                    tint = directionColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = "Recurrence",
                            value = "Recurring Transaction"
                        )
                        hasPreviousRow = true
                    }

                    if (!settings.hideStatusAndImpact) {
                        if (hasPreviousRow) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(
                                    alpha = 0.2f
                                ), modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }

                        // Status Row
                        CleanListRow(
                            icon = {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = directionColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = "Status",
                            value = "",
                            trailingBadge = {
                                Surface(
                                    shape = CircleShape,
                                    color = if (transaction.confirmed) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.outline.copy(
                                        alpha = 0.15f
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (transaction.confirmed) Color(0xFF10B981).copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(
                                            alpha = 0.3f
                                        )
                                    )
                                ) {
                                    Text(
                                        text = if (transaction.confirmed) "Confirmed" else "Pending",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (transaction.confirmed) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(
                                            horizontal = 10.dp,
                                            vertical = 4.dp
                                        )
                                    )
                                }
                            }
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(
                                alpha = 0.2f
                            ), modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        // Impact Row
                        CleanListRow(
                            icon = {
                                Icon(
                                    Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = directionColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = "Impact",
                            value = "",
                            trailingBadge = {
                                Surface(
                                    shape = CircleShape,
                                    color = directionColor.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, directionColor.copy(alpha = 0.25f))
                                ) {
                                    Text(
                                        text = if (transaction.countInTotal) "Included in Total" else "Excluded from Total",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = directionColor,
                                        modifier = Modifier.padding(
                                            horizontal = 10.dp,
                                            vertical = 4.dp
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
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
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
            )
        }
    }
}
