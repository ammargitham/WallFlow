package com.ammar.wallflow.ui.screens.home

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.ammar.wallflow.ui.common.TagChip
import com.ammar.wallflow.ui.theme.WallFlowTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.Clock

internal fun LazyStaggeredGridScope.header(
    sourceHeader: (LazyStaggeredGridScope.() -> Unit)? = null,
    onSourceAddClick: () -> Unit = {},
) {
    item(span = StaggeredGridItemSpan.FullLine) {
        SourcesRow(
            onAddClick = onSourceAddClick,
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
) {
    if (wallhavenTags.isNotEmpty()) {
        item(span = StaggeredGridItemSpan.FullLine) {
            PopularTagsRow(
                wallhavenTags = wallhavenTags,
                loading = isTagsLoading,
                onTagClick = onTagClick,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SourcesRow(
    modifier: Modifier = Modifier,
    onSourceClick: (OnlineSource) -> Unit = {},
    onAddClick: () -> Unit = {},
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item(
            key = "wallhaven",
            contentType = "tab",
        ) {
            FilterChip(
                modifier = Modifier.height(40.dp),
                shape = MaterialTheme.shapes.large,
                leadingIcon = {
                    // Box(
                    //     modifier = Modifier
                    //         .size(24.dp)
                    //         .background(
                    //             // color = Color("#0b5277".toColorInt()),
                    //             // color = MaterialTheme.colorScheme.primaryContainer,
                    //             color = MaterialTheme.colorScheme.onSecondaryContainer,
                    //             shape = MaterialTheme.shapes.large,
                    //         ),
                    // ) {
                    Icon(
                        // modifier = Modifier
                        //     .align(Alignment.Center)
                        //     .padding(4.dp),
                        // tint = contentColorFor(MaterialTheme.colorScheme.primaryContainer),
                        // tint =
                        // MaterialTheme.colorScheme.secondaryContainer,
                        painter = painterResource(R.drawable.wallhaven_logo_short),
                        contentDescription = null,
                    )
                    // }
                },
                label = { Text(text = stringResource(R.string.wallhaven_cc)) },
                selected = true,
                onClick = {
                    onSourceClick(OnlineSource.WALLHAVEN)
                },
            )
        }
        item(contentType = "button") {
            FilledTonalIconButton(
                onClick = onAddClick,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
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
                SourcesRow()
            }
        }
    }
}

@Composable
fun PopularTagsRow(
    modifier: Modifier = Modifier,
    wallhavenTags: ImmutableList<WallhavenTag> = persistentListOf(),
    loading: Boolean = false,
    onTagClick: (wallhavenTag: WallhavenTag) -> Unit = {},
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

@Composable
fun SearchBarOverflowMenu(
    modifier: Modifier = Modifier,
    items: List<MenuItem> = emptyList(),
    onItemClick: (MenuItem) -> Unit = {},
) {
    OverflowMenu(
        modifier = modifier,
    ) { closeMenu ->
        items.forEach {
            DropdownMenuItem(
                text = { Text(it.text) },
                onClick = {
                    onItemClick(it)
                    closeMenu()
                },
            )
        }
    }
}

@Composable
fun HomeFiltersBottomSheetHeader(
    modifier: Modifier = Modifier,
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
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(R.string.home_filters),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.headlineMedium,
        )
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
                HomeFiltersBottomSheetHeader()
            }
        }
    }
}
