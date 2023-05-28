package com.ammar.havenwalls.ui.settings

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ammar.havenwalls.R
import com.ammar.havenwalls.data.preferences.AppPreferences
import com.ammar.havenwalls.data.preferences.ObjectDetectionPreferences
import com.ammar.havenwalls.model.ObjectDetectionModel
import com.ammar.havenwalls.model.SavedSearchSaver
import com.ammar.havenwalls.ui.common.TopBar
import com.ammar.havenwalls.ui.common.bottombar.LocalBottomBarController
import com.ammar.havenwalls.ui.common.mainsearch.LocalMainSearchBarController
import com.ammar.havenwalls.ui.common.mainsearch.MainSearchBarState
import com.ammar.havenwalls.ui.common.navigation.TwoPaneNavigation
import com.ammar.havenwalls.ui.common.navigation.TwoPaneNavigation.Mode
import com.ammar.havenwalls.ui.common.wallpaperfilters.EditSearchModalBottomSheet
import com.ammar.havenwalls.ui.common.wallpaperfilters.SavedSearchesDialog
import com.ammar.havenwalls.ui.destinations.WallhavenApiKeyDialogDestination
import com.ammar.havenwalls.ui.theme.HavenWallsTheme
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@Composable
fun SettingsScreen(
    twoPaneController: TwoPaneNavigation.Controller,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchBarController = LocalMainSearchBarController.current
    val bottomBarController = LocalBottomBarController.current

    LaunchedEffect(Unit) {
        twoPaneController.setPaneMode(Mode.SINGLE_PANE) // hide pane 2
        searchBarController.update { MainSearchBarState(visible = false) }
        bottomBarController.update { it.copy(visible = false) }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopBar(
            navController = twoPaneController.pane1NavHostController,
            title = {
                Text(
                    text = stringResource(R.string.settings),
                    maxLines = 1,
                )
            },
            showBackButton = true,
        )
        SettingsScreenContent(
            appPreferences = uiState.appPreferences,
            model = uiState.selectedModel,
            onBlurSketchyCheckChange = viewModel::setBlurSketchy,
            onBlurNsfwCheckChange = viewModel::setBlurNsfw,
            onWallhavenApiKeyItemClick = {
                twoPaneController.navigate(WallhavenApiKeyDialogDestination)
            },
            onObjectDetectionPrefsChange = viewModel::updateSubjectDetectionPrefs,
            onObjectDetectionDelegateClick = { viewModel.showObjectDetectionDelegateOptions(true) },
            onObjectDetectionModelClick = { viewModel.showObjectDetectionModelOptions(true) },
            onManageSavedSearchesClick = { viewModel.showSavedSearches(true) }
        )
    }

    if (uiState.showObjectDetectionModelOptions) {
        ObjectDetectionModelOptionsDialog(
            models = uiState.objectDetectionModels,
            selectedModelId = uiState.appPreferences.objectDetectionPreferences.modelId,
            onOptionEditClick = {
                viewModel.showEditModelDialog(model = it, show = true)
            },
            onAddClick = { viewModel.showEditModelDialog() },
            onSaveClick = viewModel::setSelectedModel,
            onDismissRequest = { viewModel.showObjectDetectionModelOptions(false) },
        )
    }

    if (uiState.showEditModelDialog) {
        ObjectDetectionModelEditDialog(
            model = uiState.editModel,
            downloadStatus = uiState.modelDownloadStatus,
            checkNameExists = viewModel::checkModelNameExists,
            onSaveClick = viewModel::saveModel,
            onDeleteClick = {
                uiState.editModel?.run { viewModel.deleteModel(this) }
                viewModel.showEditModelDialog(model = null, show = false)
            },
            onDismissRequest = {
                viewModel.showEditModelDialog(
                    model = null,
                    show = false,
                )
            }
        )
    }

    uiState.deleteModel?.run {
        ObjectDetectionModelDeleteConfirmDialog(
            model = this,
            onConfirmClick = { viewModel.deleteModel(uiState.deleteModel, true) },
            onDismissRequest = { viewModel.deleteModel(null) },
        )
    }

    if (uiState.showObjectDetectionDelegateOptions) {
        ObjectDetectionDelegateOptionsDialog(
            selectedDelegate = uiState.appPreferences.objectDetectionPreferences.delegate,
            onSaveClick = {
                viewModel.updateSubjectDetectionPrefs(
                    uiState.appPreferences.objectDetectionPreferences.copy(delegate = it)
                )
                viewModel.showObjectDetectionDelegateOptions(false)
            },
            onDismissRequest = { viewModel.showObjectDetectionDelegateOptions(false) }
        )
    }

    if (uiState.showSavedSearches) {
        SavedSearchesDialog(
            savedSearches = uiState.savedSearches,
            title = stringResource(R.string.saved_searches),
            showActions = true,
            selectable = false,
            onEditClick = { viewModel.editSavedSearch(it) },
            onDeleteClick = { viewModel.deleteSavedSearch(it) },
            onDismissRequest = { viewModel.showSavedSearches(false) }
        )
    }

    uiState.editSavedSearch?.run {
        val state = rememberModalBottomSheetState()
        val scope = rememberCoroutineScope()
        var localSavedSearch by rememberSaveable(
            this,
            stateSaver = SavedSearchSaver,
        ) { mutableStateOf(this) }

        EditSearchModalBottomSheet(
            state = state,
            search = localSavedSearch.search,
            header = {
                EditSavedSearchBottomSheetHeader(
                    name = localSavedSearch.name,
                    saveEnabled = localSavedSearch != this@run,
                    onNameChange = { localSavedSearch = localSavedSearch.copy(name = it) },
                    onSaveClick = {
                        viewModel.updateSavedSearch(localSavedSearch)
                        scope.launch { state.hide() }.invokeOnCompletion {
                            if (!state.isVisible) {
                                viewModel.editSavedSearch(null)
                            }
                        }
                    },
                )
            },
            onChange = { localSavedSearch = localSavedSearch.copy(search = it) },
            onDismissRequest = { viewModel.editSavedSearch(null) },
        )
    }

    uiState.deleteSavedSearch?.run {
        DeleteSavedSearchConfirmDialog(
            savedSearch = this,
            onConfirmClick = { viewModel.deleteSavedSearch(this, true) },
            onDismissRequest = { viewModel.deleteSavedSearch(null) }
        )
    }
}

@Composable
fun SettingsScreenContent(
    modifier: Modifier = Modifier,
    appPreferences: AppPreferences = AppPreferences(),
    model: ObjectDetectionModel = ObjectDetectionModel.DEFAULT,
    onBlurSketchyCheckChange: (checked: Boolean) -> Unit = {},
    onBlurNsfwCheckChange: (checked: Boolean) -> Unit = {},
    onWallhavenApiKeyItemClick: () -> Unit = {},
    onObjectDetectionPrefsChange: (objectDetectionPrefs: ObjectDetectionPreferences) -> Unit = {},
    onObjectDetectionDelegateClick: () -> Unit = {},
    onObjectDetectionModelClick: () -> Unit = {},
    onManageSavedSearchesClick: () -> Unit = {},
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth()
    ) {
        accountSection(onWallhavenApiKeyItemClick = onWallhavenApiKeyItemClick)
        dividerItem()
        generalSection(
            blurSketchy = appPreferences.blurSketchy,
            blurNsfw = appPreferences.blurNsfw,
            onBlurSketchyCheckChange = onBlurSketchyCheckChange,
            onBlurNsfwCheckChange = onBlurNsfwCheckChange,
            onManageSavedSearchesClick = onManageSavedSearchesClick,
        )
        dividerItem()
        objectDetectionSection(
            enabled = appPreferences.objectDetectionPreferences.enabled,
            delegate = appPreferences.objectDetectionPreferences.delegate,
            model = model,
            onEnabledChange = {
                onObjectDetectionPrefsChange(
                    appPreferences.objectDetectionPreferences.copy(enabled = it)
                )
            },
            onDelegateClick = onObjectDetectionDelegateClick,
            onModelClick = onObjectDetectionModelClick,
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewSettingsScreenContent() {
    HavenWallsTheme {
        Surface {
            SettingsScreenContent()
        }
    }
}
