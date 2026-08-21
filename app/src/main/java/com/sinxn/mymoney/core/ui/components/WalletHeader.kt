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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sinxn.mymoney.core.data.local.model.WalletWithBalance
import com.sinxn.mymoney.core.util.MoneyFormatter

@Composable
fun WalletHeader(
    modifier: Modifier = Modifier,
    wallet: WalletWithBalance? = null,
    formatterConfig: MoneyFormatter.Config? = null,
    isExpanded: Boolean = false,
    onToggleExpand: () -> Unit = {},
    isReduced: Boolean = false,
    viewModel: WalletHeaderViewModel = hiltViewModel()
) {
    val currentWalletState by viewModel.currentWallet.collectAsState()
    val defaultFormatterConfig by viewModel.formatterConfig.collectAsState()

    val effectiveWallet = wallet ?: currentWalletState ?: return
    val effectiveConfig = formatterConfig ?: defaultFormatterConfig

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
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(color = baseColor)
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
            horizontalAlignment = Alignment.Start,

        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column() {
                    Text(
                        text = effectiveWallet.wallet.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!isReduced) {
                        Text(
                            text = "Current Balance",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (isReduced) {
                        Text(
                            text = if (effectiveWallet.isTotalValid) formattedBalanceAnnotated else AnnotatedString("Multi-Currency"),
                            style = MaterialTheme.typography.titleLarge.copy(
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.15f),
                                    offset = Offset(1f, 2f),
                                    blurRadius = 4f
                                )
                            ),
                            color = Color.White,
                            maxLines = 1
                        )
                    }


                    // Dropdown Pill
                    Surface(
                        onClick = onToggleExpand,
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = "Toggle wallet list",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            if (!isReduced) {
                Spacer(modifier = Modifier.height(16.dp))
                // Large Bold Balance with normal weight decimal digits
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
                    },
                    color = Color.White,
                    letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified
                )

                if (!effectiveWallet.isTotalValid) {
                    if (!effectiveWallet.balanceBreakdown.isNullOrEmpty()) {
                        Text(
                            text = effectiveWallet.balanceBreakdown,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    Text(
                        text = "Conversion not supported yet",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (!effectiveWallet.wallet.note.isNullOrEmpty()) {
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
    val wallets by viewModel.allWallets.collectAsState()
    val selectedWalletId by viewModel.currentWalletId.collectAsState()

    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding
    ) {
        items(wallets, key = { "wallet_${it.wallet.id}" }) { wallet ->
            WalletProfileRow(
                wallet = wallet,
                isSelected = selectedWalletId == wallet.wallet.id,
                onClick = { onWalletSelect(wallet) }
            )
        }
        item {
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
fun WalletProfileRow(
    wallet: WalletWithBalance,
    isSelected: Boolean,
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

            val formattedBalance = MoneyFormatter.format(
                amount = wallet.currentBalance,
                currencyCode = wallet.wallet.currency,
                decimals = wallet.decimals
            )

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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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


