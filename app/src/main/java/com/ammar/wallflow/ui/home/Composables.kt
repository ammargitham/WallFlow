package com.ammar.wallflow.ui.home

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
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
import com.ammar.wallflow.model.Purity
import com.ammar.wallflow.model.Tag
import com.ammar.wallflow.ui.common.OverflowMenu
import com.ammar.wallflow.ui.common.TagChip
import com.ammar.wallflow.ui.theme.WallFlowTheme
import kotlinx.datetime.Clock


@Composable
fun PopularTagsRow(
    modifier: Modifier = Modifier,
    tags: List<Tag> = emptyList(),
    loading: Boolean = false,
    onTagClick: (tag: Tag) -> Unit = {},
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item {
            Text(
                text = "${stringResource(R.string.popular_tags)}:"
            )
        }
        items(tags) {
            TagChip(
                tag = it,
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
                tags = List(5) {
                    Tag(
                        id = it.toLong(),
                        name = "Test-$it",
                        alias = emptyList(),
                        categoryId = 1,
                        category = "",
                        purity = Purity.SFW,
                        createdAt = Clock.System.now(),
                    )
                }
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
    Divider(modifier = Modifier.fillMaxWidth())
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
