package com.ammar.wallflow.ui.screens.settings

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ammar.wallflow.R
import com.ammar.wallflow.data.preferences.AutoWallpaperPreferences
import com.ammar.wallflow.destinations.WallhavenApiKeyDialogDestination
import com.ammar.wallflow.extensions.restartApp
import com.ammar.wallflow.extensions.safeLaunch
import com.ammar.wallflow.extensions.toDp
import com.ammar.wallflow.extensions.trimAll
import com.ammar.wallflow.model.search.SavedSearchSaver
import com.ammar.wallflow.navigation.AppNavGraphs.SettingsNavGraph
import com.ammar.wallflow.ui.common.LocalSystemController
import com.ammar.wallflow.ui.common.permissions.DownloadPermissionsRationalDialog
import com.ammar.wallflow.ui.common.permissions.MultiplePermissionItem
import com.ammar.wallflow.ui.common.permissions.MultiplePermissionsState
import com.ammar.wallflow.ui.common.permissions.checkSetWallpaperPermission
import com.ammar.wallflow.ui.common.permissions.isGranted
import com.ammar.wallflow.ui.common.permissions.rememberMultiplePermissionsState
import com.ammar.wallflow.ui.common.permissions.shouldShowRationale
import com.ammar.wallflow.ui.common.rememberAdaptiveBottomSheetState
import com.ammar.wallflow.ui.common.searchedit.EditSearchModalBottomSheet
import com.ammar.wallflow.ui.common.searchedit.SavedSearchesDialog
import com.ammar.wallflow.ui.screens.settings.composables.AutoWallpaperSetToDialog
import com.ammar.wallflow.ui.screens.settings.composables.ChangeDownloadLocationDialog
import com.ammar.wallflow.ui.screens.settings.composables.ClearViewedWallpapersConfirmDialog
import com.ammar.wallflow.ui.screens.settings.composables.ConstraintOptionsDialog
import com.ammar.wallflow.ui.screens.settings.composables.DeleteSavedSearchConfirmDialog
import com.ammar.wallflow.ui.screens.settings.composables.DetailContentTopBar
import com.ammar.wallflow.ui.screens.settings.composables.EditSavedSearchBottomSheetHeader
import com.ammar.wallflow.ui.screens.settings.composables.ExifWriteTypeOptionsDialog
import com.ammar.wallflow.ui.screens.settings.composables.ExtraContentTopBar
import com.ammar.wallflow.ui.screens.settings.composables.FrequencyDialog
import com.ammar.wallflow.ui.screens.settings.composables.ListContentTopBar
import com.ammar.wallflow.ui.screens.settings.composables.NextRunInfoDialog
import com.ammar.wallflow.ui.screens.settings.composables.ObjectDetectionDelegateOptionsDialog
import com.ammar.wallflow.ui.screens.settings.composables.ObjectDetectionModelDeleteConfirmDialog
import com.ammar.wallflow.ui.screens.settings.composables.ObjectDetectionModelEditDialog
import com.ammar.wallflow.ui.screens.settings.composables.ObjectDetectionModelOptionsDialog
import com.ammar.wallflow.ui.screens.settings.composables.RestartDialog
import com.ammar.wallflow.ui.screens.settings.composables.RestartReason
import com.ammar.wallflow.ui.screens.settings.composables.ThemeOptionsDialog
import com.ammar.wallflow.ui.screens.settings.detailcontents.AccountContent
import com.ammar.wallflow.ui.screens.settings.detailcontents.AutoWallpaperContent
import com.ammar.wallflow.ui.screens.settings.detailcontents.CrashReportsContent
import com.ammar.wallflow.ui.screens.settings.detailcontents.DownloadsContent
import com.ammar.wallflow.ui.screens.settings.detailcontents.LayoutSettingsScreenContent
import com.ammar.wallflow.ui.screens.settings.detailcontents.LookAndFeelContent
import com.ammar.wallflow.ui.screens.settings.detailcontents.ManageAutoWallpaperSourcesContent
import com.ammar.wallflow.ui.screens.settings.detailcontents.ObjectDetectionContent
import com.ammar.wallflow.ui.screens.settings.detailcontents.SavedSearchesContent
import com.ammar.wallflow.ui.screens.settings.detailcontents.ViewedWallpapersContent
import com.ammar.wallflow.ui.screens.settings.detailcontents.ViewedWallpapersLookOptionsContent
import com.ammar.wallflow.utils.StoragePermissions
import com.ammar.wallflow.utils.getPublicDownloadsDir
import com.ammar.wallflow.utils.getRealPath
import com.ammar.wallflow.workers.AutoWallpaperWorker.Companion.AutoWallpaperException
import com.ammar.wallflow.workers.AutoWallpaperWorker.Companion.Status
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3AdaptiveApi::class,
    ExperimentalAnimationApi::class,
)
@Destination<SettingsNavGraph>(
    start = true,
)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val systemController = LocalSystemController.current
    val systemState by systemController.state

    var selectedType by rememberSaveable { mutableStateOf(SettingsType.ACCOUNT) }
    var selectedExtraType: SettingsExtraType? by rememberSaveable { mutableStateOf(null) }
    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<Any>(
        calculatePaneScaffoldDirective(currentWindowAdaptiveInfo()).copy(
            horizontalPartitionSpacerSize = 8.dp,
        ),
    )

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
            selectedExtraType = SettingsExtraType.AUTO_WALLPAPER_SOURCES
            scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Extra)
            return@rememberMultiplePermissionsState
        }
        viewModel.updateAutoWallpaperEnabled(updatedAutoWallpaperPreferences.enabled)
    }

    val chooseDownloadLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) {
        if (it == null) {
            return@rememberLauncherForActivityResult
        }
        viewModel.updateDownloadLocation(it)
    }

    NavigableListDetailPaneScaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars),
        navigator = scaffoldNavigator,
        listPane = {
            AnimatedPane(modifier = Modifier.preferredWidth(360.dp)) {
                ListContentScaffold(
                    selectedType = selectedType,
                    isExpanded = systemState.isExpanded,
                    onBackClick = { navController.navigateUp() },
                    onItemClick = { type ->
                        selectedType = type
                        scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
                    },
                )
            }
        },
        detailPane = {
            AnimatedPane {
                DetailContentScaffold(
                    selectedType = selectedType,
                    selectedExtraType = selectedExtraType,
                    viewModel = viewModel,
                    isExpanded = systemState.isExpanded,
                    autoWallpaperPermissionsState = autoWallpaperPermissionsState,
                    onBackClick = { scaffoldNavigator.navigateBack() },
                    onWallhavenApiKeyItemClick = {
                        navController.navigate(WallhavenApiKeyDialogDestination.route)
                    },
                    onLayoutClick = {
                        selectedExtraType = SettingsExtraType.LAYOUT
                        scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Extra)
                    },
                    onViewedWallpapersLookClick = {
                        selectedExtraType = SettingsExtraType.VIEW_WALLPAPERS_LOOK
                        scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Extra)
                    },
                    onSourcesClick = {
                        selectedExtraType = SettingsExtraType.AUTO_WALLPAPER_SOURCES
                        scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Extra)
                    },
                )
            }
        },
        extraPane = {
            AnimatedPane(
                modifier = Modifier.preferredWidth(
                    (systemState.size.width / 3f).toInt().coerceAtLeast(1).toDp(),
                ),
            ) {
                LaunchedEffect(transition.targetState) {
                    if (transition.targetState == EnterExitState.PostExit) {
                        selectedExtraType = null
                    }
                }
                ExtraContentScaffold(
                    selectedExtraType = selectedExtraType,
                    viewModel = viewModel,
                    isExpanded = systemState.isExpanded,
                    onBackClick = { scaffoldNavigator.navigateBack() },
                )
            }
        },
    )

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
        val state = rememberAdaptiveBottomSheetState()
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
                viewModel.updateAutoWallpaperFreq(
                    useSameFreq = useSameFreq,
                    frequency = freq,
                    lsFrequency = lsFreq,
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
                viewModel.updateAutoWallpaperConstraints(
                    constraints = it,
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
                viewModel.updateAutoWallpaperTargets(targets)
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

    if (uiState.showRestartDialog) {
        RestartDialog(
            reason = RestartReason.ACRA_ENABLED,
            onRestartClick = { context.restartApp() },
            onCancelClick = { viewModel.updateAcraEnabled(false) },
        )
    }
}

@Composable
private fun ListContentScaffold(
    selectedType: SettingsType,
    isExpanded: Boolean,
    onBackClick: () -> Unit,
    onItemClick: (SettingsType) -> Unit,
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            ListContentTopBar(
                isExpanded = isExpanded,
                onBackClick = onBackClick,
            )
        },
        containerColor = if (isExpanded) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
    ) {
        SettingsContentList(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            isExpanded = isExpanded,
            selectedType = selectedType,
            hasSetWallpaperPermission = context.checkSetWallpaperPermission(),
            onItemClick = onItemClick,
        )
    }
}

@Composable
private fun DetailContentScaffold(
    selectedType: SettingsType,
    selectedExtraType: SettingsExtraType?,
    viewModel: SettingsViewModel,
    isExpanded: Boolean,
    autoWallpaperPermissionsState: MultiplePermissionsState,
    onBackClick: () -> Unit,
    onWallhavenApiKeyItemClick: () -> Unit,
    onLayoutClick: () -> Unit,
    onViewedWallpapersLookClick: () -> Unit,
    onSourcesClick: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            DetailContentTopBar(
                isExpanded = isExpanded,
                selectedType = selectedType,
                onBackClick = onBackClick,
            )
        },
        containerColor = if (isExpanded) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isExpanded) {
                        Modifier.padding(
                            top = 8.dp,
                            bottom = 16.dp,
                            start = 16.dp,
                            end = 16.dp,
                        )
                    } else {
                        Modifier
                    },
                ),
        ) {
            Crossfade(
                modifier = Modifier.padding(it),
                targetState = selectedType,
                label = "Settings Detail Content",
            ) { type ->
                when (type) {
                    SettingsType.ACCOUNT -> AccountContent(
                        isExpanded = isExpanded,
                        onWallhavenApiKeyItemClick = onWallhavenApiKeyItemClick,
                    )
                    SettingsType.LOOK_AND_FEEL -> LookAndFeelSettingsScreen(
                        selectedExtraType = selectedExtraType,
                        viewModel = viewModel,
                        isExpanded = isExpanded,
                        onLayoutClick = onLayoutClick,
                    )
                    SettingsType.DOWNLOADS -> DownloadsSettingsScreen(
                        viewModel = viewModel,
                        isExpanded = isExpanded,
                    )
                    SettingsType.SAVED_SEARCHES -> SavedSearchesContent(
                        isExpanded = isExpanded,
                        onManageSavedSearchesClick = {
                            viewModel.showSavedSearches(true)
                        },
                    )
                    SettingsType.VIEWED_WALLPAPERS -> ViewWallpapersSettingsScreen(
                        selectedExtraType = selectedExtraType,
                        viewModel = viewModel,
                        isExpanded = isExpanded,
                        onViewedWallpapersLookClick = onViewedWallpapersLookClick,
                    )
                    SettingsType.OBJECT_DETECTION -> ObjectDetectionSettingsScreen(
                        viewModel = viewModel,
                        isExpanded = isExpanded,
                    )
                    SettingsType.AUTO_WALLPAPER -> AutoWallpaperSettingsScreen(
                        selectedExtraType = selectedExtraType,
                        isExpanded = isExpanded,
                        viewModel = viewModel,
                        autoWallpaperPermissionsState = autoWallpaperPermissionsState,
                        onSourcesClick = onSourcesClick,
                        onChangeNowStatusChange = { status ->
                            if (status == null || !status.isSuccessOrFail()) {
                                return@AutoWallpaperSettingsScreen
                            }
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = getStatusMessage(context, status),
                                    duration = if (status is Status.Failed) {
                                        SnackbarDuration.Long
                                    } else {
                                        SnackbarDuration.Short
                                    },
                                )
                            }
                        },
                    )
                    SettingsType.CRASH_REPORTS -> CrashReportsSettingsScreen(
                        isExpanded = isExpanded,
                        viewModel = viewModel,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExtraContentScaffold(
    selectedExtraType: SettingsExtraType?,
    viewModel: SettingsViewModel,
    isExpanded: Boolean,
    onBackClick: () -> Unit,
) {
    Scaffold(
        modifier = Modifier
            .then(
                if (isExpanded) {
                    Modifier.clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            bottomStart = 16.dp,
                        ),
                    )
                } else {
                    Modifier
                },
            ),
        topBar = {
            // Box(modifier = Modifier.height(TopAppBarDefaults.MediumAppBarCollapsedHeight))
            ExtraContentTopBar(
                isExpanded = isExpanded,
                selectedExtraType = selectedExtraType,
                onBackClick = onBackClick,
            )
        },
        containerColor = if (isExpanded) {
            MaterialTheme.colorScheme.surfaceBright
        } else {
            MaterialTheme.colorScheme.surface
        },
    ) {
        Crossfade(
            modifier = Modifier.padding(it),
            targetState = selectedExtraType,
            label = "Settings Extra Content",
        ) { type ->
            if (type == null) {
                return@Crossfade
            }
            when (type) {
                SettingsExtraType.LAYOUT -> LayoutSettings(
                    viewModel = viewModel,
                    isExpanded = isExpanded,
                )
                SettingsExtraType.VIEW_WALLPAPERS_LOOK -> ViewedWallpapersLookOptions(
                    viewModel = viewModel,
                    isExpanded = isExpanded,
                )
                SettingsExtraType.AUTO_WALLPAPER_SOURCES -> ManageAutoWallpaperSources(
                    viewModel = viewModel,
                    isExpanded = isExpanded,
                )
            }
        }
    }
}

@Composable
private fun LookAndFeelSettingsScreen(
    selectedExtraType: SettingsExtraType?,
    viewModel: SettingsViewModel,
    isExpanded: Boolean,
    onLayoutClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val appPreferences = uiState.appPreferences
    val lookAndFeelPreferences = appPreferences.lookAndFeelPreferences
    LookAndFeelContent(
        isExpanded = isExpanded,
        selectedType = selectedExtraType,
        blurNsfw = appPreferences.blurNsfw,
        blurSketchy = appPreferences.blurSketchy,
        showLocalTab = lookAndFeelPreferences.showLocalTab,
        onThemeClick = { viewModel.showThemeOptionsDialog(true) },
        onLayoutClick = onLayoutClick,
        onBlurNsfwCheckChange = viewModel::setBlurNsfw,
        onBlurSketchyCheckChange = viewModel::setBlurSketchy,
        onShowLocalTabChange = { show ->
            viewModel.updateLookAndFeelPrefs(
                lookAndFeelPreferences.copy(
                    showLocalTab = show,
                ),
            )
        },
    )
}

@Composable
private fun DownloadsSettingsScreen(
    viewModel: SettingsViewModel,
    isExpanded: Boolean,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val appPreferences = uiState.appPreferences
    val prefsDownloadLocationUri = appPreferences.downloadLocation
    val downloadLocationString = if (prefsDownloadLocationUri == null) {
        getPublicDownloadsDir().absolutePath
    } else {
        getRealPath(context, prefsDownloadLocationUri) ?: prefsDownloadLocationUri.toString()
    }
    DownloadsContent(
        isExpanded = isExpanded,
        downloadLocation = downloadLocationString,
        writeTagsToExif = appPreferences.writeTagsToExif,
        tagsExifWriteType = appPreferences.tagsExifWriteType,
        onDownloadLocationClick = { viewModel.showChangeDownloadLocationDialog(true) },
        onWriteTagsToExifCheckChange = viewModel::updateWriteTagsToExif,
        onTagsWriteTypeClick = { viewModel.showTagsWriteTypeDialog(true) },
    )
}

@Composable
private fun ViewWallpapersSettingsScreen(
    viewModel: SettingsViewModel,
    isExpanded: Boolean,
    selectedExtraType: SettingsExtraType?,
    onViewedWallpapersLookClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val viewedWallpapersPreferences = uiState.appPreferences.viewedWallpapersPreferences
    ViewedWallpapersContent(
        isExpanded = isExpanded,
        selectedType = selectedExtraType,
        enabled = viewedWallpapersPreferences.enabled,
        look = viewedWallpapersPreferences.look,
        onEnabledChange = viewModel::updateRememberViewedWallpapers,
        onViewedWallpapersLookClick = onViewedWallpapersLookClick,
        onClearClick = { viewModel.showClearViewedWallpapersConfirmDialog(true) },
    )
}

@Composable
private fun ObjectDetectionSettingsScreen(
    viewModel: SettingsViewModel,
    isExpanded: Boolean,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val objectDetectionPreferences = uiState.appPreferences.objectDetectionPreferences
    ObjectDetectionContent(
        isExpanded = isExpanded,
        enabled = objectDetectionPreferences.enabled,
        delegate = objectDetectionPreferences.delegate,
        model = uiState.selectedModel,
        onEnabledChange = { enabled ->
            viewModel.updateSubjectDetectionPrefs(
                objectDetectionPreferences.copy(
                    enabled = enabled,
                ),
            )
        },
        onDelegateClick = { viewModel.showObjectDetectionDelegateOptions(true) },
        onModelClick = { viewModel.showObjectDetectionModelOptions(true) },
    )
}

@Composable
private fun AutoWallpaperSettingsScreen(
    viewModel: SettingsViewModel,
    autoWallpaperPermissionsState: MultiplePermissionsState,
    selectedExtraType: SettingsExtraType?,
    isExpanded: Boolean,
    onSourcesClick: () -> Unit,
    onChangeNowStatusChange: (Status?) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val autoWallpaperPreferences = uiState.appPreferences.autoWallpaperPreferences

    LaunchedEffect(uiState.autoWallpaperStatus) {
        onChangeNowStatusChange(uiState.autoWallpaperStatus)
    }

    AutoWallpaperContent(
        enabled = autoWallpaperPreferences.enabled,
        selectedType = selectedExtraType,
        isExpanded = isExpanded,
        sourcesSummary = getSourcesSummary(
            context = LocalContext.current,
            useSameSources = !autoWallpaperPreferences.setDifferentWallpapers,
            lightDarkEnabled = autoWallpaperPreferences.lightDarkEnabled,
            savedSearches = uiState.autoWallpaperSavedSearches,
            savedSearchEnabled = autoWallpaperPreferences.savedSearchEnabled,
            favoritesEnabled = autoWallpaperPreferences.favoritesEnabled,
            localEnabled = autoWallpaperPreferences.localEnabled,
            lsLightDarkEnabled = autoWallpaperPreferences.lsLightDarkEnabled,
            lsSavedSearchEnabled = autoWallpaperPreferences.lsSavedSearchEnabled,
            lsFavoritesEnabled = autoWallpaperPreferences.lsFavoritesEnabled,
            lsLocalEnabled = autoWallpaperPreferences.lsLocalEnabled,
        ),
        crop = autoWallpaperPreferences.crop,
        useObjectDetection = autoWallpaperPreferences.useObjectDetection,
        nextRun = uiState.autoWallpaperNextRun,
        lsNextRun = uiState.lsAutoWallpaperNextRun,
        useSameFrequency = autoWallpaperPreferences.useSameFreq,
        frequency = autoWallpaperPreferences.frequency,
        lsFrequency = autoWallpaperPreferences.lsFrequency,
        showNotification = autoWallpaperPreferences.showNotification,
        autoWallpaperStatus = uiState.autoWallpaperStatus,
        targets = autoWallpaperPreferences.targets,
        markFavorite = autoWallpaperPreferences.markFavorite,
        download = autoWallpaperPreferences.download,
        onEnabledChange = { enabled ->
            if (enabled) {
                viewModel.setTempAutoWallpaperPrefs(
                    autoWallpaperPreferences.copy(
                        enabled = true,
                    ),
                )
                // need to check if we have all permissions before enabling auto wallpaper
                autoWallpaperPermissionsState.launchMultiplePermissionRequest()
                return@AutoWallpaperContent
            }
            viewModel.updateAutoWallpaperEnabled(false)
        },
        onSourcesClick = onSourcesClick,
        onFrequencyClick = {
            viewModel.showAutoWallpaperFrequencyDialog(true)
        },
        onUseObjectDetectionChange = { use ->
            viewModel.updateAutoWallpaperPrefs(
                autoWallpaperPreferences.copy(
                    useObjectDetection = use,
                ),
            )
        },
        onConstraintsClick = {
            viewModel.showAutoWallpaperConstraintsDialog(true)
        },
        onChangeNowClick = viewModel::autoWallpaperChangeNow,
        onNextRunInfoClick = {
            viewModel.showAutoWallpaperNextRunInfoDialog(true)
        },
        onShowNotificationChange = { show ->
            viewModel.updateAutoWallpaperPrefs(
                autoWallpaperPreferences.copy(
                    showNotification = show,
                ),
            )
        },
        onSetToClick = {
            viewModel.showAutoWallpaperSetToDialog(true)
        },
        onMarkFavoriteChange = { mark ->
            viewModel.updateAutoWallpaperPrefs(
                autoWallpaperPreferences.copy(
                    markFavorite = mark,
                ),
            )
        },
        onDownloadChange = { download ->
            viewModel.updateAutoWallpaperPrefs(
                autoWallpaperPreferences.copy(
                    download = download,
                ),
            )
        },
        onCropChange = { crop ->
            viewModel.updateAutoWallpaperPrefs(
                autoWallpaperPreferences.copy(
                    crop = crop,
                ),
            )
        },
    )
}

@Composable
private fun CrashReportsSettingsScreen(
    viewModel: SettingsViewModel,
    isExpanded: Boolean,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CrashReportsContent(
        acraEnabled = uiState.appPreferences.acraEnabled,
        isExpanded = isExpanded,
        onAcraEnabledChange = viewModel::updateAcraEnabled,
    )
}

@Composable
private fun LayoutSettings(
    viewModel: SettingsViewModel,
    isExpanded: Boolean,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LayoutSettingsScreenContent(
        supportsTwoPane = isExpanded,
        layoutPreferences = uiState
            .appPreferences
            .lookAndFeelPreferences
            .layoutPreferences,
        onLayoutPreferencesChange = viewModel::updateLayoutPreferences,
    )
}

@Composable
private fun ViewedWallpapersLookOptions(
    viewModel: SettingsViewModel,
    isExpanded: Boolean,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ViewedWallpapersLookOptionsContent(
        selectedViewedWallpapersLook = uiState
            .appPreferences
            .viewedWallpapersPreferences
            .look,
        isExpanded = isExpanded,
        onOptionClick = viewModel::updateViewedWallpapersLook,
    )
}

@Composable
private fun ManageAutoWallpaperSources(
    viewModel: SettingsViewModel,
    isExpanded: Boolean,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ManageAutoWallpaperSourcesContent(
        isExpanded = isExpanded,
        useSameSources = !uiState
            .appPreferences
            .autoWallpaperPreferences
            .setDifferentWallpapers,
        hasLightDarkWallpapers = uiState.hasLightDarkWallpapers,
        hasFavorites = uiState.hasFavorites,
        savedSearches = uiState.savedSearches,
        localDirectories = uiState.localDirectories,
        homeScreenSources = uiState.homeScreenAutoWallpaperSources,
        lockScreenSources = uiState.lockScreenAutoWallpaperSources,
        onChangeUseSameSources = viewModel::updateAutoWallpaperUseSameSources,
        onChangeLightDarkEnabled = viewModel::updateAutoWallpaperLightDarkEnabled,
        onChangeUseDarkWithExtraDim = viewModel::updateAutoWallpaperUseDarkWithExtraDim,
        onChangeSavedSearchEnabled = viewModel::updateAutoWallpaperSavedSearchEnabled,
        onChangeSavedSearchIds = viewModel::updateAutoWallpaperSavedSearchIds,
        onChangeFavoritesEnabled = viewModel::updateAutoWallpaperFavoritesEnabled,
        onChangeLocalEnabled = viewModel::updateAutoWallpaperLocalEnabled,
        onChangeSelectedLocalDirs = viewModel::updateAutoWallpaperSelectedLocalDirs,
    )
}

private fun getStatusMessage(
    context: Context,
    status: Status?,
) = when (status) {
    is Status.Success -> {
        context.getString(R.string.wallpaper_changed)
    }
    is Status.Failed -> {
        if (status.e is AutoWallpaperException) {
            getFailureReasonString(context, status.e.code)
        } else {
            context.getString(R.string.wallpaper_not_changed)
        }
    }
    else -> ""
}
