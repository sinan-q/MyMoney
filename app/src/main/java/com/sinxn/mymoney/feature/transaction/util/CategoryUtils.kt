package com.sinxn.mymoney.feature.transaction.util

import com.sinxn.mymoney.core.data.local.entity.CategoryEntity

public fun resolveCategoryHierarchy(
    categoryId: String?,
    allCategories: List<CategoryEntity>
): Pair<String, String?> {
    val category = allCategories.find { it.id == categoryId }
    if (category == null) return "No Category" to null

    val cleanName = category.name.replace("  ↳ ", "")
    val parent = category.parentId?.let { parentId ->
        allCategories.find { it.id == parentId }
    }

    return if (parent != null) {
        cleanName to parent.name.replace("  ↳ ", "")
    } else {
        cleanName to null
    }
}