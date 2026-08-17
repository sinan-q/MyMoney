package com.sinxn.mymoney.feature.transaction

import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.util.CategoryType
import com.sinxn.mymoney.feature.transaction.util.resolveCategoryHierarchy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryResolutionTest {

    private val now = System.currentTimeMillis()

    @Test
    fun testResolveCategoryHierarchy_systemCategory() {
        val systemCategory = CategoryEntity(
            id = "system-category-system::debt",
            name = "Debt",
            icon = "ic_debt",
            type = CategoryType.SYSTEM,
            parentId = null,
            showReport = false,
            index = 0,
            isDeleted = false,
            lastEdit = now,
            tag = "system::debt"
        )
        val categories = listOf(systemCategory)

        val (name, parentName) = resolveCategoryHierarchy(
            categoryId = "system-category-system::debt",
            allCategories = categories
        )

        assertEquals("Debt", name)
        assertNull(parentName)
    }

    @Test
    fun testResolveCategoryHierarchy_fallbackNameWhenNotFound() {
        val (name, parentName) = resolveCategoryHierarchy(
            categoryId = "system-category-system::credit",
            allCategories = emptyList(),
            fallbackName = "Credit"
        )

        assertEquals("Credit", name)
        assertNull(parentName)
    }

    @Test
    fun testResolveCategoryHierarchy_childCategory() {
        val parent = CategoryEntity(
            id = "cat-food",
            name = "Food",
            icon = "ic_food",
            type = CategoryType.EXPENSE,
            parentId = null,
            showReport = true,
            index = 0,
            isDeleted = false,
            lastEdit = now,
            tag = null
        )
        val child = CategoryEntity(
            id = "cat-groceries",
            name = "Groceries",
            icon = "ic_groceries",
            type = CategoryType.EXPENSE,
            parentId = "cat-food",
            showReport = true,
            index = 1,
            isDeleted = false,
            lastEdit = now,
            tag = null
        )

        val (name, parentName) = resolveCategoryHierarchy(
            categoryId = "cat-groceries",
            allCategories = listOf(parent, child)
        )

        assertEquals("Groceries", name)
        assertEquals("Food", parentName)
    }

    @Test
    fun testFlattenCategories_includesSystemCategories() {
        val systemCategory = CategoryEntity(
            id = "system-category-system::debt",
            name = "Debt",
            icon = "ic_debt",
            type = CategoryType.SYSTEM,
            parentId = null,
            showReport = false,
            index = 0,
            isDeleted = false,
            lastEdit = now,
            tag = "system::debt"
        )
        val expenseCategory = CategoryEntity(
            id = "cat-food",
            name = "Food",
            icon = "ic_food",
            type = CategoryType.EXPENSE,
            parentId = null,
            showReport = true,
            index = 1,
            isDeleted = false,
            lastEdit = now,
            tag = null
        )

        val flattened = TransactionDetailsUiMapper.flattenCategories(listOf(systemCategory, expenseCategory))
        assertEquals(2, flattened.size)
        assertEquals("Debt", flattened[0].name)
        assertEquals("Food", flattened[1].name)
    }
}
