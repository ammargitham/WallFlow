package com.ammar.wallflow.ui.screens.settings.composables

import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SettingsExtraListItem(
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    ListItem(
        modifier = modifier,
        headlineContent = headlineContent,
        overlineContent = overlineContent,
        supportingContent = supportingContent,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        colors = ListItemDefaults.colors(
            containerColor = if (isExpanded) {
                MaterialTheme.colorScheme.surfaceBright
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    )
}
