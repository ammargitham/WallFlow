package com.ammar.wallflow.ui.wallpaperviewer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ammar.wallflow.data.repository.WallhavenRepository
import com.ammar.wallflow.data.repository.utils.Resource
import com.ammar.wallflow.data.repository.utils.successOr
import com.ammar.wallflow.model.wallhaven.WallhavenWallpaper
import com.ammar.wallflow.utils.DownloadManager
import com.ammar.wallflow.utils.DownloadStatus
import com.github.materiiapps.partial.Partialize
import com.github.materiiapps.partial.getOrElse
import com.github.materiiapps.partial.partial
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class WallpaperViewerViewModel @Inject constructor(
    private val application: Application,
    private val wallHavenRepository: WallhavenRepository,
    private val downloadManager: DownloadManager,
) : AndroidViewModel(
    application = application,
) {
    private val localUiState = MutableStateFlow(WallpaperViewerUiStatePartial())
    private val wallpaperIdFlow = MutableStateFlow<String?>(null)
    private val thumbUrlFlow = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val wallpaperFlow = wallpaperIdFlow.flatMapLatest {
        if (it == null) {
            flowOf(Resource.Success(null))
        } else {
            wallHavenRepository.wallpaper(it)
        }
    }

    val uiState = combine(
        localUiState,
        wallpaperFlow,
        thumbUrlFlow,
    ) { local, wallpaper, thumbUrl ->
        local.merge(
            WallpaperViewerUiState(
                wallhavenWallpaper = wallpaper.successOr(null),
                thumbUrl = thumbUrl,
                loading = wallpaper is Resource.Loading,
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WallpaperViewerUiState(),
    )

    fun setWallpaperId(wallpaperId: String?, thumbUrl: String?) {
        wallpaperIdFlow.update { wallpaperId }
        thumbUrlFlow.update { thumbUrl }
    }

    fun onWallpaperTap() = localUiState.update {
        it.copy(
            actionsVisible = partial(!it.actionsVisible.getOrElse { false }),
        )
    }

    fun onWallpaperTransform() = localUiState.update {
        it.copy(
            actionsVisible = partial(false),
        )
    }

    fun showInfo(show: Boolean = true) = localUiState.update {
        it.copy(showInfo = partial(show))
    }

    fun download() = viewModelScope.launch {
        uiState.value.wallhavenWallpaper?.run {
            val workName = downloadManager.requestDownload(application, this)
            downloadManager.getProgress(application, workName).collectLatest { state ->
                localUiState.update { it.copy(downloadStatus = partial(state)) }
            }
        }
    }

    fun downloadForSharing(onResult: (file: File?) -> Unit) {
        uiState.value.wallhavenWallpaper?.run {
            viewModelScope.launch {
                downloadManager.downloadWallpaperAsync(
                    context = application,
                    wallhavenWallpaper = this@run,
                    onLoadingChange = { loading ->
                        localUiState.update { it.copy(loading = partial(loading)) }
                    },
                    onResult = onResult,
                )
            }
        }
    }
}

@Partialize
data class WallpaperViewerUiState(
    val wallhavenWallpaper: WallhavenWallpaper? = null,
    val thumbUrl: String? = null,
    val actionsVisible: Boolean = true,
    val showInfo: Boolean = false,
    val downloadStatus: DownloadStatus? = null,
    val loading: Boolean = true,
)
