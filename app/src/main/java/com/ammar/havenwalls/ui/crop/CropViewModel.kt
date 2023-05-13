package com.ammar.havenwalls.ui.crop

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ammar.havenwalls.activities.setwallpaper.SetWallpaperActivity.Companion.EXTRA_URI
import com.ammar.havenwalls.data.db.entity.toModel
import com.ammar.havenwalls.data.preferences.ObjectDetectionPreferences
import com.ammar.havenwalls.data.repository.AppPreferencesRepository
import com.ammar.havenwalls.data.repository.ObjectDetectionModelRepository
import com.ammar.havenwalls.data.repository.utils.Resource
import com.ammar.havenwalls.extensions.TAG
import com.ammar.havenwalls.extensions.getMLModelsFileIfExists
import com.ammar.havenwalls.extensions.getRegion
import com.ammar.havenwalls.extensions.getScreenResolution
import com.ammar.havenwalls.extensions.setWallpaper
import com.ammar.havenwalls.model.DetectionWithBitmap
import com.ammar.havenwalls.model.ObjectDetectionModel
import com.ammar.havenwalls.utils.DownloadManager
import com.ammar.havenwalls.utils.DownloadManager.Companion.DownloadLocation
import com.ammar.havenwalls.utils.DownloadStatus
import com.ammar.havenwalls.utils.decodeSampledBitmapFromUri
import com.ammar.havenwalls.workers.DownloadWorker.Companion.NotificationType
import com.mr0xf00.easycrop.CropResult
import com.mr0xf00.easycrop.ImageCropper
import com.mr0xf00.easycrop.crop
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.core.ComputeSettings
import org.tensorflow.lite.task.vision.detector.ObjectDetector


class CropViewModel(
    private val application: Application,
    savedStateHandle: SavedStateHandle,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val objectDetectionModelRepository: ObjectDetectionModelRepository,
    private val downloadManager: DownloadManager,
    private val ioDispatcher: CoroutineDispatcher,
) : AndroidViewModel(application) {
    val imageCropper: ImageCropper by lazy { ImageCropper() }

    private val _uiState = MutableStateFlow(CropUiState())
    val uiState: StateFlow<CropUiState> = _uiState.asStateFlow()
    private val modelDownloadStatusFlow = MutableStateFlow<DownloadStatus?>(null)
    private val downloadedModelFlow = MutableStateFlow<File?>(null)
    private val uriFlow = savedStateHandle.getStateFlow<Uri?>(EXTRA_URI, null)
    private val appPreferencesFlow = appPreferencesRepository.appPreferencesFlow

    init {
        viewModelScope.launch {
            uriFlow.collectLatest { it?.run { setUri(this) } }
        }
        viewModelScope.launch {
            combine(
                uriFlow,
                appPreferencesFlow,
                downloadedModelFlow,
            ) { uri, appPreferences, downloadedModel ->
                Triple(uri, appPreferences, downloadedModel)
            }.collectLatest { (uri, appPreferences, downloadedModel) ->
                val objectDetectionPreferences = appPreferences.objectDetectionPreferences
                _uiState.update { it.copy(objectDetectionEnabled = objectDetectionPreferences.enabled) }
                initObjectDetection(
                    preferences = objectDetectionPreferences,
                    uri = uri,
                    modelFile = downloadedModel,
                )
            }
        }
    }

    private suspend fun initObjectDetection(
        preferences: ObjectDetectionPreferences,
        uri: Uri?,
        modelFile: File?,
    ) {
        val enabled = preferences.enabled
        val modelDownloadStatus = modelDownloadStatusFlow.value
        if (!enabled || uri == null || modelDownloadStatus is DownloadStatus.Running) {
            return
        }
        _uiState.update { it.copy(detectedObjects = Resource.Loading(emptyList())) }
        if (modelFile != null) {
            // file was downloaded, trigger objectDetection directly
            detectObjects(uri, modelFile, preferences)
            return
        }
        val modelId = preferences.modelId
        val objectDetectionModel = objectDetectionModelRepository.getById(modelId)?.toModel()
            ?: ObjectDetectionModel.DEFAULT
        if (modelId == 0L) {
            // update preferences if no model is currently set
            appPreferencesRepository.updateObjectDetectionPrefs(
                preferences.copy(
                    modelId = objectDetectionModelRepository.getByName(objectDetectionModel.name)?.id
                        ?: 0,
                )
            )
        }
        val fileName = objectDetectionModel.fileName
        val file = application.getMLModelsFileIfExists(fileName)
        if (file == null) {
            downloadModel(objectDetectionModel)
            return
        }
        detectObjects(uri, file, preferences)
    }

    private fun downloadModel(objectDetectionModel: ObjectDetectionModel) {
        val workName = downloadManager.requestDownload(
            context = application,
            url = objectDetectionModel.url,
            downloadLocation = DownloadLocation.APP_ML_MODELS,
            notificationType = NotificationType.VISIBLE,
            notificationTitle = "ML model: ${objectDetectionModel.name}",
            inferFileNameFromResponse = true,
        )
        viewModelScope.launch {
            downloadManager.getProgress(application, workName).collectLatest { state ->
                // Log.d(TAG, "state: $state")
                modelDownloadStatusFlow.update { state }
                _uiState.update { it.copy(modelDownloadStatus = state) }
                if (!state.isSuccessOrFail()) return@collectLatest
                if (state is DownloadStatus.Failed) {
                    // application.toast("Model download failed: ${state.e?.message ?: "Unknown reason"}")
                    return@collectLatest
                }
                if (state is DownloadStatus.Success) {
                    val modelPath = state.filePath
                    if (modelPath == null) {
                        Log.e(TAG, "model file path null")
                        return@collectLatest
                    }
                    downloadedModelFlow.update { File(modelPath) }
                }
            }
        }
    }

    private fun setUri(uri: Uri) {
        if (uiState.value.uri == uri) {
            return
        }
        _uiState.update { it.copy(uri = uri) }
        viewModelScope.launch {
            val result = imageCropper.crop(
                uri = uri,
                context = application,
                maxResultSize = application.getScreenResolution(true).toIntSize(),
                cacheBeforeUse = false,
            )
            if (result is CropResult.Cancelled) {
                _uiState.update { it.copy(result = Result.CANCELLED) }
                return@launch
            }
            val bitmap = (result as CropResult.Success).bitmap.asAndroidBitmap()
            application.setWallpaper(
                bitmap = bitmap,
            )
        }
    }

    private fun detectObjects(
        uri: Uri,
        model: File,
        objectDetectionPreferences: ObjectDetectionPreferences,
    ) = viewModelScope.launch(ioDispatcher) {
        try {
            val objectDetectorOptions by lazy {
                val baseOptions = BaseOptions.builder().apply {
                    when (objectDetectionPreferences.delegate) {
                        ComputeSettings.Delegate.NONE -> {}
                        ComputeSettings.Delegate.NNAPI -> useNnapi()
                        ComputeSettings.Delegate.GPU -> useGpu()
                    }
                }.build()
                ObjectDetector.ObjectDetectorOptions.builder().apply {
                    setBaseOptions(baseOptions)
                    setScoreThreshold(0.2f)
                    setMaxResults(5)
                }.build()
            }
            val objectDetector = ObjectDetector.createFromFileAndOptions(
                model,
                objectDetectorOptions,
            )
            _uiState.update {
                it.copy(
                    detectedObjects = Resource.Loading(emptyList()),
                    selectedDetection = null,
                )
            }
            val (bitmap, scale) = decodeSampledBitmapFromUri(application, uri) ?: return@launch
            val tensorImage = TensorImage.fromBitmap(bitmap.copy(Bitmap.Config.ARGB_8888, true))
            val detections = objectDetector.detect(tensorImage)
            val detectionWithBitmaps = detections.map {
                DetectionWithBitmap(
                    detection = it,
                    bitmap = bitmap.getRegion(it.boundingBox),
                )
            }
            _uiState.update {
                it.copy(
                    detectedRectScale = scale,
                    detectedObjects = Resource.Success(detectionWithBitmaps),
                    selectedDetection = detectionWithBitmaps.firstOrNull(),
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "detectObjects: ", e)
            _uiState.update {
                it.copy(
                    detectedObjects = Resource.Error(e),
                    selectedDetection = null,
                )
            }
        }
    }

    fun showDetections(show: Boolean) = _uiState.update { it.copy(showDetections = show) }

    fun selectDetection(detection: DetectionWithBitmap) = _uiState.update {
        it.copy(selectedDetection = detection)
    }

    fun setLastDetectionCropRegion(region: Rect) = _uiState.update {
        it.copy(lastDetectionCropRegion = region)
    }

    fun removeSelectedDetection() = _uiState.update { it.copy(selectedDetection = null) }

    companion object {
        fun getFactory(
            appPreferencesRepository: AppPreferencesRepository,
            objectDetectionModelRepository: ObjectDetectionModelRepository,
            downloadManager: DownloadManager,
            ioDispatcher: CoroutineDispatcher,
        ) = viewModelFactory {
            initializer {
                CropViewModel(
                    application = this[APPLICATION_KEY] as Application,
                    savedStateHandle = createSavedStateHandle(),
                    appPreferencesRepository = appPreferencesRepository,
                    objectDetectionModelRepository = objectDetectionModelRepository,
                    downloadManager = downloadManager,
                    ioDispatcher = ioDispatcher,
                )
            }
        }
    }
}

data class CropUiState(
    val uri: Uri? = null,
    val objectDetectionEnabled: Boolean = false,
    val modelDownloadStatus: DownloadStatus? = null,
    val detectedObjects: Resource<List<DetectionWithBitmap>> = Resource.Success(emptyList()),
    val detectedRectScale: Int = 1,
    val selectedDetection: DetectionWithBitmap? = null,
    val showDetections: Boolean = false,
    val lastDetectionCropRegion: Rect? = null,
    val result: Result = Result.PENDING,
)

sealed class Result {
    object PENDING : Result()
    object CANCELLED : Result()
    data class SUCCESS(val result: CropResult) : Result()
}
