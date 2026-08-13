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
import com.sinxn.mymoney.navigation.Screen

data class NavigationMenuItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val group: Int // 1: Main modules, 2: Utilities, 3: App/Settings
)

val navigationMenuItems = listOf(
    // Group 1: Main modules (Matching legacy MainActivity drawer order)
    NavigationMenuItem(Screen.Transactions.sidebarItemId, "Transactions", Icons.Default.ShoppingCart, 1),
    NavigationMenuItem(Screen.Categories.sidebarItemId, "Categories", Icons.Default.GridView, 1),
    NavigationMenuItem(Screen.Overview.sidebarItemId, "Overview", Icons.Default.Equalizer, 1),
    NavigationMenuItem(Screen.Debts.sidebarItemId, "Debts", Icons.Default.AccountBalanceWallet, 1),
    NavigationMenuItem(Screen.Budgets.sidebarItemId, "Budgets", Icons.Default.PieChart, 1),
    NavigationMenuItem(Screen.Savings.sidebarItemId, "Savings", Icons.Default.Savings, 1),
    NavigationMenuItem(Screen.Events.sidebarItemId, "Events", Icons.Default.Flag, 1),
    NavigationMenuItem(Screen.Recurrences.sidebarItemId, "Recurrences", Icons.Default.Restore, 1),
    NavigationMenuItem(Screen.Templates.sidebarItemId, "Models", Icons.Default.Bookmark, 1),
    NavigationMenuItem(Screen.Places.sidebarItemId, "Places", Icons.Default.Place, 1),
    NavigationMenuItem(Screen.People.sidebarItemId, "People", Icons.Default.People, 1),

    // Group 2: Utilities
    NavigationMenuItem("calculator", "Calculator", Icons.Default.Calculate, 2),
    NavigationMenuItem("converter", "Converter", Icons.Default.SyncAlt, 2),
    NavigationMenuItem("search_atm", "Search atm", Icons.Default.CreditCard, 2),
    NavigationMenuItem("search_bank", "Search bank", Icons.Default.AccountBalance, 2),

    // Group 3: Settings & Info
    NavigationMenuItem(Screen.Settings.sidebarItemId, "Settings", Icons.Default.Settings, 3),
    NavigationMenuItem(Screen.SupportDeveloper.sidebarItemId, "Support developer", Icons.Default.FavoriteBorder, 3),
    NavigationMenuItem(Screen.About.sidebarItemId, "About", Icons.Default.Info, 3)
)

@Composable
fun NavigationMenuContent(
    modifier: Modifier = Modifier,
    selectedItemId: String? = null,
    onWalletSelect: (WalletWithBalance) -> Unit = {},
    onAddWallet: () -> Unit = {},
    onManageWallets: () -> Unit = {},
    onItemClick: (NavigationMenuItem) -> Unit = {}
) {
    var isHeaderExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        WalletHeader(
            isExpanded = isHeaderExpanded,
            onToggleExpand = { isHeaderExpanded = !isHeaderExpanded }
        )

        if (isHeaderExpanded) {
            WalletDropdownList(
                onWalletSelect = { wallet ->
                    isHeaderExpanded = false
                    onWalletSelect(wallet)
                },
                onAddWallet = {
                    isHeaderExpanded = false
                    onAddWallet()
                },
                onManageWallets = {
                    isHeaderExpanded = false
                    onManageWallets()
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationMenuPage(
    selectedItemId: String? = null,
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
            selectedItemId = selectedItemId,
            onWalletSelect = onWalletSelect,
            onAddWallet = onAddWallet,
            onManageWallets = onManageWallets,
            onItemClick = onItemClick
        )
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
        Screen.Transactions.sidebarItemId -> {
            val targetWallet = if (currentWalletId.isEmpty()) com.sinxn.mymoney.core.util.Constants.TOTAL_WALLET_ID else currentWalletId
            navController.navigate(Screen.Transactions.createRoute(targetWallet)) {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
        Screen.Debts.sidebarItemId -> navController.navigate(Screen.Debts.routePattern) { launchSingleTop = true }
        Screen.Categories.sidebarItemId -> navController.navigate(Screen.Categories.routePattern) { launchSingleTop = true }
        Screen.Overview.sidebarItemId -> navController.navigate(Screen.Overview.routePattern) { launchSingleTop = true }
        Screen.Budgets.sidebarItemId -> navController.navigate(Screen.Budgets.routePattern) { launchSingleTop = true }
        Screen.Savings.sidebarItemId -> navController.navigate(Screen.Savings.routePattern) { launchSingleTop = true }
        Screen.Events.sidebarItemId -> navController.navigate(Screen.Events.routePattern) { launchSingleTop = true }
        Screen.Recurrences.sidebarItemId -> navController.navigate(Screen.Recurrences.routePattern) { launchSingleTop = true }
        Screen.Templates.sidebarItemId -> navController.navigate(Screen.Templates.routePattern) { launchSingleTop = true }
        Screen.Places.sidebarItemId -> navController.navigate(Screen.Places.routePattern) { launchSingleTop = true }
        Screen.People.sidebarItemId -> navController.navigate(Screen.People.routePattern) { launchSingleTop = true }
        "calculator" -> launchCalculatorIntent(context)
        "converter" -> launchConverterIntent(context)
        "search_atm" -> launchSearchIntent(context, "ATM")
        "search_bank" -> launchSearchIntent(context, "Bank")
        Screen.Settings.sidebarItemId -> navController.navigate(Screen.Settings.routePattern) { launchSingleTop = true }
        Screen.SupportDeveloper.sidebarItemId -> navController.navigate(Screen.SupportDeveloper.routePattern) { launchSingleTop = true }
        Screen.About.sidebarItemId -> navController.navigate(Screen.About.routePattern) { launchSingleTop = true }
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

