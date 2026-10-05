package com.sinxn.mymoney.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenTest {

    @Test
    fun `all screens in registry have unique baseRoute and valid properties`() {
        val allScreens = Screen.ALL
        assertTrue("Registry should not be empty", allScreens.isNotEmpty())

        val baseRoutes = mutableSetOf<String>()
        for (screen in allScreens) {
            assertTrue("Base route must not be blank for ${screen.title}", screen.baseRoute.isNotBlank())
            assertTrue("Sidebar item id must not be blank for ${screen.title}", screen.sidebarItemId.isNotBlank())
            assertTrue("Title must not be blank for ${screen.title}", screen.title.isNotBlank())
            assertTrue(
                "Duplicate baseRoute found: '${screen.baseRoute}' on ${screen.title}",
                baseRoutes.add(screen.baseRoute)
            )
        }
    }

    @Test
    fun `fromRoute correctly resolves needs_review screen`() {
        val metadata = Screen.fromRoute("needs_review")
        assertEquals(Screen.NeedsReview, metadata.screen)
        assertEquals("needs_review", metadata.selectedItemId)
        assertEquals("Needs Review Queue", metadata.title)
        assertTrue(metadata.isTopLevel)
    }

    @Test
    fun `fromRoute correctly resolves budget_overview and createRoute matches pattern`() {
        val budgetId = "budget_123"
        val route = Screen.BudgetOverview.createRoute(budgetId)
        assertEquals("budget_overview/budget_123", route)

        val metadata = Screen.fromRoute(route)
        assertEquals(Screen.BudgetOverview, metadata.screen)
        assertEquals("budgets", metadata.selectedItemId)
        assertEquals("Budget Overview", metadata.title)
        assertFalse(metadata.isTopLevel)
    }

    @Test
    fun `fromRoute handles home alias and wallet details`() {
        val homeMetadata = Screen.fromRoute("home")
        assertEquals(Screen.Transactions, homeMetadata.screen)
        assertTrue(homeMetadata.isTopLevel)

        val walletMetadata = Screen.fromRoute("wallet_details/my_wallet_id")
        assertEquals(Screen.Transactions, walletMetadata.screen)
        assertTrue(walletMetadata.isTopLevel)
    }

    @Test
    fun `fromRoute resolves sub-screens and top-level screens accurately`() {
        val debtListMeta = Screen.fromRoute("debts")
        assertEquals(Screen.Debts, debtListMeta.screen)
        assertTrue(debtListMeta.isTopLevel)

        val debtDetailsMeta = Screen.fromRoute("debt_details/debt_99")
        assertEquals(Screen.DebtDetails, debtDetailsMeta.screen)
        assertFalse(debtDetailsMeta.isTopLevel)

        val debtEditMeta = Screen.fromRoute("debt_edit?debtId=debt_99&type=1")
        assertEquals(Screen.DebtAddEdit, debtEditMeta.screen)
        assertFalse(debtEditMeta.isTopLevel)
    }

    @Test
    fun `fromRoute falls back to default on unknown route`() {
        val unknown = Screen.fromRoute("unknown_screen_route")
        assertEquals("transactions", unknown.selectedItemId)
        assertEquals("My Money", unknown.title)
        assertFalse(unknown.isTopLevel)
    }

    @Test
    fun `fromSidebarId resolves appropriate top-level screens`() {
        val debts = Screen.fromSidebarId("debts")
        assertNotNull(debts)
        assertEquals(Screen.Debts, debts)

        val overview = Screen.fromSidebarId("overview")
        assertNotNull(overview)
        assertEquals(Screen.Overview, overview)
    }

    @Test
    fun `custom field value transactions URL encoding works on pure JVM`() {
        val route = Screen.CustomFieldValueTransactions.createRoute("field_1", "Food & Dining")
        assertEquals("custom_field_value_transactions/field_1?normalizedValue=Food+%26+Dining", route.replace("%20", "+"))
        val meta = Screen.fromRoute(route)
        assertEquals(Screen.CustomFieldValueTransactions, meta.screen)
    }
}
