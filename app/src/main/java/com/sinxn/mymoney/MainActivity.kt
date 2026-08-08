package com.sinxn.mymoney

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
                        NavHost(navController = navController, startDestination = startDest) {
                            composable("home") {
                                HomeScreen(
                                    onNavigateToBackup = {
                                        navController.navigate("backup")
                                    },
                                    onNavigateToWallet = { walletId ->
                                        navController.navigate("wallet_details/$walletId")
                                    },
                                    onNavigateToSettings = {
                                        navController.navigate("settings")
                                    },
                                    onAddTransaction = {
                                        navController.navigate("transaction_details/new")
                                    },
                                    onNavigateToDebts = { walletId ->
                                        val route = if (!walletId.isNullOrBlank()) "debts?walletId=$walletId" else "debts"
                                        navController.navigate(route)
                                    },
                                    onAddDebt = { type ->
                                        navController.navigate("debt_details/new?type=$type")
                                    },
                                    onDebtClick = { debtId ->
                                        navController.navigate("debt_details/$debtId")
                                    },
                                    onNavigateToBudgets = {
                                        navController.navigate("budgets")
                                    },
                                    onNavigateToSavings = {
                                        navController.navigate("savings")
                                    },
                                    onNavigateToRecurrences = {
                                        navController.navigate("recurrences")
                                    },
                                    onNavigateMenuItem = { itemId ->
                                        com.sinxn.mymoney.core.ui.components.handleSidebarNavigation(
                                            context = this@MainActivity,
                                            navController = navController,
                                            itemId = itemId,
                                            currentRoute = "home"
                                        )
                                    }
                                )
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
                            composable(
                                "debts?walletId={walletId}",
                                arguments = listOf(
                                    androidx.navigation.navArgument("walletId") {
                                        type = androidx.navigation.NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    }
                                )
                            ) { backStackEntry ->
                                val walletId = backStackEntry.arguments?.getString("walletId")
                                com.sinxn.mymoney.feature.debt.DebtListScreen(
                                    onNavigateUp = { navController.navigateUp() },
                                    onDebtClick = { debtId ->
                                        navController.navigate("debt_details/$debtId")
                                    },
                                    onAddDebt = { type ->
                                        val addRoute = if (!walletId.isNullOrBlank()) "debt_details/new?type=$type&walletId=$walletId" else "debt_details/new?type=$type"
                                        navController.navigate(addRoute)
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
                                "debt_details/{debtId}?type={type}&walletId={walletId}",
                                arguments = listOf(
                                    androidx.navigation.navArgument("debtId") { type = androidx.navigation.NavType.StringType },
                                    androidx.navigation.navArgument("type") {
                                        type = androidx.navigation.NavType.IntType
                                        defaultValue = 0
                                    },
                                    androidx.navigation.navArgument("walletId") {
                                        type = androidx.navigation.NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    }
                                )
                            ) {
                                com.sinxn.mymoney.feature.debt.DebtDetailsScreen(
                                    onNavigateBack = { navController.navigateUp() },
                                    onRecordPayment = { debtId, walletId, debtAction ->
                                        navController.navigate("transaction_details/new?walletId=$walletId&debtId=$debtId&debtAction=$debtAction")
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
                                    onNavigateToDebts = { walletId ->
                                        val route = if (!walletId.isNullOrBlank()) "debts?walletId=$walletId" else "debts"
                                        navController.navigate(route)
                                    },
                                    onAddDebt = { walletId, type ->
                                        val route = if (!walletId.isNullOrBlank()) "debt_details/new?type=$type&walletId=$walletId" else "debt_details/new?type=$type"
                                        navController.navigate(route)
                                    },
                                    onDebtClick = { debtId ->
                                        navController.navigate("debt_details/$debtId")
                                    },
                                    onNavigateToBudgets = { walletId ->
                                        val route = if (!walletId.isNullOrBlank()) "budgets?walletId=$walletId" else "budgets"
                                        navController.navigate(route)
                                    },
                                    onNavigateToSavings = { walletId ->
                                        val route = if (!walletId.isNullOrBlank()) "savings?walletId=$walletId" else "savings"
                                        navController.navigate(route)
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
                                "transaction_details/{transactionId}?savingId={savingId}&action={action}&walletId={walletId}&debtId={debtId}&debtAction={debtAction}",
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
                                    androidx.navigation.navArgument("walletId") {
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
                            composable(
                                "budgets?walletId={walletId}",
                                arguments = listOf(
                                    androidx.navigation.navArgument("walletId") {
                                        type = androidx.navigation.NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    }
                                )
                            ) { backStackEntry ->
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

                            composable(
                                "savings?walletId={walletId}",
                                arguments = listOf(
                                    androidx.navigation.navArgument("walletId") {
                                        type = androidx.navigation.NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    }
                                )
                            ) { backStackEntry ->
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