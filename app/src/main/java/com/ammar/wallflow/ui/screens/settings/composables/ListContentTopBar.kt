package com.ammar.wallflow.ui.screens.settings.composables

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ammar.wallflow.R
import com.ammar.wallflow.ui.common.TopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ListContentTopBar(
    onBackClick: () -> Unit,
    isExpanded: Boolean = false,
) {
    TopBar(
        title = {
            Text(
                text = stringResource(R.string.settings),
                maxLines = 1,
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = if (isExpanded) {
                MaterialTheme.colorScheme.surfaceContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        showBackButton = true,
        onBackClick = onBackClick,
    )
}
