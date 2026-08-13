package com.sinxn.mymoney.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.sinxn.mymoney.core.ui.components.handleSidebarNavigation
import com.sinxn.mymoney.core.util.Constants
import com.sinxn.mymoney.feature.about.AboutScreen
import com.sinxn.mymoney.feature.budget.BudgetDetailsScreen
import com.sinxn.mymoney.feature.budget.BudgetListScreen
import com.sinxn.mymoney.feature.budget.BudgetOverviewScreen
import com.sinxn.mymoney.feature.category.CategoryListScreen
import com.sinxn.mymoney.feature.debt.DebtListScreen
import com.sinxn.mymoney.feature.event.EventListScreen
import com.sinxn.mymoney.feature.overview.OverviewScreen
import com.sinxn.mymoney.feature.people.PeopleListScreen
import com.sinxn.mymoney.feature.place.PlaceListScreen
import com.sinxn.mymoney.feature.recap.YearRecapScreen
import com.sinxn.mymoney.feature.recurrence.RecurrenceScreen
import com.sinxn.mymoney.feature.recurrence.RecurrentTransactionDetailsScreen
import com.sinxn.mymoney.feature.recurrence.RecurrentTransferDetailsScreen
import com.sinxn.mymoney.feature.saving.SavingDetailsScreen
import com.sinxn.mymoney.feature.saving.SavingListScreen
import com.sinxn.mymoney.feature.settings.BackupScreen
import com.sinxn.mymoney.feature.settings.SettingsScreen
import com.sinxn.mymoney.feature.settings.SqlConsoleScreen
import com.sinxn.mymoney.feature.support.SupportDeveloperScreen
import com.sinxn.mymoney.feature.template.TemplateListScreen
import com.sinxn.mymoney.feature.transaction.TransactionDetailsScreen
import com.sinxn.mymoney.feature.wallet.WalletDetailsScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    currentWalletId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable("home") {
            // Redirect to current wallet details
            LaunchedEffect(Unit) {
                val targetWallet = currentWalletId.ifEmpty { Constants.TOTAL_WALLET_ID }
                navController.navigate(Screen.Transactions.createRoute(targetWallet)) {
                    popUpTo("home") { inclusive = true }
                }
            }
        }
        composable(Screen.Recurrences.routePattern) {
            RecurrenceScreen(
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
                navArgument("id") { type = NavType.StringType }
            )
        ) {
            RecurrentTransactionDetailsScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(
            "recurrent_transfer_details/{id}",
            arguments = listOf(
                navArgument("id") { type = NavType.StringType }
            )
        ) {
            RecurrentTransferDetailsScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.Debts.routePattern) { _ ->
            DebtListScreen(
                onNavigateUp = { navController.navigateUp() },
                onDebtClick = { debtId ->
                    navController.navigate("debt_details/$debtId")
                },
                onAddDebt = { type ->
                    navController.navigate("debt_details/new?type=$type")
                },
                onNavigateMenuItem = { itemId ->
                    handleSidebarNavigation(
                        context = context,
                        navController = navController,
                        itemId = itemId,
                        currentWalletId = currentWalletId,
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
                navArgument("debtId") { type = NavType.StringType },
                navArgument("type") {
                    type = NavType.IntType
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
        composable(Screen.Settings.routePattern) {
            SettingsScreen(
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
            WalletDetailsScreen(
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
                    handleSidebarNavigation(
                        context = context,
                        navController = navController,
                        itemId = itemId,
                        currentWalletId = currentWalletId,
                        currentRoute = "wallet_details"
                    )
                }
            )
        }
        composable(
            "transaction_details/{transactionId}?savingId={savingId}&action={action}&debtId={debtId}&debtAction={debtAction}",
            arguments = listOf(
                navArgument("transactionId") { type = NavType.StringType },
                navArgument("savingId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("action") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("debtId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("debtAction") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "mymoney://transaction/{transactionId}" }
            )
        ) {
            TransactionDetailsScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.Budgets.routePattern) { _ ->
            BudgetListScreen(
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
                    handleSidebarNavigation(
                        context = context,
                        navController = navController,
                        itemId = itemId,
                        currentWalletId = currentWalletId,
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
                navArgument("budgetId") { type = NavType.StringType }
            )
        ) {
            BudgetOverviewScreen(
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
                navArgument("budgetId") { type = NavType.StringType }
            )
        ) {
            BudgetDetailsScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(Screen.Savings.routePattern) { backStackEntry ->
            SavingListScreen(
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
                    handleSidebarNavigation(
                        context = context,
                        navController = navController,
                        itemId = itemId,
                        currentWalletId = currentWalletId,
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
                navArgument("savingId") { type = NavType.StringType }
            )
        ) {
            SavingDetailsScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable("recap") {
            YearRecapScreen(
                onClose = { navController.popBackStack() }
            )
        }
        composable(Screen.Categories.routePattern) {
            CategoryListScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.People.routePattern) {
            PeopleListScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.Places.routePattern) {
            PlaceListScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.Events.routePattern) {
            EventListScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.Templates.routePattern) {
            TemplateListScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.Overview.routePattern) {
            OverviewScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToRecap = { navController.navigate("recap") }
            )
        }
        composable(Screen.About.routePattern) {
            AboutScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.SupportDeveloper.routePattern) {
            SupportDeveloperScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
    }
}
