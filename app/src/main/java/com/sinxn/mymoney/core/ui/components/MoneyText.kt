package com.sinxn.mymoney.core.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.sinxn.mymoney.core.ui.LocalFormatterConfig
import com.sinxn.mymoney.core.util.MoneyFormatter

@Composable
fun MoneyText(
    amount: Long,
    currencyCode: String,
    modifier: Modifier = Modifier,
    decimals: Int = 2,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    tintMode: MoneyFormatter.TintMode? = null,
    config: MoneyFormatter.Config = LocalFormatterConfig.current
) {
    if (tintMode != null) {
        val coloredText = remember(amount, currencyCode, decimals, config, tintMode) {
            MoneyFormatter.formatColored(
                amount = amount,
                currencyCode = currencyCode,
                decimals = decimals,
                config = config,
                tintMode = tintMode
            )
        }
        Text(
            text = coloredText,
            modifier = modifier,
            style = style,
            fontWeight = fontWeight
        )
    } else {
        val text = remember(amount, currencyCode, decimals, config) {
            MoneyFormatter.format(
                amount = amount,
                currencyCode = currencyCode,
                decimals = decimals,
                config = config
            )
        }
        Text(
            text = text,
            modifier = modifier,
            style = style,
            color = color,
            fontWeight = fontWeight
        )
    }
}
