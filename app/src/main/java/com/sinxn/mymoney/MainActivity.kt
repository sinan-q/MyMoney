package com.sinxn.mymoney

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sinxn.mymoney.feature.home.HomeScreen
import com.sinxn.mymoney.feature.settings.BackupScreen
import com.sinxn.mymoney.feature.settings.SqlConsoleScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.sinxn.mymoney.ui.theme.MyMoneyTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var recurrenceRepository: com.sinxn.mymoney.core.data.repository.RecurrenceRepository

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
                    val viewModel: MainViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                    val startDest by viewModel.startDestination.collectAsState()
                    
                    if (startDest != "loading") {
                        val pagerState = androidx.compose.foundation.pager.rememberPagerState(initialPage = 1, pageCount = { 2 })
                        val allWallets by viewModel.allWallets.collectAsState()
                        val formattingSettings by viewModel.formattingSettings.collectAsState()
                        val formatterConfig = com.sinxn.mymoney.core.util.MoneyFormatter.Config(
                            showCurrency = formattingSettings.showCurrency,
                            groupDigits = formattingSettings.groupDigits,
                            roundDecimals = formattingSettings.roundDecimals,
                            showPlusMinus = formattingSettings.showPlusMinus
                        )
                        val currentBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = currentBackStackEntry?.destination?.route?.substringBefore("?") ?: "wallet_details/${com.sinxn.mymoney.core.util.Constants.TOTAL_WALLET_ID}"
                        
                        val isWalletRelated = currentRoute.startsWith("wallet_details")
                        
                        var isWalletListExpanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                        
                        val currentWalletId by viewModel.currentWalletId.collectAsState()
                        
                        val selectedWallet = allWallets.find { it.wallet.id == currentWalletId }
                            ?: allWallets.firstOrNull { it.wallet.id == com.sinxn.mymoney.core.util.Constants.TOTAL_WALLET_ID }
                            ?: allWallets.firstOrNull()
                        
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
                        
                        val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

                        androidx.compose.foundation.pager.HorizontalPager(state = pagerState) { page ->
                            if (page == 1) {
                                val isTopLevelScreen = currentRoute.substringBefore("?") in listOf(
                                    "wallet_details/{walletId}", "wallet_details",
                                    "categories", "debts", "budgets", "savings",
                                    "events", "recurrences", "templates", "places", "people",
                                    "overview", "about", "support_developer"
                                )

                                androidx.compose.material3.Scaffold(
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
                                             androidx.compose.material3.TopAppBar(
                                                 title = {
                                                     androidx.compose.material3.Text(
                                                         text = pageTitle,
                                                         fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                                     )
                                                 },
                                                 navigationIcon = {
                                                     androidx.compose.material3.IconButton(
                                                         onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } }
                                                     ) {
                                                         androidx.compose.material3.Icon(
                                                             imageVector = androidx.compose.material.icons.Icons.Default.Menu,
                                                             contentDescription = "Open Sidebar"
                                                         )
                                                     }
                                                 },
                                                 colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                                                     containerColor = MaterialTheme.colorScheme.background
                                                 )
                                             )
                                         }
                                     },
                                    floatingActionButton = {
                                        if (isTopLevelScreen && isWalletRelated) {
                                            if (selectedItemId == "transactions") {
                                                androidx.compose.material3.FloatingActionButton(
                                                    onClick = { navController.navigate("transaction_details/new") },
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                                                ) {
                                                    androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Default.Add, contentDescription = "Add Transaction")
                                                }
                                            }
                                        }
                                    }
                                ) { paddingValues ->
                                    androidx.compose.foundation.layout.Box(modifier = Modifier.padding(paddingValues)) {
                                        NavHost(navController = navController, startDestination = startDest) {
                                            composable("home") {
                                                // Redirect to current wallet details
                                                androidx.compose.runtime.LaunchedEffect(Unit) {
                                                    val targetWallet = if (currentWalletId.isEmpty()) com.sinxn.mymoney.core.util.Constants.TOTAL_WALLET_ID else currentWalletId
                                                    navController.navigate("wallet_details/$targetWallet") {
                                                        popUpTo("home") { inclusive = true }
                                                    }
                                                }
                                            }
                            composable("recurrences") {
                                com.sinxn.mymoney.feature.recurrence.RecurrenceScreen(
                                    onNavigateUp = { navController.navigateUp() },
                                    onAddRecurrentTransaction = {
                                        navController.navigate("recurrent_transaction_details/new")
                                    },
                                    onAddRecurrentTransfer = {
                                        navController.navigate("recurrent_transfer_details/new")
                                    },
                                    onRecurrentTransactionClick = { id ->
                                        navController.navigate("recurrent_transaction_details/$id")
                                    },
                                    onRecurrentTransferClick = { id ->
                                        navController.navigate("recurrent_transfer_details/$id")
                                    }
                                )
                            }
                            composable(
                                "recurrent_transaction_details/{id}",
                                arguments = listOf(
                                    androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.StringType }
                                )
                            ) {
                                com.sinxn.mymoney.feature.recurrence.RecurrentTransactionDetailsScreen(
                                    onNavigateBack = { navController.navigateUp() }
                                )
                            }
                            composable(
                                "recurrent_transfer_details/{id}",
                                arguments = listOf(
                                    androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.StringType }
                                )
                            ) {
                                com.sinxn.mymoney.feature.recurrence.RecurrentTransferDetailsScreen(
                                    onNavigateBack = { navController.navigateUp() }
                                )
                            }
                            composable("debts") { backStackEntry ->
                                com.sinxn.mymoney.feature.debt.DebtListScreen(
                                    onNavigateUp = { navController.navigateUp() },
                                    onDebtClick = { debtId ->
                                        navController.navigate("debt_details/$debtId")
                                    },
                                    onAddDebt = { type ->
                                        navController.navigate("debt_details/new?type=$type")
                                    },
                                    onNavigateMenuItem = { itemId ->
                                        com.sinxn.mymoney.core.ui.components.handleSidebarNavigation(
                                            context = this@MainActivity,
                                            navController = navController,
                                            itemId = itemId,
                                            currentRoute = "debts"
                                        )
                                    },
                                    onNavigateToWallet = { walletId ->
                                        navController.navigate("wallet_details/$walletId")
                                    },
                                    onNavigateToSettings = {
                                        navController.navigate("settings")
                                    }
                                )
                            }
                            composable(
                                "debt_details/{debtId}?type={type}",
                                arguments = listOf(
                                    androidx.navigation.navArgument("debtId") { type = androidx.navigation.NavType.StringType },
                                    androidx.navigation.navArgument("type") {
                                        type = androidx.navigation.NavType.IntType
                                        defaultValue = 0
                                    }
                                )
                            ) {
                                com.sinxn.mymoney.feature.debt.DebtDetailsScreen(
                                    onNavigateBack = { navController.navigateUp() },
                                    onRecordPayment = { debtId, _, debtAction ->
                                        navController.navigate("transaction_details/new?debtId=$debtId&debtAction=$debtAction")
                                    }
                                )
                            }
                            composable("backup") {
                                BackupScreen()
                            }
                            composable("settings") {
                                com.sinxn.mymoney.feature.settings.SettingsScreen(
                                    onNavigateUp = { navController.navigateUp() },
                                    onNavigateToSqlConsole = {
                                        navController.navigate("sql_console")
                                    }
                                )
                            }
                            composable("sql_console") {
                                SqlConsoleScreen(
                                    onNavigateUp = { navController.navigateUp() }
                                )
                            }
                            composable("wallet_details/{walletId}") {
                                com.sinxn.mymoney.feature.wallet.WalletDetailsScreen(
                                    onNavigateUp = { 
                                        if (navController.previousBackStackEntry != null) {
                                            navController.navigateUp()
                                        } else {
                                            // If launched directly here, back should go home
                                            navController.navigate("home") {
                                                popUpTo("wallet_details/{walletId}") { inclusive = true }
                                            }
                                        }
                                    },
                                    onTransactionClick = { transactionId ->
                                        navController.navigate("transaction_details/$transactionId")
                                    },
                                    onNavigateToRecap = {
                                        navController.navigate("recap")
                                    },
                                    onAddTransaction = {
                                        navController.navigate("transaction_details/new")
                                    },
                                    onNavigateToDebts = { _ ->
                                        navController.navigate("debts")
                                    },
                                    onAddDebt = { _, type ->
                                        navController.navigate("debt_details/new?type=$type")
                                    },
                                    onDebtClick = { debtId ->
                                        navController.navigate("debt_details/$debtId")
                                    },
                                    onNavigateToBudgets = { _ ->
                                        navController.navigate("budgets")
                                    },
                                    onNavigateToSavings = { _ ->
                                        navController.navigate("savings")
                                    },
                                    onNavigateToWallet = { walletId ->
                                        navController.navigate("wallet_details/$walletId")
                                    },
                                    onNavigateToSettings = {
                                        navController.navigate("settings")
                                    },
                                    onNavigateMenuItem = { itemId ->
                                        com.sinxn.mymoney.core.ui.components.handleSidebarNavigation(
                                            context = this@MainActivity,
                                            navController = navController,
                                            itemId = itemId,
                                            currentRoute = "wallet_details"
                                        )
                                    }
                                )
                            }
                            composable(
                                "transaction_details/{transactionId}?savingId={savingId}&action={action}&debtId={debtId}&debtAction={debtAction}",
                                arguments = listOf(
                                    androidx.navigation.navArgument("transactionId") { type = androidx.navigation.NavType.StringType },
                                    androidx.navigation.navArgument("savingId") {
                                        type = androidx.navigation.NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    },
                                    androidx.navigation.navArgument("action") {
                                        type = androidx.navigation.NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    },
                                    androidx.navigation.navArgument("debtId") {
                                        type = androidx.navigation.NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    },
                                    androidx.navigation.navArgument("debtAction") {
                                        type = androidx.navigation.NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    }
                                ),
                                deepLinks = listOf(
                                    androidx.navigation.navDeepLink { uriPattern = "mymoney://transaction/{transactionId}" }
                                )
                            ) {
                                com.sinxn.mymoney.feature.transaction.TransactionDetailsScreen(
                                    onNavigateBack = { navController.navigateUp() }
                                )
                            }
                            composable("budgets") { backStackEntry ->
                                com.sinxn.mymoney.feature.budget.BudgetListScreen(
                                    onNavigateUp = { navController.navigateUp() },
                                    onBudgetClick = { budgetId ->
                                        navController.navigate("budget_overview/$budgetId")
                                    },
                                    onEditBudget = { budgetId ->
                                        navController.navigate("budget_details/$budgetId")
                                    },
                                    onAddBudget = {
                                        navController.navigate("budget_details/new")
                                    },
                                    onNavigateMenuItem = { itemId ->
                                        com.sinxn.mymoney.core.ui.components.handleSidebarNavigation(
                                            context = this@MainActivity,
                                            navController = navController,
                                            itemId = itemId,
                                            currentRoute = "budgets"
                                        )
                                    },
                                    onNavigateToWallet = { walletId ->
                                        navController.navigate("wallet_details/$walletId")
                                    },
                                    onNavigateToSettings = {
                                        navController.navigate("settings")
                                    }
                                )
                            }
                            composable(
                                "budget_overview/{budgetId}",
                                arguments = listOf(
                                    androidx.navigation.navArgument("budgetId") { type = androidx.navigation.NavType.StringType }
                                )
                            ) {
                                com.sinxn.mymoney.feature.budget.BudgetOverviewScreen(
                                    onNavigateBack = { navController.navigateUp() },
                                    onNavigateToEdit = { budgetId ->
                                        navController.navigate("budget_details/$budgetId")
                                    },
                                    onTransactionClick = { transactionId ->
                                        navController.navigate("transaction_details/$transactionId")
                                    }
                                )
                            }
                            composable(
                                "budget_details/{budgetId}",
                                arguments = listOf(
                                    androidx.navigation.navArgument("budgetId") { type = androidx.navigation.NavType.StringType }
                                )
                            ) {
                                com.sinxn.mymoney.feature.budget.BudgetDetailsScreen(
                                    onNavigateBack = { navController.navigateUp() }
                                )
                            }

                            composable("savings") { backStackEntry ->
                                com.sinxn.mymoney.feature.saving.SavingListScreen(
                                    onNavigateUp = { navController.navigateUp() },
                                    onSavingClick = { savingId ->
                                        navController.navigate("saving_details/$savingId")
                                    },
                                    onAddSaving = {
                                        navController.navigate("saving_details/new")
                                    },
                                    onDeposit = { savingId ->
                                        navController.navigate("transaction_details/new?savingId=$savingId&action=deposit")
                                    },
                                    onWithdraw = { savingId ->
                                        navController.navigate("transaction_details/new?savingId=$savingId&action=withdraw")
                                    },
                                    onWithdrawEverything = { savingId ->
                                        navController.navigate("transaction_details/new?savingId=$savingId&action=withdraw_everything")
                                    },
                                    onNavigateMenuItem = { itemId ->
                                        com.sinxn.mymoney.core.ui.components.handleSidebarNavigation(
                                            context = this@MainActivity,
                                            navController = navController,
                                            itemId = itemId,
                                            currentRoute = "savings"
                                        )
                                    },
                                    onNavigateToWallet = { walletId ->
                                        navController.navigate("wallet_details/$walletId")
                                    },
                                    onNavigateToSettings = {
                                        navController.navigate("settings")
                                    }
                                )
                            }
                            composable(
                                "saving_details/{savingId}",
                                arguments = listOf(
                                    androidx.navigation.navArgument("savingId") { type = androidx.navigation.NavType.StringType }
                                )
                            ) {
                                com.sinxn.mymoney.feature.saving.SavingDetailsScreen(
                                    onNavigateBack = { navController.navigateUp() }
                                )
                            }
                            composable("recap") {
                                com.sinxn.mymoney.feature.recap.YearRecapScreen(
                                    onClose = { navController.popBackStack() }
                                )
                            }
                            composable("categories") {
                                com.sinxn.mymoney.feature.category.CategoryListScreen(
                                    onNavigateBack = { navController.navigateUp() }
                                )
                            }
                            composable("people") {
                                com.sinxn.mymoney.feature.people.PeopleListScreen(
                                    onNavigateBack = { navController.navigateUp() }
                                )
                            }
                            composable("places") {
                                com.sinxn.mymoney.feature.place.PlaceListScreen(
                                    onNavigateBack = { navController.navigateUp() }
                                )
                            }
                            composable("events") {
                                com.sinxn.mymoney.feature.event.EventListScreen(
                                    onNavigateBack = { navController.navigateUp() }
                                )
                            }
                            composable("templates") {
                                com.sinxn.mymoney.feature.template.TemplateListScreen(
                                    onNavigateBack = { navController.navigateUp() }
                                )
                            }
                            composable("overview") {
                                com.sinxn.mymoney.feature.overview.OverviewScreen(
                                    onNavigateBack = { navController.navigateUp() },
                                    onNavigateToRecap = { navController.navigate("recap") }
                                )
                            }
                            composable("about") {
                                com.sinxn.mymoney.feature.about.AboutScreen(
                                    onNavigateBack = { navController.navigateUp() }
                                )
                            }
                            composable("support_developer") {
                                com.sinxn.mymoney.feature.support.SupportDeveloperScreen(
                                    onNavigateBack = { navController.navigateUp() }
                                )
                            }
                                        }
                                    }
                                }
                            } else {
                                com.sinxn.mymoney.core.ui.components.NavigationMenuPage(
                                    wallets = allWallets,
                                    selectedWallet = selectedWallet,
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
                                        com.sinxn.mymoney.core.ui.components.handleSidebarNavigation(
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
                        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                             androidx.compose.material3.CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}