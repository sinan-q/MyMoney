package com.sinxn.mymoney.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun CategoryIcon(
    modifier: Modifier = Modifier,
    iconData: IconData,
    size: Dp = 42.dp,
    textColor: Color = Color.White
) {
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
            .size(size)
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

@Immutable
data class IconData(val color: Color, val text: String)

private val iconDataCache = java.util.concurrent.ConcurrentHashMap<Long, IconData>()
private val colorCache = java.util.concurrent.ConcurrentHashMap<Int, Color>()

@Suppress("NOTHING_TO_INLINE")
private inline fun computeCacheKey(iconString: String?, categoryName: String): Long {
    val h1 = iconString?.hashCode() ?: 0
    val h2 = categoryName.hashCode()
    return (h1.toLong() shl 32) or (h2.toLong() and 0xFFFFFFFFL)
}

fun parseIconData(iconString: String?, categoryName: String): IconData {
    val cacheKey = computeCacheKey(iconString, categoryName)
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
                        val parsedInt = colorHex.toColorInt()
                        if (parsedInt == android.graphics.Color.BLACK || parsedInt == android.graphics.Color.TRANSPARENT) {
                            defaultColor
                        } else {
                            Color(parsedInt)
                        }
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
    val hash = (fnv1a(name.trim().lowercase()) % 360u)
    colorCache[hash.toInt()]?.let { return it }
    val hue = hash.toFloat()
    val color = categoryColor(hue, false)
    colorCache[hash.toInt()] = color
    return color
}
fun categoryColor(hueDeg: Float, dark: Boolean): Color {
    val l = if (dark) 0.78f else 0.58f     // adapts to the theme
    val c = 0.13f
    val h = Math.toRadians(hueDeg.toDouble())
    return Color(l, (c * cos(h)).toFloat(), (c * sin(h)).toFloat(), 1f, ColorSpaces.Oklab)
        .convert(ColorSpaces.Srgb)
}
private fun fnv1a(s: String): UInt {
    var h = 2166136261u
    for (b in s.toByteArray()) h = (h xor (b.toInt() and 0xFF).toUInt()) * 16777619u
    return h
}

fun pickHue(used: List<Float>): Float =
    (0 until 360 step 5).map { it.toFloat() }.maxByOrNull { h ->
        used.minOfOrNull { hueDistance(h, it) } ?: 360f
    }!!

private fun hueDistance(a: Float, b: Float): Float {
    val d = abs(a - b) % 360
    return min(d, 360 - d)
}
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
    CategoryIcon(
        iconData = iconData,
        modifier = modifier,
        textColor = textColor
    )
}

@Composable
fun CategoryIconExtended(
    color: Color,
    icon: ImageVector
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color
        )
    }
}

@Composable
fun CategoryIconExtended(
    color: Color,
    text: String
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = color,
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}