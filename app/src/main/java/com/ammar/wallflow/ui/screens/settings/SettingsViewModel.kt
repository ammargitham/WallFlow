package com.ammar.wallflow.ui.screens.settings

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Stable
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.WorkInfo
import com.ammar.wallflow.EFFICIENT_DET_LITE_0_MODEL_NAME
import com.ammar.wallflow.R
import com.ammar.wallflow.data.db.entity.ObjectDetectionModelEntity
import com.ammar.wallflow.data.db.entity.search.toSavedSearch
import com.ammar.wallflow.data.db.entity.toModel
import com.ammar.wallflow.data.preferences.AppPreferences
import com.ammar.wallflow.data.preferences.AutoWallpaperPreferences
import com.ammar.wallflow.data.preferences.LayoutPreferences
import com.ammar.wallflow.data.preferences.LookAndFeelPreferences
import com.ammar.wallflow.data.preferences.ObjectDetectionPreferences
import com.ammar.wallflow.data.preferences.ViewedWallpapersLook
import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.ammar.wallflow.data.repository.FavoritesRepository
import com.ammar.wallflow.data.repository.LightDarkRepository
import com.ammar.wallflow.data.repository.ObjectDetectionModelRepository
import com.ammar.wallflow.data.repository.SavedSearchRepository
import com.ammar.wallflow.data.repository.ViewedRepository
import com.ammar.wallflow.extensions.TAG
import com.ammar.wallflow.extensions.getMLModelsFileIfExists
import com.ammar.wallflow.extensions.rootCause
import com.ammar.wallflow.extensions.trimAll
import com.ammar.wallflow.extensions.workManager
import com.ammar.wallflow.model.ObjectDetectionModel
import com.ammar.wallflow.model.WallpaperTarget
import com.ammar.wallflow.model.local.LocalDirectory
import com.ammar.wallflow.model.search.SavedSearch
import com.ammar.wallflow.utils.DownloadManager
import com.ammar.wallflow.utils.DownloadStatus
import com.ammar.wallflow.utils.ExifWriteType
import com.ammar.wallflow.utils.combine
import com.ammar.wallflow.utils.getLocalDirs
import com.ammar.wallflow.utils.objectdetection.validateModelFile
import com.ammar.wallflow.workers.AutoWallpaperWorker
import com.ammar.wallflow.workers.DownloadWorker
import com.ammar.wallflow.workers.renameFile
import com.github.materiiapps.partial.Partialize
import com.github.materiiapps.partial.partial
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.Instant

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val application: Application,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val objectDetectionModelRepository: ObjectDetectionModelRepository,
    private val downloadManager: DownloadManager,
    private val savedSearchRepository: SavedSearchRepository,
    private val viewedRepository: ViewedRepository,
    lightDarkRepository: LightDarkRepository,
    favoritesRepository: FavoritesRepository,
) : AndroidViewModel(application) {
    private val localUiStateFlow = MutableStateFlow(SettingsUiStatePartial())
    private val autoWallpaperNextRunFlow = getAutoWallpaperNextRun()
    private val lsAutoWallpaperNextRunFlow = getLsAutoWallpaperNextRun()
    private var changeNowJob: Job? = null

    val uiState = combine(
        appPreferencesRepository.appPreferencesFlow,
        objectDetectionModelRepository.getAll(),
        localUiStateFlow,
        savedSearchRepository.observeAll(),
        autoWallpaperNextRunFlow,
        lsAutoWallpaperNextRunFlow,
        lightDarkRepository.observeCount(),
        favoritesRepository.observeCount(),
    ) {
            appPreferences,
            objectDetectionModels,
            localUiState,
            savedSearches,
            autoWallpaperNextRun,
            lsAutoWallpaperNextRun,
            lightDarkCount,
            favoritesCount,
        ->
        val selectedModelId = appPreferences.objectDetectionPreferences.modelId
        val selectedModel = if (selectedModelId == 0L) {
            ObjectDetectionModel.DEFAULT
        } else {
            objectDetectionModels
                .find { it.id == selectedModelId }
                ?.toModel()
                ?: ObjectDetectionModel.DEFAULT
        }
        val allSavedSearches = savedSearches.map { entity -> entity.toSavedSearch() }
        val autoWallpaperPreferences = appPreferences.autoWallpaperPreferences
        localUiState.merge(
            SettingsUiState(
                appPreferences = appPreferences,
                objectDetectionModels = objectDetectionModels.toPersistentList(),
                selectedModel = selectedModel,
                savedSearches = allSavedSearches.toPersistentList(),
                autoWallpaperSavedSearches = allSavedSearches.filter {
                    appPreferences.autoWallpaperPreferences.savedSearchIds.contains(it.id)
                }.toPersistentList(),
                autoWallpaperNextRun = autoWallpaperNextRun,
                lsAutoWallpaperNextRun = lsAutoWallpaperNextRun,
                localDirectories = getLocalDirs(application, appPreferences).toPersistentList(),
                hasLightDarkWallpapers = lightDarkCount > 0,
                hasFavorites = favoritesCount > 0,
                homeScreenAutoWallpaperSources = AutoWallpaperSources(
                    lightDarkEnabled = autoWallpaperPreferences.lightDarkEnabled,
                    useDarkWithExtraDim = autoWallpaperPreferences.useDarkWithExtraDim,
                    savedSearchEnabled = autoWallpaperPreferences.savedSearchEnabled,
                    savedSearchIds = autoWallpaperPreferences.savedSearchIds,
                    favoritesEnabled = autoWallpaperPreferences.favoritesEnabled,
                    localEnabled = autoWallpaperPreferences.localEnabled,
                    localDirs = autoWallpaperPreferences.localDirs,
                ),
                lockScreenAutoWallpaperSources = AutoWallpaperSources(
                    lightDarkEnabled = autoWallpaperPreferences.lsLightDarkEnabled,
                    useDarkWithExtraDim = autoWallpaperPreferences.lsUseDarkWithExtraDim,
                    savedSearchEnabled = autoWallpaperPreferences.lsSavedSearchEnabled,
                    savedSearchIds = autoWallpaperPreferences.lsSavedSearchIds,
                    favoritesEnabled = autoWallpaperPreferences.lsFavoritesEnabled,
                    localEnabled = autoWallpaperPreferences.lsLocalEnabled,
                    localDirs = autoWallpaperPreferences.lsLocalDirs,
                ),
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState(),
    )

    fun setBlurSketchy(blur: Boolean) = viewModelScope.launch {
        appPreferencesRepository.updateBlurSketchy(blur)
    }

    fun setBlurNsfw(blur: Boolean) = viewModelScope.launch {
        appPreferencesRepository.updateBlurNsfw(blur)
    }

    fun updateWriteTagsToExif(enabled: Boolean) = viewModelScope.launch {
        appPreferencesRepository.updateWriteTagsToExif(enabled)
    }

    fun updateTagsWriteType(writeType: ExifWriteType) = viewModelScope.launch {
        appPreferencesRepository.updateTagsWriteType(writeType)
    }

    fun updateSubjectDetectionPrefs(objectDetectionPreferences: ObjectDetectionPreferences) =
        viewModelScope.launch {
            appPreferencesRepository.updateObjectDetectionPrefs(objectDetectionPreferences)
        }

    fun showObjectDetectionDelegateOptions(show: Boolean) = localUiStateFlow.update {
        it.copy(showObjectDetectionDelegateOptions = partial(show))
    }

    fun showObjectDetectionModelOptions(show: Boolean) = localUiStateFlow.update {
        it.copy(showObjectDetectionModelOptions = partial(show))
    }

    fun showEditModelDialog(
        model: ObjectDetectionModelEntity? = null,
        show: Boolean = true,
    ) = localUiStateFlow.update {
        it.copy(
            editModel = partial(model),
            showEditModelDialog = partial(show),
        )
    }

    suspend fun checkModelNameExists(name: String, id: Long?) = id?.let {
        objectDetectionModelRepository.nameExistsExcludingId(it, name)
    } ?: objectDetectionModelRepository.nameExists(name)

    fun checkModelFileNameExists(fileName: String) =
        application.getMLModelsFileIfExists(fileName) != null

    fun saveModel(
        model: ObjectDetectionModelEntity,
        onDone: (error: Throwable?) -> Unit,
    ) = viewModelScope.launch {
        val existing = if (model.id != 0L) {
            objectDetectionModelRepository.getById(model.id)
        } else {
            null
        }
        if (model.url != existing?.url) {
            downloadModel(
                model.name,
                model.url,
                model.fileName,
            ) { state ->
                if (!state.isSuccessOrFail()) return@downloadModel
                if (state is DownloadStatus.Failed) {
                    // application.toast("Model download failed: ${state.e?.message ?: "Unknown reason"}")
                    onDone(state.e)
                    return@downloadModel
                }
                if (state is DownloadStatus.Success) {
                    val modelPath = state.filePath
                    if (modelPath == null) {
                        val msg = "model file path null"
                        Log.e(TAG, msg)
                        onDone(RuntimeException(msg))
                        return@downloadModel
                    }
                    val modelFile = modelPath.toUri().toFile()

                    // check if file is a valid tf-lite model
                    try {
                        validateModelFile(modelFile)
                    } catch (e: Exception) {
                        Log.e(TAG, "saveModel: ", e)
                        modelFile.delete()
                        onDone(e.rootCause)
                        return@downloadModel
                    }
                    existing?.fileName?.run {
                        // if this model has an existing file, delete it
                        application.getMLModelsFileIfExists(this)?.delete()
                    }
                    viewModelScope.launch {
                        try {
                            // save the new file
                            val fileName = modelFile.name
                            objectDetectionModelRepository.addOrUpdate(
                                existing?.copy(
                                    name = model.name,
                                    url = model.url,
                                    fileName = fileName,
                                ) ?: model.copy(fileName = fileName),
                            )
                            onDone(null)
                            localUiStateFlow.update {
                                it.copy(
                                    editModel = partial(null),
                                    showEditModelDialog = partial(false),
                                    modelDownloadStatus = partial(null),
                                )
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "saveModel: ", e)
                            modelFile.delete()
                            onDone(e)
                        }
                    }
                }
            }
            return@launch
        }

        val newFileName = model.fileName.trimAll()
        val existingFileName = existing.fileName.trimAll()
        if (newFileName != existingFileName) {
            // just rename file
            application.getMLModelsFileIfExists(existingFileName)?.let {
                renameFile(it, newFileName)
            }
        }

        objectDetectionModelRepository.addOrUpdate(
            existing.copy(
                name = model.name,
                fileName = newFileName,
            ),
        )
        onDone(null)
        localUiStateFlow.update {
            it.copy(
                editModel = partial(null),
                showEditModelDialog = partial(false),
                modelDownloadStatus = partial(null),
            )
        }
    }

    private suspend fun downloadModel(
        name: String,
        url: String,
        fileName: String,
        onDone: (status: DownloadStatus) -> Unit,
    ) {
        val workName = downloadManager.requestDownload(
            context = application,
            url = url,
            downloadLocation = DownloadManager.Companion.DownloadLocation.APP_ML_MODELS,
            notificationType = DownloadWorker.Companion.NotificationType.VISIBLE,
            notificationTitle = application.getString(R.string.model_download_title, name),
            fileName = fileName,
        )
        downloadManager.getProgress(application, workName).collectLatest { state ->
            localUiStateFlow.update { it.copy(modelDownloadStatus = partial(state)) }
            if (!state.isSuccessOrFail()) return@collectLatest
            onDone(state)
        }
    }

    fun deleteModel(
        model: ObjectDetectionModelEntity?,
        confirmed: Boolean = false,
    ) {
        if (model == null) {
            // just hide, without any operation
            localUiStateFlow.update { it.copy(deleteModel = partial(null)) }
            return
        }
        if (!confirmed) {
            // show confirm dialog
            localUiStateFlow.update { it.copy(deleteModel = partial(model)) }
            return
        }
        viewModelScope.launch {
            objectDetectionModelRepository.delete(model)
            application.getMLModelsFileIfExists(model.fileName)?.delete()
            val objectDetectionPreferences = uiState.value.appPreferences.objectDetectionPreferences
            if (objectDetectionPreferences.modelId != model.id) return@launch
            // if this model was selected, set the default model as selected
            val defaultModelId = objectDetectionModelRepository.getByName(
                EFFICIENT_DET_LITE_0_MODEL_NAME,
            )?.id ?: 0
            appPreferencesRepository.updateObjectDetectionPrefs(
                objectDetectionPreferences.copy(modelId = defaultModelId),
            )
        }
        // hide the confirm dialog
        localUiStateFlow.update { it.copy(deleteModel = partial(null)) }
    }

    fun setSelectedModel(entity: ObjectDetectionModelEntity?) = viewModelScope.launch {
        val modelId = entity?.id ?: objectDetectionModelRepository.getByName(
            EFFICIENT_DET_LITE_0_MODEL_NAME,
        )?.id ?: 0
        val objectDetectionPreferences = uiState.value.appPreferences.objectDetectionPreferences
        appPreferencesRepository.updateObjectDetectionPrefs(
            objectDetectionPreferences.copy(modelId = modelId),
        )
        showObjectDetectionModelOptions(false)
    }

    fun showSavedSearches(show: Boolean) = localUiStateFlow.update {
        it.copy(showSavedSearches = partial(show))
    }

    fun editSavedSearch(savedSearch: SavedSearch?) = localUiStateFlow.update {
        it.copy(
            editSavedSearch = partial(savedSearch),
            showSavedSearches = partial(false),
        )
    }

    fun updateSavedSearch(savedSearch: SavedSearch) {
        viewModelScope.launch {
            savedSearchRepository.upsert(savedSearch)
        }
    }

    fun deleteSavedSearch(
        savedSearch: SavedSearch?,
        confirmed: Boolean = false,
    ) {
        if (savedSearch == null) {
            localUiStateFlow.update { it.copy(deleteSavedSearch = partial(null)) }
            return
        }
        if (confirmed) {
            viewModelScope.launch {
                savedSearchRepository.delete(savedSearch)
                // if saved search was set as the auto wallpaper source, remove it
                val appPreferences = uiState.value.appPreferences
                val savedSearchIds = appPreferences.autoWallpaperPreferences.savedSearchIds
                if (savedSearchIds.contains(savedSearch.id)) {
                    val updatedSavedSearchIds = savedSearchIds - savedSearch.id
                    updateAutoWallpaperPrefs(
                        appPreferences.autoWallpaperPreferences.copy(
                            savedSearchIds = updatedSavedSearchIds,
                            savedSearchEnabled = updatedSavedSearchIds.isNotEmpty(),
                        ),
                        // showSourcesDialog = false,
                    )
                }
                // close the dialog
                localUiStateFlow.update { it.copy(deleteSavedSearch = partial(null)) }
            }
            return
        }
        // show the dialog
        localUiStateFlow.update { it.copy(deleteSavedSearch = partial(savedSearch)) }
    }

    fun showAutoWallpaperFrequencyDialog(show: Boolean) = localUiStateFlow.update {
        it.copy(showAutoWallpaperFrequencyDialog = partial(show))
    }

    fun showAutoWallpaperConstraintsDialog(show: Boolean) = localUiStateFlow.update {
        it.copy(showAutoWallpaperConstraintsDialog = partial(show))
    }

    fun showPermissionRationaleDialog(show: Boolean = true) = localUiStateFlow.update {
        it.copy(showPermissionRationaleDialog = partial(show))
    }

    fun setTempAutoWallpaperPrefs(autoWallpaperPreferences: AutoWallpaperPreferences?) =
        localUiStateFlow.update {
            it.copy(tempAutoWallpaperPreferences = partial(autoWallpaperPreferences))
        }

    fun autoWallpaperChangeNow() {
        changeNowJob?.cancel()
        changeNowJob = viewModelScope.launch {
            val requestId = AutoWallpaperWorker.triggerImmediate(application)
            AutoWallpaperWorker.getProgress(
                context = application,
                requestId = requestId,
            ).collectLatest { status ->
                localUiStateFlow.update { it.copy(autoWallpaperStatus = partial(status)) }
                if (status.isSuccessOrFail()) {
                    // clear status after success or failure
                    delay(2000)
                    localUiStateFlow.update { it.copy(autoWallpaperStatus = partial(null)) }
                    changeNowJob?.cancel()
                }
            }
        }
    }

    fun showAutoWallpaperNextRunInfoDialog(show: Boolean) = localUiStateFlow.update {
        it.copy(showAutoWallpaperNextRunInfoDialog = partial(show))
    }

    fun showAutoWallpaperSetToDialog(show: Boolean) = localUiStateFlow.update {
        it.copy(showAutoWallpaperSetToDialog = partial(show))
    }

    fun showThemeOptionsDialog(show: Boolean) = localUiStateFlow.update {
        it.copy(showThemeOptionsDialog = partial(show))
    }

    fun updateLookAndFeelPrefs(lookAndFeelPreferences: LookAndFeelPreferences) =
        viewModelScope.launch {
            appPreferencesRepository.updateLookAndFeelPreferences(lookAndFeelPreferences)
        }

    suspend fun checkSavedSearchNameExists(name: String, id: Long?) = id?.let {
        savedSearchRepository.existsExcludingId(it, name)
    } ?: savedSearchRepository.exists(name)

    private fun getAutoWallpaperNextRun() = application.workManager.getWorkInfosForUniqueWorkFlow(
        AutoWallpaperWorker.PERIODIC_WORK_NAME,
    ).map { getNextRun(it) }

    private fun getLsAutoWallpaperNextRun() = application.workManager.getWorkInfosForUniqueWorkFlow(
        AutoWallpaperWorker.PERIODIC_LS_WORK_NAME,
    ).map { getNextRun(it) }

    private fun getNextRun(it: List<WorkInfo>): NextRun {
        val info = it.firstOrNull() ?: return NextRun.NotScheduled
        return when (info.state) {
            WorkInfo.State.ENQUEUED -> {
                NextRun.NextRunTime(Instant.fromEpochMilliseconds(info.nextScheduleTimeMillis))
            }
            WorkInfo.State.RUNNING -> NextRun.Running
            else -> NextRun.NotScheduled
        }
    }

    fun showTagsWriteTypeDialog(show: Boolean) = localUiStateFlow.update {
        it.copy(showTagsWriteTypeDialog = partial(show))
    }

    fun showChangeDownloadLocationDialog(show: Boolean) = localUiStateFlow.update {
        it.copy(showChangeDownloadLocationDialog = partial(show))
    }

    fun updateRememberViewedWallpapers(enabled: Boolean) = viewModelScope.launch {
        appPreferencesRepository.updateViewedWallpapersPreferences(
            uiState.value.appPreferences.viewedWallpapersPreferences.copy(
                enabled = enabled,
            ),
        )
    }

    fun updateViewedWallpapersLook(look: ViewedWallpapersLook) = viewModelScope.launch {
        appPreferencesRepository.updateViewedWallpapersPreferences(
            uiState.value.appPreferences.viewedWallpapersPreferences.copy(
                look = look,
            ),
        )
    }

    fun showClearViewedWallpapersConfirmDialog(show: Boolean) = localUiStateFlow.update {
        it.copy(showClearViewedWallpapersConfirmDialog = partial(show))
    }

    fun clearViewedWallpapers() = viewModelScope.launch {
        viewedRepository.deleteAll()
    }

    fun updateDownloadLocation(uri: Uri) = viewModelScope.launch {
        application.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        appPreferencesRepository.updateDownloadLocation(uri)
        localUiStateFlow.update {
            it.copy(
                showChangeDownloadLocationDialog = partial(false),
            )
        }
    }

    fun removeDownloadLocation() = viewModelScope.launch {
        val appPreferences = uiState.value.appPreferences
        val localUris = getLocalDirs(application, appPreferences).map { it.uri }
        val currentDownloadLocation = appPreferences.downloadLocation ?: return@launch
        application.contentResolver.releasePersistableUriPermission(
            currentDownloadLocation,
            if (localUris.contains(currentDownloadLocation)) {
                // just release write permission
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            } else {
                // release both read and write
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            },
        )
        appPreferencesRepository.updateDownloadLocation(null)
        localUiStateFlow.update {
            it.copy(
                showChangeDownloadLocationDialog = partial(false),
            )
        }
    }

    fun updateLayoutPreferences(layoutPreferences: LayoutPreferences) = viewModelScope.launch {
        appPreferencesRepository.updateLookAndFeelPreferences(
            uiState.value.appPreferences.lookAndFeelPreferences.copy(
                layoutPreferences = layoutPreferences,
            ),
        )
    }

    fun updateAutoWallpaperUseSameSources(useSameSources: Boolean) = updateAutoWallpaperPrefs(
        uiState.value.appPreferences.autoWallpaperPreferences.copy(
            setDifferentWallpapers = !useSameSources,
        ),
    )

    fun updateAutoWallpaperLightDarkEnabled(
        lightDarkEnabled: Boolean,
        target: WallpaperTarget,
    ) {
        val autoWallpaperPreferences = uiState.value.appPreferences.autoWallpaperPreferences
        updateAutoWallpaperPrefs(
            when (target) {
                WallpaperTarget.HOME -> autoWallpaperPreferences.copy(
                    lightDarkEnabled = lightDarkEnabled,
                )
                WallpaperTarget.LOCKSCREEN -> autoWallpaperPreferences.copy(
                    lsLightDarkEnabled = lightDarkEnabled,
                )
            },
        )
    }

    fun updateAutoWallpaperUseDarkWithExtraDim(
        useDarkWithExtraDim: Boolean,
        target: WallpaperTarget,
    ) {
        val autoWallpaperPreferences = uiState.value.appPreferences.autoWallpaperPreferences
        updateAutoWallpaperPrefs(
            when (target) {
                WallpaperTarget.HOME -> autoWallpaperPreferences.copy(
                    useDarkWithExtraDim = useDarkWithExtraDim,
                )
                WallpaperTarget.LOCKSCREEN -> autoWallpaperPreferences.copy(
                    lsUseDarkWithExtraDim = useDarkWithExtraDim,
                )
            },
        )
    }

    fun updateAutoWallpaperSavedSearchEnabled(
        savedSearchEnabled: Boolean,
        target: WallpaperTarget,
    ) {
        val state = uiState.value
        val autoWallpaperPreferences = state.appPreferences.autoWallpaperPreferences
        val savedSearches = state.savedSearches
        updateAutoWallpaperPrefs(
            when (target) {
                WallpaperTarget.HOME -> {
                    val newSavedSearchIds = autoWallpaperPreferences.savedSearchIds.ifEmpty {
                        setOf(savedSearches.first().id)
                    }
                    autoWallpaperPreferences.copy(
                        savedSearchEnabled = savedSearchEnabled,
                        savedSearchIds = newSavedSearchIds,
                    )
                }
                WallpaperTarget.LOCKSCREEN -> {
                    val newSavedSearchIds = autoWallpaperPreferences.lsSavedSearchIds.ifEmpty {
                        setOf(savedSearches.first().id)
                    }
                    autoWallpaperPreferences.copy(
                        lsSavedSearchEnabled = savedSearchEnabled,
                        lsSavedSearchIds = newSavedSearchIds,
                    )
                }
            },
        )
    }

    fun updateAutoWallpaperSavedSearchIds(
        savedSearchIds: Set<Long>,
        target: WallpaperTarget,
    ) {
        val autoWallpaperPreferences = uiState.value.appPreferences.autoWallpaperPreferences
        updateAutoWallpaperPrefs(
            when (target) {
                WallpaperTarget.HOME -> autoWallpaperPreferences.copy(
                    savedSearchIds = savedSearchIds,
                )
                WallpaperTarget.LOCKSCREEN -> autoWallpaperPreferences.copy(
                    lsSavedSearchIds = savedSearchIds,
                )
            },
        )
    }

    fun updateAutoWallpaperFavoritesEnabled(
        favoritesEnabled: Boolean,
        target: WallpaperTarget,
    ) {
        val autoWallpaperPreferences = uiState.value.appPreferences.autoWallpaperPreferences
        updateAutoWallpaperPrefs(
            when (target) {
                WallpaperTarget.HOME -> autoWallpaperPreferences.copy(
                    favoritesEnabled = favoritesEnabled,
                )
                WallpaperTarget.LOCKSCREEN -> autoWallpaperPreferences.copy(
                    lsFavoritesEnabled = favoritesEnabled,
                )
            },
        )
    }

    fun updateAutoWallpaperLocalEnabled(
        localEnabled: Boolean,
        target: WallpaperTarget,
    ) {
        val state = uiState.value
        val autoWallpaperPreferences = state.appPreferences.autoWallpaperPreferences
        val localDirs = state.localDirectories
        updateAutoWallpaperPrefs(
            when (target) {
                WallpaperTarget.HOME -> {
                    val newUris = autoWallpaperPreferences.localDirs.ifEmpty {
                        setOf(localDirs.first().uri)
                    }
                    autoWallpaperPreferences.copy(
                        localEnabled = localEnabled,
                        localDirs = newUris,
                    )
                }
                WallpaperTarget.LOCKSCREEN -> {
                    val newUris = autoWallpaperPreferences.lsLocalDirs.ifEmpty {
                        setOf(localDirs.first().uri)
                    }
                    autoWallpaperPreferences.copy(
                        lsLocalEnabled = localEnabled,
                        lsLocalDirs = newUris,
                    )
                }
            },
        )
    }

    fun updateAutoWallpaperSelectedLocalDirs(
        localDirs: Set<Uri>,
        target: WallpaperTarget,
    ) {
        val autoWallpaperPreferences = uiState.value.appPreferences.autoWallpaperPreferences
        updateAutoWallpaperPrefs(
            when (target) {
                WallpaperTarget.HOME -> autoWallpaperPreferences.copy(
                    localDirs = localDirs,
                )
                WallpaperTarget.LOCKSCREEN -> autoWallpaperPreferences.copy(
                    lsLocalDirs = localDirs,
                )
            },
        )
    }

    fun updateAutoWallpaperTargets(targets: Set<WallpaperTarget>) {
        val autoWallpaperPreferences = uiState.value.appPreferences.autoWallpaperPreferences
        updateAutoWallpaperPrefs(
            autoWallpaperPreferences.copy(
                targets = targets,
            ),
        )
    }

    fun updateAutoWallpaperFreq(
        useSameFreq: Boolean,
        frequency: DateTimePeriod,
        lsFrequency: DateTimePeriod,
    ) {
        val autoWallpaperPreferences = uiState.value.appPreferences.autoWallpaperPreferences
        updateAutoWallpaperPrefs(
            autoWallpaperPreferences.copy(
                useSameFreq = useSameFreq,
                frequency = frequency,
                lsFrequency = lsFrequency,
            ),
        )
    }

    fun updateAutoWallpaperConstraints(constraints: Constraints) {
        val autoWallpaperPreferences = uiState.value.appPreferences.autoWallpaperPreferences
        updateAutoWallpaperPrefs(
            autoWallpaperPreferences.copy(
                constraints = constraints,
            ),
        )
    }

    fun updateAutoWallpaperEnabled(
        enabled: Boolean,
    ) = viewModelScope.launch {
        val autoWallpaperPreferences = uiState.value.appPreferences.autoWallpaperPreferences
        updateAutoWallpaperPrefs(
            context = application,
            appPreferencesRepository = appPreferencesRepository,
            prevAppPreferences = uiState.value.appPreferences,
            newAutoWallpaperPreferences = autoWallpaperPreferences.copy(
                enabled = enabled,
            ),
        )
    }

    internal fun updateAutoWallpaperPrefs(
        autoWallpaperPreferences: AutoWallpaperPreferences,
    ) = viewModelScope.launch {
        updateAutoWallpaperPrefs(
            context = application,
            appPreferencesRepository = appPreferencesRepository,
            prevAppPreferences = uiState.value.appPreferences,
            newAutoWallpaperPreferences = autoWallpaperPreferences.copy(
                enabled = autoWallpaperPreferences.anySourceEnabled,
            ),
        )
    }

    fun updateAcraEnabled(enabled: Boolean) = viewModelScope.launch {
        appPreferencesRepository.updateAcraEnabled(enabled)
        // need to restart to init acra
        localUiStateFlow.update {
            it.copy(showRestartDialog = partial(enabled))
        }
    }
}

@Stable
@Partialize
data class SettingsUiState(
    val appPreferences: AppPreferences = AppPreferences(),
    val objectDetectionModels: ImmutableList<ObjectDetectionModelEntity> = persistentListOf(),
    val selectedModel: ObjectDetectionModel = ObjectDetectionModel.DEFAULT,
    val showObjectDetectionDelegateOptions: Boolean = false,
    val showObjectDetectionModelOptions: Boolean = false,
    val editModel: ObjectDetectionModelEntity? = null,
    val showEditModelDialog: Boolean = false,
    val modelDownloadStatus: DownloadStatus? = null,
    val deleteModel: ObjectDetectionModelEntity? = null,
    val showSavedSearches: Boolean = false,
    val savedSearches: ImmutableList<SavedSearch> = persistentListOf(),
    val editSavedSearch: SavedSearch? = null,
    val deleteSavedSearch: SavedSearch? = null,
    val autoWallpaperSavedSearches: ImmutableList<SavedSearch> = persistentListOf(),
    val showAutoWallpaperFrequencyDialog: Boolean = false,
    val showAutoWallpaperConstraintsDialog: Boolean = false,
    val showPermissionRationaleDialog: Boolean = false,
    val tempAutoWallpaperPreferences: AutoWallpaperPreferences? = null,
    val autoWallpaperNextRun: NextRun = NextRun.NotScheduled,
    val lsAutoWallpaperNextRun: NextRun = NextRun.NotScheduled,
    val showAutoWallpaperNextRunInfoDialog: Boolean = false,
    val autoWallpaperStatus: AutoWallpaperWorker.Companion.Status? = null,
    val showThemeOptionsDialog: Boolean = false,
    val showAutoWallpaperSetToDialog: Boolean = false,
    val localDirectories: ImmutableList<LocalDirectory> = persistentListOf(),
    val showTagsWriteTypeDialog: Boolean = false,
    val showClearViewedWallpapersConfirmDialog: Boolean = false,
    val showChangeDownloadLocationDialog: Boolean = false,
    val hasLightDarkWallpapers: Boolean = false,
    val hasFavorites: Boolean = false,
    val homeScreenAutoWallpaperSources: AutoWallpaperSources = AutoWallpaperSources(),
    val lockScreenAutoWallpaperSources: AutoWallpaperSources = AutoWallpaperSources(),
    val showRestartDialog: Boolean = false,
)

sealed class NextRun {
    data object NotScheduled : NextRun()
    data object Running : NextRun()

    @Stable
    data class NextRunTime(val instant: Instant) : NextRun()
}

data class AutoWallpaperSources(
    val savedSearchEnabled: Boolean = false,
    val savedSearchIds: Set<Long> = emptySet(),
    val favoritesEnabled: Boolean = false,
    val localEnabled: Boolean = false,
    val localDirs: Set<Uri> = emptySet(),
    val lightDarkEnabled: Boolean = false,
    val useDarkWithExtraDim: Boolean = false,
)
