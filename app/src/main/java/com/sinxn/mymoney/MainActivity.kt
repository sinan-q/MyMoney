package com.sinxn.mymoney

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sinxn.mymoney.core.data.repository.RecurrenceRepository
import com.sinxn.mymoney.core.ui.components.NavigationMenuPage
import com.sinxn.mymoney.core.ui.components.handleSidebarNavigation
import com.sinxn.mymoney.core.util.Constants
import com.sinxn.mymoney.navigation.AppNavHost
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
            MyMoneyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val viewModel: MainViewModel = hiltViewModel()
                    val startDest by viewModel.startDestination.collectAsState()
                    
                    if (startDest != "loading") {
                        val pagerState = rememberPagerState(initialPage = 1, pageCount = { 2 })
                        val currentBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = currentBackStackEntry?.destination?.route?.substringBefore("?") ?: "wallet_details/${Constants.TOTAL_WALLET_ID}"
                        


                        val currentWalletId by viewModel.currentWalletId.collectAsState()
                        
                        val selectedItemId = when {
                            currentRoute == "home" || currentRoute.startsWith("wallet_details") -> "transactions"
                            currentRoute.startsWith("debts") || currentRoute.startsWith("debt_details") -> "debts"
                            currentRoute.startsWith("budgets") || currentRoute.startsWith("budget_details") || currentRoute.startsWith("budget_overview") -> "budgets"
                            currentRoute.startsWith("savings") || currentRoute.startsWith("saving_details") -> "savings"
                            currentRoute == "categories" -> "categories"
                            currentRoute == "events" -> "events"
                            currentRoute == "places" -> "places"
                            currentRoute == "people" -> "people"
                            currentRoute == "templates" -> "models"
                            currentRoute == "recurrences" || currentRoute.startsWith("recurrent_") -> "recurrences"
                            currentRoute == "settings" -> "settings"
                            currentRoute == "recap" || currentRoute == "overview" -> "overview"
                            currentRoute == "about" -> "about"
                            currentRoute == "support_developer" -> "support_developer"
                            else -> "transactions"
                        }
                        
                        val coroutineScope = rememberCoroutineScope()

                        HorizontalPager(state = pagerState) { page ->
                            if (page == 1) {
                                val isTopLevelScreen = currentRoute.substringBefore("?") in listOf(
                                    "wallet_details/{walletId}", "wallet_details",
                                    "categories", "debts", "budgets", "savings",
                                    "events", "recurrences", "templates", "places", "people",
                                    "overview", "about", "support_developer"
                                )

                                Scaffold(
                                     contentWindowInsets = if (isTopLevelScreen) ScaffoldDefaults.contentWindowInsets else WindowInsets(0, 0, 0, 0),
                                     topBar = {
                                         if (isTopLevelScreen) {
                                             val pageTitle = when {
                                                 currentRoute.startsWith("wallet_details") -> "Transactions"
                                                 currentRoute.startsWith("categories") -> "Categories"
                                                 currentRoute.startsWith("debts") -> "Debts & Credits"
                                                 currentRoute.startsWith("budgets") -> "Budgets"
                                                 currentRoute.startsWith("savings") -> "Savings Goals"
                                                 currentRoute.startsWith("events") -> "Events"
                                                 currentRoute.startsWith("recurrence") -> "Recurrences"
                                                 currentRoute.startsWith("templates") -> "Models"
                                                 currentRoute.startsWith("places") -> "Places"
                                                 currentRoute.startsWith("people") -> "People"
                                                 currentRoute == "overview" -> "Overview"
                                                 currentRoute == "about" -> "About"
                                                 currentRoute == "support_developer" -> "Support Developer"
                                                 else -> "My Money"
                                             }
                                             TopAppBar(
                                                 title = {
                                                     Text(
                                                         text = pageTitle,
                                                         fontWeight = FontWeight.Bold
                                                     )
                                                 },
                                                 navigationIcon = {
                                                     IconButton(
                                                         onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } }
                                                     ) {
                                                         Icon(
                                                             imageVector = Icons.Default.Menu,
                                                             contentDescription = "Open Sidebar"
                                                         )
                                                     }
                                                 },
                                                 colors = TopAppBarDefaults.topAppBarColors(
                                                     containerColor = MaterialTheme.colorScheme.background
                                                 )
                                             )
                                         }
                                     },

                                ) { paddingValues ->
                                    Box(modifier = Modifier.padding(paddingValues)) {
                                        AppNavHost(
                                            navController = navController,
                                            startDestination = startDest,
                                            currentWalletId = currentWalletId
                                        )
                                    }
                                }
                            } else {
                                NavigationMenuPage(
                                    selectedItemId = selectedItemId,
                                    onReturnToMain = {
                                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                                    },
                                    onWalletSelect = { wallet ->
                                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                                        navController.navigate("wallet_details/${wallet.wallet.id}")
                                    },
                                    onAddWallet = {
                                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                                        navController.navigate("settings")
                                    },
                                    onManageWallets = {
                                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                                        navController.navigate("settings")
                                    },
                                    onItemClick = { item ->
                                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                                        handleSidebarNavigation(
                                            context = this@MainActivity,
                                            navController = navController,
                                            itemId = item.id,
                                            currentWalletId = currentWalletId,
                                            currentRoute = currentRoute
                                        )
                                    }
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