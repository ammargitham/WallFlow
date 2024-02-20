package com.ammar.wallflow.ui.screens.settings.composables

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.DISABLED_ALPHA
import com.ammar.wallflow.R
import com.ammar.wallflow.data.preferences.AutoWallpaperPreferences
import com.ammar.wallflow.data.preferences.MutableStateAutoWallpaperPreferencesSaver
import com.ammar.wallflow.model.local.LocalDirectory
import com.ammar.wallflow.model.search.RedditSearch
import com.ammar.wallflow.model.search.SavedSearch
import com.ammar.wallflow.model.search.WallhavenSearch
import com.ammar.wallflow.ui.common.DropdownMultiple
import com.ammar.wallflow.ui.common.DropdownOption
import com.ammar.wallflow.ui.common.UnpaddedAlertDialogContent
import com.ammar.wallflow.ui.theme.WallFlowTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoWallpaperSourceOptionsDialog(
    modifier: Modifier = Modifier,
    autoWallpaperPreferences: AutoWallpaperPreferences = AutoWallpaperPreferences(),
    savedSearches: ImmutableList<SavedSearch> = persistentListOf(),
    localDirectories: ImmutableList<LocalDirectory> = persistentListOf(),
    onSaveClick: (AutoWallpaperPreferences) -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    var localPrefs by rememberSaveable(
        autoWallpaperPreferences,
        saver = MutableStateAutoWallpaperPreferencesSaver,
    ) {
        mutableStateOf(autoWallpaperPreferences)
    }
    val saveEnabled by remember {
        derivedStateOf {
            // if all sources are disabled
            if (!localPrefs.savedSearchEnabled &&
                !localPrefs.favoritesEnabled &&
                !localPrefs.localEnabled
            ) {
                return@derivedStateOf false
            }
            // if saved search is enabled and saved search id is not set
            !(localPrefs.savedSearchEnabled && localPrefs.savedSearchIds.isEmpty())
        }
    }

    BasicAlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
    ) {
        UnpaddedAlertDialogContent(
            title = { Text(text = stringResource(R.string.sources)) },
            text = {
                AutoWallpaperSourceOptionsDialogContent(
                    savedSearchEnabled = localPrefs.savedSearchEnabled,
                    favoritesEnabled = localPrefs.favoritesEnabled,
                    savedSearches = savedSearches,
                    selectedSavedSearchIds = localPrefs.savedSearchIds.toPersistentSet(),
                    localEnabled = localPrefs.localEnabled,
                    localDirectories = localDirectories,
                    onChangeSavedSearchEnabled = {
                        localPrefs = localPrefs.copy(
                            savedSearchEnabled = it,
                            savedSearchIds = localPrefs.savedSearchIds.ifEmpty {
                                val savedSearchId = savedSearches.firstOrNull()?.id
                                if (savedSearchId != null) {
                                    setOf(savedSearchId)
                                } else {
                                    emptySet()
                                }
                            },
                        )
                    },
                    onChangeFavoritesEnabled = {
                        localPrefs = localPrefs.copy(favoritesEnabled = it)
                    },
                    onChangeLocalEnabled = {
                        localPrefs = localPrefs.copy(localEnabled = it)
                    },
                    onSavedSearchIdsChange = {
                        localPrefs = localPrefs.copy(savedSearchIds = it)
                    },
                )
            },
            buttons = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(text = stringResource(R.string.cancel))
                    }
                    TextButton(
                        onClick = { onSaveClick(localPrefs) },
                        enabled = saveEnabled,
                    ) {
                        Text(text = stringResource(R.string.save))
                    }
                }
            },
        )
    }
}

@Composable
private fun AutoWallpaperSourceOptionsDialogContent(
    modifier: Modifier = Modifier,
    savedSearchEnabled: Boolean = false,
    favoritesEnabled: Boolean = false,
    savedSearches: ImmutableList<SavedSearch> = persistentListOf(),
    selectedSavedSearchIds: ImmutableSet<Long> = persistentSetOf(),
    localEnabled: Boolean = false,
    localDirectories: ImmutableList<LocalDirectory> = persistentListOf(),
    onChangeSavedSearchEnabled: (Boolean) -> Unit = {},
    onChangeFavoritesEnabled: (Boolean) -> Unit = {},
    onChangeLocalEnabled: (Boolean) -> Unit = {},
    onSavedSearchIdsChange: (Set<Long>) -> Unit = {},
) {
    val localSavedSearchEnabled = savedSearchEnabled && savedSearches.isNotEmpty()
    val localLocalEnabled = localEnabled && localDirectories.isNotEmpty()
    val savedSearchAlpha = if (savedSearches.isNotEmpty()) 1f else DISABLED_ALPHA
    val localAlpha = if (localDirectories.isNotEmpty()) 1f else DISABLED_ALPHA

    Column(
        modifier = modifier,
    ) {
        ListItem(
            modifier = Modifier
                .clickable { onChangeSavedSearchEnabled(!localSavedSearchEnabled) }
                .padding(horizontal = 8.dp),
            headlineContent = {
                Text(
                    text = stringResource(R.string.saved_search),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = savedSearchAlpha),
                )
            },
            supportingContent = if (savedSearches.isEmpty()) {
                {
                    Text(
                        text = stringResource(R.string.no_saved_searches),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = savedSearchAlpha,
                        ),
                    )
                }
            } else {
                null
            },
            leadingContent = {
                Checkbox(
                    modifier = Modifier.size(24.dp),
                    enabled = savedSearches.isNotEmpty(),
                    checked = localSavedSearchEnabled,
                    onCheckedChange = onChangeSavedSearchEnabled,
                )
            },
        )
        AnimatedVisibility(visible = localSavedSearchEnabled) {
            DropdownMultiple(
                modifier = Modifier
                    .padding(
                        start = 64.dp,
                        end = 24.dp,
                    )
                    .fillMaxWidth(),
                placeholder = { Text(text = stringResource(R.string.saved_search)) },
                emptyOptionsMessage = stringResource(R.string.no_saved_searches),
                options = savedSearches.mapTo(mutableSetOf()) {
                    DropdownOption(
                        value = it.id,
                        text = it.name,
                        icon = {
                            Icon(
                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                                painter = painterResource(
                                    when (it.search) {
                                        is WallhavenSearch -> R.drawable.wallhaven_logo_short
                                        is RedditSearch -> R.drawable.reddit
                                    },
                                ),
                                contentDescription = null,
                            )
                        },
                    )
                },
                initialSelectedOptions = selectedSavedSearchIds,
                onChange = { onSavedSearchIdsChange(it) },
            )
        }
        ListItem(
            modifier = Modifier
                .clickable { onChangeFavoritesEnabled(!favoritesEnabled) }
                .padding(horizontal = 8.dp),
            headlineContent = { Text(text = stringResource(R.string.favorites)) },
            leadingContent = {
                Checkbox(
                    modifier = Modifier.size(24.dp),
                    checked = favoritesEnabled,
                    onCheckedChange = onChangeFavoritesEnabled,
                )
            },
        )
        ListItem(
            modifier = Modifier
                .clickable { onChangeLocalEnabled(!localLocalEnabled) }
                .padding(horizontal = 8.dp),
            headlineContent = {
                Text(
                    text = stringResource(R.string.local),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = localAlpha),
                )
            },
            supportingContent = if (localDirectories.isEmpty()) {
                {
                    Text(
                        text = stringResource(R.string.no_local_dirs),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = localAlpha,
                        ),
                    )
                }
            } else {
                null
            },
            leadingContent = {
                Checkbox(
                    modifier = Modifier.size(24.dp),
                    enabled = localDirectories.isNotEmpty(),
                    checked = localLocalEnabled,
                    onCheckedChange = onChangeLocalEnabled,
                )
            },
        )
    }
}

private data class AutoWallSrcOptsDialogParameters(
    val savedSearches: List<SavedSearch> = emptyList(),
    val prefs: AutoWallpaperPreferences = AutoWallpaperPreferences(),
)

private class AutoWallSrcOptsDialogPP : CollectionPreviewParameterProvider<AutoWallSrcOptsDialogParameters>(
    listOf(
        AutoWallSrcOptsDialogParameters(),
        AutoWallSrcOptsDialogParameters(
            prefs = AutoWallpaperPreferences(
                savedSearchIds = setOf(1),
                savedSearchEnabled = true,
            ),
            savedSearches = List(3) {
                SavedSearch(
                    id = it.toLong(),
                    name = "Saved search $it",
                    search = WallhavenSearch(),
                )
            },
        ),
    ),
)

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewAutoWallpaperSourceOptionsDialog(
    @PreviewParameter(AutoWallSrcOptsDialogPP::class) parameters: AutoWallSrcOptsDialogParameters,
) {
    WallFlowTheme {
        Surface {
            AutoWallpaperSourceOptionsDialog(
                autoWallpaperPreferences = parameters.prefs,
                savedSearches = parameters.savedSearches.toPersistentList(),
            )
        }
    }
}
