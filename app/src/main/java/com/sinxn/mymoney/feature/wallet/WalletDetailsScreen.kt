package com.sinxn.mymoney.feature.wallet

import android.graphics.Color.HSVToColor
import android.graphics.Color.colorToHSV
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.hilt.navigation.compose.hiltViewModel
import com.sinxn.mymoney.core.data.local.entity.TransactionEntity
import com.sinxn.mymoney.core.data.local.model.WalletWithBalance
import com.sinxn.mymoney.core.util.MoneyFormatter
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.generateColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletDetailsScreen(
    onNavigateUp: () -> Unit,
    onTransactionClick: (String) -> Unit,
    viewModel: WalletDetailsViewModel = hiltViewModel()
) {
    val wallet by viewModel.wallet.collectAsState(initial = null)
    val transactions by viewModel.transactions.collectAsState(initial = emptyList())
    val settings by viewModel.formattingSettings.collectAsState()
    
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val showTopBarTitle by remember {
        androidx.compose.runtime.derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 300
        }
    }

    // Map Settings to Formatter Config
    val formatterConfig = MoneyFormatter.Config(
        showCurrency = settings.showCurrency,
        groupDigits = settings.groupDigits,
        roundDecimals = settings.roundDecimals,
        showPlusMinus = settings.showPlusMinus
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showTopBarTitle,
                        enter = androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.fadeOut()
                    ) {
                        Text(text = wallet?.wallet?.name ?: "Wallet Details") 
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (showTopBarTitle) MaterialTheme.colorScheme.surface else Color.Transparent,
                    navigationIconContentColor = if (showTopBarTitle) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (wallet == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                TransactionList(
                    wallet = wallet!!,
                    items = transactions,
                    decimals = wallet!!.decimals,
                    currencyCode = wallet!!.wallet.currency,
                    formatterConfig = formatterConfig,
                    dateFormat = settings.dateFormat,
                    listState = listState,
                    onTransactionClick = onTransactionClick
                )
            }
        }
    }
}

@Composable
fun WalletHeader(
    wallet: WalletWithBalance,
    formatterConfig: MoneyFormatter.Config
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
            horizontalAlignment = Alignment.Start // Left aligned looks more modern for cards
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                
                // Icon or Type
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = wallet.wallet.currency,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
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
                text = formattedBalance,
                style = MaterialTheme.typography.displayMedium.copy(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.1f),
                        offset = Offset(2f, 4f),
                        blurRadius = 8f
                    )
                ),
                color = Color.White,
                fontWeight = FontWeight.Black,
                letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified
            )
            
            if (!wallet.wallet.note.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = wallet.wallet.note!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionList(
    wallet: WalletWithBalance,
    items: List<TransactionListItem>,
    decimals: Int,
    currencyCode: String,
    formatterConfig: MoneyFormatter.Config,
    dateFormat: Int,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onTransactionClick: (String) -> Unit
) {
    // State to track collapsed keys (using formatted date string as key)
    var collapsedGroups by remember { mutableStateOf(setOf<String>()) }
    
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item {
            WalletHeader(wallet, formatterConfig)
        }
        
        if (items.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .padding(top = 100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "No transactions found", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        var currentHeader: TransactionListItem.Header? = null
        // We group items under the Month Header. The list can contain DateHeader or Transaction.
        val customGrouped = mutableListOf<Pair<TransactionListItem.Header, MutableList<TransactionListItem>>>()

        items.forEach { item ->
            when (item) {
                is TransactionListItem.Header -> {
                    currentHeader = item
                    customGrouped.add(item to mutableListOf())
                }
                is TransactionListItem.DateHeader -> {
                     currentHeader?.let {
                         customGrouped.lastOrNull()?.second?.add(item)
                    }
                }
                is TransactionListItem.Transaction -> {
                    currentHeader?.let {
                         customGrouped.lastOrNull()?.second?.add(item)
                    }
                }
            }
        }

        customGrouped.forEach { (header, groupItems) ->
            val headerKey = com.sinxn.mymoney.core.util.DateUtils.formatMonthHeader(header.date)
            val isCollapsed = collapsedGroups.contains(headerKey)

            stickyHeader(key = headerKey) {
                TransactionHeader(
                    header = header,
                    decimals = decimals,
                    currencyCode = currencyCode,
                    formatterConfig = formatterConfig,
                    isCollapsed = isCollapsed,
                    onToggle = {
                        collapsedGroups = if (isCollapsed) {
                            collapsedGroups - headerKey
                        } else {
                            collapsedGroups + headerKey
                        }
                    }
                )
            }

            if (!isCollapsed) {
                itemsIndexed(
                    items = groupItems,
                    // Use ID for transactions, Use date hash for DateHeader
                    key = { _, item ->
                        when(item) {
                            is TransactionListItem.Transaction -> item.transaction.transaction.id
                            is TransactionListItem.DateHeader -> "DH_${item.date.time}"
                            else -> "Unknown"
                        }
                    }
                ) { index, item ->

                     // Determine if this is the last item visually in this group
                     // Only Transactions can be the "Last Item" that stops the line.
                     // DateHeader always has content below it (Transactions).
                     val isLastItem = index == groupItems.lastIndex

                     Box(
                         modifier = Modifier.animateItem()
                     ) {
                         when (item) {
                             is TransactionListItem.DateHeader -> {
                                 DateHeaderItem(item, dateFormat)
                             }
                             is TransactionListItem.Transaction -> {
                                 TransactionItem(
                                     item = item.transaction,
                                     decimals = decimals,
                                     currencyCode = currencyCode,
                                     formatterConfig = formatterConfig,
                                     dateFormat = dateFormat,
                                     isLastItem = isLastItem,
                                     showDate = false, // Date is now in header
                                     onClick = { onTransactionClick(item.transaction.transaction.id) }
                                 )
                             }
                             else -> {}
                         }
                     }
                }
            }
        }
    }
}

@Composable
fun TransactionHeader(
    header: TransactionListItem.Header,
    decimals: Int,
    currencyCode: String,
    formatterConfig: MoneyFormatter.Config,
    isCollapsed: Boolean,
    onToggle: () -> Unit
) {
    val formattedDate = com.sinxn.mymoney.core.util.DateUtils.formatMonthHeader(header.date)
    val formattedTotal = MoneyFormatter.format(
        amount = header.totalAmount,
        currencyCode = currencyCode,
        decimals = decimals,
        config = formatterConfig
    )
    val formattedIncome = MoneyFormatter.format(
        amount = header.income,
        currencyCode = currencyCode,
        decimals = decimals,
        config = formatterConfig
    )
    val formattedExpense = MoneyFormatter.format(
        amount = header.expense,
        currencyCode = currencyCode,
        decimals = decimals,
        config = formatterConfig
    )
    
    // Animate arrow rotation
    val rotation by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isCollapsed) 180f else 0f,
        label = "ArrowRotation"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface) // Opaque for sticky
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp), // Increased vertical padding for touch target
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
             Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = if (isCollapsed) "Expand" else "Collapse",
                modifier = Modifier
                    .padding(end = 8.dp)
                    .rotate(rotation),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formattedTotal,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (header.totalAmount >= 0) Color(0xFF4CAF50) else Color(0xFFE53935)
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                 if (header.income > 0) {
                     Text(
                        text = "+$formattedIncome",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.padding(end = 6.dp)
                    )
                 }
                 if (header.expense > 0) {
                     Text(
                        text = "-$formattedExpense",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFE53935)
                    )
                 }
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

@Composable
fun DateHeaderItem(
    item: TransactionListItem.DateHeader,
    dateFormat: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Timeline Column
        Box(
            modifier = Modifier
                .width(56.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            // Continuous Vertical Line
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )
        }
        
        // Date Text
        Text(
            text = com.sinxn.mymoney.core.util.DateUtils.formatDate(item.date, dateFormat),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary, // Highlight color
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(vertical = 12.dp)
                .padding(end = 16.dp)
        )
    }
}

@Composable
fun TransactionItem(
    item: com.sinxn.mymoney.core.data.local.model.TransactionWithCategory,
    decimals: Int,
    currencyCode: String,
    formatterConfig: MoneyFormatter.Config,
    dateFormat: Int,
    isLastItem: Boolean,
    showDate: Boolean = true,
    onClick: () -> Unit = {}
) {
    val transaction = item.transaction
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min) // Important for full height line
    ) {
        // Timeline Column
        Box(
            modifier = Modifier
                .width(56.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            // Vertical Line
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .then(
                        if (isLastItem) {
                            Modifier
                                .fillMaxHeight(0.5f)
                                .align(Alignment.TopCenter)
                        } else {
                            Modifier
                                .fillMaxHeight()
                                .align(Alignment.Center)
                        }
                    )
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )
            
            // Icon
            CategoryIcon(
                iconString = item.categoryIcon,
                categoryName = item.categoryName ?: "?",
                modifier = Modifier
                    .size(40.dp)
                    .zIndex(1f) // Ensure icon is on top of line
            )
        }
        
        // Content Card
        Card(
            modifier = Modifier
                .padding(top = 8.dp, bottom = 8.dp, end = 16.dp)
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            onClick = onClick
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Removed Icon from here
                
                // Middle: Category & Description
                Column(modifier = Modifier.weight(1f)) {
                    // Category Name
                    item.categoryName?.let { categoryName ->
                         Text(
                            text = categoryName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Description
                    val descriptionText = if (!transaction.description.isNullOrEmpty()) {
                        transaction.description
                    } else {
                        "Transaction" 
                    }
                    
                    Text(
                        text = descriptionText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Right Side: Amount & Date
                Column(horizontalAlignment = Alignment.End) {
                    val isIncome = transaction.direction == 1
                    val amountColor = if (isIncome) Color(0xFF4CAF50) else Color(0xFFE53935)
        
                    val amount = if (isIncome) transaction.money else -transaction.money
                    val formattedMoney = MoneyFormatter.format(
                        amount = amount, // Send signed amount
                        currencyCode = currencyCode,
                        decimals = decimals,
                        config = formatterConfig
                    )
                    
                    Text(
                        text = formattedMoney,
                        style = MaterialTheme.typography.bodyLarge, // Slightly larger for emphasis
                        color = amountColor,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (showDate) {
                        val dateObj = com.sinxn.mymoney.core.util.DateUtils.parseDate(transaction.date)
                        val formattedDate = com.sinxn.mymoney.core.util.DateUtils.formatDate(dateObj, dateFormat)
                        
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

