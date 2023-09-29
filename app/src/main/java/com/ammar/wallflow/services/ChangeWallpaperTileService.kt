package com.ammar.wallflow.services

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.ammar.wallflow.MainDispatcher
import com.ammar.wallflow.R
import com.ammar.wallflow.data.preferences.AutoWallpaperPreferences
import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.ammar.wallflow.extensions.TAG
import com.ammar.wallflow.workers.AutoWallpaperWorker
import com.ammar.wallflow.workers.AutoWallpaperWorker.Companion.Status.Running
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChangeWallpaperTileService : TileService() {
    @Inject
    @MainDispatcher
    lateinit var mainDispatcher: CoroutineDispatcher

    @Inject
    lateinit var appPreferencesRepository: AppPreferencesRepository

    private var coroutineScope: CoroutineScope? = null
    private var listeningJob: Job? = null
    private var initCalled = false

    private val requestIdFlow = MutableStateFlow<UUID?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val workerStatusFlow = requestIdFlow.flatMapLatest {
        if (it == null) {
            return@flatMapLatest flowOf(null)
        }
        AutoWallpaperWorker.getProgress(this, it)
    }

    private data class State(
        val label: String,
        val subtitle: String?,
        val state: Int,
    )

    override fun onCreate() {
        super.onCreate()
        coroutineScope = CoroutineScope(Job() + mainDispatcher)
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope?.cancel()
        coroutineScope = null
        initCalled = false
    }

    override fun onTileAdded() {
        super.onTileAdded()
        Log.i(TAG, "Change wallpaper tile added")
        coroutineScope?.launch {
            appPreferencesRepository.updateTileAdded(true)
            init()
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        listeningJob = coroutineScope?.launch {
            init()
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        listeningJob?.cancel()
        listeningJob = null
        initCalled = false
    }

    override fun onClick() {
        super.onClick()
        coroutineScope?.launch {
            val prefs = getAutoWallpaperPrefs() ?: return@launch
            if (!prefs.anySourceEnabled) {
                return@launch
            }
            val requestId = AutoWallpaperWorker.triggerImmediate(
                context = this@ChangeWallpaperTileService,
                force = true,
            )
            requestIdFlow.update { requestId }
        }
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
        Log.i(TAG, "Change wallpaper tile removed")
        coroutineScope?.launch {
            appPreferencesRepository.updateTileAdded(false)
        }
    }

    private fun updateTile(state: State) {
        qsTile?.apply {
            label = state.label
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subtitle = state.subtitle
            }
            this.state = state.state
            try {
                this.updateTile()
            } catch (e: Exception) {
                Log.e(TAG, "updateTile: ", e)
            }
        }
    }

    private suspend fun getAutoWallpaperPrefs(): AutoWallpaperPreferences? {
        val appPreferences = appPreferencesRepository.appPreferencesFlow.firstOrNull()
            ?: return null
        return appPreferences.autoWallpaperPreferences
    }

    private suspend fun init() {
        if (initCalled) return
        initCalled = true
        val appPreferencesFlow = appPreferencesRepository.appPreferencesFlow
        combine(
            appPreferencesFlow,
            workerStatusFlow,
        ) { appPreferences, status ->
            val prefs = appPreferences.autoWallpaperPreferences
            State(
                label = when (status) {
                    is Running -> getString(R.string.changing_wallpaper)
                    else -> getString(R.string.change_wallpaper_tile_label)
                },
                state = when {
                    status is Running -> Tile.STATE_ACTIVE
                    !prefs.anySourceEnabled -> Tile.STATE_UNAVAILABLE
                    else -> Tile.STATE_INACTIVE
                },
                subtitle = when {
                    status is Running -> getString(R.string.changing)
                    !prefs.anySourceEnabled -> getString(R.string.no_sources_set)
                    else -> getString(R.string.tap_to_change_wallpaper)
                },
            )
        }.collectLatest {
            updateTile(it)
        }
    }

    companion object {
        private fun getComponentName(context: Context) = ComponentName(
            context.applicationContext,
            ChangeWallpaperTileService::class.java,
        )

        fun requestListeningState(context: Context) {
            try {
                requestListeningState(
                    context,
                    getComponentName(context),
                )
            } catch (e: Exception) {
                Log.e(TAG, "requestListeningState: ", e)
            }
        }
    }
}
