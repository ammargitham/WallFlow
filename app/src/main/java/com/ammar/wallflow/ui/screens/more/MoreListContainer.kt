package com.ammar.wallflow.ui.screens.more

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.BuildConfig
import com.ammar.wallflow.R
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
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(state = rememberScrollState()),
    ) {
        items.forEach { item ->
            when (item) {
                is MoreListItem.Clickable -> {
                    if (isExpanded) {
                        NavigationDrawerItem(
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
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
                    modifier = Modifier.padding(
                        vertical = 8.dp,
                        horizontal = if (isExpanded) 28.dp else 0.dp,
                    ),
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

private data class MoreListContainerProps(
    val items: List<MoreListItem>,
    val isExpanded: Boolean,
    val selectedItemValue: String? = null,
)

private val clickable = MoreListItem.Clickable(
    icon = R.drawable.baseline_settings_24,
    label = "Settings",
    value = ActiveOption.SETTINGS.name,
)

private val static = MoreListItem.Static(
    label = "Version",
    supportingText = BuildConfig.VERSION_NAME,
)

private val content = MoreListItem.Content {
    Image(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        painter = painterResource(R.drawable.ic_launcher_foreground),
        contentDescription = stringResource(R.string.app_name),
    )
}

private val previewItems = listOf(
    content,
    MoreListItem.Divider,
    clickable,
    MoreListItem.Divider,
    static,
)

private class MoreListContainerCPP : CollectionPreviewParameterProvider<MoreListContainerProps>(
    listOf(
        MoreListContainerProps(
            items = previewItems,
            isExpanded = false,
        ),
        MoreListContainerProps(
            items = previewItems,
            isExpanded = true,
            selectedItemValue = ActiveOption.SETTINGS.name,
        ),
    ),
)

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewMoreListContainer(
    @PreviewParameter(MoreListContainerCPP::class) props: MoreListContainerProps,
) {
    WallFlowTheme {
        Surface {
            MoreListContainer(
                items = props.items,
                isExpanded = props.isExpanded,
                selectedItemValue = props.selectedItemValue,
            )
        }
    }
}
