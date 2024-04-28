package com.ammar.wallflow.ui.screens.more

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
fun BoxScope.MoreList(
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    isMedium: Boolean = false,
    items: List<MoreListItem> = emptyList(),
    onItemClick: (MoreListItem.Clickable) -> Unit = {},
) {
    LazyColumn(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth(
                fraction = if (isExpanded) {
                    0.75f
                } else {
                    1f
                },
            )
            .align(Alignment.TopCenter),
    ) {
        items(items) { item ->
            when (item) {
                is MoreListItem.Clickable -> {
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
                        colors = ListItemDefaults.colors(
                            containerColor = if (isMedium || isExpanded) {
                                MaterialTheme.colorScheme.surfaceContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                        ),
                    )
                }
                is MoreListItem.Content -> item.content()
                MoreListItem.Divider -> HorizontalDivider(
                    modifier = Modifier.padding(
                        vertical = 8.dp,
                    ),
                )
                is MoreListItem.Static -> ListItem(
                    modifier = Modifier,
                    headlineContent = { Text(text = item.label) },
                    supportingContent = { Text(text = item.supportingText) },
                    colors = ListItemDefaults.colors(
                        containerColor = if (isMedium || isExpanded) {
                            MaterialTheme.colorScheme.surfaceContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                    ),
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
    val isMedium: Boolean,
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
            isMedium = false,
        ),
        MoreListContainerProps(
            items = previewItems,
            isExpanded = true,
            isMedium = false,
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
        Surface(
            color = if (props.isExpanded) {
                MaterialTheme.colorScheme.surfaceContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                MoreList(
                    isExpanded = props.isExpanded,
                    items = props.items,
                )
            }
        }
    }
}
