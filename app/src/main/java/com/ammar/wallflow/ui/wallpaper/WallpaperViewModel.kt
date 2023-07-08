package com.ammar.wallflow.ui.wallpaper

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ammar.wallflow.data.repository.WallHavenRepository
import com.ammar.wallflow.data.repository.utils.Resource
import com.ammar.wallflow.data.repository.utils.successOr
import com.ammar.wallflow.extensions.TAG
import com.ammar.wallflow.extensions.getFileNameFromUrl
import com.ammar.wallflow.extensions.getTempFileIfExists
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.utils.DownloadManager
import com.ammar.wallflow.utils.DownloadManager.Companion.DownloadLocation
import com.ammar.wallflow.utils.DownloadStatus
import com.ammar.wallflow.workers.DownloadWorker.Companion.NotificationType
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WallpaperViewModel @Inject constructor(
    private val application: Application,
    wallHavenRepository: WallHavenRepository,
    private val savedStateHandle: SavedStateHandle,
    private val downloadManager: DownloadManager,
) : AndroidViewModel(application) {
    private val wallpaperIdKey = "wallpaperId"
    private val thumbUrlKey = "thumbUrl"
    private val wallpaperIdFlow: StateFlow<String?> = savedStateHandle.getStateFlow(
        wallpaperIdKey,
        null,
    )
    private val thumbUrlFlow: StateFlow<String?> = savedStateHandle.getStateFlow(
        thumbUrlKey,
        null,
    )
    private val _uiState = MutableStateFlow(WallpaperUiState())
    val uiState: StateFlow<WallpaperUiState> = _uiState.asStateFlow()
    private val wallpaperFlow = wallpaperIdFlow.flatMapLatest {
        if (it == null) {
            flowOf(Resource.Success(null))
        } else {
            _uiState.update { state ->
                state.copy(
                    loading = true,
                    wallpaper = null,
                )
            }
            wallHavenRepository.wallpaper(it)
        }
    }

    init {
        viewModelScope.launch {
            combine(wallpaperFlow, thumbUrlFlow) { resource, thumbUrl ->
                Pair(resource, thumbUrl)
            }.collectLatest { (resource, thumbUrl) ->
                _uiState.update {
                    it.copy(
                        loading = false,
                        wallpaper = resource.successOr(null),
                        thumbUrl = thumbUrl,
                    )
                }
            }
        }
    }

    fun onWallpaperTap() = _uiState.update {
        it.copy(
            systemBarsVisible = !it.systemBarsVisible,
            actionsVisible = !it.actionsVisible,
        )
    }

    fun onWallpaperTransform() = _uiState.update {
        it.copy(
            systemBarsVisible = false,
            actionsVisible = false,
        )
    }

    fun showInfo(show: Boolean = true) = _uiState.update { it.copy(showInfo = show) }

    fun download() = viewModelScope.launch {
        uiState.value.wallpaper?.run {
            val workName = downloadManager.requestDownload(application, this)
            downloadManager.getProgress(application, workName).collectLatest { state ->
                _uiState.update { it.copy(downloadStatus = state) }
            }
        }
    }

    fun downloadForSharing(onResult: (file: File?) -> Unit) {
        uiState.value.wallpaper?.run {
            try {
                _uiState.update { it.copy(loading = true) }
                val fileName = path.getFileNameFromUrl()
                val fileIfExists = application.getTempFileIfExists(fileName)
                if (fileIfExists != null) {
                    _uiState.update { it.copy(loading = false) }
                    onResult(fileIfExists)
                    return
                }
                val workName = downloadManager.requestDownload(
                    context = application,
                    wallpaper = this,
                    downloadLocation = DownloadLocation.APP_TEMP,
                    notificationType = NotificationType.SILENT,
                )
                viewModelScope.launch {
                    downloadManager.getProgress(application, workName).collectLatest { state ->
                        if (!state.isSuccessOrFail()) return@collectLatest
                        _uiState.update { it.copy(loading = false) }
                        // if (state is DownloadStatus.Failed) {
                        //     application.toast("Download failed: ${state.e?.message ?: "Unknown reason"}")
                        // }
                        onResult(
                            if (state is DownloadStatus.Failed) null
                            else application.getTempFileIfExists(fileName)
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "shareImage: ", e)
                _uiState.update { it.copy(loading = false) }
                onResult(null)
            }
        }
    }

    fun showPermissionRationaleDialog(show: Boolean = true) = _uiState.update {
        it.copy(showPermissionRationaleDialog = show)
    }

    fun setWallpaperId(wallpaperId: String?, thumbUrl: String?) {
        savedStateHandle.run {
            set(thumbUrlKey, thumbUrl)
            set(wallpaperIdKey, wallpaperId)
        }
    }
}

data class WallpaperUiState(
    val wallpaper: Wallpaper? = null,
    val thumbUrl: String? = null,
    val systemBarsVisible: Boolean = true,
    val actionsVisible: Boolean = true,
    val showInfo: Boolean = false,
    val downloadStatus: DownloadStatus? = null,
    val loading: Boolean = true,
    val showPermissionRationaleDialog: Boolean = false,
)
