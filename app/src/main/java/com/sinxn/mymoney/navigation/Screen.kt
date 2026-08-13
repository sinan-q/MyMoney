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
    object Settings : Screen("settings", "settings", "Settings", isTopLevel = false)

    companion object {
        private val ALL = listOf(
            Transactions, Categories, Debts, Budgets, Savings,
            Events, Recurrences, Templates, Places, People,
            Overview, About, SupportDeveloper, Settings
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
                cleanRoute == "people" -> ScreenMetadata(People.sidebarItemId, People.title, isTopLevel = true)
                cleanRoute == "templates" -> ScreenMetadata(Templates.sidebarItemId, Templates.title, isTopLevel = true)
                cleanRoute == "overview" || cleanRoute == "recap" -> ScreenMetadata(Overview.sidebarItemId, Overview.title, isTopLevel = true)
                cleanRoute == "about" -> ScreenMetadata(About.sidebarItemId, About.title, isTopLevel = true)
                cleanRoute == "support_developer" -> ScreenMetadata(SupportDeveloper.sidebarItemId, SupportDeveloper.title, isTopLevel = true)
                cleanRoute == "settings" -> ScreenMetadata(Settings.sidebarItemId, Settings.title, isTopLevel = false)

                else -> {
                    val match = ALL.find { cleanRoute == it.routePattern || cleanRoute.startsWith("${it.routePattern}/") }
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
