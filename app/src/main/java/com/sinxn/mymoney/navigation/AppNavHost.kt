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
import com.sinxn.mymoney.feature.budget.BudgetAddEditScreen
import com.sinxn.mymoney.feature.budget.BudgetDetailsScreen
import com.sinxn.mymoney.feature.budget.BudgetListScreen
import com.sinxn.mymoney.feature.category.CategoryAddEditScreen
import com.sinxn.mymoney.feature.category.CategoryDetailsScreen
import com.sinxn.mymoney.feature.category.CategoryListScreen
import com.sinxn.mymoney.feature.debt.DebtAddEditScreen
import com.sinxn.mymoney.feature.debt.DebtDetailsScreen
import com.sinxn.mymoney.feature.debt.DebtListScreen
import com.sinxn.mymoney.feature.event.EventAddEditScreen
import com.sinxn.mymoney.feature.event.EventDetailsScreen
import com.sinxn.mymoney.feature.event.EventListScreen
import com.sinxn.mymoney.feature.overview.OverviewScreen
import com.sinxn.mymoney.feature.overview.PeriodDetailScreen
import com.sinxn.mymoney.feature.people.PeopleListScreen
import com.sinxn.mymoney.feature.people.PersonAddEditScreen
import com.sinxn.mymoney.feature.people.PersonDetailsScreen
import com.sinxn.mymoney.feature.place.PlaceAddEditScreen
import com.sinxn.mymoney.feature.place.PlaceDetailsScreen
import com.sinxn.mymoney.feature.place.PlaceListScreen
import com.sinxn.mymoney.feature.recap.YearRecapScreen
import com.sinxn.mymoney.feature.recurrence.RecurrenceScreen
import com.sinxn.mymoney.feature.recurrence.RecurrentTransactionAddEditScreen
import com.sinxn.mymoney.feature.recurrence.RecurrentTransactionDetailsScreen
import com.sinxn.mymoney.feature.recurrence.RecurrentTransferAddEditScreen
import com.sinxn.mymoney.feature.recurrence.RecurrentTransferDetailsScreen
import com.sinxn.mymoney.feature.saving.SavingAddEditScreen
import com.sinxn.mymoney.feature.saving.SavingDetailsScreen
import com.sinxn.mymoney.feature.saving.SavingListScreen
import com.sinxn.mymoney.feature.settings.BackupScreen
import com.sinxn.mymoney.feature.settings.SettingsScreen
import com.sinxn.mymoney.feature.settings.SqlConsoleScreen
import com.sinxn.mymoney.feature.support.SupportDeveloperScreen
import com.sinxn.mymoney.feature.template.TemplateAddEditScreen
import com.sinxn.mymoney.feature.template.TemplateDetailsScreen
import com.sinxn.mymoney.feature.template.TemplateListScreen
import com.sinxn.mymoney.feature.transaction.TransactionAddEditScreen
import com.sinxn.mymoney.feature.transaction.TransactionDetailsScreen
import com.sinxn.mymoney.feature.transfer.TransferAddEditScreen
import com.sinxn.mymoney.feature.wallet.WalletAddEditScreen
import com.sinxn.mymoney.feature.wallet.WalletDetailsScreen
import com.sinxn.mymoney.feature.wallet.WalletInfoScreen
import com.sinxn.mymoney.feature.wallet.WalletListScreen

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
                    navController.navigate(Screen.RecurrentTransactionAddEdit.createRoute())
                },
                onAddRecurrentTransfer = {
                    navController.navigate(Screen.RecurrentTransferAddEdit.createRoute())
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
                onNavigateBack = { navController.navigateUp() },
                onEditClick = { id ->
                    navController.navigate(Screen.RecurrentTransactionAddEdit.createRoute(id))
                }
            )
        }
        composable(
            Screen.RecurrentTransactionAddEdit.routePattern,
            arguments = listOf(
                navArgument("id") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            RecurrentTransactionAddEditScreen(
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
                onNavigateBack = { navController.navigateUp() },
                onEditClick = { id ->
                    navController.navigate(Screen.RecurrentTransferAddEdit.createRoute(id))
                }
            )
        }
        composable(
            Screen.RecurrentTransferAddEdit.routePattern,
            arguments = listOf(
                navArgument("id") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            RecurrentTransferAddEditScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.Debts.routePattern) { _ ->
            DebtListScreen(
                onDebtClick = { debtId ->
                    navController.navigate(Screen.DebtDetails.createRoute(debtId))
                },
                onAddDebt = { type ->
                    navController.navigate(Screen.DebtAddEdit.createRoute(type = type))
                },
                onQuickPayment = { debtId ->
                    navController.navigate(Screen.TransactionAddEdit.createRoute(debtId = debtId))
                },
            )
        }
        composable(
            Screen.DebtDetails.routePattern,
            arguments = listOf(
                navArgument("debtId") { type = NavType.StringType }
            )
        ) {
            DebtDetailsScreen(
                onNavigateBack = { navController.navigateUp() },
                onEditDebtClick = { debtId ->
                    navController.navigate(Screen.DebtAddEdit.createRoute(debtId = debtId))
                },
                onTransactionClick = { transactionId ->
                    navController.navigate(Screen.TransactionDetails.createRoute(transactionId))
                },
                onRecordPayment = { debtId, _, debtAction ->
                    navController.navigate(Screen.TransactionAddEdit.createRoute(debtId = debtId, debtAction = debtAction))
                }
            )
        }
        composable(
            Screen.DebtAddEdit.routePattern,
            arguments = listOf(
                navArgument("debtId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("type") {
                    type = NavType.IntType
                    defaultValue = 0
                }
            )
        ) {
            DebtAddEditScreen(
                onNavigateBack = { navController.navigateUp() }
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
                onTransactionClick = { transactionId ->
                    navController.navigate(Screen.TransactionDetails.createRoute(transactionId))
                },
                onAddTransaction = {
                    navController.navigate(Screen.TransactionAddEdit.createRoute())
                },
                onNavigateToWallet = { walletId ->
                    navController.navigate(Screen.Transactions.createRoute(walletId))
                },
                onAddWallet = {
                    navController.navigate(Screen.WalletAddEdit.createRoute())
                },
                onManageWallets = {
                    navController.navigate(Screen.Wallets.routePattern)
                }
            )
        }
        composable(
            Screen.TransactionDetails.routePattern,
            arguments = listOf(
                navArgument("transactionId") { type = NavType.StringType }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "mymoney://transaction/{transactionId}" }
            )
        ) {
            TransactionDetailsScreen(
                onNavigateBack = { navController.navigateUp() },
                onEditClick = { transactionId, isTransfer ->
                    if (isTransfer) {
                        navController.navigate(Screen.TransferAddEdit.createRoute(transactionId = transactionId))
                    } else {
                        navController.navigate(Screen.TransactionAddEdit.createRoute(transactionId = transactionId))
                    }
                }
            )
        }
        composable(
            Screen.TransactionAddEdit.routePattern,
            arguments = listOf(
                navArgument("transactionId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
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
                },
                navArgument("templateId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            TransactionAddEditScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(
            Screen.TransferAddEdit.routePattern,
            arguments = listOf(
                navArgument("transactionId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("transferId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("walletId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("templateId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            TransferAddEditScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.Budgets.routePattern) { _ ->
            BudgetListScreen(
                onNavigateUp = { navController.navigateUp() },
                onBudgetClick = { budgetId ->
                    navController.navigate(Screen.BudgetDetails.createRoute(budgetId))
                },
                onEditBudget = { budgetId ->
                    navController.navigate(Screen.BudgetAddEdit.createRoute(budgetId))
                },
                onAddBudget = {
                    navController.navigate(Screen.BudgetAddEdit.createRoute())
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
            Screen.BudgetDetails.routePattern,
            arguments = listOf(
                navArgument("budgetId") { type = NavType.StringType }
            )
        ) {
            BudgetDetailsScreen(
                onNavigateBack = { navController.navigateUp() },
                onEditBudgetClick = { budgetId ->
                    navController.navigate(Screen.BudgetAddEdit.createRoute(budgetId))
                },
                onTransactionClick = { transactionId ->
                    navController.navigate(Screen.TransactionDetails.createRoute(transactionId))
                }
            )
        }
        composable(
            Screen.BudgetOverview.routePattern,
            arguments = listOf(
                navArgument("budgetId") { type = NavType.StringType }
            )
        ) {
            BudgetDetailsScreen(
                onNavigateBack = { navController.navigateUp() },
                onEditBudgetClick = { budgetId ->
                    navController.navigate(Screen.BudgetAddEdit.createRoute(budgetId))
                },
                onTransactionClick = { transactionId ->
                    navController.navigate(Screen.TransactionDetails.createRoute(transactionId))
                }
            )
        }
        composable(
            Screen.BudgetAddEdit.routePattern,
            arguments = listOf(
                navArgument("budgetId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) {
            BudgetAddEditScreen(
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
                    navController.navigate(Screen.SavingAddEdit.createRoute())
                },
                onEditSaving = { savingId ->
                    navController.navigate(Screen.SavingAddEdit.createRoute(savingId))
                },
                onDeposit = { savingId ->
                    navController.navigate(Screen.TransactionAddEdit.createRoute(savingId = savingId, action = "deposit"))
                },
                onWithdraw = { savingId ->
                    navController.navigate(Screen.TransactionAddEdit.createRoute(savingId = savingId, action = "withdraw"))
                },
                onWithdrawEverything = { savingId ->
                    navController.navigate(Screen.TransactionAddEdit.createRoute(savingId = savingId, action = "withdraw_everything"))
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
                onNavigateBack = { navController.navigateUp() },
                onEditSavingClick = { savingId ->
                    navController.navigate(Screen.SavingAddEdit.createRoute(savingId))
                },
                onTransactionClick = { transactionId ->
                    navController.navigate(Screen.TransactionDetails.createRoute(transactionId))
                },
                onDeposit = { savingId ->
                    navController.navigate(Screen.TransactionAddEdit.createRoute(savingId = savingId, action = "deposit"))
                },
                onWithdraw = { savingId ->
                    navController.navigate(Screen.TransactionAddEdit.createRoute(savingId = savingId, action = "withdraw"))
                },
                onWithdrawEverything = { savingId ->
                    navController.navigate(Screen.TransactionAddEdit.createRoute(savingId = savingId, action = "withdraw_everything"))
                }
            )
        }
        composable(
            Screen.SavingAddEdit.routePattern,
            arguments = listOf(
                navArgument("savingId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) {
            SavingAddEditScreen(
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
        composable(Screen.Wallets.routePattern) {
            WalletListScreen(
                onWalletClick = { walletId ->
                    navController.navigate(Screen.WalletInfo.createRoute(walletId))
                },
                onAddWalletClick = {
                    navController.navigate(Screen.WalletAddEdit.createRoute())
                }
            )
        }
        composable(
            Screen.WalletInfo.routePattern,
            arguments = listOf(
                navArgument("walletId") { type = NavType.StringType }
            )
        ) {
            WalletInfoScreen(
                onNavigateBack = { navController.navigateUp() },
                onTransactionClick = { transactionId ->
                    navController.navigate(Screen.TransactionDetails.createRoute(transactionId))
                },
                onEditWalletClick = { walletId ->
                    navController.navigate(Screen.WalletAddEdit.createRoute(walletId))
                }
            )
        }
        composable(
            Screen.WalletAddEdit.routePattern,
            arguments = listOf(
                navArgument("walletId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) {
            WalletAddEditScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.People.routePattern) {
            PeopleListScreen(
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
                onAddPlaceClick = {
                    navController.navigate(Screen.PlaceAddEdit.createRoute())
                },
                onPlaceClick = { placeId ->
                    navController.navigate(Screen.PlaceDetails.createRoute(placeId))
                }
            )
        }
        composable(Screen.PlaceDetails.routePattern) {
            PlaceDetailsScreen(
                onNavigateBack = { navController.navigateUp() },
                onTransactionClick = { transactionId ->
                    navController.navigate(Screen.TransactionDetails.createRoute(transactionId))
                },
                onEditPlaceClick = { placeId ->
                    navController.navigate(Screen.PlaceAddEdit.createRoute(placeId))
                }
            )
        }
        composable(
            Screen.PlaceAddEdit.routePattern,
            arguments = listOf(
                navArgument("placeId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) {
            PlaceAddEditScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.Events.routePattern) {
            EventListScreen(
                onAddEventClick = {
                    navController.navigate(Screen.EventAddEdit.createRoute())
                },
                onEventClick = { eventId ->
                    navController.navigate(Screen.EventDetails.createRoute(eventId))
                }
            )
        }
        composable(Screen.EventDetails.routePattern) {
            EventDetailsScreen(
                onNavigateBack = { navController.navigateUp() },
                onTransactionClick = { transactionId ->
                    navController.navigate(Screen.TransactionDetails.createRoute(transactionId))
                },
                onEditEventClick = { eventId ->
                    navController.navigate(Screen.EventAddEdit.createRoute(eventId))
                }
            )
        }
        composable(
            Screen.EventAddEdit.routePattern,
            arguments = listOf(
                navArgument("eventId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) {
            EventAddEditScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }


        composable(Screen.Templates.routePattern) {
            TemplateListScreen(
                onAddTemplateClick = { isTransfer ->
                    navController.navigate(Screen.TemplateAddEdit.createRoute(isTransfer = isTransfer))
                },
                onTemplateClick = { templateId, isTransfer ->
                    navController.navigate(Screen.TemplateDetails.createRoute(templateId, isTransfer))
                }
            )
        }
        composable(
            Screen.TemplateDetails.routePattern,
            arguments = listOf(
                navArgument("templateId") { type = NavType.StringType },
                navArgument("isTransfer") { type = NavType.StringType; nullable = true; defaultValue = "false" }
            )
        ) {
            TemplateDetailsScreen(
                onNavigateBack = { navController.navigateUp() },
                onEditTemplateClick = { templateId, isTransfer ->
                    navController.navigate(Screen.TemplateAddEdit.createRoute(templateId, isTransfer))
                },
                onUseInTransactionClick = { templateId, isTransfer ->
                    if (isTransfer) {
                        navController.navigate(Screen.TransferAddEdit.createRoute(templateId = templateId))
                    } else {
                        navController.navigate(Screen.TransactionAddEdit.createRoute(templateId = templateId))
                    }
                }
            )
        }
        composable(
            Screen.TemplateAddEdit.routePattern,
            arguments = listOf(
                navArgument("templateId") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("isTransfer") { type = NavType.StringType; nullable = true; defaultValue = "false" }
            )
        ) {
            TemplateAddEditScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.Overview.routePattern) {
            OverviewScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToRecap = { navController.navigate(Screen.Recap.routePattern) },
                onPeriodClick = { startDate, endDate ->
                    navController.navigate(Screen.PeriodDetail.createRoute(startDate, endDate))
                }
            )
        }
        composable(
            route = Screen.PeriodDetail.routePattern,
            arguments = listOf(
                navArgument("startDate") { type = NavType.StringType; defaultValue = "" },
                navArgument("endDate") { type = NavType.StringType; defaultValue = "" }
            )
        ) {
            PeriodDetailScreen(
                onNavigateBack = { navController.navigateUp() },
                onTransactionClick = { transactionId ->
                    navController.navigate(Screen.TransactionDetails.createRoute(transactionId))
                }
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
                    navController.navigate(Screen.WalletAddEdit.createRoute())
                },
                onManageWallets = {
                    navController.navigate(Screen.Wallets.routePattern)
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
