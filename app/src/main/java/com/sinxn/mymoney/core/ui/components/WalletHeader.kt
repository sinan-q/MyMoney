package com.sinxn.mymoney.core.ui.components

import android.graphics.Color.HSVToColor
import android.graphics.Color.colorToHSV
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Menu
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
import com.sinxn.mymoney.core.data.local.model.WalletWithBalance
import com.sinxn.mymoney.core.util.MoneyFormatter

@Composable
fun WalletHeader(
    wallet: WalletWithBalance,
    formatterConfig: MoneyFormatter.Config,
    isExpanded: Boolean = false,
    onToggleExpand: () -> Unit = {},
    onMenuClick: (() -> Unit)? = null
) {
    val baseColor = remember(wallet.wallet.name) { generateColor(wallet.wallet.name) }
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
                            text = wallet.wallet.name,
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
                            text = wallet.wallet.currency,
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
                amount = wallet.currentBalance,
                currencyCode = wallet.wallet.currency,
                decimals = wallet.decimals,
                config = formatterConfig
            )
            
            // Large Bold Balance
            Text(
                text = if (wallet.isTotalValid) formattedBalance else "Multi-Currency",
                style = if (wallet.isTotalValid) {
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

            if (!wallet.isTotalValid) {
                if (!wallet.balanceBreakdown.isNullOrEmpty()) {
                    Text(
                        text = wallet.balanceBreakdown,
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
            
            if (!wallet.wallet.note.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = wallet.wallet.note!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}

