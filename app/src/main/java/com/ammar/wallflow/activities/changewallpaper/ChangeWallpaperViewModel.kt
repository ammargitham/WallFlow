package com.ammar.wallflow.activities.changewallpaper

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ammar.wallflow.data.preferences.Theme
import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.ammar.wallflow.extensions.TAG
import com.ammar.wallflow.workers.AutoWallpaperWorker
import com.ammar.wallflow.workers.AutoWallpaperWorker.Companion.Status as AutoWallpaperWorkerStatus
import com.github.materiiapps.partial.Partialize
import com.github.materiiapps.partial.partial
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ChangeWallpaperViewModel @Inject constructor(
    private val application: Application,
    appPreferencesRepository: AppPreferencesRepository,
) : AndroidViewModel(application = application) {
    private val localUiStateFlow = MutableStateFlow(ChangeWallpaperActivityUiStatePartial())

    val uiState = combine(
        appPreferencesRepository.appPreferencesFlow,
        localUiStateFlow,
    ) { appPreferences, local ->
        local.merge(
            ChangeWallpaperActivityUiState(
                theme = appPreferences.lookAndFeelPreferences.theme,
                hasSources = appPreferences.autoWallpaperPreferences.anySourceEnabled,
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChangeWallpaperActivityUiState(),
    )

    fun changeNow() = viewModelScope.launch {
        Log.d(TAG, "changeNow: triggering immediate wallpaper change")
        val requestId = AutoWallpaperWorker.triggerImmediate(
            context = application,
            force = true,
        )
        Log.d(TAG, "changeNow: triggered with request id: $requestId")
        AutoWallpaperWorker.getProgress(
            context = application,
            requestId = requestId,
        ).collectLatest { status ->
            Log.d(TAG, "changeNow: change status: $status")
            localUiStateFlow.update { it.copy(autoWallpaperStatus = partial(status)) }
        }
    }
}

@Partialize
data class ChangeWallpaperActivityUiState(
    val theme: Theme = Theme.SYSTEM,
    val hasSources: Boolean = false,
    val autoWallpaperStatus: AutoWallpaperWorkerStatus? = null,
)
