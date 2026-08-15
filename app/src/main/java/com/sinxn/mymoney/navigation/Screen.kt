package com.sinxn.mymoney.navigation

import com.sinxn.mymoney.core.util.Constants

data class ScreenMetadata(
    val selectedItemId: String,
    val title: String,
    val isTopLevel: Boolean
)

sealed class Screen(
    val routePattern: String,
    val sidebarItemId: String,
    val title: String,
    val isTopLevel: Boolean = true
) {
    object Transactions : Screen("wallet_details", "transactions", "Transactions", isTopLevel = true) {
        fun createRoute(walletId: String = Constants.TOTAL_WALLET_ID) = "wallet_details/$walletId"
    }

    object Categories : Screen("categories", "categories", "Categories", isTopLevel = true)
    object Debts : Screen("debts", "debts", "Debts & Credits", isTopLevel = true)
    object Budgets : Screen("budgets", "budgets", "Budgets", isTopLevel = true)
    object Savings : Screen("savings", "savings", "Savings Goals", isTopLevel = true)
    object Events : Screen("events", "events", "Events", isTopLevel = true)
    object Recurrences : Screen("recurrences", "recurrences", "Recurrences", isTopLevel = true)
    object Templates : Screen("templates", "models", "Models", isTopLevel = true)
    object Places : Screen("places", "places", "Places", isTopLevel = true)
    object People : Screen("people", "people", "People", isTopLevel = true)
    object Overview : Screen("overview", "overview", "Overview", isTopLevel = true)
    object About : Screen("about", "about", "About", isTopLevel = true)
    object SupportDeveloper : Screen("support_developer", "support_developer", "Support Developer", isTopLevel = true)
    object Menu : Screen("menu", "menu", "Menu", isTopLevel = true)
    object Settings : Screen("settings", "settings", "Settings", isTopLevel = false)

    // Sub-screens
    object DebtDetails : Screen("debt_details/{debtId}?type={type}", "debts", "Debt Details", isTopLevel = false) {
        fun createRoute(debtId: String, type: Int? = null): String {
            return if (type != null) "debt_details/$debtId?type=$type" else "debt_details/$debtId"
        }
    }

    object BudgetDetails : Screen("budget_details/{budgetId}", "budgets", "Budget Details", isTopLevel = false) {
        fun createRoute(budgetId: String): String = "budget_details/$budgetId"
    }

    object BudgetOverview : Screen("budget_overview/{budgetId}", "budgets", "Budget Overview", isTopLevel = false) {
        fun createRoute(budgetId: String): String = "budget_overview/$budgetId"
    }

    object SavingDetails : Screen("saving_details/{savingId}", "savings", "Savings Goal Details", isTopLevel = false) {
        fun createRoute(savingId: String): String = "saving_details/$savingId"
    }

    object RecurrentTransactionDetails : Screen("recurrent_transaction_details/{id}", "recurrences", "Recurrent Transaction Details", isTopLevel = false) {
        fun createRoute(id: String = "new"): String = "recurrent_transaction_details/$id"
    }

    object RecurrentTransferDetails : Screen("recurrent_transfer_details/{id}", "recurrences", "Recurrent Transfer Details", isTopLevel = false) {
        fun createRoute(id: String = "new"): String = "recurrent_transfer_details/$id"
    }

    object TransactionDetails : Screen(
        "transaction_details/{transactionId}?savingId={savingId}&action={action}&debtId={debtId}&debtAction={debtAction}",
        "transactions",
        "Transaction Details",
        isTopLevel = false
    ) {
        fun createRoute(
            transactionId: String = "new",
            savingId: String? = null,
            action: String? = null,
            debtId: String? = null,
            debtAction: String? = null
        ): String {
            val params = mutableListOf<String>()
            if (savingId != null) params.add("savingId=$savingId")
            if (action != null) params.add("action=$action")
            if (debtId != null) params.add("debtId=$debtId")
            if (debtAction != null) params.add("debtAction=$debtAction")
            return if (params.isEmpty()) "transaction_details/$transactionId" else "transaction_details/$transactionId?${params.joinToString("&")}"
        }
    }

    object CategoryDetails : Screen("category_details/{categoryId}", "categories", "Category Details", isTopLevel = false) {
        fun createRoute(categoryId: String): String = "category_details/$categoryId"
    }

    object CategoryAddEdit : Screen("category_edit?categoryId={categoryId}&type={type}&parentId={parentId}", "categories", "Category Edit", isTopLevel = false) {
        fun createRoute(
            categoryId: String? = null,
            type: Int? = null,
            parentId: String? = null
        ): String {
            val params = mutableListOf<String>()
            if (categoryId != null) params.add("categoryId=$categoryId")
            if (type != null) params.add("type=$type")
            if (parentId != null) params.add("parentId=$parentId")
            return if (params.isEmpty()) "category_edit" else "category_edit?${params.joinToString("&")}"
        }
    }

    object PersonDetails : Screen("person_details/{personId}", "people", "Person Details", isTopLevel = false) {
        fun createRoute(personId: String): String = "person_details/$personId"
    }

    object PersonAddEdit : Screen("person_edit?personId={personId}", "people", "Person Edit", isTopLevel = false) {
        fun createRoute(personId: String? = null): String {
            return if (personId != null) "person_edit?personId=$personId" else "person_edit"
        }
    }

    object SqlConsole : Screen("sql_console", "settings", "SQL Console", isTopLevel = false)
    object Backup : Screen("backup", "settings", "Backup", isTopLevel = false)
    object Recap : Screen("recap", "overview", "Year Recap", isTopLevel = false)

    companion object {
        private val ALL: List<Screen>
            get() = listOfNotNull(
                Transactions, Categories, Debts, Budgets, Savings,
                Events, Recurrences, Templates, Places, People,
                Overview, About, SupportDeveloper, Menu, Settings,
                DebtDetails, CategoryDetails, PersonDetails, PersonAddEdit, BudgetDetails, BudgetOverview, SavingDetails,
                RecurrentTransactionDetails, RecurrentTransferDetails, TransactionDetails,
                SqlConsole, Backup, Recap
            )

        fun fromSidebarId(id: String): Screen? = ALL.find { it.sidebarItemId == id }

        fun fromRoute(route: String?): ScreenMetadata {
            if (route.isNullOrEmpty()) {
                return ScreenMetadata(
                    selectedItemId = Transactions.sidebarItemId,
                    title = Transactions.title,
                    isTopLevel = true
                )
            }

            val cleanRoute = route.substringBefore("?")

            return when {
                cleanRoute == "home" || cleanRoute.startsWith("wallet_details") ->
                    ScreenMetadata(Transactions.sidebarItemId, Transactions.title, isTopLevel = true)

                cleanRoute.startsWith("transaction_details") ->
                    ScreenMetadata(Transactions.sidebarItemId, TransactionDetails.title, isTopLevel = false)

                cleanRoute.startsWith("debts") || cleanRoute.startsWith("debt_details") ->
                    ScreenMetadata(Debts.sidebarItemId, Debts.title, isTopLevel = cleanRoute == "debts")

                cleanRoute.startsWith("budgets") || cleanRoute.startsWith("budget_") ->
                    ScreenMetadata(Budgets.sidebarItemId, Budgets.title, isTopLevel = cleanRoute == "budgets")

                cleanRoute.startsWith("savings") || cleanRoute.startsWith("saving_") ->
                    ScreenMetadata(Savings.sidebarItemId, Savings.title, isTopLevel = cleanRoute == "savings")

                cleanRoute.startsWith("recurrences") || cleanRoute.startsWith("recurrent_") ->
                    ScreenMetadata(Recurrences.sidebarItemId, Recurrences.title, isTopLevel = cleanRoute == "recurrences")

                cleanRoute == "categories" -> ScreenMetadata(Categories.sidebarItemId, Categories.title, isTopLevel = true)
                cleanRoute == "events" -> ScreenMetadata(Events.sidebarItemId, Events.title, isTopLevel = true)
                cleanRoute == "places" -> ScreenMetadata(Places.sidebarItemId, Places.title, isTopLevel = true)
                cleanRoute == "people" || cleanRoute.startsWith("person_") -> ScreenMetadata(People.sidebarItemId, People.title, isTopLevel = cleanRoute == "people")
                cleanRoute == "templates" -> ScreenMetadata(Templates.sidebarItemId, Templates.title, isTopLevel = true)
                cleanRoute == "overview" || cleanRoute == "recap" -> ScreenMetadata(Overview.sidebarItemId, Overview.title, isTopLevel = true)
                cleanRoute == "about" -> ScreenMetadata(About.sidebarItemId, About.title, isTopLevel = true)
                cleanRoute == "support_developer" -> ScreenMetadata(SupportDeveloper.sidebarItemId, SupportDeveloper.title, isTopLevel = true)
                cleanRoute == "menu" -> ScreenMetadata(Menu.sidebarItemId, Menu.title, isTopLevel = true)
                cleanRoute == "settings" -> ScreenMetadata(Settings.sidebarItemId, Settings.title, isTopLevel = false)

                else -> {
                    val match = ALL.find { screen ->
                        val base = screen.routePattern.substringBefore("/{")
                        cleanRoute == screen.routePattern || cleanRoute == base || cleanRoute.startsWith("$base/")
                    }
                    if (match != null) {
                        ScreenMetadata(
                            selectedItemId = match.sidebarItemId,
                            title = match.title,
                            isTopLevel = match.isTopLevel && cleanRoute == match.routePattern
                        )
                    } else {
                        ScreenMetadata(Transactions.sidebarItemId, "My Money", false)
                    }
                }
            }
        }
    }
}
