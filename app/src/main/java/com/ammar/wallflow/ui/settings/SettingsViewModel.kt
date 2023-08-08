package com.ammar.wallflow.ui.settings

import android.app.Application
import android.util.Log
import androidx.compose.runtime.Stable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.ammar.wallflow.EFFICIENT_DET_LITE_0_MODEL_NAME
import com.ammar.wallflow.R
import com.ammar.wallflow.data.db.entity.ObjectDetectionModelEntity
import com.ammar.wallflow.data.db.entity.toModel
import com.ammar.wallflow.data.db.entity.toSavedSearch
import com.ammar.wallflow.data.preferences.AppPreferences
import com.ammar.wallflow.data.preferences.AutoWallpaperPreferences
import com.ammar.wallflow.data.preferences.LookAndFeelPreferences
import com.ammar.wallflow.data.preferences.ObjectDetectionPreferences
import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.ammar.wallflow.data.repository.ObjectDetectionModelRepository
import com.ammar.wallflow.data.repository.SavedSearchRepository
import com.ammar.wallflow.extensions.TAG
import com.ammar.wallflow.extensions.getMLModelsFileIfExists
import com.ammar.wallflow.extensions.workManager
import com.ammar.wallflow.model.ObjectDetectionModel
import com.ammar.wallflow.model.SavedSearch
import com.ammar.wallflow.utils.DownloadManager
import com.ammar.wallflow.utils.DownloadStatus
import com.ammar.wallflow.workers.AutoWallpaperWorker
import com.ammar.wallflow.workers.DownloadWorker
import com.github.materiiapps.partial.Partialize
import com.github.materiiapps.partial.partial
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val application: Application,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val objectDetectionModelRepository: ObjectDetectionModelRepository,
    private val downloadManager: DownloadManager,
    private val savedSearchRepository: SavedSearchRepository,
) : AndroidViewModel(application) {
    private val localUiStateFlow = MutableStateFlow(SettingsUiStatePartial())
    private val autoWallpaperNextRunFlow = getAutoWallpaperNextRun()

    val uiState = combine(
        appPreferencesRepository.appPreferencesFlow,
        objectDetectionModelRepository.getAll(),
        localUiStateFlow,
        savedSearchRepository.getAll(),
        autoWallpaperNextRunFlow,
    ) { appPreferences, objectDetectionModels, localUiState, savedSearches, autoWallpaperNextRun ->
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
        localUiState.merge(
            SettingsUiState(
                appPreferences = appPreferences,
                objectDetectionModels = objectDetectionModels,
                selectedModel = selectedModel,
                savedSearches = allSavedSearches,
                autoWallpaperSavedSearch = allSavedSearches.find {
                    appPreferences.autoWallpaperPreferences.savedSearchId == it.id
                },
                autoWallpaperNextRun = autoWallpaperNextRun,
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
                    existing?.fileName?.run {
                        // if this model has an existing file, delete it
                        application.getMLModelsFileIfExists(this)?.delete()
                    }
                    viewModelScope.launch {
                        try {
                            // save the new file
                            val fileName = File(modelPath).name
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
                            onDone(e)
                        }
                    }
                }
            }
            return@launch
        }
        objectDetectionModelRepository.addOrUpdate(existing.copy(name = model.name))
        onDone(null)
    }

    private suspend fun downloadModel(
        name: String,
        url: String,
        onDone: (status: DownloadStatus) -> Unit,
    ) {
        val workName = downloadManager.requestDownload(
            context = application,
            url = url,
            downloadLocation = DownloadManager.Companion.DownloadLocation.APP_ML_MODELS,
            notificationType = DownloadWorker.Companion.NotificationType.VISIBLE,
            notificationTitle = application.getString(R.string.model_download_title, name),
            inferFileNameFromResponse = true,
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
            savedSearchRepository.addOrUpdateSavedSearch(savedSearch)
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
                savedSearchRepository.deleteSavedSearch(savedSearch)
                // close the dialog
                localUiStateFlow.update { it.copy(deleteSavedSearch = partial(null)) }
            }
            return
        }
        // show the dialog
        localUiStateFlow.update { it.copy(deleteSavedSearch = partial(savedSearch)) }
    }

    fun updateAutoWallpaperPrefs(autoWallpaperPreferences: AutoWallpaperPreferences) {
        if (autoWallpaperPreferences.enabled && autoWallpaperPreferences.savedSearchId <= 0) {
            localUiStateFlow.update {
                it.copy(
                    tempAutoWallpaperPreferences = partial(autoWallpaperPreferences),
                    showAutoWallpaperSavedSearchesDialog = partial(true),
                )
            }
            return
        }
        viewModelScope.launch {
            appPreferencesRepository.updateAutoWallpaperPrefs(autoWallpaperPreferences)
            if (autoWallpaperPreferences.enabled) {
                // only reschedule if enabled or frequency or constraints change
                val currentPrefs = uiState.value.appPreferences.autoWallpaperPreferences
                if (
                    currentPrefs.enabled &&
                    currentPrefs.frequency == autoWallpaperPreferences.frequency &&
                    currentPrefs.constraints == autoWallpaperPreferences.constraints
                ) {
                    return@launch
                }
                // schedule worker with updated preferences
                AutoWallpaperWorker.schedule(
                    context = application,
                    constraints = autoWallpaperPreferences.constraints,
                    interval = autoWallpaperPreferences.frequency,
                    appPreferencesRepository = appPreferencesRepository,
                )
            } else {
                // stop the worker
                AutoWallpaperWorker.stop(
                    context = application,
                    appPreferencesRepository = appPreferencesRepository,
                )
            }
        }
    }

    fun showAutoWallpaperSavedSearchesDialog(show: Boolean) = localUiStateFlow.update {
        it.copy(showAutoWallpaperSavedSearchesDialog = partial(show))
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
        AutoWallpaperWorker.triggerImmediate(application)
        viewModelScope.launch {
            AutoWallpaperWorker.getProgress(
                application,
                AutoWallpaperWorker.IMMEDIATE_WORK_NAME,
            ).collectLatest { status ->
                localUiStateFlow.update { it.copy(autoWallpaperStatus = partial(status)) }
                if (status.isSuccessOrFail()) {
                    // clear status after success or failure
                    delay(2000)
                    localUiStateFlow.update { it.copy(autoWallpaperStatus = partial(null)) }
                }
            }
        }
    }

    fun showAutoWallpaperNextRunInfoDialog(show: Boolean) = localUiStateFlow.update {
        it.copy(showAutoWallpaperNextRunInfoDialog = partial(show))
    }

    fun showThemeOptionsDialog(show: Boolean) = localUiStateFlow.update {
        it.copy(showThemeOptionsDialog = partial(show))
    }

    fun updateLookAndFeelPrefs(lookAndFeelPreferences: LookAndFeelPreferences) =
        viewModelScope.launch {
            appPreferencesRepository.updateLookAndFeelPreferences(lookAndFeelPreferences)
        }

    private fun getAutoWallpaperNextRun() = application.workManager.getWorkInfosForUniqueWorkFlow(
        AutoWallpaperWorker.PERIODIC_WORK_NAME,
    ).map {
        val info = it.firstOrNull() ?: return@map NextRun.NotScheduled
        return@map when (info.state) {
            WorkInfo.State.ENQUEUED -> {
                NextRun.NextRunTime(Instant.fromEpochMilliseconds(info.nextScheduleTimeMillis))
            }
            WorkInfo.State.RUNNING -> NextRun.Running
            else -> NextRun.NotScheduled
        }
    }
}

@Partialize
data class SettingsUiState(
    val appPreferences: AppPreferences = AppPreferences(),
    val objectDetectionModels: List<ObjectDetectionModelEntity> = emptyList(),
    val selectedModel: ObjectDetectionModel = ObjectDetectionModel.DEFAULT,
    val showObjectDetectionDelegateOptions: Boolean = false,
    val showObjectDetectionModelOptions: Boolean = false,
    val editModel: ObjectDetectionModelEntity? = null,
    val showEditModelDialog: Boolean = false,
    val modelDownloadStatus: DownloadStatus? = null,
    val deleteModel: ObjectDetectionModelEntity? = null,
    val showSavedSearches: Boolean = false,
    val savedSearches: List<SavedSearch> = emptyList(),
    val editSavedSearch: SavedSearch? = null,
    val deleteSavedSearch: SavedSearch? = null,
    val autoWallpaperSavedSearch: SavedSearch? = null,
    val showAutoWallpaperSavedSearchesDialog: Boolean = false,
    val showAutoWallpaperFrequencyDialog: Boolean = false,
    val showAutoWallpaperConstraintsDialog: Boolean = false,
    val showPermissionRationaleDialog: Boolean = false,
    val tempAutoWallpaperPreferences: AutoWallpaperPreferences? = null,
    val autoWallpaperNextRun: NextRun = NextRun.NotScheduled,
    val showAutoWallpaperNextRunInfoDialog: Boolean = false,
    val autoWallpaperStatus: AutoWallpaperWorker.Companion.Status? = null,
    val showThemeOptionsDialog: Boolean = false,
)

sealed class NextRun {
    object NotScheduled : NextRun()
    object Running : NextRun()

    @Stable
    data class NextRunTime(val instant: Instant) : NextRun()
}
