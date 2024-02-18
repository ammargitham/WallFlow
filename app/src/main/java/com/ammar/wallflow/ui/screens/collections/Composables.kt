package com.ammar.wallflow.ui.screens.collections

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.R
import com.ammar.wallflow.model.CollectionCategory
import com.ammar.wallflow.ui.theme.WallFlowTheme

internal fun LazyStaggeredGridScope.header(
    selectedCategory: CollectionCategory = CollectionCategory.FAVORITES,
    onCategoryClick: (CollectionCategory) -> Unit = {},
) {
    item(span = StaggeredGridItemSpan.FullLine) {
        CategoriesRow(
            selected = selectedCategory,
            onCategoryClick = onCategoryClick,
        )
    }
}

@Composable
fun CategoriesRow(
    modifier: Modifier = Modifier,
    selected: CollectionCategory = CollectionCategory.FAVORITES,
    onCategoryClick: (CollectionCategory) -> Unit = {},
) {
    Row(
        modifier = modifier.scrollable(
            state = rememberScrollState(),
            orientation = Orientation.Horizontal,
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CollectionCategory.entries.forEach {
            CategoryChip(
                category = it,
                selected = selected == it,
                onClick = { onCategoryClick(it) },
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewHeader() {
    WallFlowTheme {
        Surface {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(8.dp),
            ) {
                header()
            }
        }
    }
}

@Composable
private fun CategoryChip(
    category: CollectionCategory,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit = {},
) {
    FilterChip(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderWidth = 0.dp,
            borderColor = Color.Transparent,
        ),
        leadingIcon = {
            AnimatedContent(
                targetState = selected,
                label = "leading icon",
            ) {
                Icon(
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                    painter = painterResource(
                        if (it) {
                            R.drawable.baseline_check_24
                        } else {
                            when (category) {
                                CollectionCategory.FAVORITES -> R.drawable.baseline_favorite_24
                                CollectionCategory.LIGHT_DARK -> R.drawable.baseline_light_dark
                            }
                        },
                    ),
                    contentDescription = null,
                )
            }
        },
        label = {
            Text(
                text = stringResource(
                    when (category) {
                        CollectionCategory.FAVORITES -> R.string.favorites
                        CollectionCategory.LIGHT_DARK -> R.string.light_dark
                    },
                ),
            )
        },
        selected = selected,
        onClick = onClick,
    )
}
