package com.sinxn.mymoney.feature.recap

import android.media.MediaPlayer
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.LocalTaxi
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Subway
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// --- Data Models ---

enum class RecapSlideType {
    INTRO,
    TOTAL_SPENT,
    WEEKEND_WARRIOR,
    COFFEE_METRIC,
    FOODIE_FACTOR,
    TRAVEL_STATS,    // New
    MOVIE_STATS,     // New
    LIFESTYLE_STATS, // New
    TOP_CATEGORIES_LIST,
    TOP_SPENT_TIMES,
    ROLLERCOASTER,
    LONGEST_STREAK,
    TOP_CATEGORY,
    BUSIEST_DAY,
    TOP_CONNECTION,
    OUTRO
}

data class RecapData(
    val year: Int = 2025,
    val totalSpent: String = "₹4,52,000",
    val topCategoryName: String = "Indian Oil",
    val topCategoryAmount: String = "₹2,250",
    val topCategoryCount: Int = 9,
    val busiestDay: String = "Thursday",
    val busiestDayCount: Int = 45,
    val topConnectionName: String = "Muhammed Jaseel P",
    val topConnectionCount: Int = 20,
    val weekendPercentage: Int = 65,
    val coffeeMetricLowCount: Int = 142,
    val coffeeMetricHighCount: Int = 12,
    val foodiePercentage: Int = 40,
    val longestStreakDays: Int = 15,
    val rollercoasterMonth: String = "December",
    val rollercoasterAmount: String = "₹85,000",
    val topCategories: List<Pair<String, String>> = listOf("Food" to "₹1.2L", "Travel" to "₹80k", "Gadgets" to "₹50k"),
    val topMonth: String = "October",
    val topWeek: String = "Nov 12-19",
    val topDayDate: String = "Oct 25",
    // New Params
    val travelTopSubcategory: String = "Metro",
    val travelTopSubcategoryCount: Int = 140,
    val travelStats: Map<String, Int> = mapOf("Metro" to 140, "Uber" to 12, "Bus" to 45, "Train" to 8),
    val movieHours: Int = 36,
    val movieCount: Int = 14,
    val lifestyleAmount: String = "₹45,000"
)

// --- Main Screen ---

@Composable
fun YearRecapScreen(
    data: RecapData = RecapData(),
    onClose: () -> Unit
) {
    // Premium Dark Theme Override
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            surface = Color(0xFF101010),
            onSurface = Color.White
        )
    ) {
        var currentSlideIndex by remember { mutableIntStateOf(0) }
        val slides = RecapSlideType.entries.toTypedArray()
        var isPaused by remember { mutableStateOf(false) }

        // Audio Playback
        val context = androidx.compose.ui.platform.LocalContext.current
        val mediaPlayer = remember {
            MediaPlayer.create(context, com.sinxn.mymoney.R.raw.recap_bg_music).apply {
                isLooping = true
            }
        }

        DisposableEffect(Unit) {
            mediaPlayer?.start()
            onDispose {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            }
        }

        LaunchedEffect(isPaused) {
            if (isPaused) {
                if (mediaPlayer?.isPlaying == true) mediaPlayer?.pause()
            } else {
                if (mediaPlayer?.isPlaying == false) mediaPlayer?.start()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Background Animation (Global)
            AnimatedBackground(currentSlideIndex)

            // Main Content Area
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPaused = true
                                tryAwaitRelease()
                                isPaused = false
                            },
                            onTap = { offset ->
                                if (offset.x < size.width / 2) {
                                    // Previous
                                    if (currentSlideIndex > 0) currentSlideIndex--
                                } else {
                                    // Next
                                    if (currentSlideIndex < slides.lastIndex) {
                                        currentSlideIndex++
                                    } else {
                                        onClose()
                                    }
                                }
                            }
                        )
                    }
            ) {
                // Progress Bars
                StoryProgressHeader(
                    totalSlides = slides.size,
                    currentIndex = currentSlideIndex,
                    isPaused = isPaused,
                    onSlideComplete = {
                        if (currentSlideIndex < slides.lastIndex) {
                            currentSlideIndex++
                        } else {
                            onClose()
                        }
                    }
                )

                // Slide Content with Slide Transition
                AnimatedContent(
                    targetState = slides[currentSlideIndex],
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(500)) + 
                         slideInVertically { height -> height / 10 }).togetherWith(
                        fadeOut(animationSpec = tween(500)) + 
                        slideOutVertically { height -> -height / 10 })
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    label = "SlideTransition"
                ) { slide ->
                    when (slide) {
                        RecapSlideType.INTRO -> IntroSlide(data.year)
                        RecapSlideType.TOTAL_SPENT -> TotalSpentSlide(data.totalSpent)
                        RecapSlideType.WEEKEND_WARRIOR -> WeekendWarriorSlide(data)
                        RecapSlideType.COFFEE_METRIC -> CoffeeMetricSlide(data)
                        RecapSlideType.FOODIE_FACTOR -> FoodieFactorSlide(data)
                        RecapSlideType.TRAVEL_STATS -> TravelStatsSlide(data)
                        RecapSlideType.MOVIE_STATS -> MovieStatsSlide(data)
                        RecapSlideType.LIFESTYLE_STATS -> LifestyleStatsSlide(data)
                        RecapSlideType.TOP_CATEGORIES_LIST -> TopCategoriesSlide(data)
                        RecapSlideType.TOP_SPENT_TIMES -> TopSpentTimesSlide(data)
                        RecapSlideType.ROLLERCOASTER -> RollercoasterSlide(data)
                        RecapSlideType.LONGEST_STREAK -> LongestStreakSlide(data)
                        RecapSlideType.TOP_CATEGORY -> TopCategorySlide(data)
                        RecapSlideType.BUSIEST_DAY -> BusiestDaySlide(data)
                        RecapSlideType.TOP_CONNECTION -> TopConnectionSlide(data)
                        RecapSlideType.OUTRO -> OutroSlide()
                    }
                }
            }

            // Close Button
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ... existing components ...

// --- New Slides ---

@Composable
fun WeekendWarriorSlide(data: RecapData) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text(
                text = if(data.weekendPercentage > 50) "THE WEEKEND" else "THE WEEKDAY",
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 2.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
            Text(
                text = if(data.weekendPercentage > 50) "WARRIOR" else "WORKER",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Black
                ),
                color = Color(0xFFFF9800)
            )
            Spacer(modifier = Modifier.height(48.dp))
            
            // Tug of War Visualization
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Mon-Fri", color = Color.White, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                // Bar
                Box(
                    modifier = Modifier
                        .weight(3f)
                        .height(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .fillMaxWidth(data.weekendPercentage / 100f) // Simplified representation
                            .align(Alignment.CenterEnd)
                            .background(Color(0xFFFF9800))
                    )
                }
                Text("Sat-Sun", color = Color.White, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "${data.weekendPercentage}% of your spending happened on weekends.",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                color = Color.White
            )
        }
    }
}

@Composable
fun CoffeeMetricSlide(data: RecapData) {
     Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text(
                text = "THE COFFEE METRIC",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(48.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                // Small Cup
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.ShoppingBag, // Placeholder for Cup
                        contentDescription = "Small",
                        modifier = Modifier.size(40.dp),
                        tint = Color(0xFFBCAAA4)
                    )
                    Text("${data.coffeeMetricLowCount}", style = MaterialTheme.typography.displayMedium, color = Color.White)
                    Text("< ₹50", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha=0.5f))
                }
                
                // Big Cup
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.Star, // Placeholder for Luxury
                        contentDescription = "Big",
                        modifier = Modifier.size(80.dp),
                        tint = Color(0xFFFFD700)
                    )
                    Text("${data.coffeeMetricHighCount}", style = MaterialTheme.typography.displayMedium, color = Color.White)
                    Text("> ₹500", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha=0.5f))
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "You love the little things!",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun FoodieFactorSlide(data: RecapData) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
         Box(
            modifier = Modifier
                .size(300.dp)
                .background(Color.White.copy(alpha = 0.1f), CircleShape),
             contentAlignment = Alignment.Center
         ) {
             Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(Color(0xFFFF5722), CircleShape) // Pizza Sauce Color
             )
         }
         
         Column(horizontalAlignment = Alignment.CenterHorizontally) {
             Text(
                 text = "FOODIE FACTOR",
                 style = MaterialTheme.typography.labelLarge,
                 color = Color.White
             )
             Text(
                 text = "${data.foodiePercentage}%",
                 style = MaterialTheme.typography.displayLarge.copy(
                     fontSize = 100.sp,
                     fontWeight = FontWeight.Black
                 ),
                 color = Color.White
             )
             Text(
                 text = "of your income went to satisfying your cravings.",
                 style = MaterialTheme.typography.bodyLarge,
                 color = Color.White.copy(alpha = 0.8f),
                 modifier = Modifier.padding(horizontal = 32.dp),
                 textAlign = TextAlign.Center
             )
         }
    }
}

@Composable
fun TopCategoriesSlide(data: RecapData) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
        Column(modifier = Modifier.padding(24.dp).padding(top = 64.dp)) {
            Text("THE BIG THREE", style = MaterialTheme.typography.displayMedium.copy(fontFamily = FontFamily.Serif), color = Color.White)
            Spacer(modifier = Modifier.height(32.dp))
            
            data.topCategories.forEachIndexed { index, (name, amount) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "#${index + 1}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.width(48.dp)
                        )
                        Text(
                            text = name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = amount,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF64FFDA)
                    )
                }
                androidx.compose.material3.HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            }
        }
    }
}

@Composable
fun TopSpentTimesSlide(data: RecapData) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally, 
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            TimeStatItem("Top Month", data.topMonth)
            TimeStatItem("Top Week", data.topWeek)
            TimeStatItem("Top Day", data.topDayDate)
        }
    }
}

@Composable
fun TimeStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.6f))
        Text(value, style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
    }
}

@Composable
fun RollercoasterSlide(data: RecapData) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            val path = androidx.compose.ui.graphics.Path()
            path.moveTo(0f, size.height)
            path.cubicTo(
                size.width * 0.2f, size.height * 0.8f,
                size.width * 0.4f, size.height * 0.9f,
                size.width * 0.5f, size.height * 0.2f // The Peak
            )
            path.cubicTo(
                size.width * 0.6f, size.height * 0.1f,
                size.width * 0.8f, size.height * 0.8f,
                size.width, size.height * 0.5f
            )
            drawPath(path, color = Color(0xFFFF4081), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8.dp.toPx()))
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
             Text(
                text = "ROLLERCOASTER MONTH",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = data.rollercoasterMonth,
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
                color = Color.White
            )
            Text(
                text = "With ${data.rollercoasterAmount} spent!",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun LongestStreakSlide(data: RecapData) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
         Column(horizontalAlignment = Alignment.CenterHorizontally) {
             Text(
                 text = "${data.longestStreakDays} DAYS",
                 style = MaterialTheme.typography.displayLarge.copy(fontSize = 100.sp, fontWeight = FontWeight.Bold),
                 color = Color(0xFF00E676)
             )
             Text(
                 text = "SPENDING STREAK",
                 style = MaterialTheme.typography.headlineSmall,
                 color = Color.White
             )
        }
    }
}

// --- Components ---

@Composable
fun StoryProgressHeader(
    totalSlides: Int,
    currentIndex: Int,
    isPaused: Boolean,
    onSlideComplete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 8.dp, end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (i in 0 until totalSlides) {
            StoryProgressBar(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp),
                index = i,
                currentIndex = currentIndex,
                isPaused = isPaused,
                onCompleted = onSlideComplete
            )
        }
    }
}

@Composable
fun StoryProgressBar(
    modifier: Modifier = Modifier,
    index: Int,
    currentIndex: Int,
    isPaused: Boolean,
    onCompleted: () -> Unit
) {
    // Isolated state for each bar
    val progress = remember { Animatable(0f) }

    LaunchedEffect(index, currentIndex, isPaused) {
        if (index < currentIndex) {
            progress.snapTo(1f)
        } else if (index > currentIndex) {
            progress.snapTo(0f)
        } else {
            // Current Slide Logic
            if (!isPaused) {
                // Determine if we need to restart or continue
                // If it was already full (e.g. from previous cycle?), reset.
                // Or if we interpret "resume" properly.
                
                // If value is exactly 1f and we are now 'current' again (unlikely unless navigating back),
                // we should probably reset.
                // However, if we paused at 0.5f, we want to continue.
                
                if (progress.value >= 1f) {
                    progress.snapTo(0f)
                }
                
                val duration = (5000 * (1f - progress.value)).toInt()
                if (duration > 0) {
                     progress.animateTo(
                         targetValue = 1f,
                         animationSpec = tween(durationMillis = duration, easing = LinearEasing)
                     )
                     onCompleted()
                }
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .background(Color.White.copy(alpha = 0.2f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.value)
                .background(Color.White)
        )
    }
}


// --- Slides ---

@Composable
fun IntroSlide(year: Int) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "YOUR",
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 4.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$year",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                modifier = Modifier.scale(1.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "UNWRAPPED",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black
                ),
                color = Color(0xFFFF5757) // Jupiter-ish Red/Orange accent
            )
        }
    }
}

@Composable
fun TotalSpentSlide(amount: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "This year, you spent a grand total of...",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = amount,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = Color.White,
                lineHeight = 64.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "That's a lot of memories!",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun TopCategorySlide(data: RecapData) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Radar/Circle background effect matches the Jupiter screenshot
        Canvas(modifier = Modifier.size(400.dp)) {
            drawCircle(color = Color.White.copy(alpha = 0.05f), radius = size.minDimension / 2)
            drawCircle(color = Color.White.copy(alpha = 0.05f), radius = size.minDimension / 2.8f)
            drawCircle(color = Color.White.copy(alpha = 0.05f), radius = size.minDimension / 4.5f)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "You spent the most on",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = data.topCategoryName,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Icon Placeholder
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                 Icon(
                     imageVector = Icons.Rounded.ShoppingBag,
                     contentDescription = "Category",
                     modifier = Modifier.size(40.dp),
                     tint = Color.White
                 )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = data.topCategoryCount.toString(),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 120.sp,
                    fontFamily = FontFamily.Serif
                ),
                color = Color.White
            )
            Text(
                text = "TRANSACTIONS",
                style = MaterialTheme.typography.labelLarge,
                letterSpacing = 4.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(8.dp))
             Text(
                text = "worth ${data.topCategoryAmount}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun BusiestDaySlide(data: RecapData) {
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    // Fake graph data relative to the busy day
    val heights = listOf(0.4f, 0.6f, 0.3f, 1.0f, 0.8f, 0.9f, 0.5f) // Thursday is max

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "Your fingers were busiest on",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = data.busiestDay,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF64FFDA), // Teal accent
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(56.dp))

            // Graph visual
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.height(200.dp)
            ) {
                days.forEachIndexed { index, day ->
                    val isBusyDay = day == "T" && index == 3 // Simple check for Thursday
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .fillMaxSize(fraction = 0.8f * heights[index]) // reduced height for safety
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    if (isBusyDay) Color(0xFF64FFDA) else Color.White.copy(alpha = 0.2f)
                                )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isBusyDay) Color(0xFF64FFDA) else Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }
             Spacer(modifier = Modifier.height(32.dp))
             Text(
                text = "${data.busiestDayCount} Transactions",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }
    }
}

@Composable
fun TopConnectionSlide(data: RecapData) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
         // Waves background
         Canvas(modifier = Modifier.fillMaxWidth().height(300.dp)) {
             val waveColor = Color(0xFF69F0AE).copy(alpha = 0.2f)
             // Simple line drawing to mimic waves
             val width = size.width
             val height = size.height
             
             // Draw some sine waves
             for(i in 0..5) {
                 // Simplified conceptual lines
                 drawLine(
                     color = waveColor,
                     start = Offset(0f, height/2 + i * 20),
                     end = Offset(width, height/2 + i * 20 - 50),
                     strokeWidth = 2.dp.toPx()
                 )
             }
         }
         
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "Your #1 for payments this year was",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = data.topConnectionName,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                textAlign = TextAlign.Start,
                lineHeight = 48.sp
            )
            
            Spacer(modifier = Modifier.height(120.dp))
            
            Text(
                text = data.topConnectionCount.toString(),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 120.sp,
                    fontFamily = FontFamily.Serif
                ),
                color = Color.White
            )
             Text(
                text = "TRANSACTIONS",
                style = MaterialTheme.typography.labelLarge,
                letterSpacing = 4.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
             Spacer(modifier = Modifier.height(8.dp))
             Text(
                text = "between you two",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun OutroSlide() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
             Icon(
                 imageVector = Icons.Rounded.Star,
                 contentDescription = "Star",
                 modifier = Modifier.size(64.dp),
                 tint = Color(0xFFFFD700)
             )
             Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "See you in 2026!",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
        }
    }
}

// --- Specific Category Slides ---

@Composable
fun TravelStatsSlide(data: RecapData) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text("URBAN EXPLORER", style = MaterialTheme.typography.displaySmall.copy(fontFamily = FontFamily.Serif), color = Color.White)
            Spacer(modifier = Modifier.height(32.dp))
            
            // Race Track
            data.travelStats.entries.sortedByDescending { it.value }.take(4).forEachIndexed { index, (mode, count) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon based on mode
                    val icon = when(mode) {
                        "Metro" -> Icons.Rounded.Subway // Using standard icons, mapping required
                        "Uber" -> Icons.Rounded.LocalTaxi
                        "Bus" -> Icons.Rounded.DirectionsBus
                        "Train" -> Icons.Rounded.Train
                        else -> Icons.AutoMirrored.Rounded.DirectionsBike
                    }
                    
                    Icon(icon, contentDescription = mode, tint = Color.White, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Bar
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(count / data.travelTopSubcategoryCount.toFloat())
                                .background(if (index == 0) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.5f))
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("$count", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "You took the ${data.travelTopSubcategory} most often!",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                color = Color.White
            )
        }
    }
}

@Composable
fun MovieStatsSlide(data: RecapData) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Ticket Stub Visual
            Box(
                modifier = Modifier
                    .width(240.dp)
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFFFC107)) // Gold/Yellow Ticket
            ) {
                 Column(
                     modifier = Modifier.fillMaxSize(),
                     horizontalAlignment = Alignment.CenterHorizontally,
                     verticalArrangement = Arrangement.Center
                 ) {
                     Text("CINEMA TICKET", style = MaterialTheme.typography.labelSmall, color = Color.Black.copy(alpha = 0.6f))
                     Text("${data.movieCount} MOVIES", style = MaterialTheme.typography.titleLarge, color = Color.Black, fontWeight = FontWeight.Black)
                     androidx.compose.material3.HorizontalDivider(
                         modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                         color = Color.Black.copy(alpha = 0.2f),
                         thickness = 2.dp
                     )
                     Text("${data.movieHours} HOURS", style = MaterialTheme.typography.displayMedium, color = Color.Black, fontWeight = FontWeight.Black)
                 }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                text = "That's a lot of popcorn!",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
        }
    }
}

@Composable
fun LifestyleStatsSlide(data: RecapData) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
             Text("TREAT YOURSELF", style = MaterialTheme.typography.displayMedium.copy(fontFamily = FontFamily.Serif), color = Color(0xFFE040FB))
             Spacer(modifier = Modifier.height(48.dp))
             
             // Battery/Meter
             Box(
                 modifier = Modifier
                     .width(120.dp)
                     .height(200.dp)
                     .border(4.dp, Color.White, RoundedCornerShape(16.dp))
                     .padding(8.dp),
                 contentAlignment = Alignment.BottomCenter
             ) {
                 Box(
                     modifier = Modifier
                         .fillMaxWidth()
                         .fillMaxHeight(0.85f) // filled level
                         .clip(RoundedCornerShape(8.dp))
                         .background(
                             Brush.verticalGradient(
                                 colors = listOf(Color(0xFFE040FB), Color(0xFF7C4DFF))
                             )
                         )
                 )
             }
             
             Spacer(modifier = Modifier.height(24.dp))
             Text(
                 text = "You invested",
                 style = MaterialTheme.typography.bodyLarge,
                 color = Color.White.copy(alpha = 0.7f)
             )
             Text(
                 text = data.lifestyleAmount,
                 style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                 color = Color.White
             )
             Text(
                 text = "in yourself.",
                 style = MaterialTheme.typography.bodyLarge,
                 color = Color.White.copy(alpha = 0.7f)
             )
        }
    }
}

@Composable
fun AnimatedBackground(slideIndex: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    // Dynamic Gradient based on slide
    val colors = when(slideIndex) {
        0, 1 -> listOf(Color(0xFF2C3E50), Color(0xFF000000)) // Dark Blue/Black
        2 -> listOf(Color(0xFF4A00E0).copy(alpha = 0.6f), Color(0xFF8E2DE2).copy(alpha = 0.4f), Color.Black) // Purple/Pink
        3 -> listOf(Color(0xFF000000), Color(0xFF0F9B0F).copy(alpha = 0.3f)) // Dark Greenish
        else -> listOf(Color(0xFF232526), Color(0xFF414345)) // Grey/Black
    }
    
    Box(modifier = Modifier
        .fillMaxSize()
        .background(Brush.verticalGradient(colors))
    ) {
        // Overlay some "noise" or blobs for premium feel using Canvas if needed
        // For now, the distinct gradients provide the shift.
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewRecap() {
    YearRecapScreen(onClose = {})
}
