package com.ammar.havenwalls.ui.common.fab

import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun FloatingActionButton(
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    text: @Composable () -> Unit = {},
    icon: @Composable () -> Unit = {},
    onClick: () -> Unit = {},
) {
    ExtendedFloatingActionButton(
        modifier = modifier,
        onClick = onClick,
        expanded = expanded,
        icon = icon,
        text = text,
    )
}
