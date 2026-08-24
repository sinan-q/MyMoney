package com.sinxn.mymoney.feature.transaction

import androidx.compose.ui.graphics.Color
import com.sinxn.mymoney.core.data.local.entity.CategoryEntity
import com.sinxn.mymoney.core.util.CategoryType
import kotlin.math.abs

object TransactionDetailsUiMapper {

    /**
     * Orders categories so child categories immediately follow their parent, preserving natural names.
     */
    fun flattenCategories(categories: List<CategoryEntity>): List<CategoryEntity> {
        val parents = categories.filter { it.parentId == null }.sortedBy { it.index }
        val result = mutableListOf<CategoryEntity>()

        parents.forEach { parent ->
            result.add(parent)
            val children = categories.filter { it.parentId == parent.id }.sortedBy { it.index }
            result.addAll(children)
        }
        return result
    }

    /**
     * Generates a deterministic Color based on the category name hash code.
     */
    fun generateCategoryColor(name: String): Color {
        val hash = name.hashCode()
        val hue = abs(hash % 360).toFloat()
        return Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.6f, 0.8f)))
    }
}
