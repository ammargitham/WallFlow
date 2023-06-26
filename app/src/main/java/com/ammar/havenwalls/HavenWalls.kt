package com.ammar.havenwalls

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.work.Configuration
import com.ammar.havenwalls.data.repository.AppPreferencesRepository
import com.ammar.havenwalls.extensions.TAG
import com.ammar.havenwalls.utils.NotificationChannels
import com.ammar.havenwalls.workers.AutoWallpaperWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltAndroidApp
class HavenWalls : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var appPreferencesRepository: AppPreferencesRepository

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

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
    }

    private fun scheduleAutoWallpaperWorker() {
        with(ProcessLifecycleOwner.get()) {
            lifecycleScope.launch {
                val scheduled = AutoWallpaperWorker.checkIfScheduled(
                    context = this@HavenWalls,
                    appPreferencesRepository = appPreferencesRepository,
                )
                if (scheduled) return@launch
                val prefs = appPreferencesRepository.appPreferencesFlow.first()
                    .autoWallpaperPreferences
                AutoWallpaperWorker.schedule(
                    context = this@HavenWalls,
                    constraints = prefs.constraints,
                    interval = prefs.frequency,
                    appPreferencesRepository = appPreferencesRepository,
                )
            }
        }
    }
}
