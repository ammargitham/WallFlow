package com.ammar.wallflow.ui.screens.more

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.ui.theme.WallFlowTheme

@Composable
fun MoreListContainer(
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    items: List<MoreListItem> = emptyList(),
    selectedItemValue: String? = null,
    onItemClick: (MoreListItem.Clickable) -> Unit = {},
) {
    PermanentDrawerSheet(
        modifier = modifier.fillMaxSize(),
    ) {
        items.forEach { item ->
            when (item) {
                is MoreListItem.Clickable -> {
                    if (isExpanded) {
                        NavigationDrawerItem(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            icon = item.icon?.let {
                                {
                                    Icon(
                                        painter = painterResource(it),
                                        contentDescription = null,
                                    )
                                }
                            },
                            label = { Text(text = item.label) },
                            selected = item.value == selectedItemValue,
                            onClick = { onItemClick(item) },
                        )
                    } else {
                        ListItem(
                            modifier = Modifier.clickable { onItemClick(item) },
                            leadingContent = item.icon?.let {
                                {
                                    Icon(
                                        painter = painterResource(it),
                                        contentDescription = null,
                                    )
                                }
                            },
                            headlineContent = { Text(text = item.label) },
                        )
                    }
                }
                is MoreListItem.Content -> item.content()
                MoreListItem.Divider -> HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                )
                is MoreListItem.Static -> ListItem(
                    modifier = Modifier.padding(
                        horizontal = if (isExpanded) 12.dp else 0.dp,
                    ),
                    headlineContent = { Text(text = item.label) },
                    supportingContent = { Text(text = item.supportingText) },
                )
            }
        }
    }
}

sealed interface MoreListItem {
    data class Clickable(
        @DrawableRes val icon: Int? = null,
        val label: String,
        val value: String,
    ) : MoreListItem

    data object Divider : MoreListItem

    data class Static(
        val label: String,
        val supportingText: String,
    ) : MoreListItem

    data class Content(
        val content: @Composable () -> Unit,
    ) : MoreListItem
}

private class MoreListContainerCPP : CollectionPreviewParameterProvider<Boolean>(
    listOf(
        false,
        true,
    ),
)

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewMoreListContainer(
    @PreviewParameter(MoreListContainerCPP::class) isExpanded: Boolean,
) {
    WallFlowTheme {
        Surface {
            MoreListContainer(
                isExpanded = isExpanded,
            )
        }
    }
}
