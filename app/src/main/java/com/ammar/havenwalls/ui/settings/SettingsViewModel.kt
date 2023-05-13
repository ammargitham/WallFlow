package com.ammar.havenwalls.ui.settings

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ammar.havenwalls.EFFICIENT_DET_LITE_0_MODEL_NAME
import com.ammar.havenwalls.data.db.entity.ObjectDetectionModelEntity
import com.ammar.havenwalls.data.db.entity.toModel
import com.ammar.havenwalls.data.preferences.AppPreferences
import com.ammar.havenwalls.data.preferences.ObjectDetectionPreferences
import com.ammar.havenwalls.data.repository.AppPreferencesRepository
import com.ammar.havenwalls.data.repository.ObjectDetectionModelRepository
import com.ammar.havenwalls.extensions.TAG
import com.ammar.havenwalls.extensions.getMLModelsFileIfExists
import com.ammar.havenwalls.model.ObjectDetectionModel
import com.ammar.havenwalls.ui.common.UiStateViewModel
import com.ammar.havenwalls.utils.DownloadManager
import com.ammar.havenwalls.utils.DownloadStatus
import com.ammar.havenwalls.workers.DownloadWorker
import com.github.materiiapps.partial.Partialize
import com.github.materiiapps.partial.partial
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val application: Application,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val objectDetectionModelRepository: ObjectDetectionModelRepository,
    private val downloadManager: DownloadManager,
) : AndroidViewModel(application), UiStateViewModel<SettingsUiState> {
    private val localUiStateFlow = MutableStateFlow(SettingsUiStatePartial())

    override val uiState = combine(
        appPreferencesRepository.appPreferencesFlow,
        objectDetectionModelRepository.getAll(),
        localUiStateFlow,
    ) { appPreferences, objectDetectionModels, localUiState ->
        val selectedModelId = appPreferences.objectDetectionPreferences.modelId
        val selectedModel = if (selectedModelId == 0L) ObjectDetectionModel.DEFAULT else {
            objectDetectionModels
                .find { it.id == selectedModelId }
                ?.toModel()
                ?: ObjectDetectionModel.DEFAULT
        }
        localUiState.merge(
            SettingsUiState(
                appPreferences = appPreferences,
                objectDetectionModels = objectDetectionModels,
                selectedModel = selectedModel,
            )
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
        } else null
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
                                ) ?: model.copy(fileName = fileName)
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
            notificationTitle = "ML model: $name",
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
                EFFICIENT_DET_LITE_0_MODEL_NAME
            )?.id ?: 0
            appPreferencesRepository.updateObjectDetectionPrefs(
                objectDetectionPreferences.copy(modelId = defaultModelId)
            )
        }
        // hide the confirm dialog
        localUiStateFlow.update { it.copy(deleteModel = partial(null)) }
    }

    fun setSelectedModel(entity: ObjectDetectionModelEntity?) = viewModelScope.launch {
        val modelId = entity?.id ?: objectDetectionModelRepository.getByName(
            EFFICIENT_DET_LITE_0_MODEL_NAME
        )?.id ?: 0
        val objectDetectionPreferences = uiState.value.appPreferences.objectDetectionPreferences
        appPreferencesRepository.updateObjectDetectionPrefs(
            objectDetectionPreferences.copy(modelId = modelId)
        )
        showObjectDetectionModelOptions(false)
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
)
