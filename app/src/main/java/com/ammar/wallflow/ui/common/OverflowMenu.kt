package com.ammar.wallflow.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ammar.wallflow.R

@Composable
fun OverflowMenu(
    modifier: Modifier = Modifier,
    menuModifier: Modifier = Modifier,
    content: @Composable (closeMenu: () -> Unit) -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    val closeMenu = remember { { showMenu = false } }

    Box(
        modifier = modifier.wrapContentSize(Alignment.BottomEnd),
    ) {
        IconButton(
            onClick = {
                showMenu = !showMenu
            },
        ) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = stringResource(R.string.more),
            )
        }
        DropdownMenu(
            modifier = menuModifier,
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            content(closeMenu)
        }
    }
}
