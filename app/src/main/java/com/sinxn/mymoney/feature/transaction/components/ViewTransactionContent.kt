package com.sinxn.mymoney.feature.transaction.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.util.MoneyFormatter
import com.sinxn.mymoney.feature.transaction.TransactionDetailsUiState
import com.sinxn.mymoney.feature.transaction.util.resolveCategoryHierarchy
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.text.ifEmpty

@Composable
fun ViewTransactionContent(
    uiState: TransactionDetailsUiState,
    settings: com.sinxn.mymoney.core.data.preferences.FormattingSettings,
    onEditClick: () -> Unit
) {
    val transaction = uiState.transaction?.transaction ?: return
    val scrollState = rememberScrollState()

    val isTransfer = uiState.isTransfer || transaction.direction == 2 || transaction.type == 1 || transaction.type == 2
    val (categoryName, parentCategoryName) = if (isTransfer) {
        "Transfer" to null
    } else {
        resolveCategoryHierarchy(
            transaction.categoryId,
            uiState.availableCategories
        )
    }
    val categoryIcon = if (isTransfer) "{\"type\":\"color\",\"color\":\"#0284C7\",\"name\":\"⇄\"}" else uiState.transaction?.categoryIcon

    val directionColor = if (isTransfer) Color(0xFF0284C7) else when (transaction.direction) {
        1 -> Color(0xFF10B981) // Income Mint
        else -> Color(0xFFE11D48) // Expense Rose
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 100.dp)
    ) {
        // 1. Centered Hero Amount Display (Matching Edit Screen Style)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Clean Large Amount Text
            val formattedAmountValue = MoneyFormatter.format(
                amount = transaction.money,
                currencyCode = "",
                decimals = uiState.currencyDecimals,
                config = MoneyFormatter.Config(
                    showCurrency = false,
                    groupDigits = settings.groupDigits,
                    roundDecimals = settings.roundDecimals,
                    showPlusMinus = false
                )
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
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formattedAmountValue,
                    fontSize = 58.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-1.5).sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Category Icon & Name Chip (Below Amount)
            Surface(
                shape = CircleShape,
                color = directionColor.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, directionColor.copy(alpha = 0.25f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    CategoryIcon(
                        iconString = categoryIcon,
                        categoryName = categoryName,
                        modifier = Modifier.size(18.dp)
                    )
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

        // 2. Description Card (Separate Card if description exists)
        if (!transaction.description.isNullOrEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = directionColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // 3. Primary Details Card (Wallet, Date & Time)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column {
                // Wallet Row(s)
                if (uiState.isTransfer || transaction.direction == 2) {
                    ViewDetailRow(
                        icon = {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = directionColor, modifier = Modifier.size(22.dp))
                        },
                        label = "From Wallet",
                        value = uiState.walletName.ifEmpty { "Source Wallet" }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))

                    ViewDetailRow(
                        icon = {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = directionColor, modifier = Modifier.size(22.dp))
                        },
                        label = "To Wallet",
                        value = uiState.targetWalletName.ifEmpty { "Target Wallet" }
                    )
                } else {
                    ViewDetailRow(
                        icon = {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = directionColor, modifier = Modifier.size(22.dp))
                        },
                        label = "Wallet",
                        value = uiState.walletName.ifEmpty { "Default Wallet" }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))

                // Date & Time Row
                val parsedDate = DateUtils.parseDate(transaction.date)
                val formattedDate = DateUtils.formatDate(parsedDate, settings.dateFormat)
                val formattedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(parsedDate)

                ViewDetailRow(
                    icon = {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = directionColor, modifier = Modifier.size(20.dp))
                    },
                    label = if (settings.hideTime) "Date" else "Date & Time",
                    value = if (settings.hideTime) formattedDate else "$formattedDate at $formattedTime"
                )

                if (uiState.people.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))
                    ViewDetailRow(
                        icon = { Icon(Icons.Default.People, contentDescription = null, tint = directionColor, modifier = Modifier.size(20.dp)) },
                        label = "People",
                        value = uiState.people.joinToString { it.name }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Secondary Details Card (Matching Edit Screen Card 2)
        val hasSecondaryContent = uiState.place != null ||
                uiState.event != null ||
                !transaction.note.isNullOrEmpty() ||
                uiState.attachments.isNotEmpty() ||
                !settings.hideStatusAndImpact

        if (hasSecondaryContent) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column {
                    var hasPreviousRow = false

                    uiState.place?.let { place ->
                        ViewDetailRow(
                            icon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = directionColor, modifier = Modifier.size(20.dp)) },
                            label = "Place",
                            value = place.name
                        )
                        hasPreviousRow = true
                    }

                    uiState.event?.let { event ->
                        if (hasPreviousRow) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))
                        }
                        ViewDetailRow(
                            icon = { Icon(Icons.Default.Event, contentDescription = null, tint = directionColor, modifier = Modifier.size(20.dp)) },
                            label = "Event",
                            value = event.name
                        )
                        hasPreviousRow = true
                    }

                    if (!transaction.note.isNullOrEmpty()) {
                        if (hasPreviousRow) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))
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
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))
                        }
                        ViewDetailRow(
                            icon = { Icon(Icons.Default.AttachFile, contentDescription = null, tint = directionColor, modifier = Modifier.size(20.dp)) },
                            label = "Attachments",
                            value = "${uiState.attachments.size} files"
                        )
                        hasPreviousRow = true
                    }

                    if (!settings.hideStatusAndImpact) {
                        if (hasPreviousRow) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))
                        }

                        // Status Row with Badge
                        ViewDetailRow(
                            icon = { Icon(Icons.Default.Info, contentDescription = null, tint = directionColor, modifier = Modifier.size(20.dp)) },
                            label = "Status",
                            value = if (transaction.confirmed) "Confirmed" else "Pending",
                            trailingBadge = {
                                Surface(
                                    shape = CircleShape,
                                    color = if (transaction.confirmed) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, if (transaction.confirmed) Color(0xFF10B981).copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = if (transaction.confirmed) "Confirmed" else "Pending",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (transaction.confirmed) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))

                        // Impact Row with Badge
                        ViewDetailRow(
                            icon = { Icon(Icons.Default.Tune, contentDescription = null, tint = directionColor, modifier = Modifier.size(20.dp)) },
                            label = "Impact",
                            value = if (transaction.countInTotal) "Included in Total" else "Excluded from Total",
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
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
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

@Composable
private fun ViewDetailRow(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    trailingBadge: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (trailingBadge != null) {
            Box(modifier = Modifier.padding(top = 2.dp)) {
                trailingBadge()
            }
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp
            )
        }
    }
}
