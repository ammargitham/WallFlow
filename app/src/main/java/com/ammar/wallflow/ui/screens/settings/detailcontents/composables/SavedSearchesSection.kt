package com.ammar.wallflow.ui.screens.settings.detailcontents.composables

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.DISABLED_ALPHA
import com.ammar.wallflow.R
import com.ammar.wallflow.model.search.RedditSearch
import com.ammar.wallflow.model.search.SavedSearch
import com.ammar.wallflow.model.search.WallhavenSearch
import com.ammar.wallflow.ui.common.DropdownMultiple
import com.ammar.wallflow.ui.common.DropdownOption
import com.ammar.wallflow.ui.common.SectionHeader
import com.ammar.wallflow.ui.screens.settings.composables.SettingsExtraListItem
import com.ammar.wallflow.ui.theme.WallFlowTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun ColumnScope.SavedSearchesSection(
    savedSearches: ImmutableList<SavedSearch> = persistentListOf(),
    savedSearchEnabled: Boolean = false,
    savedSearchIds: Set<Long> = emptySet(),
    lightDarkEnabled: Boolean = false,
    isExpanded: Boolean = false,
    onChangeSavedSearchEnabled: (Boolean) -> Unit = {},
    onChangeSavedSearchIds: (Set<Long>) -> Unit = {},
) {
    val disabled = savedSearches.isEmpty() || lightDarkEnabled
    val alpha = if (disabled) DISABLED_ALPHA else 1f
    val supportingTextRes: Int? = if (savedSearches.isEmpty()) {
        R.string.no_saved_searches
    } else if (lightDarkEnabled) {
        R.string.light_dark_enabled
    } else {
        null
    }

    SectionHeader(text = stringResource(R.string.saved_searches))
    SettingsExtraListItem(
        modifier = Modifier.clickable(enabled = !disabled) {
            onChangeSavedSearchEnabled(!savedSearchEnabled)
        },
        isExpanded = isExpanded,
        headlineContent = {
            Text(
                text = stringResource(R.string.use_saved_searches),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            )
        },
        supportingContent = if (supportingTextRes != null) {
            {
                Text(
                    text = stringResource(supportingTextRes),
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
                enabled = !disabled,
                checked = savedSearchEnabled && !disabled,
                onCheckedChange = onChangeSavedSearchEnabled,
            )
        },
    )
    AnimatedVisibility(visible = savedSearchEnabled && !disabled) {
        DropdownMultiple(
            modifier = Modifier
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                )
                .fillMaxWidth(),
            showOptionClearAction = savedSearchIds.size > 1,
            placeholder = { Text(text = stringResource(R.string.choose_saved_searches)) },
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
            selected = savedSearchIds,
            onChange = onChangeSavedSearchIds,
        )
    }
}

private data class SavedSearchesSectionParameters(
    val savedSearches: List<SavedSearch> = emptyList(),
    val savedSearchEnabled: Boolean = false,
    val savedSearchIds: Set<Long> = emptySet(),
    val lightDarkEnabled: Boolean = false,
)

private class SavedSearchesSectionPPP :
    CollectionPreviewParameterProvider<SavedSearchesSectionParameters>(
        listOf(
            SavedSearchesSectionParameters(),
            SavedSearchesSectionParameters(
                savedSearchEnabled = true,
            ),
            SavedSearchesSectionParameters(
                savedSearchEnabled = true,
                savedSearches = List(3) {
                    SavedSearch(
                        id = it.toLong(),
                        name = "Saved search $it",
                        search = WallhavenSearch(),
                    )
                },
            ),
            SavedSearchesSectionParameters(
                savedSearchEnabled = true,
                savedSearches = List(3) {
                    SavedSearch(
                        id = it.toLong(),
                        name = "Saved search $it",
                        search = WallhavenSearch(),
                    )
                },
                lightDarkEnabled = true,
            ),
        ),
    )

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewSavedSearchesSection(
    @PreviewParameter(
        provider = SavedSearchesSectionPPP::class,
    ) parameters: SavedSearchesSectionParameters,
) {
    WallFlowTheme {
        Surface {
            Column {
                SavedSearchesSection(
                    savedSearches = parameters.savedSearches.toImmutableList(),
                    savedSearchEnabled = parameters.savedSearchEnabled,
                    savedSearchIds = parameters.savedSearchIds,
                    lightDarkEnabled = parameters.lightDarkEnabled,
                )
            }
        }
    }
}
