package com.sinxn.mymoney.feature.category.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sinxn.mymoney.core.ui.components.CategoryIcon
import com.sinxn.mymoney.core.ui.components.ReorderDragHandle
import com.sinxn.mymoney.core.ui.components.ReorderableLazyColumn
import com.sinxn.mymoney.feature.category.ParentCategoryItem

@Composable
fun CategoryReorderableList(
    items: List<ParentCategoryItem>,
    lazyListState: LazyListState,
    isReorderEnabled: Boolean = true,
    onCategoryClick: (String) -> Unit,
    onExpandToggle: (String) -> Unit,
    onReorderParents: (List<String>) -> Unit,
    onReorderSubcategories: (List<String>) -> Unit
) {
    ReorderableLazyColumn(
        items = items,
        key = { it.category.id },
        lazyListState = lazyListState,
        isReorderEnabled = isReorderEnabled,
        itemShape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentType = { "parent_category_card" },
        modifier = Modifier.fillMaxSize(),
        onReorder = { reordered -> onReorderParents(reordered.map { it.category.id }) }
    ) { parentItem, _, handleModifier ->
        ParentCategoryCard(
            parentItem = parentItem,
            isReorderEnabled = isReorderEnabled,
            handleModifier = handleModifier,
            onCategoryClick = onCategoryClick,
            onExpandToggle = onExpandToggle,
            onReorderSubcategories = onReorderSubcategories
        )
    }
}

@Composable
private fun ParentCategoryCard(
    parentItem: ParentCategoryItem,
    isReorderEnabled: Boolean,
    handleModifier: Modifier,
    onCategoryClick: (String) -> Unit,
    onExpandToggle: (String) -> Unit,
    onReorderSubcategories: (List<String>) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Parent Category Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCategoryClick(parentItem.category.id) }
                .padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isReorderEnabled) {
                ReorderDragHandle(
                    modifier = handleModifier.padding(end = 10.dp),
                    enabled = true
                )
            }

            CategoryIcon(iconData = parentItem.iconData)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = parentItem.cleanName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (parentItem.subcategories.isNotEmpty()) {
                    Text(
                        text = "${parentItem.subcategories.size} subcategor${if (parentItem.subcategories.size == 1) "y" else "ies"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            if (parentItem.subcategories.isNotEmpty()) {
                IconButton(
                    onClick = { onExpandToggle(parentItem.category.id) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (parentItem.isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (parentItem.isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Subcategories Expandable Section
        AnimatedVisibility(
            visible = parentItem.isExpanded && parentItem.subcategories.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            SubcategoriesSection(
                subcategories = parentItem.subcategories,
                isReorderEnabled = isReorderEnabled,
                onSubcategoryClick = onCategoryClick,
                onReorderSubcategories = onReorderSubcategories
            )
        }
    }
}
