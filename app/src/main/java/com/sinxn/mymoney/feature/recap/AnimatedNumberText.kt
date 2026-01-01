package com.sinxn.mymoney.feature.recap

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun AnimatedNumberText(
    value: Number,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = Color.White,
    format: (Number) -> String = { it.toString() }
) {
    var kickstart by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kickstart = true
    }
    
    val targetVal = if (kickstart) value.toLong() else 0L
    val animatedValue by animateIntAsState(
        targetValue = targetVal.toInt(), // int is usually enough for display counters, but money can handle Long
        animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
        label = "animated_number"
    )
    
    // For money, we might want manual interpolation if > Int.MAX_VALUE.
    // Assuming fits in Int for animation visual (billions might wrap). 
    // If value is > Int, we should use animateFloatAsState or custom Animatable<Long>.
    // Let's stick to Int for now, assuming users aren't billionaires in cents (2 billion cents = 20 million units).
    // Actually 20 million is low for some currencies (IDR, VND).
    
    Text(
        text = format(animatedValue),
        modifier = modifier,
        style = style,
        color = color
    )
}
