package com.ammar.wallflow.ui.screens.settings.composables

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun SettingsDetailListItem(
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    selected: Boolean = false,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    ListItem(
        // clip first then apply root Modifier
        modifier = Modifier.then(
            if (isExpanded) {
                Modifier.clip(
                    RoundedCornerShape(
                        topStart = (if (isFirst) 16 else 0).dp,
                        topEnd = (if (isFirst) 16 else 0).dp,
                        bottomStart = (if (isLast) 16 else 0).dp,
                        bottomEnd = (if (isLast) 16 else 0).dp,
                    ),
                )
            } else {
                Modifier
            }.then(modifier),
        ),
        headlineContent = headlineContent,
        overlineContent = overlineContent,
        supportingContent = supportingContent,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        colors = ListItemDefaults.colors(
            containerColor = if (isExpanded) {
                if (selected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceBright
                }
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    )
}
