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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Unarchive
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
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.sinxn.mymoney.core.data.local.entity.TransactionEntity
import com.sinxn.mymoney.core.data.local.model.WalletWithBalance
import com.sinxn.mymoney.core.util.MoneyFormatter
import com.sinxn.mymoney.core.util.DateUtils
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.generateColor
import com.sinxn.mymoney.feature.recap.YearRecapScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletDetailsScreen(
    onNavigateUp: () -> Unit,
    onTransactionClick: (String) -> Unit,
    onNavigateToRecap: () -> Unit,
    viewModel: WalletDetailsViewModel = hiltViewModel()
) {
    val wallet by viewModel.wallet.collectAsState(initial = null)
    val transactions by viewModel.transactions.collectAsState(initial = emptyList())
    val settings by viewModel.formattingSettings.collectAsState()
    
    var showSettings by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    
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
                actions = {
                    if (wallet?.wallet?.id != com.sinxn.mymoney.core.util.Constants.TOTAL_WALLET_ID) {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Wallet Settings",
                                tint = if (showTopBarTitle) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }
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
                    onTransactionClick = onTransactionClick,
                    onRecapClick = onNavigateToRecap
                )
            }

            if (showSettings && wallet != null) {
                WalletSettingsBottomSheet(
                    wallet = wallet!!,
                    onDismiss = { showSettings = false },
                    onToggleCountInTotal = { viewModel.toggleCountInTotal() },
                    onToggleArchived = { viewModel.toggleArchived() },
                    sheetState = sheetState
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletSettingsBottomSheet(
    wallet: WalletWithBalance,
    onDismiss: () -> Unit,
    onToggleCountInTotal: () -> Unit,
    onToggleArchived: () -> Unit,
    sheetState: SheetState
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Wallet Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Show in Total Toggle
            Surface(
                onClick = onToggleCountInTotal,
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, 
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        Column {
                            Text(
                                text = "Show in Total",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Include this wallet in global balance",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Switch(
                        checked = wallet.wallet.countInTotal,
                        onCheckedChange = { onToggleCountInTotal() }
                    )
                }
            }
            
            // Archive Toggle
            Surface(
                onClick = onToggleArchived,
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, 
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (wallet.wallet.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        Column {
                            Text(
                                text = if (wallet.wallet.isArchived) "Unarchive Wallet" else "Archive Wallet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (wallet.wallet.isArchived) "Restore wallet to active list" else "Hide wallet from active list",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Switch(
                        checked = wallet.wallet.isArchived,
                        onCheckedChange = { onToggleArchived() }
                    )
                }
            }
            
            // Helpful note
            Text(
                text = "Settings changed here will immediately affect your budget and global totals.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
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
                text = if (wallet.isTotalValid) formattedBalance else "Multi-Currency",
                style = if (wallet.isTotalValid) {
                    MaterialTheme.typography.displayMedium.copy(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.1f),
                            offset = Offset(2f, 4f),
                            blurRadius = 8f
                        )
                    )
                } else {
                    MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                },
                color = Color.White,
                fontWeight = FontWeight.Black,
                letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified
            )

            if (!wallet.isTotalValid) {
                if (!wallet.balanceBreakdown.isNullOrEmpty()) {
                    Text(
                        text = wallet.balanceBreakdown,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Text(
                    text = "Conversion not supported yet",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
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
    onTransactionClick: (String) -> Unit,
    onRecapClick: () -> Unit
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
                is TransactionListItem.Transaction -> {
                    currentHeader?.let {
                         customGrouped.lastOrNull()?.second?.add(item)
                    }
                }
                else -> {}
            }
        }

        customGrouped.forEach { (header, groupItems) ->
            val headerKey = DateUtils.formatMonthHeader(header.date)
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
                    key = { _, item ->
                        when(item) {
                            is TransactionListItem.Transaction -> item.transaction.transaction.id
                            else -> "Unknown"
                        }
                    }
                ) { index, item ->

                     val isLastItem = index == groupItems.lastIndex

                     Box(
                         modifier = Modifier.animateItem()
                     ) {
                         when (item) {
                             is TransactionListItem.Transaction -> {
                                 val trans = item.transaction
                                 TransactionItem(
                                     item = trans,
                                     decimals = trans.decimals,
                                     // Priority: DB Symbol -> DB ISO Code -> Wallet ISO Code
                                     currencyCode = trans.currencySymbol ?: trans.currencyCode ?: currencyCode,
                                     formatterConfig = formatterConfig,
                                     dateFormat = dateFormat,
                                     isLastItem = isLastItem,
                                     showDate = true,
                                     onClick = { onTransactionClick(trans.transaction.id) }
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
    val formattedDate = DateUtils.formatMonthHeader(header.date)
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
                text = if (header.isTotalValid) formattedTotal else "Multi-Currency",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (!header.isTotalValid) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else if (header.totalAmount >= 0) {
                    Color(0xFF4CAF50)
                } else {
                    Color(0xFFE53935)
                }
            )
            
            if (header.isTotalValid) {
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
            } else {
                if (!header.balanceBreakdown.isNullOrEmpty()) {
                     Text(
                        text = header.balanceBreakdown,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                }
                Text(
                    text = "Mixed currencies",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
                        val dateObj = DateUtils.parseDate(transaction.date)
                        val formattedDate = DateUtils.formatDate(dateObj, dateFormat)
                        
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

@Preview(showBackground = true)
@Composable
fun PreviewRecap() {
    RecapBanner {  }
}

@Composable
fun RecapBanner(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF1A237E), Color(0xFF0D47A1)) // Deep Blue premium gradient
                )
            )
            .clickable(onClick = onClick)
    ) {
         Canvas(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.1f)
        ) {
             drawCircle(color = Color.White, radius = size.minDimension, center = Offset(x = size.width, y = 0f))
        }

        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                 Icon(
                     imageVector = Icons.Rounded.CardGiftcard,
                     contentDescription = "Recap",
                     tint = Color.White
                 )
            }
            
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(
                    text = "Your 2025 Recap",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "See your spending highlights!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}
