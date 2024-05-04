package com.ammar.wallflow

import android.app.ActivityManager
import android.app.Application
import android.os.Process
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.ammar.wallflow.data.network.coil.WallhavenFallbackInterceptor
import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.ammar.wallflow.extensions.TAG
import com.ammar.wallflow.utils.CrashReportHelper
import com.ammar.wallflow.utils.NotificationChannels
import com.ammar.wallflow.workers.AutoWallpaperWorker
import com.ammar.wallflow.workers.CleanupWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.acra.ACRA
import org.acra.data.StringFormat
import org.acra.ktx.initAcra

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
        if (isAcraProcess()) {
            // No need to setup anything if we are running the CrashReportActivity
            return
        }
        initializeAcra()
        NotificationChannels.createChannels(this)
        scheduleAutoWallpaperWorker()
        scheduleCleanupWorker()
    }

    private fun initializeAcra() {
        if (isAcraProcess()) {
            // Do not init acra when we are in an acra process.
            // This prevents any [crash -> report -> crash -> ...] loops
            // in a rare chance that CrashReportActivity crashes itself
            return
        }
        with(ProcessLifecycleOwner.get()) {
            lifecycleScope.launch {
                val acraEnabled = appPreferencesRepository
                    .appPreferencesFlow
                    .firstOrNull()
                    ?.acraEnabled
                    ?: true
                if (acraEnabled) {
                    initAcra {
                        buildConfigClass = BuildConfig::class.java
                        reportFormat = StringFormat.KEY_VALUE_LIST
                        reportContent = CrashReportHelper.REPORT_FIELDS
                    }
                }
            }
        }
    }

    /**
     * From https://gitlab.com/fdroid/fdroidclient/-/blob/master/app/src/main/java/org/fdroid/fdroid/FDroidApp.java?ref_type=heads#L429
     * Checks if the current process is an acra process
     */
    private fun isAcraProcess(): Boolean {
        if (ACRA.isACRASenderServiceProcess()) {
            return true
        }
        val manager = ContextCompat.getSystemService(
            this,
            ActivityManager::class.java,
        ) ?: return false
        val processes = manager.runningAppProcesses ?: return false
        val pid = Process.myPid()
        return processes.any { processInfo ->
            processInfo.pid == pid && ACRA_ID == processInfo.processName
        }
    }

    private fun scheduleAutoWallpaperWorker() {
        with(ProcessLifecycleOwner.get()) {
            lifecycleScope.launch {
                val appPreferences = appPreferencesRepository
                    .appPreferencesFlow
                    .firstOrNull() ?: return@launch
                val autoWallpaperPreferences = appPreferences.autoWallpaperPreferences
                if (!autoWallpaperPreferences.enabled) {
                    return@launch
                }
                val workerNeedsUpdate = AutoWallpaperWorker.checkIfNeedsUpdate(
                    appPreferencesRepository = appPreferencesRepository,
                )
                val scheduled = AutoWallpaperWorker.checkIfAnyScheduled(
                    context = this@WallFlowApplication,
                    appPreferencesRepository = appPreferencesRepository,
                )
                if (scheduled && !workerNeedsUpdate) {
                    return@launch
                }
                AutoWallpaperWorker.schedule(
                    context = this@WallFlowApplication,
                    autoWallpaperPreferences = autoWallpaperPreferences,
                    appPreferencesRepository = appPreferencesRepository,
                    enqueuePolicy = if (scheduled) {
                        ExistingPeriodicWorkPolicy.UPDATE
                    } else {
                        ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
                    },
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

    companion object {
        private const val ACRA_ID = BuildConfig.APPLICATION_ID + ":acra"
    }
}
