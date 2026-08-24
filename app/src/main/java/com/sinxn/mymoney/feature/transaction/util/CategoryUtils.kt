package com.sinxn.mymoney.feature.transaction.util

import com.sinxn.mymoney.core.data.local.entity.CategoryEntity

/**
 * Resolves a category's name and its parent category's name in O(1) time using an indexed map.
 */
public fun resolveCategoryHierarchy(
    categoryId: String?,
    categoryMap: Map<String, CategoryEntity>,
    fallbackName: String? = null
): Pair<String, String?> {
    if (categoryId == null) return (fallbackName ?: "No Category") to null
    val category = categoryMap[categoryId] ?: return (fallbackName ?: "No Category") to null
    val parent = category.parentId?.let { categoryMap[it] }

    return if (parent != null) {
        category.name to parent.name
    } else {
        category.name to null
    }
}

/**
 * Convenience overload for backwards compatibility with raw CategoryEntity lists.
 */
public fun resolveCategoryHierarchy(
    categoryId: String?,
    allCategories: List<CategoryEntity>,
    fallbackName: String? = null
): Pair<String, String?> {
    if (categoryId == null) return (fallbackName ?: "No Category") to null
    val category = allCategories.find { it.id == categoryId } ?: return (fallbackName ?: "No Category") to null
    val parent = category.parentId?.let { parentId ->
        allCategories.find { it.id == parentId }
    }

    return if (parent != null) {
        category.name to parent.name
    } else {
        category.name to null
    }
}