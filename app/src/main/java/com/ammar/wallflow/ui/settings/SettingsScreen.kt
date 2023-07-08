package com.ammar.wallflow.ui.settings

import android.Manifest
import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ammar.wallflow.R
import com.ammar.wallflow.data.preferences.AppPreferences
import com.ammar.wallflow.data.preferences.AutoWallpaperPreferences
import com.ammar.wallflow.data.preferences.ObjectDetectionPreferences
import com.ammar.wallflow.model.ObjectDetectionModel
import com.ammar.wallflow.model.SavedSearch
import com.ammar.wallflow.model.SavedSearchSaver
import com.ammar.wallflow.ui.common.TopBar
import com.ammar.wallflow.ui.common.bottomWindowInsets
import com.ammar.wallflow.ui.common.bottombar.LocalBottomBarController
import com.ammar.wallflow.ui.common.mainsearch.LocalMainSearchBarController
import com.ammar.wallflow.ui.common.mainsearch.MainSearchBarState
import com.ammar.wallflow.ui.common.navigation.TwoPaneNavigation
import com.ammar.wallflow.ui.common.navigation.TwoPaneNavigation.Mode
import com.ammar.wallflow.ui.common.permissions.DownloadPermissionsRationalDialog
import com.ammar.wallflow.ui.common.permissions.MultiplePermissionItem
import com.ammar.wallflow.ui.common.permissions.checkSetWallpaperPermission
import com.ammar.wallflow.ui.common.permissions.isGranted
import com.ammar.wallflow.ui.common.permissions.rememberMultiplePermissionsState
import com.ammar.wallflow.ui.common.permissions.shouldShowRationale
import com.ammar.wallflow.ui.common.searchedit.EditSearchModalBottomSheet
import com.ammar.wallflow.ui.common.searchedit.SavedSearchesDialog
import com.ammar.wallflow.ui.destinations.LayoutSettingsScreenDestination
import com.ammar.wallflow.ui.destinations.WallhavenApiKeyDialogDestination
import com.ammar.wallflow.ui.settings.composables.ConstraintOptionsDialog
import com.ammar.wallflow.ui.settings.composables.DeleteSavedSearchConfirmDialog
import com.ammar.wallflow.ui.settings.composables.EditSavedSearchBottomSheetHeader
import com.ammar.wallflow.ui.settings.composables.FrequencyDialog
import com.ammar.wallflow.ui.settings.composables.NextRunInfoDialog
import com.ammar.wallflow.ui.settings.composables.ObjectDetectionDelegateOptionsDialog
import com.ammar.wallflow.ui.settings.composables.ObjectDetectionModelDeleteConfirmDialog
import com.ammar.wallflow.ui.settings.composables.ObjectDetectionModelEditDialog
import com.ammar.wallflow.ui.settings.composables.ObjectDetectionModelOptionsDialog
import com.ammar.wallflow.ui.settings.composables.SavedSearchOptionsDialog
import com.ammar.wallflow.ui.settings.composables.ThemeOptionsDialog
import com.ammar.wallflow.ui.settings.composables.accountSection
import com.ammar.wallflow.ui.settings.composables.autoWallpaperSection
import com.ammar.wallflow.ui.settings.composables.dividerItem
import com.ammar.wallflow.ui.settings.composables.generalSection
import com.ammar.wallflow.ui.settings.composables.lookAndFeelSection
import com.ammar.wallflow.ui.settings.composables.objectDetectionSection
import com.ammar.wallflow.ui.theme.WallFlowTheme
import com.ammar.wallflow.workers.AutoWallpaperWorker
import com.google.modernstorage.permissions.StoragePermissions
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.delay
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
    val context = LocalContext.current

    val storagePerms = remember {
        StoragePermissions.getPermissions(
            action = StoragePermissions.Action.READ_AND_WRITE,
            types = listOf(StoragePermissions.FileType.Image),
            createdBy = StoragePermissions.CreatedBy.Self
        ).map { MultiplePermissionItem(permission = it) }
    }

    @SuppressLint("InlinedApi")
    val autoWallpaperPermissionsState = rememberMultiplePermissionsState(
        permissions = storagePerms + MultiplePermissionItem(
            permission = Manifest.permission.POST_NOTIFICATIONS,
            minimumSdk = Build.VERSION_CODES.TIRAMISU,
        )
    ) { permissionStates ->
        val showRationale = permissionStates.map { it.status.shouldShowRationale }.any { it }
        if (showRationale) {
            viewModel.showPermissionRationaleDialog(true)
            return@rememberMultiplePermissionsState
        }
        // check if storage permissions are granted (notification permission is optional)
        val storagePermStrings = storagePerms.map { it.permission }
        val allGranted = permissionStates
            .filter { it.permission in storagePermStrings }
            .all { it.status.isGranted }
        val updatedAutoWallpaperPreferences = if (!allGranted) {
            // disable auto wallpaper
            uiState.tempAutoWallpaperPreferences?.copy(enabled = false)
        } else {
            uiState.tempAutoWallpaperPreferences
        } ?: AutoWallpaperPreferences()
        viewModel.setTempAutoWallpaperPrefs(null)
        viewModel.updateAutoWallpaperPrefs(updatedAutoWallpaperPreferences)
    }

    LaunchedEffect(Unit) {
        twoPaneController.setPaneMode(Mode.SINGLE_PANE) // hide pane 2
        searchBarController.update { MainSearchBarState(visible = false) }
        bottomBarController.update { it.copy(visible = false) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(bottomWindowInsets)
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
            hasSetWallpaperPermission = context.checkSetWallpaperPermission(),
            autoWallpaperNextRun = uiState.autoWallpaperNextRun,
            autoWallpaperSavedSearch = uiState.autoWallpaperSavedSearch,
            autoWallpaperStatus = uiState.autoWallpaperStatus,
            onBlurSketchyCheckChange = viewModel::setBlurSketchy,
            onBlurNsfwCheckChange = viewModel::setBlurNsfw,
            onWallhavenApiKeyItemClick = {
                twoPaneController.navigate(WallhavenApiKeyDialogDestination)
            },
            onObjectDetectionPrefsChange = viewModel::updateSubjectDetectionPrefs,
            onObjectDetectionDelegateClick = { viewModel.showObjectDetectionDelegateOptions(true) },
            onObjectDetectionModelClick = { viewModel.showObjectDetectionModelOptions(true) },
            onManageSavedSearchesClick = { viewModel.showSavedSearches(true) },
            onAutoWallpaperPresChange = {
                if (it.enabled) {
                    viewModel.setTempAutoWallpaperPrefs(it)
                    // need to check if we have all permissions before enabling auto wallpaper
                    autoWallpaperPermissionsState.launchMultiplePermissionRequest()
                    return@SettingsScreenContent
                }
                viewModel.updateAutoWallpaperPrefs(it)
            },
            onAutoWallpaperSavedSearchClick = { viewModel.showAutoWallpaperSavedSearchesDialog(true) },
            onAutoWallpaperFrequencyClick = { viewModel.showAutoWallpaperFrequencyDialog(true) },
            onAutoWallpaperConstraintsClick = { viewModel.showAutoWallpaperConstraintsDialog(true) },
            onAutoWallpaperChangeNowClick = viewModel::autoWallpaperChangeNow,
            onAutoWallpaperNextRunInfoClick = { viewModel.showAutoWallpaperNextRunInfoDialog(true) },
            onThemeClick = { viewModel.showThemeOptionsDialog(true) },
            onLayoutClick = { twoPaneController.navigate(LayoutSettingsScreenDestination) }
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

    if (uiState.showAutoWallpaperSavedSearchesDialog) {
        SavedSearchOptionsDialog(
            savedSearches = uiState.savedSearches,
            selectedSavedSearchId = uiState.appPreferences.autoWallpaperPreferences.savedSearchId,
            onSaveClick = {
                val prefs = uiState.tempAutoWallpaperPreferences
                    ?: uiState.appPreferences.autoWallpaperPreferences
                viewModel.updateAutoWallpaperPrefs(
                    prefs.copy(
                        enabled = true,
                        savedSearchId = it,
                    )
                )
                viewModel.setTempAutoWallpaperPrefs(null)
                viewModel.showAutoWallpaperSavedSearchesDialog(false)
            },
            onDismissRequest = { viewModel.showAutoWallpaperSavedSearchesDialog(false) }
        )
    }

    if (uiState.showAutoWallpaperFrequencyDialog) {
        FrequencyDialog(
            frequency = uiState.appPreferences.autoWallpaperPreferences.frequency,
            onSaveClick = {
                viewModel.updateAutoWallpaperPrefs(
                    uiState.appPreferences.autoWallpaperPreferences.copy(
                        frequency = it,
                    )
                )
                viewModel.showAutoWallpaperFrequencyDialog(false)
            },
            onDismissRequest = { viewModel.showAutoWallpaperFrequencyDialog(false) },
        )
    }

    if (uiState.showAutoWallpaperConstraintsDialog) {
        ConstraintOptionsDialog(
            constraints = uiState.appPreferences.autoWallpaperPreferences.constraints,
            onSaveClick = {
                viewModel.updateAutoWallpaperPrefs(
                    uiState.appPreferences.autoWallpaperPreferences.copy(
                        constraints = it,
                    )
                )
                viewModel.showAutoWallpaperConstraintsDialog(false)
            },
            onDismissRequest = { viewModel.showAutoWallpaperConstraintsDialog(false) },
        )
    }

    if (uiState.showPermissionRationaleDialog) {
        DownloadPermissionsRationalDialog(
            permissions = autoWallpaperPermissionsState.shouldShowRationale.keys.map { it.permission },
            onConfirmOrDismiss = { viewModel.showPermissionRationaleDialog(false) }
        )
    }

    if (uiState.showAutoWallpaperNextRunInfoDialog) {
        NextRunInfoDialog(
            onDismissRequest = { viewModel.showAutoWallpaperNextRunInfoDialog(false) }
        )
    }

    if (uiState.showThemeOptionsDialog) {
        ThemeOptionsDialog(
            theme = uiState.appPreferences.lookAndFeelPreferences.theme,
            onSaveClick = {
                viewModel.updateLookAndFeelPrefs(
                    uiState.appPreferences.lookAndFeelPreferences.copy(
                        theme = it,
                    )
                )
                viewModel.showThemeOptionsDialog(false)
            },
            onDismissRequest = { viewModel.showThemeOptionsDialog(false) },
        )
    }
}

@Composable
fun SettingsScreenContent(
    modifier: Modifier = Modifier,
    appPreferences: AppPreferences = AppPreferences(),
    model: ObjectDetectionModel = ObjectDetectionModel.DEFAULT,
    autoWallpaperSavedSearch: SavedSearch? = null,
    hasSetWallpaperPermission: Boolean = true,
    autoWallpaperNextRun: NextRun = NextRun.NotScheduled,
    autoWallpaperStatus: AutoWallpaperWorker.Companion.Status? = null,
    onBlurSketchyCheckChange: (checked: Boolean) -> Unit = {},
    onBlurNsfwCheckChange: (checked: Boolean) -> Unit = {},
    onWallhavenApiKeyItemClick: () -> Unit = {},
    onObjectDetectionPrefsChange: (objectDetectionPrefs: ObjectDetectionPreferences) -> Unit = {},
    onObjectDetectionDelegateClick: () -> Unit = {},
    onObjectDetectionModelClick: () -> Unit = {},
    onManageSavedSearchesClick: () -> Unit = {},
    onAutoWallpaperPresChange: (AutoWallpaperPreferences) -> Unit = {},
    onAutoWallpaperSavedSearchClick: () -> Unit = {},
    onAutoWallpaperFrequencyClick: () -> Unit = {},
    onAutoWallpaperConstraintsClick: () -> Unit = {},
    onAutoWallpaperChangeNowClick: () -> Unit = {},
    onAutoWallpaperNextRunInfoClick: () -> Unit = {},
    onThemeClick: () -> Unit = {},
    onLayoutClick: () -> Unit = {},
) {
    Box(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth()
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
            lookAndFeelSection(
                onThemeClick = onThemeClick,
                onLayoutClick = onLayoutClick,
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
            dividerItem()
            if (hasSetWallpaperPermission) {
                autoWallpaperSection(
                    enabled = appPreferences.autoWallpaperPreferences.enabled,
                    savedSearchName = autoWallpaperSavedSearch?.name,
                    useObjectDetection = appPreferences.autoWallpaperPreferences.useObjectDetection,
                    nextRun = autoWallpaperNextRun,
                    frequency = appPreferences.autoWallpaperPreferences.frequency,
                    showNotification = appPreferences.autoWallpaperPreferences.showNotification,
                    autoWallpaperStatus = autoWallpaperStatus,
                    onEnabledChange = {
                        onAutoWallpaperPresChange(
                            appPreferences.autoWallpaperPreferences.copy(
                                enabled = it,
                            )
                        )
                    },
                    onSavedSearchClick = onAutoWallpaperSavedSearchClick,
                    onFrequencyClick = onAutoWallpaperFrequencyClick,
                    onUseObjectDetectionChange = {
                        onAutoWallpaperPresChange(
                            appPreferences.autoWallpaperPreferences.copy(
                                useObjectDetection = it,
                            )
                        )
                    },
                    onConstraintsClick = onAutoWallpaperConstraintsClick,
                    onChangeNowClick = onAutoWallpaperChangeNowClick,
                    onNextRunInfoClick = onAutoWallpaperNextRunInfoClick,
                    onShowNotificationChange = {
                        onAutoWallpaperPresChange(
                            appPreferences.autoWallpaperPreferences.copy(
                                showNotification = it,
                            )
                        )
                    },
                )
            }
        }

        AnimatedVisibility(
            modifier = Modifier.align(Alignment.BottomCenter),
            visible = autoWallpaperStatus?.isSuccessOrFail() == true,
            enter = slideInVertically(
                initialOffsetY = { x -> x },
            ),
            exit = slideOutVertically(
                targetOffsetY = { x -> x },
            ),
        ) {
            Snackbar(
                modifier = Modifier.padding(16.dp),
            ) {
                Text(text = stringResource(R.string.wallpaper_changed))
            }
        }
    }

}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewSettingsScreenContent() {
    WallFlowTheme {
        Surface {
            val coroutineScope = rememberCoroutineScope()
            var autoWallpaperStatus: AutoWallpaperWorker.Companion.Status? by remember {
                mutableStateOf(null)
            }
            SettingsScreenContent(
                appPreferences = AppPreferences(
                    autoWallpaperPreferences = AutoWallpaperPreferences(
                        enabled = true,
                    ),
                ),
                autoWallpaperStatus = autoWallpaperStatus,
                onAutoWallpaperChangeNowClick = {
                    autoWallpaperStatus = AutoWallpaperWorker.Companion.Status.Success
                    coroutineScope.launch {
                        delay(5000)
                        autoWallpaperStatus = null
                    }
                }
            )
        }
    }
}
