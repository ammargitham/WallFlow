package com.ammar.havenwalls.ui.home

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ammar.havenwalls.R
import com.ammar.havenwalls.data.common.Purity
import com.ammar.havenwalls.model.Tag
import com.ammar.havenwalls.ui.common.OverflowMenu
import com.ammar.havenwalls.ui.common.TagChip
import com.ammar.havenwalls.ui.theme.HavenWallsTheme
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
    HavenWallsTheme {
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

data class MenuItem(
    val text: String,
    val value: String,
)
