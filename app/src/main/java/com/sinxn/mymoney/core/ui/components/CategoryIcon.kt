package com.sinxn.mymoney.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawWithContent
import org.json.JSONObject
import kotlin.math.abs

import androidx.compose.foundation.layout.padding

import androidx.compose.ui.text.PlatformTextStyle

import androidx.compose.ui.text.style.LineHeightStyle

@Composable
fun CategoryIcon(
    iconString: String?,
    categoryName: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White
) {
    val iconData = remember(iconString, categoryName) {
        parseIconData(iconString, categoryName)
    }
    
    val defaultStyle = MaterialTheme.typography.titleMedium
    var textSize by remember { mutableStateOf(16.sp) }
    
    val textStyle = remember(defaultStyle, textSize) {
        defaultStyle.copy(
            fontSize = textSize,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeight = textSize,
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.Both
            )
        )
    }

    Box(
        modifier = modifier
            .background(iconData.color, CircleShape)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = iconData.text,
            style = textStyle,
            color = textColor,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            onTextLayout = { textLayoutResult ->
                if (textLayoutResult.didOverflowWidth || textLayoutResult.didOverflowHeight) {
                    val newSize = textSize * 0.9f
                    if (newSize.value > 6f) {
                        textSize = newSize
                    }
                }
            }
        )
    }
}

data class IconData(val color: Color, val text: String)

fun parseIconData(iconString: String?, categoryName: String): IconData {
    val defaultText = categoryName.firstOrNull()?.toString()?.uppercase() ?: "?"
    val defaultColor = generateColor(categoryName)

    if (iconString.isNullOrEmpty()) {
        return IconData(defaultColor, defaultText)
    }

    try {
        if (iconString.trim().startsWith("{")) {
            val json = JSONObject(iconString)
            val type = json.optString("type")
            
            if (type == "color") {
                val colorHex = json.optString("color")
                val name = json.optString("name")
                
                val color = if (colorHex.isNotEmpty()) {
                    try {
                         Color(android.graphics.Color.parseColor(colorHex))
                    } catch (e: Exception) { defaultColor }
                } else defaultColor
                
                val text = name.ifEmpty { defaultText }
                return IconData(color, text)
            }
        }
    } catch (e: Exception) {
    }
    
    return IconData(defaultColor, defaultText)
}

fun generateColor(name: String): Color {
    val hash = name.hashCode()
    val hue = abs(hash % 360).toFloat()
    return Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.6f, 0.8f)))
}
