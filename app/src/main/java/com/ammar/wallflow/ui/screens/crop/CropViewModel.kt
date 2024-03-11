package com.ammar.wallflow.ui.screens.crop

import android.app.Application
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Handler
import android.util.Log
import android.view.Display
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ammar.wallflow.R
import com.ammar.wallflow.data.db.entity.toModel
import com.ammar.wallflow.data.preferences.ObjectDetectionPreferences
import com.ammar.wallflow.data.preferences.Theme
import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.ammar.wallflow.data.repository.ObjectDetectionModelRepository
import com.ammar.wallflow.data.repository.utils.Resource
import com.ammar.wallflow.extensions.TAG
import com.ammar.wallflow.extensions.displayManager
import com.ammar.wallflow.extensions.getMLModelsFileIfExists
import com.ammar.wallflow.extensions.getScreenResolution
import com.ammar.wallflow.extensions.setWallpaper
import com.ammar.wallflow.extensions.toast
import com.ammar.wallflow.model.DetectionWithBitmap
import com.ammar.wallflow.model.ObjectDetectionModel
import com.ammar.wallflow.model.WallpaperTarget
import com.ammar.wallflow.utils.DownloadManager
import com.ammar.wallflow.utils.DownloadManager.Companion.DownloadLocation
import com.ammar.wallflow.utils.DownloadStatus
import com.ammar.wallflow.utils.objectdetection.detectObjects as actualDetectObjects
import com.ammar.wallflow.workers.DownloadWorker.Companion.NotificationType
import com.github.materiiapps.partial.Partialize
import com.github.materiiapps.partial.partial
import com.mr0xf00.easycrop.CropResult
import com.mr0xf00.easycrop.ImageCropper
import com.mr0xf00.easycrop.crop
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CropViewModel(
    private val application: Application,
    private val uri: Uri,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val objectDetectionModelRepository: ObjectDetectionModelRepository,
    private val downloadManager: DownloadManager,
    private val ioDispatcher: CoroutineDispatcher,
) : AndroidViewModel(application) {
    val imageCropper: ImageCropper by lazy { ImageCropper() }
    private val localUiStateFlow = MutableStateFlow(CropUiStatePartial())
    private val modelDownloadStatusFlow = MutableStateFlow<DownloadStatus?>(null)
    private val downloadedModelFlow = MutableStateFlow<File?>(null)
    private val appPreferencesFlow = appPreferencesRepository.appPreferencesFlow
    private val displaysFlow = callbackFlow {
        val displayManager = application.displayManager
        val displays = displayManager.displays
            .associateBy { it.displayId }
            .toMutableMap()

        val listener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) {
                displays[displayId] = displayManager.getDisplay(displayId) ?: return
                trySend(displays.values.toList())
            }

            override fun onDisplayChanged(displayId: Int) {
                displays[displayId] = application.displayManager.getDisplay(displayId) ?: return
                trySend(displays.values.toList())
            }

            override fun onDisplayRemoved(displayId: Int) {
                displays.remove(displayId)
                trySend(displays.values.toList())
            }
        }

        trySend(displays.values.toList())
        val handler = Handler(application.mainLooper)
        displayManager.registerDisplayListener(listener, handler)

        awaitClose {
            displayManager.unregisterDisplayListener(listener)
        }
    }

    val uiState = combine(
        localUiStateFlow,
        appPreferencesFlow,
        displaysFlow,
    ) { localUiState, appPreferences, displays ->
        val objectDetectionPreferences = appPreferences.objectDetectionPreferences
        localUiState.merge(
            CropUiState(
                uri = uri,
                objectDetectionPreferences = objectDetectionPreferences,
                theme = appPreferences.lookAndFeelPreferences.theme,
                displays = displays,
                selectedDisplay = displays.firstOrNull(),
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CropUiState(),
    )

    private val detectionState = combine(
        appPreferencesFlow,
        downloadedModelFlow,
    ) { appPreferences, downloadedModel ->
        val objectDetectionPreferences = appPreferences.objectDetectionPreferences
        DetectionState(
            objectDetectionPreferences = objectDetectionPreferences,
            objectDetectionModelFile = downloadedModel,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DetectionState(),
    )

    init {
        viewModelScope.launch { setUri(uri) }
        viewModelScope.launch {
            detectionState.collectLatest {
                try {
                    initObjectDetection(
                        preferences = it.objectDetectionPreferences,
                        uri = uri,
                        modelFile = it.objectDetectionModelFile,
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "detection error: ", e)
                }
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
        localUiStateFlow.update {
            it.copy(
                detectedObjects = partial(Resource.Loading(emptyList())),
            )
        }
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
                    modelId = objectDetectionModelRepository.getByName(objectDetectionModel.name)
                        ?.id
                        ?: 0,
                ),
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

    private fun downloadModel(objectDetectionModel: ObjectDetectionModel) = viewModelScope.launch {
        val workName = downloadManager.requestDownload(
            context = application,
            url = objectDetectionModel.url,
            downloadLocation = DownloadLocation.APP_ML_MODELS,
            notificationType = NotificationType.VISIBLE,
            notificationTitle = application.getString(
                R.string.model_download_title,
                objectDetectionModel.name,
            ),
            fileName = objectDetectionModel.fileName,
        )
        downloadManager.getProgress(application, workName).collectLatest { state ->
            modelDownloadStatusFlow.update { state }
            localUiStateFlow.update { it.copy(modelDownloadStatus = partial(state)) }
            if (!state.isSuccessOrFail()) return@collectLatest
            if (state is DownloadStatus.Failed) {
                return@collectLatest
            }
            if (state is DownloadStatus.Success) {
                val modelPath = state.filePath
                if (modelPath == null) {
                    Log.e(TAG, "model file path null")
                    return@collectLatest
                }
                downloadedModelFlow.update { modelPath.toUri().toFile() }
            }
        }
    }

    private suspend fun setUri(uri: Uri) {
        val result = imageCropper.crop(
            uri = uri,
            context = application,
            maxResultSize = application.getScreenResolution(
                true,
                uiState.value.selectedDisplay?.displayId ?: Display.DEFAULT_DISPLAY,
            ),
            cacheBeforeUse = false,
        )
        val state = uiState.value
        val cropRect = if (!state.crop) {
            if (state.imageSize == Size.Unspecified) {
                return
            } else {
                state.imageSize.toRect()
            }
        } else {
            state.lastCropRegion
        }
        if (result is CropResult.Cancelled || cropRect == null) {
            localUiStateFlow.update { it.copy(result = partial(Result.Cancelled)) }
            return
        }
        val success = result as CropResult.Success
        localUiStateFlow.update { it.copy(result = partial(Result.Pending(success.bitmap))) }
        val display = state.displays.find {
            it.displayId == state.selectedDisplay?.displayId
        } ?: application.displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        val applied = application.setWallpaper(
            display = display,
            uri = uri,
            cropRect = cropRect,
            targets = state.wallpaperTargets,
        )
        application.toast(
            application.getString(
                if (applied) {
                    R.string.wallpaper_changed
                } else {
                    R.string.failed_to_change_wallpaper
                },
            ),
        )
        localUiStateFlow.update { it.copy(result = partial(Result.Success(result))) }
    }

    private fun detectObjects(
        uri: Uri,
        model: File,
        objectDetectionPreferences: ObjectDetectionPreferences,
    ) = viewModelScope.launch(ioDispatcher) {
        try {
            localUiStateFlow.update {
                it.copy(
                    detectedObjects = partial(Resource.Loading(emptyList())),
                    selectedDetection = partial(null),
                )
            }
            val (scale, detectionWithBitmaps) = actualDetectObjects(
                context = application,
                uri = uri,
                model = model,
                objectDetectionPreferences = objectDetectionPreferences,
            )
            localUiStateFlow.update {
                it.copy(
                    detectedRectScale = partial(scale),
                    detectedObjects = partial(Resource.Success(detectionWithBitmaps)),
                    selectedDetection = partial(detectionWithBitmaps.firstOrNull()),
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "detectObjects: ", e)
            localUiStateFlow.update {
                it.copy(
                    detectedObjects = partial(Resource.Error(e)),
                    selectedDetection = partial(null),
                )
            }
        }
    }

    fun showDetections(show: Boolean) = localUiStateFlow.update {
        it.copy(showDetections = partial(show))
    }

    fun selectDetection(detection: DetectionWithBitmap) = localUiStateFlow.update {
        it.copy(selectedDetection = partial(detection))
    }

    fun setLastCropRegion(region: Rect) = localUiStateFlow.update {
        it.copy(lastCropRegion = partial(region))
    }

    fun removeSelectedDetectionAndUpdateRegion(region: Rect) = localUiStateFlow.update {
        it.copy(
            selectedDetection = partial(null),
            lastCropRegion = partial(region),
        )
    }

    fun setWallpaperTargets(wallpaperTargets: Set<WallpaperTarget>) = localUiStateFlow.update {
        it.copy(wallpaperTargets = partial(wallpaperTargets))
    }

    fun setSelectedDisplay(display: Display) = localUiStateFlow.update {
        it.copy(selectedDisplay = partial(display))
    }

    fun onCropChange(crop: Boolean) {
        localUiStateFlow.update {
            it.copy(
                crop = partial(crop),
                lastCropRegion = if (!crop) {
                    partial(null)
                } else {
                    it.lastCropRegion
                },
            )
        }
        imageCropper.cropState?.enabled = crop
    }

    fun setImageSize(imageSize: Size) = localUiStateFlow.update {
        it.copy(imageSize = partial(imageSize))
    }

    companion object {
        fun getFactory(
            uri: Uri,
            appPreferencesRepository: AppPreferencesRepository,
            objectDetectionModelRepository: ObjectDetectionModelRepository,
            downloadManager: DownloadManager,
            ioDispatcher: CoroutineDispatcher,
        ) = viewModelFactory {
            initializer {
                CropViewModel(
                    application = this[APPLICATION_KEY] as Application,
                    uri = uri,
                    appPreferencesRepository = appPreferencesRepository,
                    objectDetectionModelRepository = objectDetectionModelRepository,
                    downloadManager = downloadManager,
                    ioDispatcher = ioDispatcher,
                )
            }
        }
    }
}

@Partialize
data class CropUiState(
    val uri: Uri? = null,
    val objectDetectionPreferences: ObjectDetectionPreferences = ObjectDetectionPreferences(
        enabled = false,
    ),
    val modelDownloadStatus: DownloadStatus? = null,
    val detectedObjects: Resource<List<DetectionWithBitmap>>? = null,
    val detectedRectScale: Int = 1,
    val selectedDetection: DetectionWithBitmap? = null,
    val showDetections: Boolean = false,
    val lastCropRegion: Rect? = null,
    val result: Result = Result.NotStarted,
    val wallpaperTargets: Set<WallpaperTarget> = setOf(
        WallpaperTarget.HOME,
        WallpaperTarget.LOCKSCREEN,
    ),
    val theme: Theme = Theme.SYSTEM,
    val displays: List<Display> = emptyList(),
    val selectedDisplay: Display? = null,
    val crop: Boolean = true,
    val imageSize: Size = Size.Unspecified,
)

data class DetectionState(
    val objectDetectionPreferences: ObjectDetectionPreferences = ObjectDetectionPreferences(
        enabled = false,
    ),
    val objectDetectionModelFile: File? = null,
)

sealed class Result {
    data object NotStarted : Result()
    data object Cancelled : Result()
    data class Pending(val bitmap: ImageBitmap) : Result()
    data class Success(val result: CropResult) : Result()
}
