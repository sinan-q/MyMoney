package com.sinxn.mymoney.feature.wallet

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sinxn.mymoney.core.data.local.entity.TransactionEntity
import com.sinxn.mymoney.core.data.local.model.WalletWithBalance
import com.sinxn.mymoney.core.util.MoneyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletDetailsScreen(
    onNavigateUp: () -> Unit,
    viewModel: WalletDetailsViewModel = hiltViewModel()
) {
    val wallet by viewModel.wallet.collectAsState(initial = null)
    val transactions by viewModel.transactions.collectAsState(initial = emptyList())
    val settings by viewModel.formattingSettings.collectAsState()

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
                title = { Text(text = wallet?.wallet?.name ?: "Wallet Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Wallet Summary Header
            wallet?.let { walletData ->
                WalletHeader(walletData, formatterConfig)
            }

            // Transactions List
            wallet?.let { walletData ->
                TransactionList(
                    items = transactions,
                    decimals = walletData.decimals,
                    currencyCode = walletData.wallet.currency,
                    formatterConfig = formatterConfig,
                    dateFormat = settings.dateFormat
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Total Balance",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            val formattedBalance = MoneyFormatter.format(
                amount = wallet.currentBalance,
                currencyCode = wallet.wallet.currency,
                decimals = wallet.decimals,
                config = formatterConfig
            )
            Text(
                text = formattedBalance,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionList(
    items: List<TransactionListItem>,
    decimals: Int,
    currencyCode: String,
    formatterConfig: MoneyFormatter.Config,
    dateFormat: Int
) {
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No transactions found", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        // State to track collapsed keys (using formatted date string as key)
        var collapsedGroups by remember { mutableStateOf(setOf<String>()) }
        
        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            
            var currentHeader: TransactionListItem.Header? = null
            val customGrouped = mutableListOf<Pair<TransactionListItem.Header, MutableList<TransactionListItem.Transaction>>>()
            
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
                }
            }
            
            customGrouped.forEach { (header, transactions) ->
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
                    items(
                        items = transactions,
                        key = { it.transaction.transaction.id }
                    ) { transactionItem ->
                         Box(
                             modifier = Modifier
                                 .padding(horizontal = 16.dp, vertical = 4.dp)
                                 .animateItem()
                         ) {
                             TransactionItem(transactionItem.transaction, decimals, currencyCode, formatterConfig, dateFormat)
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
                imageVector = androidx.compose.material.icons.Icons.Default.KeyboardArrowUp,
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
fun TransactionItem(
    item: com.sinxn.mymoney.core.data.local.model.TransactionWithCategory,
    decimals: Int,
    currencyCode: String,
    formatterConfig: MoneyFormatter.Config,
    dateFormat: Int
) {
    val transaction = item.transaction
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            CategoryIcon(
                iconString = item.categoryIcon,
                categoryName = item.categoryName ?: "?",
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

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
                
                // Description (or "No Description" if empty, but maybe cleaner to show note or type if empty?)
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
            .background(iconData.color, androidx.compose.foundation.shape.CircleShape),
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
            // If "resource" or other JSON type, fallback to default (Letter Avatar)
        }
    } catch (e: Exception) {
        // Not a JSON string or parse error, fallback to default
    }
    
    // If raw string (e.g. "ic_food") or failed JSON, use default logic
    return IconData(defaultColor, defaultText)
}

fun generateColor(name: String): Color {
    val hash = name.hashCode()
    val hue = kotlin.math.abs(hash % 360).toFloat()
    return Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.6f, 0.8f)))
}
