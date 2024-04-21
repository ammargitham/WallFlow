package com.ammar.wallflow.ui.screens.settings

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ammar.wallflow.R
import com.ammar.wallflow.data.preferences.AppPreferences
import com.ammar.wallflow.data.preferences.AutoWallpaperPreferences
import com.ammar.wallflow.data.preferences.ObjectDetectionPreferences
import com.ammar.wallflow.destinations.MainWallhavenApiKeyDialogDestination
import com.ammar.wallflow.destinations.MoreDetailWallhavenApiKeyDialogDestination
import com.ammar.wallflow.destinations.SettingsForMoreDetailLayoutSettingsScreenDestination
import com.ammar.wallflow.destinations.SettingsForMoreDetailManageAutoWallpaperSourcesScreenDestination
import com.ammar.wallflow.destinations.SettingsLayoutSettingsScreenDestination
import com.ammar.wallflow.destinations.SettingsManageAutoWallpaperSourcesScreenDestination
import com.ammar.wallflow.extensions.safeLaunch
import com.ammar.wallflow.extensions.trimAll
import com.ammar.wallflow.model.ObjectDetectionModel
import com.ammar.wallflow.model.search.SavedSearch
import com.ammar.wallflow.model.search.SavedSearchSaver
import com.ammar.wallflow.ui.common.LocalSystemController
import com.ammar.wallflow.ui.common.TopBar
import com.ammar.wallflow.ui.common.bottomWindowInsets
import com.ammar.wallflow.ui.common.bottombar.LocalBottomBarController
import com.ammar.wallflow.ui.common.mainsearch.LocalMainSearchBarController
import com.ammar.wallflow.ui.common.permissions.DownloadPermissionsRationalDialog
import com.ammar.wallflow.ui.common.permissions.MultiplePermissionItem
import com.ammar.wallflow.ui.common.permissions.checkSetWallpaperPermission
import com.ammar.wallflow.ui.common.permissions.isGranted
import com.ammar.wallflow.ui.common.permissions.rememberMultiplePermissionsState
import com.ammar.wallflow.ui.common.permissions.shouldShowRationale
import com.ammar.wallflow.ui.common.searchedit.EditSearchModalBottomSheet
import com.ammar.wallflow.ui.common.searchedit.SavedSearchesDialog
import com.ammar.wallflow.ui.navigation.AppNavGraphs
import com.ammar.wallflow.ui.screens.settings.composables.AutoWallpaperSetToDialog
import com.ammar.wallflow.ui.screens.settings.composables.ChangeDownloadLocationDialog
import com.ammar.wallflow.ui.screens.settings.composables.ClearViewedWallpapersConfirmDialog
import com.ammar.wallflow.ui.screens.settings.composables.ConstraintOptionsDialog
import com.ammar.wallflow.ui.screens.settings.composables.DeleteSavedSearchConfirmDialog
import com.ammar.wallflow.ui.screens.settings.composables.EditSavedSearchBottomSheetHeader
import com.ammar.wallflow.ui.screens.settings.composables.ExifWriteTypeOptionsDialog
import com.ammar.wallflow.ui.screens.settings.composables.FrequencyDialog
import com.ammar.wallflow.ui.screens.settings.composables.NextRunInfoDialog
import com.ammar.wallflow.ui.screens.settings.composables.ObjectDetectionDelegateOptionsDialog
import com.ammar.wallflow.ui.screens.settings.composables.ObjectDetectionModelDeleteConfirmDialog
import com.ammar.wallflow.ui.screens.settings.composables.ObjectDetectionModelEditDialog
import com.ammar.wallflow.ui.screens.settings.composables.ObjectDetectionModelOptionsDialog
import com.ammar.wallflow.ui.screens.settings.composables.ThemeOptionsDialog
import com.ammar.wallflow.ui.screens.settings.composables.ViewedWallpapersLookOptionsDialog
import com.ammar.wallflow.ui.screens.settings.composables.accountSection
import com.ammar.wallflow.ui.screens.settings.composables.autoWallpaperSection
import com.ammar.wallflow.ui.screens.settings.composables.dividerItem
import com.ammar.wallflow.ui.screens.settings.composables.generalSection
import com.ammar.wallflow.ui.screens.settings.composables.lookAndFeelSection
import com.ammar.wallflow.ui.screens.settings.composables.objectDetectionSection
import com.ammar.wallflow.ui.screens.settings.composables.viewedWallpapersSection
import com.ammar.wallflow.ui.theme.WallFlowTheme
import com.ammar.wallflow.utils.StoragePermissions
import com.ammar.wallflow.utils.getPublicDownloadsDir
import com.ammar.wallflow.utils.getRealPath
import com.ammar.wallflow.utils.objectdetection.objectsDetector
import com.ammar.wallflow.workers.AutoWallpaperWorker.Companion.AutoWallpaperException
import com.ammar.wallflow.workers.AutoWallpaperWorker.Companion.Status
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.navigate
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Destination<AppNavGraphs.SettingsNavGraph>(
    start = true,
)
@Destination<AppNavGraphs.SettingsForMoreDetailNavGraph>(
    start = true,
)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchBarController = LocalMainSearchBarController.current
    val bottomBarController = LocalBottomBarController.current
    val context = LocalContext.current
    val systemController = LocalSystemController.current
    val systemState by systemController.state

    val storagePerms = remember {
        StoragePermissions.getPermissions(
            action = StoragePermissions.Action.READ_AND_WRITE,
            types = listOf(StoragePermissions.FileType.Image),
            createdBy = StoragePermissions.CreatedBy.Self,
        ).map { MultiplePermissionItem(permission = it) }
    }

    @SuppressLint("InlinedApi")
    val autoWallpaperPermissionsState = rememberMultiplePermissionsState(
        permissions = storagePerms + MultiplePermissionItem(
            permission = Manifest.permission.POST_NOTIFICATIONS,
            minimumSdk = Build.VERSION_CODES.TIRAMISU,
        ),
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
        if (!updatedAutoWallpaperPreferences.anySourceEnabled) {
            navController.navigate(
                if (systemState.isExpanded) {
                    SettingsForMoreDetailManageAutoWallpaperSourcesScreenDestination
                } else {
                    SettingsManageAutoWallpaperSourcesScreenDestination
                },
            )
            return@rememberMultiplePermissionsState
        }
        viewModel.updateAutoWallpaperPrefs(updatedAutoWallpaperPreferences)
    }

    val chooseDownloadLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) {
        if (it == null) {
            return@rememberLauncherForActivityResult
        }
        viewModel.updateDownloadLocation(it)
    }

    val prefsDownloadLocationUri = uiState.appPreferences.downloadLocation
    val downloadLocationString = if (prefsDownloadLocationUri == null) {
        getPublicDownloadsDir().absolutePath
    } else {
        getRealPath(context, prefsDownloadLocationUri) ?: prefsDownloadLocationUri.toString()
    }

    LaunchedEffect(Unit) {
        searchBarController.update { it.copy(visible = false) }
    }

    LaunchedEffect(systemState.isExpanded) {
        bottomBarController.update { it.copy(visible = systemState.isExpanded) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(bottomWindowInsets),
    ) {
        if (!systemState.isExpanded) {
            TopBar(
                navController = navController,
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        maxLines = 1,
                    )
                },
                showBackButton = true,
            )
        }

        SettingsScreenContent(
            appPreferences = uiState.appPreferences,
            model = uiState.selectedModel,
            hasSetWallpaperPermission = context.checkSetWallpaperPermission(),
            autoWallpaperNextRun = uiState.autoWallpaperNextRun,
            lsAutoWallpaperNextRun = uiState.lsAutoWallpaperNextRun,
            autoWallpaperSavedSearches = uiState.autoWallpaperSavedSearches,
            autoWallpaperStatus = uiState.autoWallpaperStatus,
            showLocalTab = uiState.appPreferences.lookAndFeelPreferences.showLocalTab,
            downloadLocation = downloadLocationString,
            onBlurSketchyCheckChange = viewModel::setBlurSketchy,
            onBlurNsfwCheckChange = viewModel::setBlurNsfw,
            onWriteTagsToExifCheckChange = viewModel::updateWriteTagsToExif,
            onTagsWriteTypeClick = { viewModel.showTagsWriteTypeDialog(true) },
            onDownloadLocationClick = { viewModel.showChangeDownloadLocationDialog(true) },
            onWallhavenApiKeyItemClick = {
                navController.navigate(
                    if (systemState.isExpanded) {
                        MoreDetailWallhavenApiKeyDialogDestination
                    } else {
                        MainWallhavenApiKeyDialogDestination
                    },
                )
            },
            onObjectDetectionPrefsChange = viewModel::updateSubjectDetectionPrefs,
            onObjectDetectionDelegateClick = { viewModel.showObjectDetectionDelegateOptions(true) },
            onObjectDetectionModelClick = { viewModel.showObjectDetectionModelOptions(true) },
            onManageSavedSearchesClick = { viewModel.showSavedSearches(true) },
            onAutoWallpaperPrefsChange = {
                if (it.enabled) {
                    viewModel.setTempAutoWallpaperPrefs(it)
                    // need to check if we have all permissions before enabling auto wallpaper
                    autoWallpaperPermissionsState.launchMultiplePermissionRequest()
                    return@SettingsScreenContent
                }
                viewModel.updateAutoWallpaperPrefs(it)
            },
            onAutoWallpaperSourcesClick = {
                navController.navigate(
                    if (systemState.isExpanded) {
                        SettingsForMoreDetailManageAutoWallpaperSourcesScreenDestination
                    } else {
                        SettingsManageAutoWallpaperSourcesScreenDestination
                    },
                )
            },
            onAutoWallpaperFrequencyClick = { viewModel.showAutoWallpaperFrequencyDialog(true) },
            onAutoWallpaperConstraintsClick = {
                viewModel.showAutoWallpaperConstraintsDialog(true)
            },
            onAutoWallpaperChangeNowClick = viewModel::autoWallpaperChangeNow,
            onAutoWallpaperNextRunInfoClick = {
                viewModel.showAutoWallpaperNextRunInfoDialog(true)
            },
            onAutoWallpaperSetToClick = {
                viewModel.showAutoWallpaperSetToDialog(true)
            },
            onThemeClick = { viewModel.showThemeOptionsDialog(true) },
            onLayoutClick = {
                navController.navigate(
                    if (systemState.isExpanded) {
                        SettingsForMoreDetailLayoutSettingsScreenDestination
                    } else {
                        SettingsLayoutSettingsScreenDestination
                    },
                )
            },
            onShowLocalTabChange = {
                viewModel.updateLookAndFeelPrefs(
                    uiState.appPreferences.lookAndFeelPreferences.copy(
                        showLocalTab = it,
                    ),
                )
            },
            onViewedWallpapersEnabledChange = viewModel::updateRememberViewedWallpapers,
            onViewedWallpapersLookClick = { viewModel.showViewedWallpapersLookDialog(true) },
            onViewedWallpapersClearClick = {
                viewModel.showClearViewedWallpapersConfirmDialog(true)
            },
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
            checkFileNameExists = viewModel::checkModelFileNameExists,
            showDeleteAction = uiState.objectDetectionModels.size > 1,
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
            },
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
                    uiState.appPreferences.objectDetectionPreferences.copy(delegate = it),
                )
                viewModel.showObjectDetectionDelegateOptions(false)
            },
            onDismissRequest = { viewModel.showObjectDetectionDelegateOptions(false) },
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
            onDismissRequest = { viewModel.showSavedSearches(false) },
        )
    }

    uiState.editSavedSearch?.run {
        val state = rememberModalBottomSheetState()
        val scope = rememberCoroutineScope()
        var localSavedSearch by rememberSaveable(
            this,
            stateSaver = SavedSearchSaver,
        ) { mutableStateOf(this) }
        var nameHasError by rememberSaveable { mutableStateOf(false) }

        EditSearchModalBottomSheet(
            state = state,
            search = localSavedSearch.search,
            header = {
                EditSavedSearchBottomSheetHeader(
                    name = localSavedSearch.name,
                    saveEnabled = !nameHasError && localSavedSearch != this@run,
                    nameHasError = nameHasError,
                    onNameChange = {
                        localSavedSearch = localSavedSearch.copy(name = it)
                        if (it.isBlank()) {
                            nameHasError = true
                            return@EditSavedSearchBottomSheetHeader
                        }
                        scope.launch {
                            nameHasError = viewModel.checkSavedSearchNameExists(
                                name = localSavedSearch.name.trimAll(),
                                id = localSavedSearch.id,
                            )
                        }
                    },
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
            showNSFW = uiState.appPreferences.wallhavenApiKey.isNotBlank(),
            onChange = { localSavedSearch = localSavedSearch.copy(search = it) },
            onDismissRequest = { viewModel.editSavedSearch(null) },
        )
    }

    uiState.deleteSavedSearch?.run {
        DeleteSavedSearchConfirmDialog(
            savedSearch = this,
            onConfirmClick = { viewModel.deleteSavedSearch(this, true) },
            onDismissRequest = { viewModel.deleteSavedSearch(null) },
        )
    }

    if (uiState.showAutoWallpaperFrequencyDialog) {
        FrequencyDialog(
            useSameFreq = uiState.appPreferences.autoWallpaperPreferences.useSameFreq,
            frequency = uiState.appPreferences.autoWallpaperPreferences.frequency,
            lsFrequency = uiState.appPreferences.autoWallpaperPreferences.lsFrequency,
            onSaveClick = { useSameFreq, freq, lsFreq ->
                viewModel.updateAutoWallpaperPrefs(
                    uiState.appPreferences.autoWallpaperPreferences.copy(
                        useSameFreq = useSameFreq,
                        frequency = freq,
                        lsFrequency = lsFreq,
                    ),
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
                    ),
                )
                viewModel.showAutoWallpaperConstraintsDialog(false)
            },
            onDismissRequest = { viewModel.showAutoWallpaperConstraintsDialog(false) },
        )
    }

    if (uiState.showPermissionRationaleDialog) {
        DownloadPermissionsRationalDialog(
            permissions = autoWallpaperPermissionsState.shouldShowRationale.keys.map {
                it.permission
            },
            onConfirmOrDismiss = { viewModel.showPermissionRationaleDialog(false) },
        )
    }

    if (uiState.showAutoWallpaperNextRunInfoDialog) {
        NextRunInfoDialog(
            onDismissRequest = { viewModel.showAutoWallpaperNextRunInfoDialog(false) },
        )
    }

    if (uiState.showThemeOptionsDialog) {
        ThemeOptionsDialog(
            theme = uiState.appPreferences.lookAndFeelPreferences.theme,
            onSaveClick = {
                viewModel.updateLookAndFeelPrefs(
                    uiState.appPreferences.lookAndFeelPreferences.copy(
                        theme = it,
                    ),
                )
                viewModel.showThemeOptionsDialog(false)
            },
            onDismissRequest = { viewModel.showThemeOptionsDialog(false) },
        )
    }

    if (uiState.showAutoWallpaperSetToDialog) {
        AutoWallpaperSetToDialog(
            selectedTargets = uiState.appPreferences.autoWallpaperPreferences.targets,
            onSaveClick = { targets ->
                viewModel.updateAutoWallpaperPrefs(
                    uiState.appPreferences.autoWallpaperPreferences.copy(
                        targets = targets,
                    ),
                )
                viewModel.showAutoWallpaperSetToDialog(false)
            },
            onDismissRequest = { viewModel.showAutoWallpaperSetToDialog(false) },
        )
    }

    if (uiState.showTagsWriteTypeDialog) {
        ExifWriteTypeOptionsDialog(
            selectedExifWriteType = uiState.appPreferences.tagsExifWriteType,
            onSaveClick = {
                viewModel.updateTagsWriteType(it)
                viewModel.showTagsWriteTypeDialog(false)
            },
            onDismissRequest = { viewModel.showTagsWriteTypeDialog(false) },
        )
    }

    if (uiState.showViewedWallpapersLookDialog) {
        ViewedWallpapersLookOptionsDialog(
            selectedViewedWallpapersLook = uiState.appPreferences.viewedWallpapersPreferences.look,
            onSaveClick = {
                viewModel.updateViewedWallpapersLook(it)
                viewModel.showViewedWallpapersLookDialog(false)
            },
            onDismissRequest = { viewModel.showViewedWallpapersLookDialog(false) },
        )
    }

    if (uiState.showClearViewedWallpapersConfirmDialog) {
        ClearViewedWallpapersConfirmDialog(
            onConfirmClick = {
                viewModel.clearViewedWallpapers()
                viewModel.showClearViewedWallpapersConfirmDialog(false)
            },
            onDismissRequest = {
                viewModel.showClearViewedWallpapersConfirmDialog(false)
            },
        )
    }

    if (uiState.showChangeDownloadLocationDialog) {
        ChangeDownloadLocationDialog(
            defaultLocation = getPublicDownloadsDir().toUri(),
            customLocation = uiState.appPreferences.downloadLocation,
            onDefaultClick = {
                viewModel.removeDownloadLocation()
            },
            onCustomClick = {
                chooseDownloadLocationLauncher.safeLaunch(context, null)
            },
            onCustomEditClick = {
                chooseDownloadLocationLauncher.safeLaunch(
                    context = context,
                    input = uiState.appPreferences.downloadLocation,
                )
            },
            onDismissRequest = {
                viewModel.showChangeDownloadLocationDialog(false)
            },
        )
    }
}

@Composable
fun SettingsScreenContent(
    modifier: Modifier = Modifier,
    appPreferences: AppPreferences = AppPreferences(),
    model: ObjectDetectionModel = ObjectDetectionModel.DEFAULT,
    autoWallpaperSavedSearches: ImmutableList<SavedSearch> = persistentListOf(),
    hasSetWallpaperPermission: Boolean = true,
    autoWallpaperNextRun: NextRun = NextRun.NotScheduled,
    lsAutoWallpaperNextRun: NextRun = NextRun.NotScheduled,
    autoWallpaperStatus: Status? = null,
    showLocalTab: Boolean = true,
    downloadLocation: String = "",
    onBlurSketchyCheckChange: (checked: Boolean) -> Unit = {},
    onBlurNsfwCheckChange: (checked: Boolean) -> Unit = {},
    onWriteTagsToExifCheckChange: (checked: Boolean) -> Unit = {},
    onTagsWriteTypeClick: () -> Unit = {},
    onDownloadLocationClick: () -> Unit = {},
    onWallhavenApiKeyItemClick: () -> Unit = {},
    onObjectDetectionPrefsChange: (objectDetectionPrefs: ObjectDetectionPreferences) -> Unit = {},
    onObjectDetectionDelegateClick: () -> Unit = {},
    onObjectDetectionModelClick: () -> Unit = {},
    onManageSavedSearchesClick: () -> Unit = {},
    onAutoWallpaperPrefsChange: (AutoWallpaperPreferences) -> Unit = {},
    onAutoWallpaperSourcesClick: () -> Unit = {},
    onAutoWallpaperFrequencyClick: () -> Unit = {},
    onAutoWallpaperConstraintsClick: () -> Unit = {},
    onAutoWallpaperChangeNowClick: () -> Unit = {},
    onAutoWallpaperNextRunInfoClick: () -> Unit = {},
    onAutoWallpaperSetToClick: () -> Unit = {},
    onThemeClick: () -> Unit = {},
    onLayoutClick: () -> Unit = {},
    onShowLocalTabChange: (Boolean) -> Unit = {},
    onViewedWallpapersEnabledChange: (Boolean) -> Unit = {},
    onViewedWallpapersLookClick: () -> Unit = {},
    onViewedWallpapersClearClick: () -> Unit = {},
) {
    val context = LocalContext.current

    Box(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
        ) {
            accountSection(onWallhavenApiKeyItemClick = onWallhavenApiKeyItemClick)
            dividerItem()
            generalSection(
                blurSketchy = appPreferences.blurSketchy,
                blurNsfw = appPreferences.blurNsfw,
                writeTagsToExif = appPreferences.writeTagsToExif,
                tagsExifWriteType = appPreferences.tagsExifWriteType,
                downloadLocation = downloadLocation,
                onBlurSketchyCheckChange = onBlurSketchyCheckChange,
                onBlurNsfwCheckChange = onBlurNsfwCheckChange,
                onWriteTagsToExifCheckChange = onWriteTagsToExifCheckChange,
                onTagsWriteTypeClick = onTagsWriteTypeClick,
                onManageSavedSearchesClick = onManageSavedSearchesClick,
                onDownloadLocationClick = onDownloadLocationClick,
            )
            dividerItem()
            viewedWallpapersSection(
                enabled = appPreferences.viewedWallpapersPreferences.enabled,
                look = appPreferences.viewedWallpapersPreferences.look,
                onEnabledChange = onViewedWallpapersEnabledChange,
                onViewedWallpapersLookClick = onViewedWallpapersLookClick,
                onClearClick = onViewedWallpapersClearClick,
            )
            dividerItem()
            lookAndFeelSection(
                showLocalTab = showLocalTab,
                onThemeClick = onThemeClick,
                onLayoutClick = onLayoutClick,
                onShowLocalTabChange = onShowLocalTabChange,
            )
            if (objectsDetector.isEnabled) {
                dividerItem()
                objectDetectionSection(
                    enabled = appPreferences.objectDetectionPreferences.enabled,
                    delegate = appPreferences.objectDetectionPreferences.delegate,
                    model = model,
                    onEnabledChange = {
                        onObjectDetectionPrefsChange(
                            appPreferences.objectDetectionPreferences.copy(enabled = it),
                        )
                    },
                    onDelegateClick = onObjectDetectionDelegateClick,
                    onModelClick = onObjectDetectionModelClick,
                )
            }
            dividerItem()
            if (hasSetWallpaperPermission) {
                autoWallpaperSection(
                    enabled = appPreferences.autoWallpaperPreferences.enabled,
                    sourcesSummary = getSourcesSummary(
                        context = context,
                        useSameSources = !appPreferences
                            .autoWallpaperPreferences
                            .setDifferentWallpapers,
                        lightDarkEnabled = appPreferences
                            .autoWallpaperPreferences
                            .lightDarkEnabled,
                        savedSearches = autoWallpaperSavedSearches,
                        savedSearchEnabled = appPreferences
                            .autoWallpaperPreferences
                            .savedSearchEnabled,
                        favoritesEnabled = appPreferences
                            .autoWallpaperPreferences
                            .favoritesEnabled,
                        localEnabled = appPreferences
                            .autoWallpaperPreferences
                            .localEnabled,
                        lsLightDarkEnabled = appPreferences
                            .autoWallpaperPreferences
                            .lsLightDarkEnabled,
                        lsSavedSearchEnabled = appPreferences
                            .autoWallpaperPreferences
                            .lsSavedSearchEnabled,
                        lsFavoritesEnabled = appPreferences
                            .autoWallpaperPreferences
                            .lsFavoritesEnabled,
                        lsLocalEnabled = appPreferences
                            .autoWallpaperPreferences
                            .lsLocalEnabled,
                    ),
                    crop = appPreferences
                        .autoWallpaperPreferences
                        .crop,
                    useObjectDetection = appPreferences.autoWallpaperPreferences.useObjectDetection,
                    nextRun = autoWallpaperNextRun,
                    lsNextRun = lsAutoWallpaperNextRun,
                    useSameFrequency = appPreferences.autoWallpaperPreferences.useSameFreq,
                    frequency = appPreferences.autoWallpaperPreferences.frequency,
                    lsFrequency = appPreferences.autoWallpaperPreferences.lsFrequency,
                    showNotification = appPreferences.autoWallpaperPreferences.showNotification,
                    autoWallpaperStatus = autoWallpaperStatus,
                    targets = appPreferences.autoWallpaperPreferences.targets,
                    markFavorite = appPreferences.autoWallpaperPreferences.markFavorite,
                    download = appPreferences.autoWallpaperPreferences.download,
                    onEnabledChange = {
                        onAutoWallpaperPrefsChange(
                            appPreferences.autoWallpaperPreferences.copy(
                                enabled = it,
                            ),
                        )
                    },
                    onSourcesClick = onAutoWallpaperSourcesClick,
                    onFrequencyClick = onAutoWallpaperFrequencyClick,
                    onUseObjectDetectionChange = {
                        onAutoWallpaperPrefsChange(
                            appPreferences.autoWallpaperPreferences.copy(
                                useObjectDetection = it,
                            ),
                        )
                    },
                    onConstraintsClick = onAutoWallpaperConstraintsClick,
                    onChangeNowClick = onAutoWallpaperChangeNowClick,
                    onNextRunInfoClick = onAutoWallpaperNextRunInfoClick,
                    onShowNotificationChange = {
                        onAutoWallpaperPrefsChange(
                            appPreferences.autoWallpaperPreferences.copy(
                                showNotification = it,
                            ),
                        )
                    },
                    onSetToClick = onAutoWallpaperSetToClick,
                    onMarkFavoriteChange = {
                        onAutoWallpaperPrefsChange(
                            appPreferences.autoWallpaperPreferences.copy(
                                markFavorite = it,
                            ),
                        )
                    },
                    onDownloadChange = {
                        onAutoWallpaperPrefsChange(
                            appPreferences.autoWallpaperPreferences.copy(
                                download = it,
                            ),
                        )
                    },
                    onCropChange = {
                        onAutoWallpaperPrefsChange(
                            appPreferences.autoWallpaperPreferences.copy(
                                crop = it,
                            ),
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
            Snackbar(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = when (autoWallpaperStatus) {
                        is Status.Success -> {
                            stringResource(R.string.wallpaper_changed)
                        }
                        is Status.Failed -> {
                            if (autoWallpaperStatus.e is AutoWallpaperException) {
                                getFailureReasonString(autoWallpaperStatus.e.code)
                            } else {
                                stringResource(R.string.wallpaper_not_changed)
                            }
                        }
                        else -> ""
                    },
                )
            }
        }
    }
}

private fun getSourcesSummary(
    context: Context,
    useSameSources: Boolean,
    lightDarkEnabled: Boolean,
    savedSearches: ImmutableList<SavedSearch>,
    savedSearchEnabled: Boolean,
    favoritesEnabled: Boolean,
    localEnabled: Boolean,
    lsLightDarkEnabled: Boolean,
    lsSavedSearchEnabled: Boolean,
    lsFavoritesEnabled: Boolean,
    lsLocalEnabled: Boolean,
) = mutableListOf<String>().apply {
    if (useSameSources) {
        if (lightDarkEnabled) {
            add(context.getString(R.string.light_dark))
            return@apply
        }
        if (savedSearchEnabled && savedSearches.size > 0) {
            val searchNames = if (savedSearches.size > 2) {
                context.resources.getQuantityString(
                    R.plurals.n_searches,
                    savedSearches.size,
                    savedSearches.size,
                )
            } else {
                savedSearches.joinToString(", ") { it.name }
            }
            add("${context.getString(R.string.saved_search)} ($searchNames)")
        }
        if (favoritesEnabled) {
            add(context.getString(R.string.favorites))
        }
        if (localEnabled) {
            add(context.getString(R.string.local))
        }
    } else {
        val homeCount = getSourcesCount(
            lightDarkEnabled,
            savedSearchEnabled,
            favoritesEnabled,
            localEnabled,
        )
        if (homeCount > 0) {
            add(
                context.resources.getQuantityString(
                    R.plurals.home_screen_sources,
                    homeCount,
                    homeCount,
                ),
            )
        }
        val lsCount = getSourcesCount(
            lsLightDarkEnabled,
            lsSavedSearchEnabled,
            lsFavoritesEnabled,
            lsLocalEnabled,
        )
        if (lsCount > 0) {
            add(
                context.resources.getQuantityString(
                    R.plurals.lock_screen_sources,
                    lsCount,
                    lsCount,
                ),
            )
        }
    }
}.joinToString(", ")

private fun getSourcesCount(
    lightDarkEnabled: Boolean,
    savedSearchEnabled: Boolean,
    favoritesEnabled: Boolean,
    localEnabled: Boolean,
): Int {
    if (lightDarkEnabled) {
        return 1
    }
    var count = 0
    if (savedSearchEnabled) {
        count += 1
    }
    if (favoritesEnabled) {
        count += 1
    }
    if (localEnabled) {
        count += 1
    }
    return count
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewSettingsScreenContent() {
    WallFlowTheme {
        Surface {
            val coroutineScope = rememberCoroutineScope()
            var autoWallpaperStatus: Status? by remember {
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
                    autoWallpaperStatus = Status.Success
                    coroutineScope.launch {
                        delay(5000)
                        autoWallpaperStatus = null
                    }
                },
            )
        }
    }
}
