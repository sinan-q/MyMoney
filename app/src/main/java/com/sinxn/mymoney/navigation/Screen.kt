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
    object NeedsReview : Screen("needs_review", "needs_review", "Needs Review Queue", isTopLevel = true)
    object Settings : Screen("settings", "settings", "Settings", isTopLevel = false)

    // Sub-screens
    object DebtDetails : Screen("debt_details/{debtId}", "debts", "Debt Details", isTopLevel = false) {
        fun createRoute(debtId: String): String = "debt_details/$debtId"
    }

    object DebtAddEdit : Screen("debt_edit?debtId={debtId}&type={type}", "debts", "Debt Edit", isTopLevel = false) {
        fun createRoute(debtId: String? = null, type: Int? = null): String {
            val params = mutableListOf<String>()
            if (debtId != null) params.add("debtId=$debtId")
            if (type != null) params.add("type=$type")
            return if (params.isEmpty()) "debt_edit" else "debt_edit?${params.joinToString("&")}"
        }
    }

    object BudgetDetails : Screen("budget_details/{budgetId}", "budgets", "Budget Details", isTopLevel = false) {
        fun createRoute(budgetId: String): String = "budget_details/$budgetId"
    }

    object BudgetAddEdit : Screen("budget_edit?budgetId={budgetId}", "budgets", "Budget Edit", isTopLevel = false) {
        fun createRoute(budgetId: String? = null): String = if (budgetId != null) "budget_edit?budgetId=$budgetId" else "budget_edit"
    }

    object BudgetOverview : Screen("budget_overview/{budgetId}", "budgets", "Budget Overview", isTopLevel = false) {
        fun createRoute(budgetId: String): String = "budget_details/$budgetId"
    }

    object SavingDetails : Screen("saving_details/{savingId}", "savings", "Savings Goal Details", isTopLevel = false) {
        fun createRoute(savingId: String): String = "saving_details/$savingId"
    }

    object SavingAddEdit : Screen("saving_edit?savingId={savingId}", "savings", "Savings Goal Edit", isTopLevel = false) {
        fun createRoute(savingId: String? = null): String = if (savingId != null) "saving_edit?savingId=$savingId" else "saving_edit"
    }

    object RecurrentTransactionDetails : Screen("recurrent_transaction_details/{id}", "recurrences", "Recurrent Transaction Details", isTopLevel = false) {
        fun createRoute(id: String): String = "recurrent_transaction_details/$id"
    }

    object RecurrentTransactionAddEdit : Screen("recurrent_transaction_edit?id={id}", "recurrences", "Recurrent Transaction Edit", isTopLevel = false) {
        fun createRoute(id: String? = null): String = if (id != null) "recurrent_transaction_edit?id=$id" else "recurrent_transaction_edit"
    }

    object RecurrentTransferDetails : Screen("recurrent_transfer_details/{id}", "recurrences", "Recurrent Transfer Details", isTopLevel = false) {
        fun createRoute(id: String): String = "recurrent_transfer_details/$id"
    }

    object RecurrentTransferAddEdit : Screen("recurrent_transfer_edit?id={id}", "recurrences", "Recurrent Transfer Edit", isTopLevel = false) {
        fun createRoute(id: String? = null): String = if (id != null) "recurrent_transfer_edit?id=$id" else "recurrent_transfer_edit"
    }

    object TransactionDetails : Screen("transaction_details/{transactionId}", "transactions", "Transaction Details", isTopLevel = false) {
        fun createRoute(transactionId: String): String = "transaction_details/$transactionId"
    }

    object TransactionAddEdit : Screen(
        "transaction_edit?transactionId={transactionId}&savingId={savingId}&action={action}&debtId={debtId}&debtAction={debtAction}&templateId={templateId}",
        "transactions",
        "Transaction Edit",
        isTopLevel = false
    ) {
        fun createRoute(
            transactionId: String? = null,
            savingId: String? = null,
            action: String? = null,
            debtId: String? = null,
            debtAction: String? = null,
            templateId: String? = null
        ): String {
            val params = mutableListOf<String>()
            if (transactionId != null && transactionId != "new") params.add("transactionId=$transactionId")
            if (savingId != null) params.add("savingId=$savingId")
            if (action != null) params.add("action=$action")
            if (debtId != null) params.add("debtId=$debtId")
            if (debtAction != null) params.add("debtAction=$debtAction")
            if (templateId != null) params.add("templateId=$templateId")
            return if (params.isEmpty()) "transaction_edit" else "transaction_edit?${params.joinToString("&")}"
        }
    }

    object TransferAddEdit : Screen(
        "transfer_edit?transactionId={transactionId}&transferId={transferId}&walletId={walletId}&templateId={templateId}",
        "transactions",
        "Transfer Edit",
        isTopLevel = false
    ) {
        fun createRoute(
            transactionId: String? = null,
            transferId: String? = null,
            walletId: String? = null,
            templateId: String? = null
        ): String {
            val params = mutableListOf<String>()
            if (transactionId != null && transactionId != "new") params.add("transactionId=$transactionId")
            if (transferId != null && transferId != "new") params.add("transferId=$transferId")
            if (walletId != null) params.add("walletId=$walletId")
            if (templateId != null) params.add("templateId=$templateId")
            return if (params.isEmpty()) "transfer_edit" else "transfer_edit?${params.joinToString("&")}"
        }
    }

    object CategoryDetails : Screen("category_details/{categoryId}", "categories", "Category Details", isTopLevel = false) {
        fun createRoute(categoryId: String): String = "category_details/$categoryId"
    }

    object CustomFieldDetail : Screen("custom_field_detail/{fieldId}", "categories", "Custom Field Detail", isTopLevel = false) {
        fun createRoute(fieldId: String): String = "custom_field_detail/$fieldId"
    }

    object CustomFieldValueTransactions : Screen("custom_field_value_transactions/{fieldId}?normalizedValue={normalizedValue}", "categories", "Custom Field Value Transactions", isTopLevel = false) {
        fun createRoute(fieldId: String, normalizedValue: String): String {
            val encodedValue = android.net.Uri.encode(normalizedValue)
            return "custom_field_value_transactions/$fieldId?normalizedValue=$encodedValue"
        }
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

    object EventDetails : Screen("event_details/{eventId}", "events", "Event Details", isTopLevel = false) {
        fun createRoute(eventId: String): String = "event_details/$eventId"
    }

    object EventAddEdit : Screen("event_edit?eventId={eventId}", "events", "Event Edit", isTopLevel = false) {
        fun createRoute(eventId: String? = null): String {
            return if (eventId != null) "event_edit?eventId=$eventId" else "event_edit"
        }
    }

    object PlaceDetails : Screen("place_details/{placeId}", "places", "Place Details", isTopLevel = false) {
        fun createRoute(placeId: String): String = "place_details/$placeId"
    }

    object PlaceAddEdit : Screen("place_edit?placeId={placeId}", "places", "Place Edit", isTopLevel = false) {
        fun createRoute(placeId: String? = null): String {
            return if (placeId != null) "place_edit?placeId=$placeId" else "place_edit"
        }
    }

    object TemplateDetails : Screen("template_details/{templateId}?isTransfer={isTransfer}", "templates", "Template Details", isTopLevel = false) {
        fun createRoute(templateId: String, isTransfer: Boolean = false): String = "template_details/$templateId?isTransfer=$isTransfer"
    }

    object TemplateAddEdit : Screen("template_edit?templateId={templateId}&isTransfer={isTransfer}", "templates", "Template Edit", isTopLevel = false) {
        fun createRoute(templateId: String? = null, isTransfer: Boolean = false): String {
            return if (templateId != null) "template_edit?templateId=$templateId&isTransfer=$isTransfer" else "template_edit?isTransfer=$isTransfer"
        }
    }

    object Wallets : Screen("wallets", "wallets", "Wallets", isTopLevel = true)

    object WalletInfo : Screen("wallet_info/{walletId}", "wallets", "Wallet Details", isTopLevel = false) {
        fun createRoute(walletId: String): String = "wallet_info/$walletId"
    }

    object WalletAddEdit : Screen("wallet_edit?walletId={walletId}", "wallets", "Wallet Edit", isTopLevel = false) {
        fun createRoute(walletId: String? = null): String {
            return if (walletId != null && walletId != "new") "wallet_edit?walletId=$walletId" else "wallet_edit"
        }
    }

    object SqlConsole : Screen("sql_console", "settings", "SQL Console", isTopLevel = false)
    object Backup : Screen("backup", "settings", "Backup", isTopLevel = false)
    object Recap : Screen("recap", "overview", "Year Recap", isTopLevel = false)
    object PeriodDetail : Screen("period_detail?startDate={startDate}&endDate={endDate}", "overview", "Period Details", isTopLevel = false) {
        fun createRoute(startDate: String, endDate: String): String =
            "period_detail?startDate=$startDate&endDate=$endDate"
    }

    companion object {
        private val ALL: List<Screen>
            get() = listOfNotNull(
                Transactions, Categories, Debts, Budgets, Savings,
                Events, Recurrences, Templates, Places, People,
                Overview, About, SupportDeveloper, Menu, Settings,
                Wallets, WalletInfo, WalletAddEdit,
                DebtDetails, DebtAddEdit, CategoryDetails, CustomFieldDetail, CustomFieldValueTransactions, PersonDetails, PersonAddEdit, EventDetails, EventAddEdit,
                PlaceDetails, PlaceAddEdit,
                BudgetDetails, BudgetAddEdit, BudgetOverview, SavingDetails, SavingAddEdit,
                RecurrentTransactionDetails, RecurrentTransactionAddEdit, RecurrentTransferDetails, RecurrentTransferAddEdit, TransactionDetails, TransactionAddEdit, TransferAddEdit,
                TemplateDetails, TemplateAddEdit,
                SqlConsole, Backup, Recap, PeriodDetail
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

                cleanRoute.startsWith("transaction_details") || cleanRoute.startsWith("transaction_edit") || cleanRoute.startsWith("transfer_edit") ->
                    ScreenMetadata(Transactions.sidebarItemId, TransactionDetails.title, isTopLevel = false)

                cleanRoute.startsWith("debts") || cleanRoute.startsWith("debt_") ->
                    ScreenMetadata(Debts.sidebarItemId, Debts.title, isTopLevel = cleanRoute == "debts")

                cleanRoute.startsWith("budgets") || cleanRoute.startsWith("budget_") ->
                    ScreenMetadata(Budgets.sidebarItemId, Budgets.title, isTopLevel = cleanRoute == "budgets")

                cleanRoute.startsWith("savings") || cleanRoute.startsWith("saving_") ->
                    ScreenMetadata(Savings.sidebarItemId, Savings.title, isTopLevel = cleanRoute == "savings")

                cleanRoute.startsWith("recurrences") || cleanRoute.startsWith("recurrent_") ->
                    ScreenMetadata(Recurrences.sidebarItemId, Recurrences.title, isTopLevel = cleanRoute == "recurrences")

                cleanRoute == "wallets" -> ScreenMetadata(Wallets.sidebarItemId, Wallets.title, isTopLevel = true)
                cleanRoute.startsWith("wallet_info") -> ScreenMetadata(Wallets.sidebarItemId, WalletInfo.title, isTopLevel = false)
                cleanRoute.startsWith("wallet_edit") -> ScreenMetadata(Wallets.sidebarItemId, WalletAddEdit.title, isTopLevel = false)
                cleanRoute == "categories" || cleanRoute.startsWith("category_") ->
                    ScreenMetadata(Categories.sidebarItemId, Categories.title, isTopLevel = cleanRoute == "categories")
                cleanRoute == "events" || cleanRoute.startsWith("event_") -> ScreenMetadata(Events.sidebarItemId, Events.title, isTopLevel = cleanRoute == "events")
                cleanRoute == "places" || cleanRoute.startsWith("place_") -> ScreenMetadata(Places.sidebarItemId, Places.title, isTopLevel = cleanRoute == "places")
                cleanRoute == "people" || cleanRoute.startsWith("person_") -> ScreenMetadata(People.sidebarItemId, People.title, isTopLevel = cleanRoute == "people")
                cleanRoute == "templates" || cleanRoute.startsWith("template_") -> ScreenMetadata(Templates.sidebarItemId, Templates.title, isTopLevel = cleanRoute == "templates")
                cleanRoute == "overview" -> ScreenMetadata(Overview.sidebarItemId, Overview.title, isTopLevel = true)
                cleanRoute == "recap" -> ScreenMetadata(Overview.sidebarItemId, Recap.title, isTopLevel = false)
                cleanRoute.startsWith("period_") -> ScreenMetadata(Overview.sidebarItemId, PeriodDetail.title, isTopLevel = false)
                cleanRoute == "about" -> ScreenMetadata(About.sidebarItemId, About.title, isTopLevel = true)
                cleanRoute == "support_developer" -> ScreenMetadata(SupportDeveloper.sidebarItemId, SupportDeveloper.title, isTopLevel = true)
                cleanRoute == "menu" -> ScreenMetadata(Menu.sidebarItemId, Menu.title, isTopLevel = true)
                cleanRoute == "settings" -> ScreenMetadata(Settings.sidebarItemId, Settings.title, isTopLevel = false)

                else -> {
                    val match = ALL.find { screen ->
                        val base = screen.routePattern.substringBefore("?").substringBefore("/{")
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
