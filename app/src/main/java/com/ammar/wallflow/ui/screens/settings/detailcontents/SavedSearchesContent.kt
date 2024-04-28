package com.ammar.wallflow.ui.screens.settings.detailcontents

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ammar.wallflow.R
import com.ammar.wallflow.ui.screens.settings.composables.SettingsDetailListItem
import com.ammar.wallflow.ui.theme.WallFlowTheme

@Composable
internal fun SavedSearchesContent(
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    onManageSavedSearchesClick: () -> Unit = {},
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
    ) {
        item {
            SettingsDetailListItem(
                modifier = Modifier.clickable(onClick = onManageSavedSearchesClick),
                isExpanded = isExpanded,
                isFirst = true,
                isLast = true,
                headlineContent = { Text(text = stringResource(R.string.manager_saved_searches)) },
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewSavedSearchesContent() {
    WallFlowTheme {
        Surface {
            SavedSearchesContent()
        }
    }
}
