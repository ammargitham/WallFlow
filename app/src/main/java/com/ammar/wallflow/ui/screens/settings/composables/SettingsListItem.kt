package com.ammar.wallflow.ui.screens.settings.composables

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.R
import com.ammar.wallflow.ui.common.NoRippleInteractionSource
import com.ammar.wallflow.ui.theme.WallFlowTheme

fun LazyListScope.settingsListItem(
    @StringRes labelRes: Int,
    modifier: Modifier = Modifier,
    supportingContent: @Composable (() -> Unit)? = null,
    @DrawableRes iconRes: Int? = null,
    isExpanded: Boolean = false,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    settingsListItem(
        modifier = modifier,
        headlineContent = {
            Text(text = stringResource(labelRes))
        },
        supportingContent = supportingContent,
        iconRes = iconRes,
        iconContentDescriptionRes = labelRes,
        isExpanded = isExpanded,
        selected = selected,
        enabled = enabled,
        onClick = onClick,
    )
}

fun LazyListScope.settingsListItem(
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    supportingContent: @Composable (() -> Unit)? = null,
    @DrawableRes iconRes: Int? = null,
    @StringRes iconContentDescriptionRes: Int? = null,
    isExpanded: Boolean = false,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    item(contentType = "settings_list_item") {
        if (isExpanded) {
            NavigationDrawerItem(
                modifier = modifier,
                selected = selected,
                label = {
                    Column {
                        headlineContent()
                        if (supportingContent != null) {
                            CompositionLocalProvider(
                                LocalContentColor provides ListItemDefaults.colors()
                                    .supportingTextColor,
                                LocalTextStyle provides LocalTextStyle.current.merge(
                                    MaterialTheme.typography.bodyMedium,
                                ),
                                content = supportingContent,
                            )
                        }
                    }
                },
                icon = iconRes?.let {
                    {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = iconContentDescriptionRes?.let { res ->
                                stringResource(res)
                            },
                        )
                    }
                },
                interactionSource = if (enabled) {
                    null
                } else {
                    remember { NoRippleInteractionSource() }
                },
                onClick = if (enabled) {
                    onClick
                } else {
                    {}
                },
            )
        } else {
            ListItem(
                modifier = modifier.clickable(
                    enabled = enabled,
                    onClick = onClick,
                ),
                leadingContent = iconRes?.let {
                    {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = iconContentDescriptionRes?.let { res ->
                                stringResource(res)
                            },
                        )
                    }
                },
                headlineContent = headlineContent,
                supportingContent = supportingContent,
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewSettingsListItem() {
    WallFlowTheme {
        Surface {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(8.dp),
            ) {
                settingsListItem(
                    labelRes = R.string.label,
                    iconRes = R.drawable.baseline_favorite_24,
                )
                settingsListItem(
                    isExpanded = true,
                    labelRes = R.string.label,
                    selected = true,
                )
                settingsListItem(
                    isExpanded = true,
                    labelRes = R.string.label,
                    selected = true,
                    supportingContent = {
                        Text(text = "Subtitle")
                    },
                )
            }
        }
    }
}
