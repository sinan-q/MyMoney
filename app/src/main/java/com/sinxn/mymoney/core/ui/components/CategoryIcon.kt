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

    val fontSize = remember(iconData.text) {
        when {
            iconData.text.length <= 1 -> 16.sp
            iconData.text.length == 2 -> 13.sp
            else -> 11.sp
        }
    }

    val defaultStyle = MaterialTheme.typography.titleMedium
    val textStyle = remember(defaultStyle, fontSize) {
        defaultStyle.copy(
            fontSize = fontSize,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeight = fontSize,
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
            softWrap = false
        )
    }
}

data class IconData(val color: Color, val text: String)

private val iconDataCache = java.util.concurrent.ConcurrentHashMap<String, IconData>()
private val colorCache = java.util.concurrent.ConcurrentHashMap<String, Color>()

fun parseIconData(iconString: String?, categoryName: String): IconData {
    val cacheKey = "${iconString.orEmpty()}__$categoryName"
    iconDataCache[cacheKey]?.let { return it }

    val defaultText = categoryName.firstOrNull()?.toString()?.uppercase() ?: "?"
    val defaultColor = generateColor(categoryName)

    if (iconString.isNullOrEmpty()) {
        val result = IconData(defaultColor, defaultText)
        iconDataCache[cacheKey] = result
        return result
    }

    try {
        val trimmed = iconString.trim()
        if (trimmed.startsWith("{")) {
            val json = JSONObject(trimmed)
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
                val result = IconData(color, text)
                iconDataCache[cacheKey] = result
                return result
            }
        }
    } catch (e: Exception) {
    }
    
    val result = IconData(defaultColor, defaultText)
    iconDataCache[cacheKey] = result
    return result
}

fun generateColor(name: String): Color {
    colorCache[name]?.let { return it }
    val hash = name.hashCode()
    val hue = abs(hash % 360).toFloat()
    val color = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.6f, 0.8f)))
    colorCache[name] = color
    return color
}

