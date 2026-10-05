package com.sinxn.mymoney

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sinxn.mymoney.core.data.preferences.toFormatterConfig
import com.sinxn.mymoney.core.data.repository.RecurrenceRepository
import com.sinxn.mymoney.core.ui.LocalFormatterConfig
import com.sinxn.mymoney.core.ui.LocalFormattingSettings
import com.sinxn.mymoney.core.ui.components.handleSidebarNavigation
import com.sinxn.mymoney.core.ui.components.navigationMenuItems
import com.sinxn.mymoney.core.util.Constants
import com.sinxn.mymoney.navigation.AppNavHost
import com.sinxn.mymoney.navigation.Screen
import com.sinxn.mymoney.ui.theme.MyMoneyTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var recurrenceRepository: RecurrenceRepository

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            recurrenceRepository.processPendingRecurrences()
        }
        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val formattingSettings by viewModel.formattingSettings.collectAsState()
            val formatterConfig = remember(formattingSettings) { formattingSettings.toFormatterConfig() }

            CompositionLocalProvider(
                LocalFormatterConfig provides formatterConfig,
                LocalFormattingSettings provides formattingSettings
            ) {
                MyMoneyTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val navController = rememberNavController()
                        val startDest by viewModel.startDestination.collectAsState()
                    
                    if (startDest != "loading") {
                        var dynamicModuleItemId by remember { mutableStateOf(Screen.Overview.sidebarItemId) }

                        val currentBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = currentBackStackEntry?.destination?.route ?: "wallet_details/${Constants.TOTAL_WALLET_ID}"
                        val currentWalletId by viewModel.currentWalletId.collectAsState()
                        
                        val screenMetadata = remember(currentRoute) {
                            Screen.fromRoute(currentRoute)
                        }
                        val selectedItemId = screenMetadata.selectedItemId
                        val isTopLevelScreen = screenMetadata.isTopLevel
                        val pageTitle = screenMetadata.title
                        val isTransactions = selectedItemId == Screen.Transactions.sidebarItemId
                        val isMenu = selectedItemId == Screen.Menu.sidebarItemId

                        // Track active module in dynamic slot when visiting top-level non-transaction screens
                        LaunchedEffect(selectedItemId, isTopLevelScreen) {
                            if (!isTransactions && !isMenu && isTopLevelScreen && selectedItemId.isNotBlank()) {
                                dynamicModuleItemId = selectedItemId
                            }
                        }

                        // Retrieve metadata for dynamic middle tab (defaults to Overview)
                        val dynamicMenuItem = remember(dynamicModuleItemId) {
                            navigationMenuItems.find { it.id == dynamicModuleItemId }
                                ?: navigationMenuItems.find { it.id == Screen.Overview.sidebarItemId }
                        }
                        val dynamicTabTitle = dynamicMenuItem?.title ?: "Overview"
                        val dynamicTabIcon = dynamicMenuItem?.icon ?: Icons.Default.Equalizer

                        // Navigate back to Transactions on Back Press from any top-level module/menu
                        BackHandler(enabled = isTopLevelScreen && !isTransactions) {
                            handleSidebarNavigation(
                                context = this@MainActivity,
                                navController = navController,
                                itemId = Screen.Transactions.sidebarItemId,
                                currentWalletId = currentWalletId
                            )
                        }

                        Scaffold(
                            contentWindowInsets = WindowInsets(0, 0, 0, 0),
                            topBar = {
                                AnimatedVisibility(
                                    visible = isTopLevelScreen,
                                    enter = slideInVertically(
                                        initialOffsetY = { -it },
                                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                                    ) + expandVertically(
                                        expandFrom = Alignment.Top,
                                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                                    ) + fadeIn(
                                        animationSpec = tween(durationMillis = 200)
                                    ),
                                    exit = slideOutVertically(
                                        targetOffsetY = { -it },
                                        animationSpec = tween(durationMillis = 250, easing = FastOutLinearInEasing)
                                    ) + shrinkVertically(
                                        shrinkTowards = Alignment.Top,
                                        animationSpec = tween(durationMillis = 250, easing = FastOutLinearInEasing)
                                    ) + fadeOut(
                                        animationSpec = tween(durationMillis = 150)
                                    )
                                ) {
                                    TopAppBar(
                                        title = {
                                            AnimatedContent(
                                                targetState = pageTitle,
                                                transitionSpec = {
                                                    fadeIn(
                                                        animationSpec = tween(200)
                                                    ) togetherWith fadeOut(
                                                        animationSpec = tween(200)
                                                    )
                                                },
                                                label = "TopAppBarTitle"
                                            ) { targetTitle ->
                                                Text(
                                                    text = targetTitle,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        },
                                        colors = TopAppBarDefaults.topAppBarColors(
                                            containerColor = MaterialTheme.colorScheme.background
                                        )
                                    )
                                }
                            },
                            bottomBar = {
                                NavigationBar(
                                    tonalElevation = 6.dp
                                ) {
                                    // Tab 1: Transactions (Anchor)
                                    NavigationBarItem(
                                        selected = isTransactions,
                                        onClick = {
                                            handleSidebarNavigation(
                                                context = this@MainActivity,
                                                navController = navController,
                                                itemId = Screen.Transactions.sidebarItemId,
                                                currentWalletId = currentWalletId
                                            )
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Default.ShoppingCart,
                                                contentDescription = "Transactions"
                                            )
                                        },
                                        label = { Text("Transactions") }
                                    )

                                    // Tab 2: Dynamic Module (Overview / Categories / Budgets / Debts etc.)
                                    NavigationBarItem(
                                        selected = !isTransactions && !isMenu,
                                        onClick = {
                                            handleSidebarNavigation(
                                                context = this@MainActivity,
                                                navController = navController,
                                                itemId = dynamicModuleItemId,
                                                currentWalletId = currentWalletId
                                            )
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = dynamicTabIcon,
                                                contentDescription = dynamicTabTitle
                                            )
                                        },
                                        label = { Text(dynamicTabTitle) }
                                    )

                                    // Tab 3: Menu (Hub)
                                    NavigationBarItem(
                                        selected = isMenu,
                                        onClick = {
                                            handleSidebarNavigation(
                                                context = this@MainActivity,
                                                navController = navController,
                                                itemId = Screen.Menu.sidebarItemId,
                                                currentWalletId = currentWalletId
                                            )
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Default.Menu,
                                                contentDescription = "Menu"
                                            )
                                        },
                                        label = { Text("Menu") }
                                    )
                                }
                            }
                        ) { paddingValues ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues)
                            ) {
                                AppNavHost(
                                    navController = navController,
                                    startDestination = startDest,
                                    currentWalletId = currentWalletId
                                )
                            }
                        }
                    } else {
                        // Show Loading Screen or Splash
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    }
                }
            }
        }
    }
}