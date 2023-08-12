package com.ammar.wallflow

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.ammar.wallflow.data.network.coil.WallhavenFallbackInterceptor
import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.ammar.wallflow.extensions.TAG
import com.ammar.wallflow.utils.NotificationChannels
import com.ammar.wallflow.workers.AutoWallpaperWorker
import com.ammar.wallflow.workers.CleanupWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

@HiltAndroidApp
class WallFlowApplication : Application(), Configuration.Provider, ImageLoaderFactory {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var appPreferencesRepository: AppPreferencesRepository

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    lateinit var okHttpClient: OkHttpClient

    init {
        if (BuildConfig.DEBUG) {
            try {
                Class.forName("dalvik.system.CloseGuard")
                    .getMethod("setEnabled", Boolean::class.javaPrimitiveType)
                    .invoke(null, true)
            } catch (e: Exception) {
                Log.e(TAG, "Error while enabling CloseGuard", e)
            }
        }
    }

    override val workManagerConfiguration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.WARN)
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.createChannels(this)
        scheduleAutoWallpaperWorker()
        scheduleCleanupWorker()
    }

    private fun scheduleAutoWallpaperWorker() {
        with(ProcessLifecycleOwner.get()) {
            lifecycleScope.launch {
                val scheduled = AutoWallpaperWorker.checkIfScheduled(
                    context = this@WallFlowApplication,
                    appPreferencesRepository = appPreferencesRepository,
                )
                if (scheduled) return@launch
                val prefs = appPreferencesRepository.appPreferencesFlow.first()
                    .autoWallpaperPreferences
                AutoWallpaperWorker.schedule(
                    context = this@WallFlowApplication,
                    constraints = prefs.constraints,
                    interval = prefs.frequency,
                    appPreferencesRepository = appPreferencesRepository,
                )
            }
        }
    }

    private fun scheduleCleanupWorker() {
        with(ProcessLifecycleOwner.get()) {
            lifecycleScope.launch {
                val scheduled = CleanupWorker.checkIfScheduled(context = this@WallFlowApplication)
                if (scheduled) return@launch
                CleanupWorker.schedule(context = this@WallFlowApplication)
            }
        }
    }

    override fun newImageLoader() = ImageLoader.Builder(this)
        .okHttpClient(okHttpClient)
        .components {
            add(WallhavenFallbackInterceptor())
        }
        .build()
}
