package com.sinxn.mymoney.core.ui.components

import android.graphics.Color.HSVToColor
import android.graphics.Color.colorToHSV
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.sinxn.mymoney.core.data.local.model.WalletWithBalance
import com.sinxn.mymoney.core.util.MoneyFormatter

@Composable
fun WalletHeader(
    wallet: WalletWithBalance? = null,
    formatterConfig: MoneyFormatter.Config? = null,
    isExpanded: Boolean = false,
    onToggleExpand: () -> Unit = {},
    onMenuClick: (() -> Unit)? = null,
    viewModel: WalletHeaderViewModel = hiltViewModel()
) {
    val currentWalletState by viewModel.currentWallet.collectAsState()
    val defaultFormatterConfig by viewModel.formatterConfig.collectAsState()

    val effectiveWallet = wallet ?: currentWalletState ?: return
    val effectiveConfig = formatterConfig ?: defaultFormatterConfig

    val baseColor = remember(effectiveWallet.wallet.name) { generateColor(effectiveWallet.wallet.name) }
    val secondaryColor = remember(baseColor) { 
        // Derive a darker/different hue for gradient
        Color(HSVToColor(FloatArray(3).apply {
            colorToHSV(baseColor.toArgb(), this)
            this[2] *= 0.7f // Darken
            this[0] = (this[0] + 30) % 360 // Shift hue
        }))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(baseColor, secondaryColor)
                )
            )
    ) {
        // Subtle decorative background circles for "Premium" look
        Canvas(
            modifier = Modifier
                .size(150.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 40.dp, y = 40.dp)
        ) {
            drawCircle(
                color = Color.White.copy(alpha = 0.1f),
                radius = size.minDimension
            )
        }

        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onMenuClick != null) {
                        IconButton(
                            onClick = onMenuClick,
                            modifier = Modifier.size(36.dp).padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = Color.White
                            )
                        }
                    }
                    Column {
                        Text(
                            text = effectiveWallet.wallet.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Current Balance",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                // Dropdown Pill (Toggles list below replacing transaction list, same as sidebar)
                Surface(
                    onClick = onToggleExpand,
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = effectiveWallet.wallet.currency,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = "Toggle wallet list",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val formattedBalance = MoneyFormatter.format(
                amount = effectiveWallet.currentBalance,
                currencyCode = effectiveWallet.wallet.currency,
                decimals = effectiveWallet.decimals,
                config = effectiveConfig
            )
            
            // Large Bold Balance
            Text(
                text = if (effectiveWallet.isTotalValid) formattedBalance else "Multi-Currency",
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
                fontWeight = FontWeight.Black,
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
                    text = effectiveWallet.wallet.note!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    fontStyle = FontStyle.Italic
                )
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
            Surface(
                shape = CircleShape,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = wallet.wallet.name.take(1).uppercase(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

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


