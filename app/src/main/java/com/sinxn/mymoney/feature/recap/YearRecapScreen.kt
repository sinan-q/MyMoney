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
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalTaxi
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Subway
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material.icons.rounded.LocalGasStation
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.animation.core.animateIntAsState
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
    BUSIEST_DAY,
    BIGGEST_PURCHASE,
    INCOME_EXPENSE,
    OUTRO
}

// Dummy Wallet Data for UI
data class WalletItem(
    val id: Long,
    val name: String,
    val balance: String,
    val isArchived: Boolean = false
)

val dummyWallets = listOf(
    WalletItem(1, "Main Account", "₹2,50,000"),
    WalletItem(2, "Savings", "₹1,00,000"),
    WalletItem(3, "Credit Card", "-₹25,000"),
    WalletItem(4, "Cash", "₹5,000"),
    WalletItem(5, "Old Bank (Archived)", "₹0", isArchived = true),
    WalletItem(6, "Closed Card (Archived)", "₹0", isArchived = true)
)

data class RecapData(
    val year: Int = 2025,
    val totalSpent: String = "₹4,52,000",
    val totalTransactionCount: Int = 542,
    // Busiest day by COUNT
    val busiestDayByCount: String = "Thursday",
    val busiestDayCount: Int = 45,
    // Busiest day by AMOUNT
    val busiestDayByAmount: String = "Saturday",
    val busiestDayAmount: String = "₹18,500",
    val weekendPercentage: Int = 65,
    val weekendAmount: String = "₹12,500",
    val coffeeMetricLowCount: Int = 142,
    val coffeeMetricHighCount: Int = 12,
    val coffeeMetricAmount: String = "₹4,200",
    val foodiePercentage: Int = 40,
    val foodieCount: Int = 156,
    val foodieAmount: String = "₹1,85,000",
    val longestStreakDays: Int = 15,
    val longestStreakDateRange: String = "Mar 5 - Mar 19",
    val longestStreakCount: Int = 32,
    val longestStreakAmount: String = "₹15,000",
    val topCategories: List<Pair<String, String>> = listOf("Food" to "₹1.2L", "Travel" to "₹80k", "Gadgets" to "₹50k"),
    val topCategoriesByCount: List<Pair<String, Int>> = listOf("Groceries" to 89, "Food" to 72, "Transport" to 58),
    // Top by COUNT (for TopSpentTimesSlide)
    val topMonthByCount: String = "October",
    val topMonthCount: Int = 142,
    val topWeekByCount: String = "Nov 12-19",
    val topWeekCount: Int = 38,
    val topDayByCount: String = "Oct 25",
    val topDayCount: Int = 12,
    // Top by AMOUNT (for RollercoasterSlide)
    val topMonthByAmount: String = "December",
    val topMonthAmount: String = "₹85,000",
    val topWeekByAmount: String = "Dec 20-26",
    val topWeekAmount: String = "₹28,000",
    val topDayByAmount: String = "Dec 25",
    val topDayAmount: String = "₹12,500",
    // Travel Params - using data class for richer info
    val uberCount: Int = 43,
    val uberAmount: String = "₹8,500",
    val busCount: Int = 16,
    val busAmount: String = "₹320",
    val trainCount: Int = 8,
    val trainAmount: String = "₹1,200",
    val metroRechargeCount: Int = 40,
    val metroAmount: String = "₹6,000",
    val fuelCount: Int = 24,
    val fuelAmount: String = "₹12,500",
    val travelTotalAmount: String = "₹28,520",
    val movieHours: Int = 36,
    val movieCount: Int = 14,
    val movieAmount: String = "₹6,500",
    val lifestyleAmount: String = "₹45,000",
    // New Slides Stats
    val biggestPurchaseName: String = "Apple MacBook Air",
    val biggestPurchaseAmount: String = "₹1,14,900",
    val biggestPurchaseDate: String = "Oct 15",
    val smallestPurchaseName: String = "Matches",
    val smallestPurchaseAmount: String = "₹1.00",
    val totalIncome: String = "₹8,50,000",
    val totalExpense: String = "₹4,52,000",
    val incomeExpenseRatio: Int = 53 // 53% of income spent
)

// --- Main Screen ---

@Composable
fun YearRecapScreen(
    data: RecapData = RecapData(),
    onClose: () -> Unit
) {
    // Audio Playback - OUTSIDE MaterialTheme to prevent recomposition issues
    val context = androidx.compose.ui.platform.LocalContext.current
    val mediaPlayer = remember {
        MediaPlayer.create(context, com.sinxn.mymoney.R.raw.recap_bg_music)?.apply {
            isLooping = true
        }
    }
    
    var isPaused by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        mediaPlayer?.start()
        onDispose {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
    }

    LaunchedEffect(isPaused) {
        if (isPaused) {
            if (mediaPlayer?.isPlaying == true) mediaPlayer.pause()
        } else {
            if (mediaPlayer?.isPlaying == false) mediaPlayer.start()
        }
    }

    // Premium Dark Theme Override
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            surface = Color(0xFF101010),
            onSurface = Color.White
        )
    ) {
        var showWalletSelection by remember { mutableStateOf(true) }
        var selectedWalletIds by remember { mutableStateOf(setOf<Long>()) }
        var currentSlideIndex by remember { mutableIntStateOf(0) }
        val slides = RecapSlideType.entries.toTypedArray()

        if (showWalletSelection) {
            // Wallet Selection Screen
            WalletSelectionScreen(
                wallets = dummyWallets,
                selectedIds = selectedWalletIds,
                onSelectionChanged = { selectedWalletIds = it },
                onProceed = { showWalletSelection = false }
            )
        } else {

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
                        RecapSlideType.TOTAL_SPENT -> TotalSpentSlide(data)
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
                        RecapSlideType.BUSIEST_DAY -> BusiestDaySlide(data)
                        RecapSlideType.BIGGEST_PURCHASE -> BiggestPurchaseSlide(data)
                        RecapSlideType.INCOME_EXPENSE -> IncomeExpenseSlide(data)
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
        } // End of else (showWalletSelection)
    }
}

// ... existing components ...

// --- Wallet Selection Screen ---

@Composable
fun WalletSelectionScreen(
    wallets: List<WalletItem>,
    selectedIds: Set<Long>,
    onSelectionChanged: (Set<Long>) -> Unit,
    onProceed: () -> Unit
) {
    val activeWallets = wallets.filter { !it.isArchived }
    val archivedWallets = wallets.filter { it.isArchived }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Same background as recap
        AnimatedBackground(0)
        PremiumLineBackground(LinePattern.NET)
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                "SELECT WALLETS",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Choose which wallets to include in your recap",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Active Wallets Section
            Text(
                "ACTIVE WALLETS",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF64FFDA),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            activeWallets.forEach { wallet ->
                WalletSelectionItem(
                    wallet = wallet,
                    isSelected = selectedIds.contains(wallet.id),
                    onToggle = {
                        val newSet = if (selectedIds.contains(wallet.id)) {
                            selectedIds - wallet.id
                        } else {
                            selectedIds + wallet.id
                        }
                        onSelectionChanged(newSet)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Archived Wallets Section
            if (archivedWallets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "ARCHIVED WALLETS",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                archivedWallets.forEach { wallet ->
                    WalletSelectionItem(
                        wallet = wallet,
                        isSelected = selectedIds.contains(wallet.id),
                        onToggle = {
                            val newSet = if (selectedIds.contains(wallet.id)) {
                                selectedIds - wallet.id
                            } else {
                                selectedIds + wallet.id
                            }
                            onSelectionChanged(newSet)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Proceed Button
            androidx.compose.material3.Button(
                onClick = onProceed,
                enabled = selectedIds.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF64FFDA),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "Start Recap (${selectedIds.size} selected)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun WalletSelectionItem(
    wallet: WalletItem,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) Color(0xFF64FFDA).copy(alpha = 0.15f)
                else Color.White.copy(alpha = 0.05f)
            )
            .clickable { onToggle() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                wallet.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                wallet.balance,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
        
        // Checkbox circle
        Box(
            modifier = Modifier
                .size(24.dp)
                .border(
                    width = 2.dp,
                    color = if (isSelected) Color(0xFF64FFDA) else Color.White.copy(alpha = 0.3f),
                    shape = CircleShape
                )
                .background(
                    if (isSelected) Color(0xFF64FFDA) else Color.Transparent,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Close, // Using close as checkmark substitute
                    contentDescription = "Selected",
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// --- New Slides ---



@Composable
fun CoffeeMetricSlide(data: RecapData) {
     Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        PremiumLineBackground(LinePattern.CURVES)
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
                text = "${data.coffeeMetricLowCount} small purchases under ₹50",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
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
         PremiumLineBackground(LinePattern.RINGS, color = Color.White.copy(alpha = 0.05f))
         Column(
             horizontalAlignment = Alignment.CenterHorizontally,
             modifier = Modifier.padding(24.dp)
         ) {
             Text(
                 text = "FOODIE FACTOR",
                 style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                 color = Color.White
             )
             Spacer(modifier = Modifier.height(32.dp))
             
             // Circle visual with percentage inside
             Box(
                 modifier = Modifier
                     .size(180.dp)
                     .background(Color(0xFFFF5722), CircleShape),
                 contentAlignment = Alignment.Center
             ) {
                 Text(
                     text = "${data.foodiePercentage}%",
                     style = MaterialTheme.typography.displayLarge.copy(
                         fontSize = 56.sp,
                         fontWeight = FontWeight.Black
                     ),
                     color = Color.White
                 )
             }
             
             Spacer(modifier = Modifier.height(32.dp))
             Text(
                 text = "of your income went to food",
                 style = MaterialTheme.typography.titleMedium,
                 color = Color.White.copy(alpha = 0.8f),
                 textAlign = TextAlign.Center
             )
             Spacer(modifier = Modifier.height(16.dp))
             Text(
                 text = data.foodieAmount,
                 style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                 color = Color(0xFFFFD54F)
             )
             Text(
                 text = "${data.foodieCount} transactions",
                 style = MaterialTheme.typography.bodyMedium,
                 color = Color.White.copy(alpha = 0.6f)
             )
             Text(
                 text = "in deliciousness",
                 style = MaterialTheme.typography.bodyLarge,
                 color = Color.White.copy(alpha = 0.6f)
             )
         }
    }
}

@Composable
fun TopCategoriesSlide(data: RecapData) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
        PremiumLineBackground(LinePattern.NET)
        Column(modifier = Modifier.padding(24.dp).padding(top = 48.dp)) {
            // By Amount Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "TOP BY AMOUNT",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFFFD54F)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            data.topCategories.forEachIndexed { index, (name, amount) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "#${index + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.width(32.dp)
                        )
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = amount,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFFFD54F)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            androidx.compose.material3.HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(24.dp))
            
            // By Count Section
            Text(
                "TOP BY COUNT",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF64FFDA)
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            data.topCategoriesByCount.forEachIndexed { index, (name, count) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "#${index + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.width(32.dp)
                        )
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "$count txns",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF64FFDA)
                    )
                }
            }
        }
    }
}

@Composable
fun TopSpentTimesSlide(data: RecapData) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        PremiumLineBackground(LinePattern.CROSSHATCH)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                "MOST ACTIVE",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                "by transaction count",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(40.dp))
            TimeStatItemByCount("Month", data.topMonthByCount, data.topMonthCount)
            Spacer(modifier = Modifier.height(24.dp))
            TimeStatItemByCount("Week", data.topWeekByCount, data.topWeekCount)
            Spacer(modifier = Modifier.height(24.dp))
            TimeStatItemByCount("Day", data.topDayByCount, data.topDayCount)
        }
    }
}

@Composable
fun TimeStatItemByCount(label: String, value: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.6f))
        Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
        Text("$count transactions", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF64FFDA))
    }
}

@Composable
fun RollercoasterSlide(data: RecapData) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        PremiumLineBackground(LinePattern.WAVES, color = Color.White.copy(alpha = 0.05f))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                "BIG SPENDER",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFFFF4081)
            )
            Text(
                "by amount spent",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(40.dp))
            TimeStatItemByAmount("Month", data.topMonthByAmount, data.topMonthAmount)
            Spacer(modifier = Modifier.height(24.dp))
            TimeStatItemByAmount("Week", data.topWeekByAmount, data.topWeekAmount)
            Spacer(modifier = Modifier.height(24.dp))
            TimeStatItemByAmount("Day", data.topDayByAmount, data.topDayAmount)
        }
    }
}

@Composable
fun TimeStatItemByAmount(label: String, value: String, amount: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.6f))
        Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
        Text(amount, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFFFFD54F))
    }
}

@Composable
fun LongestStreakSlide(data: RecapData) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
         PremiumLineBackground(LinePattern.RINGS)
         Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
             Text(
                 text = "${data.longestStreakDays} DAYS",
                 style = MaterialTheme.typography.displayLarge.copy(fontSize = 80.sp, fontWeight = FontWeight.Bold),
                 color = Color(0xFF00E676)
             )
             Text(
                 text = "SPENDING STREAK",
                 style = MaterialTheme.typography.headlineSmall,
                 color = Color.White
             )
             Spacer(modifier = Modifier.height(8.dp))
             Text(
                 text = data.longestStreakDateRange,
                 style = MaterialTheme.typography.titleMedium,
                 color = Color.White.copy(alpha = 0.6f)
             )
             Spacer(modifier = Modifier.height(32.dp))
             Row(
                 modifier = Modifier.fillMaxWidth(),
                 horizontalArrangement = Arrangement.SpaceEvenly
             ) {
                 Column(horizontalAlignment = Alignment.CenterHorizontally) {
                     Text(
                         text = "${data.longestStreakCount}",
                         style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                         color = Color(0xFF64FFDA)
                     )
                     Text(
                         text = "Transactions",
                         style = MaterialTheme.typography.labelMedium,
                         color = Color.White.copy(alpha = 0.7f)
                     )
                 }
                 Column(horizontalAlignment = Alignment.CenterHorizontally) {
                     Text(
                         text = data.longestStreakAmount,
                         style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                         color = Color(0xFFFFD54F)
                     )
                     Text(
                         text = "Spent",
                         style = MaterialTheme.typography.labelMedium,
                         color = Color.White.copy(alpha = 0.7f)
                     )
                 }
             }
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

// --- Background Graphics ---

enum class LinePattern {
    WAVES, NET, CURVES, RINGS, CROSSHATCH
}

@Composable
fun PremiumLineBackground(pattern: LinePattern, color: Color = Color.White.copy(alpha = 0.1f)) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        when(pattern) {
            LinePattern.WAVES -> {
                // Horizontal Sine Waves
                val wavePath = androidx.compose.ui.graphics.Path()
                for (i in 0..5) {
                    val yOffset = height * (0.3f + i * 0.1f)
                    wavePath.reset()
                    wavePath.moveTo(0f, yOffset)
                    
                    var x = 0f
                    while (x <= width) {
                        wavePath.cubicTo(
                            x + width * 0.1f, yOffset - 50f * (if(i%2==0) 1 else -1),
                            x + width * 0.2f, yOffset + 50f * (if(i%2==0) 1 else -1),
                            x + width * 0.3f, yOffset
                        )
                        x += width * 0.3f
                    }
                    drawPath(wavePath, color = color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
                }
            }
            LinePattern.NET -> {
                // Diagonal interconnecting lines
                val step = 60.dp.toPx()
                for (x in -width.toInt()..width.toInt() step step.toInt()) {
                    drawLine(color, start = Offset(x.toFloat(), 0f), end = Offset(x + height, height), strokeWidth = 1.dp.toPx())
                }
                for (x in 0..width.toInt()*2 step step.toInt()) {
                    drawLine(color, start = Offset(x.toFloat(), 0f), end = Offset(x - height, height), strokeWidth = 1.dp.toPx())
                }
            }
            LinePattern.CURVES -> {
                 // Vertical flowing curves
                 val path = androidx.compose.ui.graphics.Path()
                 for(i in 0..3) {
                     path.reset()
                     path.moveTo(width * (0.2f + i * 0.2f), 0f)
                     path.cubicTo(
                         width * (0.1f + i * 0.2f), height * 0.3f,
                         width * (0.3f + i * 0.2f), height * 0.7f,
                         width * (0.2f + i * 0.2f), height
                     )
                     drawPath(path, color = color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = (2+i).dp.toPx()))
                 }
            }
            LinePattern.RINGS -> {
                // Concentric circles center
                for (i in 1..5) {
                    drawCircle(color = color, radius = width * (0.15f * i), center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
                }
            }
            LinePattern.CROSSHATCH -> {
                val step = 40.dp.toPx()
                 for (y in 0..height.toInt() step step.toInt()) {
                    drawLine(color, start = Offset(0f, y.toFloat()), end = Offset(width, y.toFloat()), strokeWidth = 1.dp.toPx())
                }
                 for (x in 0..width.toInt() step step.toInt()) {
                    drawLine(color, start = Offset(x.toFloat(), 0f), end = Offset(x.toFloat(), height), strokeWidth = 1.dp.toPx())
                }
            }
        }
    }
}

// --- Specific Category Slides ---

@Composable
fun TotalSpentSlide(data: RecapData) {
    // Parse amount for animation (Assuming format "₹4,52,000")
    val rawAmount = data.totalSpent.filter { it.isDigit() }.toIntOrNull() ?: 0
    val animatedAmount by animateIntAsState(
        targetValue = rawAmount,
        animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
        label = "amount"
    )
    
    // Quick formatter to put commas back (Simplified for Indian Locale usually)
    val formattedAnimatedAmount = "₹" + java.text.NumberFormat.getIntegerInstance(java.util.Locale("en", "IN")).format(animatedAmount)

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        PremiumLineBackground(LinePattern.NET)
        ParticleEffect(modifier = Modifier.fillMaxSize()) // Add floating particles
        
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "This year, you spent a grand total of...",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(32.dp))
            
            // Animated Number
            Text(
                text = formattedAnimatedAmount,
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
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "${data.totalTransactionCount} Transactions",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF64FFDA)
            )
        }
    }
}

@Composable
fun WeekendWarriorSlide(data: RecapData) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        PremiumLineBackground(LinePattern.CROSSHATCH, color = Color.White.copy(alpha = 0.05f))
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            // ... existing content
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
                            .fillMaxHeight()
                            .fillMaxWidth(data.weekendPercentage / 100f)
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
             Spacer(modifier = Modifier.height(8.dp))
             Text(
                text = "Totaling ${data.weekendAmount}",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}



@Composable
fun BusiestDaySlide(data: RecapData) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    // Fake graph data
    val heights = listOf(0.4f, 0.6f, 0.3f, 1.0f, 0.5f, 0.9f, 0.5f)
    
    // Determine which days to highlight (based on dummy data - Thursday=3 for count, Saturday=5 for amount)
    val countDayIndex = 3 // Thursday
    val amountDayIndex = 5 // Saturday

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        PremiumLineBackground(LinePattern.CROSSHATCH)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                "BUSIEST DAYS",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            // Week Graph
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.height(160.dp)
            ) {
                days.forEachIndexed { index, day ->
                    val isCountDay = index == countDayIndex
                    val isAmountDay = index == amountDayIndex
                    
                    val barColor = when {
                        isCountDay -> Color(0xFF64FFDA) // Teal for count
                        isAmountDay -> Color(0xFFFFD54F) // Amber for amount
                        else -> Color.White.copy(alpha = 0.2f)
                    }
                    val textColor = when {
                        isCountDay -> Color(0xFF64FFDA)
                        isAmountDay -> Color(0xFFFFD54F)
                        else -> Color.White.copy(alpha = 0.5f)
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .fillMaxHeight(fraction = 0.8f * heights[index])
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(barColor)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = day.take(1),
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // By Count
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(Color(0xFF64FFDA), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("By Count", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                    }
                    Text(
                        data.busiestDayByCount,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF64FFDA)
                    )
                    Text(
                        "${data.busiestDayCount} txns",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
                
                // By Amount
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(Color(0xFFFFD54F), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("By Amount", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                    }
                    Text(
                        data.busiestDayByAmount,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFFFD54F)
                    )
                    Text(
                        data.busiestDayAmount,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

// --- Specific Category Slides ---

@Composable
fun TravelStatsSlide(data: RecapData) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        PremiumLineBackground(LinePattern.WAVES)
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                "URBAN EXPLORER",
                style = MaterialTheme.typography.displaySmall.copy(fontFamily = FontFamily.Serif),
                color = Color.White,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(40.dp))

            // Uber
            TravelStatRow(
                icon = Icons.Rounded.LocalTaxi,
                label = "${data.uberCount} Uber travels",
                amount = data.uberAmount
            )
            // Bus
            TravelStatRow(
                icon = Icons.Rounded.DirectionsBus,
                label = "${data.busCount} Bus rides",
                amount = data.busAmount
            )
            // Train
            TravelStatRow(
                icon = Icons.Rounded.Train,
                label = "${data.trainCount} Train journeys",
                amount = data.trainAmount
            )
            // Metro
            TravelStatRow(
                icon = Icons.Rounded.Subway,
                label = "${data.metroRechargeCount} Metro recharges",
                amount = data.metroAmount
            )
            // Fuel/Oil
            TravelStatRow(
                icon = Icons.Rounded.LocalGasStation,
                label = "${data.fuelCount} Fuel fill-ups",
                amount = data.fuelAmount
            )

            Spacer(modifier = Modifier.height(32.dp))
            androidx.compose.material3.HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Total Travel Spend",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    data.travelTotalAmount,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF00E5FF)
                )
            }
        }
    }
}

@Composable
fun TravelStatRow(icon: ImageVector, label: String, amount: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, color = Color.White)
        }
        Text(amount, style = MaterialTheme.typography.titleMedium, color = Color(0xFF64FFDA), fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MovieStatsSlide(data: RecapData) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        PremiumLineBackground(LinePattern.CURVES)
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
                text = "Spending ${data.movieAmount}",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
             Spacer(modifier = Modifier.height(8.dp))
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
        PremiumLineBackground(LinePattern.NET)
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

@Composable
fun OutroSlide() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        PremiumLineBackground(LinePattern.WAVES, color = Color.White.copy(alpha = 0.05f))
        
        // Confetti!
        ConfettiExplosion(modifier = Modifier.fillMaxSize())
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                "See you in 2026!",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Keep making every\ntransaction count.",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// --- New Slides ---

@Composable
fun BiggestPurchaseSlide(data: RecapData) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        PremiumLineBackground(LinePattern.CROSSHATCH)
        ParticleEffect(modifier = Modifier.fillMaxSize()) // Add floating particles

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                "BIGGEST SPEND",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFFFF4081)
            )
            Spacer(modifier = Modifier.height(40.dp))
            
            // Icon or Visual
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                 Icon(
                     imageVector = Icons.Rounded.ShoppingBag,
                     contentDescription = null,
                     tint = Color.White,
                     modifier = Modifier.size(48.dp)
                 )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                data.biggestPurchaseName,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                data.biggestPurchaseAmount,
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFFFFD54F)
            )
             Spacer(modifier = Modifier.height(8.dp))
            Text(
                "on ${data.biggestPurchaseDate}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            androidx.compose.material3.HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(32.dp))
            
            // Smallest Purchase Section
             Text(
                "SMALLEST SPEND",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF64FFDA)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    data.smallestPurchaseName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "•",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    data.smallestPurchaseAmount,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF64FFDA)
                )
            }
        }
    }
}

@Composable
fun IncomeExpenseSlide(data: RecapData) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        PremiumLineBackground(LinePattern.NET)
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                "MONEY FLOW",
                style = MaterialTheme.typography.displaySmall.copy(fontFamily = FontFamily.Serif),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(48.dp))
            
            // Income
             Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "TOTAL INCOME",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Text(
                    data.totalIncome,
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF00E676) // Green
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Visual Ratio Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF00E676).copy(alpha = 0.3f))
            ) {
                // Expense portion
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(data.incomeExpenseRatio / 100f)
                        .background(Color(0xFFFF5252)) // Red
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
             Text(
                "You spent ${data.incomeExpenseRatio}% of what you earned",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Expense
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "TOTAL EXPENSE",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Text(
                    data.totalExpense,
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFFF5252) // Red
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewRecap() {
    YearRecapScreen(onClose = {})
}
