package com.ammar.wallflow.ui.screens.settings.autowallpapersources

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ammar.wallflow.R
import com.ammar.wallflow.model.WallpaperTarget
import com.ammar.wallflow.model.local.LocalDirectory
import com.ammar.wallflow.model.search.SavedSearch
import com.ammar.wallflow.model.search.WallhavenSearch
import com.ammar.wallflow.navigation.AppNavGraphs
import com.ammar.wallflow.ui.common.LocalSystemController
import com.ammar.wallflow.ui.common.TopBar
import com.ammar.wallflow.ui.common.bottombar.LocalBottomBarController
import com.ammar.wallflow.ui.screens.settings.autowallpapersources.composables.FavoritesSection
import com.ammar.wallflow.ui.screens.settings.autowallpapersources.composables.LightDarkSection
import com.ammar.wallflow.ui.screens.settings.autowallpapersources.composables.LocalSection
import com.ammar.wallflow.ui.screens.settings.autowallpapersources.composables.SavedSearchesSection
import com.ammar.wallflow.ui.screens.settings.composables.wallpaperTargetString
import com.ammar.wallflow.ui.theme.WallFlowTheme
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Destination<AppNavGraphs.SettingsNavGraph>
@Destination<AppNavGraphs.SettingsForMoreDetailNavGraph>
@Composable
fun ManageAutoWallpaperSourcesScreen(
    navController: NavController,
    viewModel: ManageAutoWallpaperSourcesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bottomBarController = LocalBottomBarController.current
    val systemController = LocalSystemController.current
    val systemState by systemController.state

    LaunchedEffect(systemState.isExpanded) {
        bottomBarController.update { it.copy(visible = systemState.isExpanded) }
    }

    Scaffold(
        topBar = {
            TopBar(
                navController = navController,
                title = {
                    Text(
                        text = stringResource(R.string.sources),
                        maxLines = 1,
                    )
                },
                showBackButton = true,
            )
        },
    ) {
        ManageAutoWallpaperSourcesScreenContent(
            modifier = Modifier.padding(it),
            useSameSources = uiState.useSameSources,
            hasLightDarkWallpapers = uiState.hasLightDarkWallpapers,
            hasFavorites = uiState.hasFavorites,
            savedSearches = uiState.savedSearches,
            localDirectories = uiState.localDirectories,
            homeScreenSources = uiState.homeScreenSources,
            lockScreenSources = uiState.lockScreenSources,
            onChangeUseSameSources = viewModel::updateUseSameSources,
            onChangeLightDarkEnabled = viewModel::updateLightDarkEnabled,
            onChangeUseDarkWithExtraDim = viewModel::updateUseDarkWithExtraDim,
            onChangeSavedSearchEnabled = viewModel::updateSavedSearchEnabled,
            onChangeSavedSearchIds = viewModel::updateSavedSearchIds,
            onChangeFavoritesEnabled = viewModel::updateFavoritesEnabled,
            onChangeLocalEnabled = viewModel::updateLocalEnabled,
            onChangeSelectedLocalDirs = viewModel::updateSelectedLocalDirs,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageAutoWallpaperSourcesScreenContent(
    modifier: Modifier = Modifier,
    useSameSources: Boolean = true,
    hasLightDarkWallpapers: Boolean = false,
    hasFavorites: Boolean = false,
    savedSearches: ImmutableList<SavedSearch> = persistentListOf(),
    localDirectories: ImmutableList<LocalDirectory> = persistentListOf(),
    homeScreenSources: AutoWallpaperSources = AutoWallpaperSources(),
    lockScreenSources: AutoWallpaperSources = AutoWallpaperSources(),
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
        ListItem(
            modifier = Modifier.clickable { onChangeUseSameSources(!useSameSources) },
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
            onChangeLightDarkEnabled = { onChangeLightDarkEnabled(it, activeTarget) },
            onChangeUseDarkWithExtraDim = { onChangeUseDarkWithExtraDim(it, activeTarget) },
        )
        SavedSearchesSection(
            savedSearches = savedSearches,
            savedSearchEnabled = activeSources.savedSearchEnabled,
            savedSearchIds = activeSources.savedSearchIds,
            lightDarkEnabled = activeSources.lightDarkEnabled,
            onChangeSavedSearchEnabled = { onChangeSavedSearchEnabled(it, activeTarget) },
            onChangeSavedSearchIds = { onChangeSavedSearchIds(it, activeTarget) },
        )
        FavoritesSection(
            favoritesEnabled = activeSources.favoritesEnabled,
            hasFavorites = hasFavorites,
            lightDarkEnabled = activeSources.lightDarkEnabled,
            onChangeFavoritesEnabled = { onChangeFavoritesEnabled(it, activeTarget) },
        )
        LocalSection(
            localDirectories = localDirectories,
            localEnabled = activeSources.localEnabled,
            selectedUris = activeSources.localDirs,
            lightDarkEnabled = activeSources.lightDarkEnabled,
            onChangeLocalEnabled = { onChangeLocalEnabled(it, activeTarget) },
            onChangeSelectedUris = { onChangeSelectedLocalDirs(it, activeTarget) },
        )
    }
}

private data class ManageAutoWallpaperSourcesParameters(
    val savedSearches: List<SavedSearch> = emptyList(),
    val localDirectories: List<LocalDirectory> = emptyList(),
    val useSameSources: Boolean = true,
    val hasLightDarkWallpapers: Boolean = false,
    val homeScreenSources: AutoWallpaperSources = AutoWallpaperSources(),
    val lockScreenSources: AutoWallpaperSources = AutoWallpaperSources(),
)

private class ManageAutoWallpaperSourcesParametersPP :
    CollectionPreviewParameterProvider<ManageAutoWallpaperSourcesParameters>(
        listOf(
            ManageAutoWallpaperSourcesParameters(),
            ManageAutoWallpaperSourcesParameters(
                useSameSources = false,
                hasLightDarkWallpapers = true,
                homeScreenSources = AutoWallpaperSources(
                    lightDarkEnabled = true,
                    useDarkWithExtraDim = true,
                ),
            ),
            ManageAutoWallpaperSourcesParameters(
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
            ManageAutoWallpaperSourcesParameters(
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
        ManageAutoWallpaperSourcesParametersPP::class,
    ) parameters: ManageAutoWallpaperSourcesParameters,
) {
    WallFlowTheme {
        Surface {
            Column {
                ManageAutoWallpaperSourcesScreenContent(
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
