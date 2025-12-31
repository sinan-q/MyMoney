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

@Composable
fun CategoryIcon(
    iconString: String?,
    categoryName: String,
    modifier: Modifier = Modifier
) {
    val iconData = remember(iconString, categoryName) {
        parseIconData(iconString, categoryName)
    }
    
    Box(
        modifier = modifier
            .background(iconData.color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = iconData.text,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
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
            val json = org.json.JSONObject(iconString)
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
    val hue = kotlin.math.abs(hash % 360).toFloat()
    return Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.6f, 0.8f)))
}
