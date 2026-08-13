package com.sinxn.mymoney.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sinxn.mymoney.core.data.local.model.WalletWithBalance
import com.sinxn.mymoney.core.util.MoneyFormatter

data class NavigationMenuItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val group: Int // 1: Main modules, 2: Utilities, 3: App/Settings
)

val navigationMenuItems = listOf(
    // Group 1: Main modules (Matching legacy MainActivity drawer order)
    NavigationMenuItem("transactions", "Transactions", Icons.Default.ShoppingCart, 1),
    NavigationMenuItem("categories", "Categories", Icons.Default.GridView, 1),
    NavigationMenuItem("overview", "Overview", Icons.Default.Equalizer, 1),
    NavigationMenuItem("debts", "Debts", Icons.Default.AccountBalanceWallet, 1),
    NavigationMenuItem("budgets", "Budgets", Icons.Default.PieChart, 1),
    NavigationMenuItem("savings", "Savings", Icons.Default.Savings, 1),
    NavigationMenuItem("events", "Events", Icons.Default.Flag, 1),
    NavigationMenuItem("recurrences", "Recurrences", Icons.Default.Restore, 1),
    NavigationMenuItem("models", "Models", Icons.Default.Bookmark, 1),
    NavigationMenuItem("places", "Places", Icons.Default.Place, 1),
    NavigationMenuItem("people", "People", Icons.Default.People, 1),

    // Group 2: Utilities
    NavigationMenuItem("calculator", "Calculator", Icons.Default.Calculate, 2),
    NavigationMenuItem("converter", "Converter", Icons.Default.SyncAlt, 2),
    NavigationMenuItem("search_atm", "Search atm", Icons.Default.CreditCard, 2),
    NavigationMenuItem("search_bank", "Search bank", Icons.Default.AccountBalance, 2),

    // Group 3: Settings & Info
    NavigationMenuItem("settings", "Settings", Icons.Default.Settings, 3),
    NavigationMenuItem("support_developer", "Support developer", Icons.Default.FavoriteBorder, 3),
    NavigationMenuItem("about", "About", Icons.Default.Info, 3)
)

@Composable
fun NavigationMenuContent(
    modifier: Modifier = Modifier,
    wallets: List<WalletWithBalance> = emptyList(),
    selectedWallet: WalletWithBalance? = null,
    selectedItemId: String? = null,
    formatterConfig: MoneyFormatter.Config = MoneyFormatter.Config(),
    onWalletSelect: (WalletWithBalance) -> Unit = {},
    onAddWallet: () -> Unit = {},
    onManageWallets: () -> Unit = {},
    onItemClick: (NavigationMenuItem) -> Unit = {}
) {
    var isHeaderExpanded by remember { mutableStateOf(false) }
    val headerWallet = selectedWallet ?: wallets.firstOrNull()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        if (headerWallet != null) {
            item {
                WalletHeader(
                    wallet = headerWallet,
                    formatterConfig = formatterConfig,
                    activeSection = selectedItemId ?: "transactions",
                    isExpanded = isHeaderExpanded,
                    onToggleExpand = { isHeaderExpanded = !isHeaderExpanded }
                )
            }
        }

        if (isHeaderExpanded) {
            items(wallets) { wallet ->
                WalletProfileRow(
                    wallet = wallet,
                    isSelected = selectedWallet?.wallet?.id == wallet.wallet.id,
                    onClick = {
                        isHeaderExpanded = false
                        onWalletSelect(wallet)
                    }
                )
            }
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                ActionProfileRow(
                    title = "New wallet",
                    icon = Icons.Default.Add,
                    onClick = {
                        isHeaderExpanded = false
                        onAddWallet()
                    }
                )
                ActionProfileRow(
                    title = "Manage wallets",
                    icon = Icons.Default.Settings,
                    onClick = {
                        isHeaderExpanded = false
                        onManageWallets()
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }
        } else {
            // Group 1 Items
            val group1 = navigationMenuItems.filter { it.group == 1 }
            items(group1) { item ->
                NavigationDrawerItemRow(
                    item = item,
                    isSelected = selectedItemId == item.id,
                    onClick = { onItemClick(item) }
                )
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }

            // Group 2 Items
            val group2 = navigationMenuItems.filter { it.group == 2 }
            items(group2) { item ->
                NavigationDrawerItemRow(
                    item = item,
                    isSelected = selectedItemId == item.id,
                    onClick = { onItemClick(item) }
                )
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }

            // Group 3 Items
            val group3 = navigationMenuItems.filter { it.group == 3 }
            items(group3) { item ->
                NavigationDrawerItemRow(
                    item = item,
                    isSelected = selectedItemId == item.id,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationMenuPage(
    wallets: List<WalletWithBalance> = emptyList(),
    selectedWallet: WalletWithBalance? = null,
    selectedItemId: String? = null,
    formatterConfig: MoneyFormatter.Config = MoneyFormatter.Config(),
    onReturnToMain: () -> Unit = {},
    onWalletSelect: (WalletWithBalance) -> Unit = {},
    onAddWallet: () -> Unit = {},
    onManageWallets: () -> Unit = {},
    onItemClick: (NavigationMenuItem) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Navigation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onReturnToMain) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to main"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        NavigationMenuContent(
            modifier = Modifier.padding(paddingValues),
            wallets = wallets,
            selectedWallet = selectedWallet,
            selectedItemId = selectedItemId,
            formatterConfig = formatterConfig,
            onWalletSelect = onWalletSelect,
            onAddWallet = onAddWallet,
            onManageWallets = onManageWallets,
            onItemClick = onItemClick
        )
    }
}



@Composable
fun WalletProfileRow(
    wallet: WalletWithBalance,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = wallet.wallet.name.take(1).uppercase(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = wallet.wallet.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            val formattedBalance = MoneyFormatter.format(
                amount = wallet.currentBalance,
                currencyCode = wallet.wallet.currency,
                decimals = wallet.decimals
            )

            Text(
                text = formattedBalance,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ActionProfileRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun NavigationDrawerItemRow(
    item: NavigationMenuItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(32.dp))

            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

fun handleSidebarNavigation(
    context: android.content.Context,
    navController: androidx.navigation.NavController,
    itemId: String,
    currentWalletId: String = com.sinxn.mymoney.core.util.Constants.TOTAL_WALLET_ID,
    currentRoute: String? = null,
    onLocalSectionSelect: ((String) -> Unit)? = null
) {
    when (itemId) {
        "transactions" -> {
            val targetWallet = if (currentWalletId.isEmpty()) com.sinxn.mymoney.core.util.Constants.TOTAL_WALLET_ID else currentWalletId
            navController.navigate("wallet_details/$targetWallet") {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
        "debts" -> {
            navController.navigate("debts") { launchSingleTop = true }
        }
        "categories" -> navController.navigate("categories") { launchSingleTop = true }
        "overview" -> navController.navigate("overview") { launchSingleTop = true }
        "budgets" -> navController.navigate("budgets") { launchSingleTop = true }
        "savings" -> navController.navigate("savings") { launchSingleTop = true }
        "events" -> navController.navigate("events") { launchSingleTop = true }
        "recurrences" -> navController.navigate("recurrences") { launchSingleTop = true }
        "models" -> navController.navigate("templates") { launchSingleTop = true }
        "places" -> navController.navigate("places") { launchSingleTop = true }
        "people" -> navController.navigate("people") { launchSingleTop = true }
        "calculator" -> launchCalculatorIntent(context)
        "converter" -> launchConverterIntent(context)
        "search_atm" -> launchSearchIntent(context, "ATM")
        "search_bank" -> launchSearchIntent(context, "Bank")
        "settings" -> navController.navigate("settings") { launchSingleTop = true }
        "support_developer" -> navController.navigate("support_developer") { launchSingleTop = true }
        "about" -> navController.navigate("about") { launchSingleTop = true }
        else -> {
            try {
                navController.navigate(itemId) { launchSingleTop = true }
            } catch (e: Exception) {
                // Ignore invalid route
            }
        }
    }
}

private fun launchCalculatorIntent(context: android.content.Context) {
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_APP_CALCULATOR)
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val calcIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.calculator")
                ?: context.packageManager.getLaunchIntentForPackage("com.sec.android.app.popupcalculator")
            if (calcIntent != null) {
                calcIntent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(calcIntent)
            } else {
                android.widget.Toast.makeText(context, "Calculator app not found", android.widget.Toast.LENGTH_SHORT).show()
            }
        } catch (e2: Exception) {
            android.widget.Toast.makeText(context, "Calculator app not found", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

private fun launchConverterIntent(context: android.content.Context) {
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com/search?q=currency+converter")).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Unable to open browser", android.widget.Toast.LENGTH_SHORT).show()
    }
}

private fun launchSearchIntent(context: android.content.Context, query: String) {
    val geoUri = android.net.Uri.parse("geo:0,0?q=$query")
    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, geoUri).apply {
        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val webUri = android.net.Uri.parse("https://www.google.com/maps/search/$query")
            val webIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, webUri).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
        } catch (e2: Exception) {
            android.widget.Toast.makeText(context, "Unable to open maps", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

private fun launchSupportIntent(context: android.content.Context) {
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com")).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Unable to open browser", android.widget.Toast.LENGTH_SHORT).show()
    }
}

