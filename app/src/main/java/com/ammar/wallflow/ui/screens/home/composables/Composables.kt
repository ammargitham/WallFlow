package com.ammar.wallflow.ui.screens.home.composables

import android.content.res.Configuration
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3x.FilterChip
import androidx.compose.material3x.FilterChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.R
import com.ammar.wallflow.model.MenuItem
import com.ammar.wallflow.model.OnlineSource
import com.ammar.wallflow.model.Purity
import com.ammar.wallflow.model.wallhaven.WallhavenTag
import com.ammar.wallflow.ui.common.OverflowMenu
import com.ammar.wallflow.ui.common.PlaceholderChip
import com.ammar.wallflow.ui.common.TagChip
import com.ammar.wallflow.ui.theme.WallFlowTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.Clock

internal fun LazyStaggeredGridScope.header(
    sources: ImmutableList<OnlineSource> = persistentListOf(),
    selectedSource: OnlineSource = OnlineSource.WALLHAVEN,
    sourceHeader: (LazyStaggeredGridScope.() -> Unit)? = null,
    onSourceClick: (OnlineSource) -> Unit = {},
    onManageSourcesClick: () -> Unit = {},
) {
    item(span = StaggeredGridItemSpan.FullLine) {
        SourcesRow(
            sources = sources,
            selected = selectedSource,
            onSourceClick = onSourceClick,
            onManageClick = onManageSourcesClick,
        )
    }
    sourceHeader?.invoke(this)
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewGridHeader() {
    WallFlowTheme {
        Surface {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(8.dp),
            ) {
                header(
                    sources = persistentListOf(OnlineSource.WALLHAVEN),
                    sourceHeader = {
                        wallhavenHeader(
                            wallhavenTags = List(5) {
                                WallhavenTag(
                                    id = it.toLong(),
                                    name = "tag_$it",
                                    alias = emptyList(),
                                    categoryId = it.toLong(),
                                    category = "category",
                                    purity = Purity.SFW,
                                    createdAt = Clock.System.now(),
                                )
                            }.toPersistentList(),
                            isTagsLoading = false,
                            onTagClick = {},
                        )
                    },
                )
            }
        }
    }
}

internal fun LazyStaggeredGridScope.wallhavenHeader(
    wallhavenTags: ImmutableList<WallhavenTag> = persistentListOf(),
    isTagsLoading: Boolean = false,
    onTagClick: (wallhavenTag: WallhavenTag) -> Unit = {},
    onTagLongClick: (wallhavenTag: WallhavenTag) -> Unit = {},
) {
    if (wallhavenTags.isNotEmpty()) {
        item(span = StaggeredGridItemSpan.FullLine) {
            PopularTagsRow(
                wallhavenTags = wallhavenTags,
                loading = isTagsLoading,
                onTagClick = onTagClick,
                onTagLongClick = onTagLongClick,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SourcesRow(
    modifier: Modifier = Modifier,
    sources: ImmutableList<OnlineSource> = persistentListOf(),
    selected: OnlineSource = OnlineSource.WALLHAVEN,
    onSourceClick: (OnlineSource) -> Unit = {},
    onManageClick: () -> Unit = {},
) {
    Row(
        modifier = modifier.scrollable(
            state = rememberScrollState(),
            orientation = Orientation.Horizontal,
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (sources.isEmpty()) {
            repeat(2) {
                PlaceholderChip(
                    modifier = Modifier.width(120.dp),
                )
            }
        } else {
            sources.forEach {
                SourceChip(
                    source = it,
                    selected = selected == it,
                    onClick = { onSourceClick(it) },
                )
            }
        }
        if (sources.isNotEmpty()) {
            FilledTonalIconButton(
                modifier = Modifier.size(32.dp),
                onClick = onManageClick,
            ) {
                Icon(
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                    imageVector = if (sources.size == OnlineSource.entries.size) {
                        Icons.Default.Edit
                    } else {
                        Icons.Default.Add
                    },
                    contentDescription = stringResource(R.string.add),
                )
            }
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewSourcesRow() {
    WallFlowTheme {
        Surface {
            Box(modifier = Modifier.padding(8.dp)) {
                Column {
                    SourcesRow(
                        sources = persistentListOf(),
                    )
                    SourcesRow(
                        sources = persistentListOf(
                            OnlineSource.WALLHAVEN,
                            OnlineSource.REDDIT,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SourceChip(
    source: OnlineSource,
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
            borderWidth = 0.dp,
            borderColor = Color.Transparent,
        ),
        leadingIcon = {
            Icon(
                modifier = Modifier.size(FilterChipDefaults.IconSize),
                painter = painterResource(
                    when (source) {
                        OnlineSource.WALLHAVEN -> R.drawable.wallhaven_logo_short
                        OnlineSource.REDDIT -> R.drawable.reddit
                    },
                ),
                contentDescription = null,
            )
        },
        label = {
            Text(
                text = stringResource(
                    when (source) {
                        OnlineSource.WALLHAVEN -> R.string.wallhaven_cc
                        OnlineSource.REDDIT -> R.string.reddit
                    },
                ),
            )
        },
        selected = selected,
        onClick = onClick,
    )
}

@Composable
fun PopularTagsRow(
    modifier: Modifier = Modifier,
    wallhavenTags: ImmutableList<WallhavenTag> = persistentListOf(),
    loading: Boolean = false,
    onTagClick: (wallhavenTag: WallhavenTag) -> Unit = {},
    onTagLongClick: (wallhavenTag: WallhavenTag) -> Unit = {},
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item {
            Text(
                text = "${stringResource(R.string.popular_tags)}:",
            )
        }
        items(wallhavenTags) {
            TagChip(
                wallhavenTag = it,
                loading = loading,
                onClick = { onTagClick(it) },
                onLongClick = { onTagLongClick(it) },
            )
        }
    }
}

@Preview(widthDp = 300)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 300)
@Composable
private fun PreviewPopularTagsRow() {
    WallFlowTheme {
        Surface {
            PopularTagsRow(
                wallhavenTags = List(5) {
                    WallhavenTag(
                        id = it.toLong(),
                        name = "Test-$it",
                        alias = emptyList(),
                        categoryId = 1,
                        category = "",
                        purity = Purity.SFW,
                        createdAt = Clock.System.now(),
                    )
                }.toPersistentList(),
            )
        }
    }
}

@Composable
fun SearchBarFiltersToggle(
    modifier: Modifier = Modifier,
    checked: Boolean = false,
    onCheckedChange: (Boolean) -> Unit = {},
) {
    IconToggleButton(
        modifier = modifier,
        checked = checked,
        onCheckedChange = onCheckedChange,
    ) {
        Icon(
            painterResource(R.drawable.baseline_filter_alt_24),
            contentDescription = null,
        )
    }
}

// @Composable
// fun SearchBarOverflowMenu(
//     modifier: Modifier = Modifier,
//     items: List<MenuItem> = emptyList(),
//     onItemClick: (MenuItem) -> Unit = {},
// ) {
//     OverflowMenu(
//         modifier = modifier,
//     ) { closeMenu ->
//         items.forEach {
//             DropdownMenuItem(
//                 text = { Text(it.text) },
//                 onClick = {
//                     onItemClick(it)
//                     closeMenu()
//                 },
//             )
//         }
//     }
// }

@Composable
fun FiltersBottomSheetHeader(
    source: OnlineSource,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.home_filters),
    saveEnabled: Boolean = true,
    onSaveClick: () -> Unit = {},
    onSaveAsClick: () -> Unit = {},
    onLoadClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val menuItems = remember(context) {
        listOf(
            MenuItem(
                text = context.getString(R.string.save_as),
                value = "save_as",
                onClick = onSaveAsClick,
            ),
            MenuItem(
                text = context.getString(R.string.load),
                value = "load",
                onClick = onLoadClick,
            ),
        )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(
                    when (source) {
                        OnlineSource.WALLHAVEN -> R.string.wallhaven_cc
                        OnlineSource.REDDIT -> R.string.reddit
                    },
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Spacer(modifier = Modifier.requiredWidth(8.dp))
        Button(
            enabled = saveEnabled,
            onClick = onSaveClick,
        ) {
            Text(stringResource(R.string.save))
        }
        OverflowMenu { closeMenu ->
            menuItems.forEach {
                DropdownMenuItem(
                    text = { Text(it.text) },
                    onClick = {
                        it.onClick?.invoke()
                        closeMenu()
                    },
                )
            }
        }
    }
    HorizontalDivider(modifier = Modifier.fillMaxWidth())
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewEditSearchBottomSheetHeader() {
    WallFlowTheme {
        Surface {
            Column {
                FiltersBottomSheetHeader(
                    source = OnlineSource.WALLHAVEN,
                )
            }
        }
    }
}
