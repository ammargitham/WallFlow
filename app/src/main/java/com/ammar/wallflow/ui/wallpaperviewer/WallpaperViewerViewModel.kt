package com.ammar.wallflow.ui.wallpaperviewer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ammar.wallflow.data.repository.local.LocalWallpapersRepository
import com.ammar.wallflow.data.repository.reddit.RedditRepository
import com.ammar.wallflow.data.repository.utils.Resource
import com.ammar.wallflow.data.repository.utils.successOr
import com.ammar.wallflow.data.repository.wallhaven.WallhavenRepository
import com.ammar.wallflow.model.DownloadableWallpaper
import com.ammar.wallflow.model.Source
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.utils.DownloadManager
import com.ammar.wallflow.utils.DownloadStatus
import com.github.materiiapps.partial.Partialize
import com.github.materiiapps.partial.getOrElse
import com.github.materiiapps.partial.partial
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
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
    private val wallhavenRepository: WallhavenRepository,
    private val redditRepository: RedditRepository,
    private val localWallpapersRepository: LocalWallpapersRepository,
    private val downloadManager: DownloadManager,
) : AndroidViewModel(
    application = application,
) {
    private val localUiState = MutableStateFlow(WallpaperViewerUiStatePartial())
    private val argsFlow = MutableStateFlow(WallpaperViewerArgs())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val wallpaperFlow = argsFlow.flatMapLatest {
        if (it.source == null || it.wallpaperId == null) {
            flowOf(Resource.Success(null))
        } else {
            when (it.source) {
                Source.WALLHAVEN -> wallhavenRepository.wallpaper(it.wallpaperId)
                Source.REDDIT -> redditRepository.wallpaper(it.wallpaperId)
                Source.LOCAL -> localWallpapersRepository.wallpaper(application, it.wallpaperId)
            }
        }
    }

    val uiState = combine(
        localUiState,
        wallpaperFlow,
        argsFlow,
    ) { local, wallpaper, args ->
        local.merge(
            WallpaperViewerUiState(
                wallpaper = wallpaper.successOr(null),
                thumbData = args.thumbData,
                loading = wallpaper is Resource.Loading,
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WallpaperViewerUiState(),
    )

    fun setWallpaper(
        source: Source,
        wallpaperId: String?,
        thumbData: String?,
    ) = argsFlow.update {
        WallpaperViewerArgs(
            source = source,
            wallpaperId = wallpaperId,
            thumbData = thumbData,
        )
    }

    fun onWallpaperTap() = localUiState.update {
        it.copy(
            actionsVisible = partial(!it.actionsVisible.getOrElse { true }),
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

    fun download() {
        var job: Job? = null
        job = viewModelScope.launch {
            uiState.value.wallpaper?.run {
                if (this !is DownloadableWallpaper) {
                    return@run
                }
                val workName = downloadManager.requestDownload(
                    context = application,
                    wallpaper = this,
                )
                downloadManager.getProgress(
                    context = application,
                    workName = workName,
                ).collectLatest { state ->
                    localUiState.update { it.copy(downloadStatus = partial(state)) }
                    if (state.isSuccessOrFail()) {
                        job?.cancel()
                    }
                }
            }
        }
    }

    fun downloadForSharing(onResult: (file: File?) -> Unit) {
        uiState.value.wallpaper?.run {
            if (this !is DownloadableWallpaper) {
                return@run
            }
            var job: Job? = null
            job = viewModelScope.launch {
                downloadManager.downloadWallpaperAsync(
                    context = application,
                    wallpaper = this@run,
                    onLoadingChange = { loading ->
                        localUiState.update { it.copy(loading = partial(loading)) }
                    },
                    onResult = {
                        onResult(it)
                        job?.cancel()
                    },
                )
            }
        }
    }
}

@Partialize
data class WallpaperViewerUiState(
    val wallpaper: Wallpaper? = null,
    val thumbData: String? = null,
    val actionsVisible: Boolean = true,
    val showInfo: Boolean = false,
    val downloadStatus: DownloadStatus? = null,
    val loading: Boolean = true,
)

data class WallpaperViewerArgs(
    val source: Source? = null,
    val wallpaperId: String? = null,
    val thumbData: String? = null,
)
