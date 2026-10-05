package com.sinxn.mymoney.core.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sinxn.mymoney.core.data.local.model.WalletWithBalance
import com.sinxn.mymoney.core.ui.LocalFormatterConfig
import com.sinxn.mymoney.core.util.Constants
import com.sinxn.mymoney.core.util.MoneyFormatter

@Composable
fun WalletHeader(
    modifier: Modifier = Modifier,
    wallet: WalletWithBalance? = null,
    formatterConfig: MoneyFormatter.Config = LocalFormatterConfig.current,
    isExpanded: Boolean = false,
    onToggleExpand: () -> Unit = {},
    isReduced: Boolean = false,
    viewModel: WalletHeaderViewModel = hiltViewModel()
) {
    val currentWalletState by viewModel.currentWallet.collectAsState()

    val effectiveWallet = wallet ?: currentWalletState ?: return
    val effectiveConfig = formatterConfig

    val baseColor = remember(effectiveWallet.wallet.icon, effectiveWallet.wallet.name) {
        parseIconData(effectiveWallet.wallet.icon, effectiveWallet.wallet.name).color
    }

    val animatedInnerPadding by animateDpAsState(
        targetValue = if (isReduced) 12.dp else 20.dp,
        label = "walletHeaderInnerPadding"
    )

    val formattedBalance = remember(effectiveWallet, effectiveConfig) {
        MoneyFormatter.format(
            amount = effectiveWallet.currentBalance,
            currencyCode = effectiveWallet.wallet.currency,
            decimals = effectiveWallet.decimals,
            config = effectiveConfig
        )
    }

    val formattedBalanceAnnotated = remember(formattedBalance) {
        MoneyFormatter.formatBalanceWithNonBoldDecimals(formattedBalance)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            //.padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            //.background(color = baseColor)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .clickable { onToggleExpand() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = animatedInnerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row (modifier = Modifier.weight(1f, fill = false), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    CategoryIcon(
                        iconString = effectiveWallet.wallet.icon,
                        categoryName = effectiveWallet.wallet.name
                    )
                    Text(
                        text = effectiveWallet.wallet.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                }
                if (isReduced) {
                    Row(Modifier.fillMaxWidth().weight(1f, fill =false), horizontalArrangement = Arrangement.End) {
                        Text(
                            text = if (effectiveWallet.isTotalValid) formattedBalanceAnnotated else AnnotatedString(
                                "Multi-Currency"
                            ),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1
                        )
                    }

                }
            }
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {

//
//                    // Dropdown Pill
//                    Surface(
//                        onClick = onToggleExpand,
//                        shape = RoundedCornerShape(16.dp),
//                    ) {
//                        Row(
//                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
//                            verticalAlignment = Alignment.CenterVertically,
//                            horizontalArrangement = Arrangement.spacedBy(2.dp)
//                        ) {
//                            Icon(
//                                imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
//                                contentDescription = "Toggle wallet list",
//                                modifier = Modifier.size(16.dp)
//                            )
//                        }
//                    }
//                }
            }

            if (!isReduced) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (effectiveWallet.isTotalValid) formattedBalanceAnnotated else AnnotatedString("Multi-Currency"),
                    style = if (effectiveWallet.isTotalValid) {
                        MaterialTheme.typography.displayMedium.copy(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.1f),
                                offset = Offset(2f, 4f),
                                blurRadius = 8f
                            )
                        )
                    } else {
                        MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                )
            }

            if (!isReduced) {
                if (effectiveWallet.balanceBreakdown != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = effectiveWallet.balanceBreakdown!!,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                if (!effectiveWallet.isTotalValid) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Currencies: ${effectiveWallet.wallet.currency}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                if (!effectiveWallet.wallet.note.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = effectiveWallet.wallet.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        }
    }
}

@Composable
fun WalletDropdownList(
    onWalletSelect: (WalletWithBalance) -> Unit,
    onAddWallet: () -> Unit,
    onManageWallets: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(bottom = 24.dp),
    viewModel: WalletHeaderViewModel = hiltViewModel()
) {
    val activeWallets by viewModel.activeWallets.collectAsState()
    val archivedWallets by viewModel.archivedWallets.collectAsState()
    val totalWallet by viewModel.totalWallet.collectAsState()
    val selectedWalletId by viewModel.currentWalletId.collectAsState()
    val formatterConfig by viewModel.formatterConfig.collectAsState()

    var isArchivedExpanded by remember(archivedWallets, selectedWalletId) {
        mutableStateOf(archivedWallets.any { it.wallet.id == selectedWalletId })
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding
    ) {
        // 1. Active Wallets
        items(activeWallets, key = { "active_wallet_${it.wallet.id}" }) { wallet ->
            WalletProfileRow(
                wallet = wallet,
                isSelected = selectedWalletId == wallet.wallet.id,
                formatterConfig = formatterConfig,
                onClick = { onWalletSelect(wallet) }
            )
        }

        // 2. Total Wallet (at the end after active wallets)
        totalWallet?.let { total ->
            item(key = "wallet_${total.wallet.id}") {
                WalletProfileRow(
                    wallet = total,
                    isSelected = selectedWalletId == total.wallet.id,
                    formatterConfig = formatterConfig,
                    onClick = { onWalletSelect(total) }
                )
            }
        }

        // 3. Archived Wallets Dropdown (under another dropdown at the end after the total wallet)
        if (archivedWallets.isNotEmpty()) {
            item(key = "archived_wallets_header") {
                ArchivedDropdownHeaderRow(
                    count = archivedWallets.size,
                    isExpanded = isArchivedExpanded,
                    onClick = { isArchivedExpanded = !isArchivedExpanded }
                )
            }

            if (isArchivedExpanded) {
                items(archivedWallets, key = { "archived_wallet_${it.wallet.id}" }) { wallet ->
                    WalletProfileRow(
                        wallet = wallet,
                        isSelected = selectedWalletId == wallet.wallet.id,
                        formatterConfig = formatterConfig,
                        onClick = { onWalletSelect(wallet) }
                    )
                }
            }
        }

        // 4. Action Items
        item(key = "wallet_dropdown_actions") {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            ActionProfileRow(
                title = "New wallet",
                icon = Icons.Default.Add,
                onClick = onAddWallet
            )
            ActionProfileRow(
                title = "Manage wallets",
                icon = Icons.Default.Settings,
                onClick = onManageWallets
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}

@Composable
fun ArchivedDropdownHeaderRow(
    count: Int,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Archive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = "Archived ($count)",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = if (isExpanded) "Collapse archived" else "Expand archived",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun WalletProfileRow(
    wallet: WalletWithBalance,
    isSelected: Boolean,
    formatterConfig: MoneyFormatter.Config = LocalFormatterConfig.current,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIcon(
                iconString = wallet.wallet.icon,
                categoryName = wallet.wallet.name,
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = wallet.wallet.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            val formattedBalance = if (!wallet.isTotalValid && wallet.wallet.id == Constants.TOTAL_WALLET_ID) {
                wallet.balanceBreakdown ?: "Multi-Currency"
            } else {
                MoneyFormatter.format(
                    amount = wallet.currentBalance,
                    currencyCode = wallet.wallet.currency,
                    decimals = wallet.decimals,
                    config = formatterConfig
                )
            }

            Text(
                text = formattedBalance,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ActionProfileRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}


