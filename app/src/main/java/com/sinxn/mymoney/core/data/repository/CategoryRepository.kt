package com.sinxn.mymoney.core.data.repository

import com.sinxn.mymoney.core.data.local.dao.MoneyDao
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val moneyDao: MoneyDao
) {
    fun getCategories(): Flow<List<CategoryEntity>> {
        return moneyDao.getCategories()
    }

    suspend fun getCategoryById(id: String): CategoryEntity? {
        return moneyDao.getCategoryById(id)
    }

    suspend fun saveCategory(
        id: String?,
        name: String,
        icon: String,
        type: Int, // 0: Income, 1: Expense, 2: System (CategoryType)
        parentId: String? = null,
        showReport: Boolean = true,
        index: Int = 0,
        tag: String? = null
    ): String {
        val now = System.currentTimeMillis()
        val categoryId = id ?: UUID.randomUUID().toString()

        val category = CategoryEntity(
            id = categoryId,
            name = name,
            icon = icon,
            type = type,
            parentId = parentId,
            showReport = showReport,
            index = index,
            isDeleted = false,
            lastEdit = now,
            tag = tag
        )

        moneyDao.insertCategory(category)
        return categoryId
    }

    suspend fun deleteCategory(categoryId: String) {
        val now = System.currentTimeMillis()
        moneyDao.softDeleteCategory(categoryId, now)
    }
}
