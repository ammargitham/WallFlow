package com.ammar.wallflow.ui.screens.settings.autowallpapersources.composables

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.DISABLED_ALPHA
import com.ammar.wallflow.R
import com.ammar.wallflow.model.local.LocalDirectory
import com.ammar.wallflow.ui.common.DropdownMultiple
import com.ammar.wallflow.ui.common.DropdownOption
import com.ammar.wallflow.ui.common.SectionHeader
import com.ammar.wallflow.ui.theme.WallFlowTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun LocalSection(
    localDirectories: ImmutableList<LocalDirectory> = persistentListOf(),
    localEnabled: Boolean = false,
    selectedUris: Set<Uri> = emptySet(),
    onChangeLocalEnabled: (Boolean) -> Unit = {},
    onChangeSelectedUris: (Set<Uri>) -> Unit = {},
) {
    val alpha = if (localDirectories.isNotEmpty()) 1f else DISABLED_ALPHA

    SectionHeader(text = stringResource(R.string.local))
    ListItem(
        modifier = Modifier.clickable(
            enabled = localDirectories.isNotEmpty(),
        ) { onChangeLocalEnabled(!localEnabled) },
        headlineContent = {
            Text(
                text = stringResource(R.string.use_local_dirs),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            )
        },
        supportingContent = if (localDirectories.isEmpty()) {
            {
                Text(
                    text = stringResource(R.string.no_local_dirs),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = alpha,
                    ),
                )
            }
        } else {
            null
        },
        trailingContent = {
            Switch(
                enabled = localDirectories.isNotEmpty(),
                checked = localEnabled && localDirectories.isNotEmpty(),
                onCheckedChange = onChangeLocalEnabled,
            )
        },
    )
    AnimatedVisibility(visible = localEnabled && localDirectories.isNotEmpty()) {
        DropdownMultiple(
            modifier = Modifier
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                )
                .fillMaxWidth(),
            showOptionClearAction = selectedUris.size > 1,
            placeholder = { Text(text = stringResource(R.string.choose_local_dirs)) },
            emptyOptionsMessage = stringResource(R.string.no_local_dirs),
            options = localDirectories.mapTo(mutableSetOf()) {
                DropdownOption(
                    value = it.uri,
                    text = it.path,
                )
            },
            initialSelectedOptions = selectedUris,
            onChange = onChangeSelectedUris,
        )
    }
}

private data class LocalSectionParameters(
    val localDirectories: List<LocalDirectory> = emptyList(),
    val localEnabled: Boolean = false,
)

private class LocalSectionPPP : CollectionPreviewParameterProvider<LocalSectionParameters>(
    listOf(
        LocalSectionParameters(),
        LocalSectionParameters(
            localEnabled = true,
        ),
        LocalSectionParameters(
            localEnabled = true,
            localDirectories = listOf(
                LocalDirectory(
                    uri = Uri.EMPTY,
                    path = "test",
                ),
            ),
        ),
    ),
)

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewLocalSection(
    @PreviewParameter(provider = LocalSectionPPP::class) parameters: LocalSectionParameters,
) {
    WallFlowTheme {
        Surface {
            LocalSection(
                localDirectories = parameters.localDirectories.toImmutableList(),
                localEnabled = parameters.localEnabled,
            )
        }
    }
}
