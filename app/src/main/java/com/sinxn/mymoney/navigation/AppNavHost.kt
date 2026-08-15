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
import com.sinxn.mymoney.feature.category.CategoryAddEditScreen
import com.sinxn.mymoney.feature.category.CategoryDetailsScreen
import com.sinxn.mymoney.feature.category.CategoryListScreen
import com.sinxn.mymoney.feature.debt.DebtListScreen
import com.sinxn.mymoney.feature.event.EventListScreen
import com.sinxn.mymoney.feature.overview.OverviewScreen
import com.sinxn.mymoney.feature.people.PeopleListScreen
import com.sinxn.mymoney.feature.people.PersonAddEditScreen
import com.sinxn.mymoney.feature.people.PersonDetailsScreen
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
                    navController.navigate(Screen.RecurrentTransactionDetails.createRoute("new"))
                },
                onAddRecurrentTransfer = {
                    navController.navigate(Screen.RecurrentTransferDetails.createRoute("new"))
                },
                onRecurrentTransactionClick = { id ->
                    navController.navigate(Screen.RecurrentTransactionDetails.createRoute(id))
                },
                onRecurrentTransferClick = { id ->
                    navController.navigate(Screen.RecurrentTransferDetails.createRoute(id))
                }
            )
        }
        composable(
            Screen.RecurrentTransactionDetails.routePattern,
            arguments = listOf(
                navArgument("id") { type = NavType.StringType }
            )
        ) {
            RecurrentTransactionDetailsScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(
            Screen.RecurrentTransferDetails.routePattern,
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
                    navController.navigate(Screen.DebtDetails.createRoute(debtId))
                },
                onAddDebt = { type ->
                    navController.navigate(Screen.DebtDetails.createRoute("new", type))
                },
                onNavigateMenuItem = { itemId ->
                    handleSidebarNavigation(
                        context = context,
                        navController = navController,
                        itemId = itemId,
                        currentWalletId = currentWalletId
                    )
                },
                onNavigateToWallet = { walletId ->
                    navController.navigate(Screen.Transactions.createRoute(walletId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.routePattern)
                }
            )
        }
        composable(
            Screen.DebtDetails.routePattern,
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
                    navController.navigate(Screen.TransactionDetails.createRoute("new", debtId = debtId, debtAction = debtAction))
                }
            )
        }
        composable(Screen.Backup.routePattern) {
            BackupScreen(
                onNavigateUp = { navController.navigateUp() }
            )
        }
        composable(Screen.Settings.routePattern) {
            SettingsScreen(
                onNavigateUp = { navController.navigateUp() },
                onNavigateToImport = {
                    navController.navigate(Screen.Backup.routePattern)
                },
                onNavigateToSqlConsole = {
                    navController.navigate(Screen.SqlConsole.routePattern)
                }
            )
        }
        composable(Screen.SqlConsole.routePattern) {
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
                    navController.navigate(Screen.TransactionDetails.createRoute(transactionId))
                },
                onNavigateToRecap = {
                    navController.navigate(Screen.Recap.routePattern)
                },
                onAddTransaction = {
                    navController.navigate(Screen.TransactionDetails.createRoute("new"))
                },
                onNavigateToDebts = { _ ->
                    navController.navigate(Screen.Debts.routePattern)
                },
                onAddDebt = { _, type ->
                    navController.navigate(Screen.DebtDetails.createRoute("new", type))
                },
                onDebtClick = { debtId ->
                    navController.navigate(Screen.DebtDetails.createRoute(debtId))
                },
                onNavigateToBudgets = { _ ->
                    navController.navigate(Screen.Budgets.routePattern)
                },
                onNavigateToSavings = { _ ->
                    navController.navigate(Screen.Savings.routePattern)
                },
                onNavigateToWallet = { walletId ->
                    navController.navigate(Screen.Transactions.createRoute(walletId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.routePattern)
                },
                onNavigateMenuItem = { itemId ->
                    handleSidebarNavigation(
                        context = context,
                        navController = navController,
                        itemId = itemId,
                        currentWalletId = currentWalletId
                    )
                }
            )
        }
        composable(
            Screen.TransactionDetails.routePattern,
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
                    navController.navigate(Screen.BudgetOverview.createRoute(budgetId))
                },
                onEditBudget = { budgetId ->
                    navController.navigate(Screen.BudgetDetails.createRoute(budgetId))
                },
                onAddBudget = {
                    navController.navigate(Screen.BudgetDetails.createRoute("new"))
                },
                onNavigateMenuItem = { itemId ->
                    handleSidebarNavigation(
                        context = context,
                        navController = navController,
                        itemId = itemId,
                        currentWalletId = currentWalletId
                    )
                },
                onNavigateToWallet = { walletId ->
                    navController.navigate(Screen.Transactions.createRoute(walletId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.routePattern)
                }
            )
        }
        composable(
            Screen.BudgetOverview.routePattern,
            arguments = listOf(
                navArgument("budgetId") { type = NavType.StringType }
            )
        ) {
            BudgetOverviewScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToEdit = { budgetId ->
                    navController.navigate(Screen.BudgetDetails.createRoute(budgetId))
                },
                onTransactionClick = { transactionId ->
                    navController.navigate(Screen.TransactionDetails.createRoute(transactionId))
                }
            )
        }
        composable(
            Screen.BudgetDetails.routePattern,
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
                    navController.navigate(Screen.SavingDetails.createRoute(savingId))
                },
                onAddSaving = {
                    navController.navigate(Screen.SavingDetails.createRoute("new"))
                },
                onDeposit = { savingId ->
                    navController.navigate(Screen.TransactionDetails.createRoute("new", savingId = savingId, action = "deposit"))
                },
                onWithdraw = { savingId ->
                    navController.navigate(Screen.TransactionDetails.createRoute("new", savingId = savingId, action = "withdraw"))
                },
                onWithdrawEverything = { savingId ->
                    navController.navigate(Screen.TransactionDetails.createRoute("new", savingId = savingId, action = "withdraw_everything"))
                },
                onNavigateMenuItem = { itemId ->
                    handleSidebarNavigation(
                        context = context,
                        navController = navController,
                        itemId = itemId,
                        currentWalletId = currentWalletId
                    )
                },
                onNavigateToWallet = { walletId ->
                    navController.navigate(Screen.Transactions.createRoute(walletId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.routePattern)
                }
            )
        }
        composable(
            Screen.SavingDetails.routePattern,
            arguments = listOf(
                navArgument("savingId") { type = NavType.StringType }
            )
        ) {
            SavingDetailsScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.Recap.routePattern) {
            YearRecapScreen(
                onClose = { navController.popBackStack() }
            )
        }
        composable(Screen.Categories.routePattern) {
            CategoryListScreen(
                onNavigateBack = { navController.navigateUp() },
                onCategoryClick = { categoryId ->
                    navController.navigate(Screen.CategoryDetails.createRoute(categoryId))
                },
                onAddCategoryClick = { type ->
                    navController.navigate(Screen.CategoryAddEdit.createRoute(type = type))
                }
            )
        }
        composable(
            Screen.CategoryDetails.routePattern,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType }
            )
        ) {
            CategoryDetailsScreen(
                onNavigateBack = { navController.navigateUp() },
                onTransactionClick = { transactionId ->
                    navController.navigate(Screen.TransactionDetails.createRoute(transactionId))
                },
                onSubcategoryClick = { subcategoryId ->
                    navController.navigate(Screen.CategoryDetails.createRoute(subcategoryId))
                },
                onEditCategoryClick = { categoryId ->
                    navController.navigate(Screen.CategoryAddEdit.createRoute(categoryId = categoryId))
                },
                onAddSubcategoryClick = { parentId, type ->
                    navController.navigate(Screen.CategoryAddEdit.createRoute(parentId = parentId, type = type))
                }
            )
        }
        composable(
            Screen.CategoryAddEdit.routePattern,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("type") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("parentId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) {
            CategoryAddEditScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.People.routePattern) {
            PeopleListScreen(
                onNavigateBack = { navController.navigateUp() },
                onPersonClick = { personId ->
                    navController.navigate(Screen.PersonDetails.createRoute(personId))
                },
                onAddPersonClick = {
                    navController.navigate(Screen.PersonAddEdit.createRoute())
                }
            )
        }
        composable(
            Screen.PersonDetails.routePattern,
            arguments = listOf(
                navArgument("personId") { type = NavType.StringType }
            )
        ) {
            PersonDetailsScreen(
                onNavigateBack = { navController.navigateUp() },
                onTransactionClick = { transactionId ->
                    navController.navigate(Screen.TransactionDetails.createRoute(transactionId))
                },
                onEditPersonClick = { personId ->
                    navController.navigate(Screen.PersonAddEdit.createRoute(personId))
                }
            )
        }
        composable(
            Screen.PersonAddEdit.routePattern,
            arguments = listOf(
                navArgument("personId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) {
            PersonAddEditScreen(
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
                onNavigateToRecap = { navController.navigate(Screen.Recap.routePattern) }
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
        composable(Screen.Menu.routePattern) {
            com.sinxn.mymoney.core.ui.components.NavigationMenuContent(
                selectedItemId = null,
                onWalletSelect = { wallet ->
                    navController.navigate(Screen.Transactions.createRoute(wallet.wallet.id)) {
                        popUpTo(navController.graph.id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onAddWallet = {
                    navController.navigate(Screen.Settings.routePattern)
                },
                onManageWallets = {
                    navController.navigate(Screen.Settings.routePattern)
                },
                onItemClick = { item ->
                    handleSidebarNavigation(
                        context = context,
                        navController = navController,
                        itemId = item.id,
                        currentWalletId = currentWalletId
                    )
                }
            )
        }
    }
}
