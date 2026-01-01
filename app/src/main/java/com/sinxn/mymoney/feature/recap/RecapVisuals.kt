package com.sinxn.mymoney.feature.recap

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// --- Particles ---

class Particle(
    val x: Float,
    val y: Float,
    val size: Float,
    val speed: Float,
    val angle: Float,
    val color: Color
)

@Composable
fun ParticleEffect(modifier: Modifier = Modifier) {
    val particles = remember {
        List(50) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextFloat() * 4 + 1,
                speed = Random.nextFloat() * 0.002f + 0.001f,
                angle = Random.nextFloat() * 2 * PI.toFloat(),
                color = Color.White.copy(alpha = Random.nextFloat() * 0.5f)
            )
        }
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        particles.forEach { particle ->
            // Simple movement logic
            val currentX = (particle.x + cos(particle.angle) * particle.speed * (time * 10000 % 1000)).toFloat() % 1f
            val currentY = (particle.y + sin(particle.angle) * particle.speed * (time * 10000 % 1000)).toFloat() % 1f
            
            // Wrap around
            val xPos = if (currentX < 0) width + currentX * width else currentX * width
            val yPos = if (currentY < 0) height + currentY * height else currentY * height
            
            drawCircle(
                color = particle.color,
                radius = particle.size,
                center = Offset(xPos, yPos)
            )
        }
    }
}


// --- Confetti ---

data class Confetti(
    var x: Float,
    var y: Float,
    var speedX: Float,
    var speedY: Float,
    val color: Color,
    val rotationSpeed: Float,
    var rotation: Float = 0f
)

@Composable
fun ConfettiExplosion(modifier: Modifier = Modifier) {
    val confettiList = remember {
        val colors = listOf(
            Color(0xFF64FFDA), Color(0xFFFFD54F), Color(0xFFFF4081), Color(0xFF00E5FF), Color.White
        )
        List(100) {
            Confetti(
                x = 0.5f,
                y = 0.5f, // Start from center
                speedX = (Random.nextFloat() - 0.5f) * 0.05f,
                speedY = (Random.nextFloat() - 0.5f) * 0.05f,
                color = colors.random(),
                rotationSpeed = (Random.nextFloat() - 0.5f) * 10f
            )
        }
    }
    
    var time by remember { mutableLongStateOf(0L) }
    
    LaunchedEffect(Unit) {
        val startTime = withFrameNanos { it }
        while (isActive) {
            withFrameNanos { frameTime ->
                time = frameTime - startTime
            }
        }
    }
    
    Canvas(modifier = modifier) {
        val currentDrawTime = time // Force invalidation
        val width = size.width
        val height = size.height
        
        // Physics simulation step (simplified)
        // In a real game engine this would be uniform, here it's tied to frame updates
        
        confettiList.forEach { p ->
            // Update position
            p.x += p.speedX
            p.y += p.speedY
            p.rotation += p.rotationSpeed
            
            // Gravity
            p.speedY += 0.001f
            
            // Draw
            val xPos = p.x * width
            val yPos = p.y * height
            
            if (xPos in 0f..width && yPos in 0f..height + 100) { // Allow falling off screen
                 withTransform({
                     translate(left = xPos, top = yPos)
                     rotate(degrees = p.rotation)
                 }) {
                     drawRect(
                         color = p.color,
                         topLeft = Offset(-10f, -5f),
                         size = androidx.compose.ui.geometry.Size(20f, 10f)
                     )
                 }
            }
        }
    }
}
