package com.ammar.wallflow.ui.screens.settings.detailcontents

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.ammar.wallflow.R
import com.ammar.wallflow.model.WallpaperTarget
import com.ammar.wallflow.model.local.LocalDirectory
import com.ammar.wallflow.model.search.SavedSearch
import com.ammar.wallflow.model.search.WallhavenSearch
import com.ammar.wallflow.ui.screens.settings.AutoWallpaperSources
import com.ammar.wallflow.ui.screens.settings.composables.SettingsExtraListItem
import com.ammar.wallflow.ui.screens.settings.composables.wallpaperTargetString
import com.ammar.wallflow.ui.screens.settings.detailcontents.composables.FavoritesSection
import com.ammar.wallflow.ui.screens.settings.detailcontents.composables.LightDarkSection
import com.ammar.wallflow.ui.screens.settings.detailcontents.composables.LocalSection
import com.ammar.wallflow.ui.screens.settings.detailcontents.composables.SavedSearchesSection
import com.ammar.wallflow.ui.theme.WallFlowTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAutoWallpaperSourcesContent(
    modifier: Modifier = Modifier,
    useSameSources: Boolean = true,
    hasLightDarkWallpapers: Boolean = false,
    hasFavorites: Boolean = false,
    savedSearches: ImmutableList<SavedSearch> = persistentListOf(),
    localDirectories: ImmutableList<LocalDirectory> = persistentListOf(),
    homeScreenSources: AutoWallpaperSources = AutoWallpaperSources(),
    lockScreenSources: AutoWallpaperSources = AutoWallpaperSources(),
    isExpanded: Boolean = false,
    onChangeUseSameSources: (Boolean) -> Unit = {},
    onChangeLightDarkEnabled: (Boolean, WallpaperTarget) -> Unit = { _, _ -> },
    onChangeUseDarkWithExtraDim: (Boolean, WallpaperTarget) -> Unit = { _, _ -> },
    onChangeSavedSearchEnabled: (Boolean, WallpaperTarget) -> Unit = { _, _ -> },
    onChangeSavedSearchIds: (Set<Long>, WallpaperTarget) -> Unit = { _, _ -> },
    onChangeFavoritesEnabled: (Boolean, WallpaperTarget) -> Unit = { _, _ -> },
    onChangeLocalEnabled: (Boolean, WallpaperTarget) -> Unit = { _, _ -> },
    onChangeSelectedLocalDirs: (Set<Uri>, WallpaperTarget) -> Unit = { _, _ -> },
) {
    var activeTabIndex by remember { mutableIntStateOf(0) }
    val activeTarget = WallpaperTarget.entries.getOrNull(activeTabIndex) ?: WallpaperTarget.HOME
    val activeSources = if (useSameSources) {
        homeScreenSources
    } else {
        when (activeTarget) {
            WallpaperTarget.HOME -> homeScreenSources
            WallpaperTarget.LOCKSCREEN -> lockScreenSources
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(state = rememberScrollState()),
    ) {
        SettingsExtraListItem(
            modifier = Modifier.clickable { onChangeUseSameSources(!useSameSources) },
            isExpanded = isExpanded,
            headlineContent = {
                Text(
                    text = stringResource(R.string.use_same_sources_homescreen_lockscreen),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            trailingContent = {
                Switch(
                    checked = useSameSources,
                    onCheckedChange = onChangeUseSameSources,
                )
            },
        )
        HorizontalDivider()
        AnimatedVisibility(visible = !useSameSources) {
            SecondaryTabRow(selectedTabIndex = activeTabIndex) {
                WallpaperTarget.entries.forEachIndexed { index, target ->
                    Tab(
                        selected = activeTabIndex == index,
                        onClick = { activeTabIndex = index },
                        text = {
                            Text(
                                text = wallpaperTargetString(target),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
            }
        }
        LightDarkSection(
            hasLightDarkWallpapers = hasLightDarkWallpapers,
            lightDarkEnabled = activeSources.lightDarkEnabled,
            useDarkWithExtraDim = activeSources.useDarkWithExtraDim,
            isExpanded = isExpanded,
            onChangeLightDarkEnabled = { onChangeLightDarkEnabled(it, activeTarget) },
            onChangeUseDarkWithExtraDim = { onChangeUseDarkWithExtraDim(it, activeTarget) },
        )
        SavedSearchesSection(
            savedSearches = savedSearches,
            savedSearchEnabled = activeSources.savedSearchEnabled,
            savedSearchIds = activeSources.savedSearchIds,
            lightDarkEnabled = activeSources.lightDarkEnabled,
            isExpanded = isExpanded,
            onChangeSavedSearchEnabled = { onChangeSavedSearchEnabled(it, activeTarget) },
            onChangeSavedSearchIds = { onChangeSavedSearchIds(it, activeTarget) },
        )
        FavoritesSection(
            favoritesEnabled = activeSources.favoritesEnabled,
            hasFavorites = hasFavorites,
            lightDarkEnabled = activeSources.lightDarkEnabled,
            isExpanded = isExpanded,
            onChangeFavoritesEnabled = { onChangeFavoritesEnabled(it, activeTarget) },
        )
        LocalSection(
            localDirectories = localDirectories,
            localEnabled = activeSources.localEnabled,
            selectedUris = activeSources.localDirs,
            lightDarkEnabled = activeSources.lightDarkEnabled,
            isExpanded = isExpanded,
            onChangeLocalEnabled = { onChangeLocalEnabled(it, activeTarget) },
            onChangeSelectedUris = { onChangeSelectedLocalDirs(it, activeTarget) },
        )
    }
}

private data class ManageAutoWallpaperSourcesContentParameters(
    val savedSearches: List<SavedSearch> = emptyList(),
    val localDirectories: List<LocalDirectory> = emptyList(),
    val useSameSources: Boolean = true,
    val hasLightDarkWallpapers: Boolean = false,
    val homeScreenSources: AutoWallpaperSources = AutoWallpaperSources(),
    val lockScreenSources: AutoWallpaperSources = AutoWallpaperSources(),
)

private class ManageAutoWallpaperSourcesContentParametersPP :
    CollectionPreviewParameterProvider<ManageAutoWallpaperSourcesContentParameters>(
        listOf(
            ManageAutoWallpaperSourcesContentParameters(),
            ManageAutoWallpaperSourcesContentParameters(
                useSameSources = false,
                hasLightDarkWallpapers = true,
                homeScreenSources = AutoWallpaperSources(
                    lightDarkEnabled = true,
                    useDarkWithExtraDim = true,
                ),
            ),
            ManageAutoWallpaperSourcesContentParameters(
                useSameSources = false,
                homeScreenSources = AutoWallpaperSources(
                    savedSearchEnabled = true,
                    savedSearchIds = setOf(1),
                ),
                savedSearches = List(3) {
                    SavedSearch(
                        id = it.toLong(),
                        name = "Saved search $it",
                        search = WallhavenSearch(),
                    )
                },
            ),
            ManageAutoWallpaperSourcesContentParameters(
                useSameSources = false,
                homeScreenSources = AutoWallpaperSources(
                    localEnabled = true,
                    localDirs = setOf(Uri.EMPTY),
                ),
                localDirectories = List(3) {
                    LocalDirectory(
                        uri = Uri.EMPTY,
                        path = "test",
                    )
                },
            ),
        ),
    )

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewManageAutoWallpaperSourcesScreenContent(
    @PreviewParameter(
        ManageAutoWallpaperSourcesContentParametersPP::class,
    ) parameters: ManageAutoWallpaperSourcesContentParameters,
) {
    WallFlowTheme {
        Surface {
            Column {
                ManageAutoWallpaperSourcesContent(
                    useSameSources = parameters.useSameSources,
                    hasLightDarkWallpapers = parameters.hasLightDarkWallpapers,
                    savedSearches = parameters.savedSearches.toImmutableList(),
                    localDirectories = parameters.localDirectories.toImmutableList(),
                    homeScreenSources = parameters.homeScreenSources,
                    lockScreenSources = parameters.lockScreenSources,
                )
            }
        }
    }
}
