package com.sinxn.mymoney.navigation

import com.sinxn.mymoney.core.util.Constants
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class ScreenMetadata(
    val selectedItemId: String,
    val title: String,
    val isTopLevel: Boolean,
    val screen: Screen? = null
)

sealed class Screen(
    val routePattern: String,
    val sidebarItemId: String,
    val title: String,
    val isTopLevel: Boolean = true
) {
    val baseRoute: String = routePattern.substringBefore("?").substringBefore("/{")

    data object Transactions : Screen("wallet_details/{walletId}", "transactions", "Transactions", isTopLevel = true) {
        fun createRoute(walletId: String = Constants.TOTAL_WALLET_ID): String = "wallet_details/$walletId"
    }

    data object Categories : Screen("categories", "categories", "Categories", isTopLevel = true)
    data object Debts : Screen("debts", "debts", "Debts & Credits", isTopLevel = true)
    data object Budgets : Screen("budgets", "budgets", "Budgets", isTopLevel = true)
    data object Savings : Screen("savings", "savings", "Savings Goals", isTopLevel = true)
    data object Events : Screen("events", "events", "Events", isTopLevel = true)
    data object Recurrences : Screen("recurrences", "recurrences", "Recurrences", isTopLevel = true)
    data object Templates : Screen("templates", "models", "Models", isTopLevel = true)
    data object Places : Screen("places", "places", "Places", isTopLevel = true)
    data object People : Screen("people", "people", "People", isTopLevel = true)
    data object Overview : Screen("overview", "overview", "Overview", isTopLevel = true)
    data object About : Screen("about", "about", "About", isTopLevel = true)
    data object SupportDeveloper : Screen("support_developer", "support_developer", "Support Developer", isTopLevel = true)
    data object Menu : Screen("menu", "menu", "Menu", isTopLevel = true)
    data object NeedsReview : Screen("needs_review", "needs_review", "Needs Review Queue", isTopLevel = true)
    data object Settings : Screen("settings", "settings", "Settings", isTopLevel = false)

    // Sub-screens
    data object DebtDetails : Screen("debt_details/{debtId}", "debts", "Debt Details", isTopLevel = false) {
        fun createRoute(debtId: String): String = "debt_details/$debtId"
    }

    data object DebtAddEdit : Screen("debt_edit?debtId={debtId}&type={type}", "debts", "Debt Edit", isTopLevel = false) {
        fun createRoute(debtId: String? = null, type: Int? = null): String {
            val params = mutableListOf<String>()
            if (debtId != null) params.add("debtId=$debtId")
            if (type != null) params.add("type=$type")
            return if (params.isEmpty()) "debt_edit" else "debt_edit?${params.joinToString("&")}"
        }
    }

    data object BudgetDetails : Screen("budget_details/{budgetId}", "budgets", "Budget Details", isTopLevel = false) {
        fun createRoute(budgetId: String): String = "budget_details/$budgetId"
    }

    data object BudgetAddEdit : Screen("budget_edit?budgetId={budgetId}", "budgets", "Budget Edit", isTopLevel = false) {
        fun createRoute(budgetId: String? = null): String = if (budgetId != null) "budget_edit?budgetId=$budgetId" else "budget_edit"
    }

    data object BudgetOverview : Screen("budget_overview/{budgetId}", "budgets", "Budget Overview", isTopLevel = false) {
        fun createRoute(budgetId: String): String = "budget_overview/$budgetId"
    }

    data object SavingDetails : Screen("saving_details/{savingId}", "savings", "Savings Goal Details", isTopLevel = false) {
        fun createRoute(savingId: String): String = "saving_details/$savingId"
    }

    data object SavingAddEdit : Screen("saving_edit?savingId={savingId}", "savings", "Savings Goal Edit", isTopLevel = false) {
        fun createRoute(savingId: String? = null): String = if (savingId != null) "saving_edit?savingId=$savingId" else "saving_edit"
    }

    data object RecurrentTransactionDetails : Screen("recurrent_transaction_details/{id}", "recurrences", "Recurrent Transaction Details", isTopLevel = false) {
        fun createRoute(id: String): String = "recurrent_transaction_details/$id"
    }

    data object RecurrentTransactionAddEdit : Screen("recurrent_transaction_edit?id={id}", "recurrences", "Recurrent Transaction Edit", isTopLevel = false) {
        fun createRoute(id: String? = null): String = if (id != null) "recurrent_transaction_edit?id=$id" else "recurrent_transaction_edit"
    }

    data object RecurrentTransferDetails : Screen("recurrent_transfer_details/{id}", "recurrences", "Recurrent Transfer Details", isTopLevel = false) {
        fun createRoute(id: String): String = "recurrent_transfer_details/$id"
    }

    data object RecurrentTransferAddEdit : Screen("recurrent_transfer_edit?id={id}", "recurrences", "Recurrent Transfer Edit", isTopLevel = false) {
        fun createRoute(id: String? = null): String = if (id != null) "recurrent_transfer_edit?id=$id" else "recurrent_transfer_edit"
    }

    data object TransactionDetails : Screen("transaction_details/{transactionId}", "transactions", "Transaction Details", isTopLevel = false) {
        fun createRoute(transactionId: String): String = "transaction_details/$transactionId"
    }

    data object TransactionAddEdit : Screen(
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

    data object TransferAddEdit : Screen(
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

    data object CategoryDetails : Screen("category_details/{categoryId}", "categories", "Category Details", isTopLevel = false) {
        fun createRoute(categoryId: String): String = "category_details/$categoryId"
    }

    data object CustomFieldDetail : Screen("custom_field_detail/{fieldId}", "categories", "Custom Field Detail", isTopLevel = false) {
        fun createRoute(fieldId: String): String = "custom_field_detail/$fieldId"
    }

    data object CustomFieldValueTransactions : Screen("custom_field_value_transactions/{fieldId}?normalizedValue={normalizedValue}", "categories", "Custom Field Value Transactions", isTopLevel = false) {
        fun createRoute(fieldId: String, normalizedValue: String): String {
            val encodedValue = encodeParam(normalizedValue)
            return "custom_field_value_transactions/$fieldId?normalizedValue=$encodedValue"
        }
    }

    data object CategoryAddEdit : Screen("category_edit?categoryId={categoryId}&type={type}&parentId={parentId}", "categories", "Category Edit", isTopLevel = false) {
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

    data object PersonDetails : Screen("person_details/{personId}", "people", "Person Details", isTopLevel = false) {
        fun createRoute(personId: String): String = "person_details/$personId"
    }

    data object PersonAddEdit : Screen("person_edit?personId={personId}", "people", "Person Edit", isTopLevel = false) {
        fun createRoute(personId: String? = null): String {
            return if (personId != null) "person_edit?personId=$personId" else "person_edit"
        }
    }

    data object EventDetails : Screen("event_details/{eventId}", "events", "Event Details", isTopLevel = false) {
        fun createRoute(eventId: String): String = "event_details/$eventId"
    }

    data object EventAddEdit : Screen("event_edit?eventId={eventId}", "events", "Event Edit", isTopLevel = false) {
        fun createRoute(eventId: String? = null): String {
            return if (eventId != null) "event_edit?eventId=$eventId" else "event_edit"
        }
    }

    data object PlaceDetails : Screen("place_details/{placeId}", "places", "Place Details", isTopLevel = false) {
        fun createRoute(placeId: String): String = "place_details/$placeId"
    }

    data object PlaceAddEdit : Screen("place_edit?placeId={placeId}", "places", "Place Edit", isTopLevel = false) {
        fun createRoute(placeId: String? = null): String {
            return if (placeId != null) "place_edit?placeId=$placeId" else "place_edit"
        }
    }

    data object TemplateDetails : Screen("template_details/{templateId}?isTransfer={isTransfer}", "templates", "Template Details", isTopLevel = false) {
        fun createRoute(templateId: String, isTransfer: Boolean = false): String = "template_details/$templateId?isTransfer=$isTransfer"
    }

    data object TemplateAddEdit : Screen("template_edit?templateId={templateId}&isTransfer={isTransfer}", "templates", "Template Edit", isTopLevel = false) {
        fun createRoute(templateId: String? = null, isTransfer: Boolean = false): String {
            return if (templateId != null) "template_edit?templateId=$templateId&isTransfer=$isTransfer" else "template_edit?isTransfer=$isTransfer"
        }
    }

    data object Wallets : Screen("wallets", "wallets", "Wallets", isTopLevel = true)

    data object WalletInfo : Screen("wallet_info/{walletId}", "wallets", "Wallet Details", isTopLevel = false) {
        fun createRoute(walletId: String): String = "wallet_info/$walletId"
    }

    data object WalletAddEdit : Screen("wallet_edit?walletId={walletId}", "wallets", "Wallet Edit", isTopLevel = false) {
        fun createRoute(walletId: String? = null): String {
            return if (walletId != null && walletId != "new") "wallet_edit?walletId=$walletId" else "wallet_edit"
        }
    }

    data object SqlConsole : Screen("sql_console", "settings", "SQL Console", isTopLevel = false)
    data object Backup : Screen("backup", "settings", "Backup", isTopLevel = false)
    data object Recap : Screen("recap", "overview", "Year Recap", isTopLevel = false)
    data object PeriodDetail : Screen("period_detail?startDate={startDate}&endDate={endDate}", "overview", "Period Details", isTopLevel = false) {
        fun createRoute(startDate: String, endDate: String): String =
            "period_detail?startDate=$startDate&endDate=$endDate"
    }

    companion object {
        val ALL: List<Screen> by lazy {
            listOf(
                Transactions, Categories, Debts, Budgets, Savings,
                Events, Recurrences, Templates, Places, People,
                Overview, About, SupportDeveloper, Menu, NeedsReview, Settings,
                Wallets, WalletInfo, WalletAddEdit,
                DebtDetails, DebtAddEdit, CategoryDetails, CustomFieldDetail, CustomFieldValueTransactions,
                PersonDetails, PersonAddEdit, EventDetails, EventAddEdit,
                PlaceDetails, PlaceAddEdit,
                BudgetDetails, BudgetAddEdit, BudgetOverview, SavingDetails, SavingAddEdit,
                RecurrentTransactionDetails, RecurrentTransactionAddEdit,
                RecurrentTransferDetails, RecurrentTransferAddEdit,
                TransactionDetails, TransactionAddEdit, TransferAddEdit,
                TemplateDetails, TemplateAddEdit,
                SqlConsole, Backup, Recap, PeriodDetail
            )
        }

        fun fromSidebarId(id: String): Screen? =
            ALL.find { it.sidebarItemId == id && it.isTopLevel } ?: ALL.find { it.sidebarItemId == id }

        fun findScreen(route: String?): Screen? {
            if (route.isNullOrBlank()) return null
            if (route == "home") return Transactions

            val cleanRoute = route.substringBefore("?")
            val routeBase = cleanRoute.substringBefore("/{").substringBefore("/")

            return ALL.find { screen ->
                cleanRoute == screen.routePattern || routeBase == screen.baseRoute
            }
        }

        fun fromRoute(route: String?): ScreenMetadata {
            if (route.isNullOrEmpty()) {
                return ScreenMetadata(
                    selectedItemId = Transactions.sidebarItemId,
                    title = Transactions.title,
                    isTopLevel = true,
                    screen = Transactions
                )
            }

            val matchedScreen = findScreen(route)
            return if (matchedScreen != null) {
                ScreenMetadata(
                    selectedItemId = matchedScreen.sidebarItemId,
                    title = matchedScreen.title,
                    isTopLevel = matchedScreen.isTopLevel,
                    screen = matchedScreen
                )
            } else {
                ScreenMetadata(
                    selectedItemId = Transactions.sidebarItemId,
                    title = "My Money",
                    isTopLevel = false,
                    screen = null
                )
            }
        }
    }
}

private fun encodeParam(value: String): String {
    return URLEncoder.encode(value, StandardCharsets.UTF_8.name())
        .replace("+", "%20")
}
