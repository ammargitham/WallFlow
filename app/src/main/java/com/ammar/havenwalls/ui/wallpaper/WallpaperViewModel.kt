package com.ammar.havenwalls.ui.wallpaper

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ammar.havenwalls.data.repository.WallHavenRepository
import com.ammar.havenwalls.data.repository.utils.Resource
import com.ammar.havenwalls.extensions.TAG
import com.ammar.havenwalls.extensions.getFileNameFromUrl
import com.ammar.havenwalls.extensions.getTempFileIfExists
import com.ammar.havenwalls.model.Wallpaper
import com.ammar.havenwalls.ui.common.UiStateViewModel
import com.ammar.havenwalls.ui.navArgs
import com.ammar.havenwalls.utils.DownloadManager
import com.ammar.havenwalls.utils.DownloadManager.Companion.DownloadLocation
import com.ammar.havenwalls.utils.DownloadStatus
import com.ammar.havenwalls.workers.DownloadWorker.Companion.NotificationType
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


@HiltViewModel
class WallpaperViewModel @Inject constructor(
    private val application: Application,
    wallHavenRepository: WallHavenRepository,
    savedStateHandle: SavedStateHandle,
    private val downloadManager: DownloadManager,
) : AndroidViewModel(application), UiStateViewModel<WallpaperUiState> {
    private val navArgs: WallpaperScreenNavArgs = savedStateHandle.navArgs()
    private val _uiState = MutableStateFlow(WallpaperUiState(navArgs.wallpaperId))
    override val uiState: StateFlow<WallpaperUiState> = _uiState

    init {
        viewModelScope.launch {
            wallHavenRepository.wallpaper(navArgs.wallpaperId).collectLatest { resource ->
                _uiState.update {
                    if (resource !is Resource.Success || resource.data == null) {
                        return@update it.copy(loading = false)
                    }
                    it.copy(
                        loading = false,
                        wallpaper = resource.data
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

    fun showNotificationPermissionRationaleDialog(show: Boolean = true) = _uiState.update {
        it.copy(showNotificationPermissionRationaleDialog = show)
    }
}

data class WallpaperUiState(
    val wallpaperId: String,
    val wallpaper: Wallpaper? = null,
    val systemBarsVisible: Boolean = true,
    val actionsVisible: Boolean = true,
    val showInfo: Boolean = false,
    val downloadStatus: DownloadStatus? = null,
    val loading: Boolean = true,
    val showNotificationPermissionRationaleDialog: Boolean = false,
)
