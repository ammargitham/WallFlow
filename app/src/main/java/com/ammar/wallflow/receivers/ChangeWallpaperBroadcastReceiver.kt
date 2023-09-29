package com.ammar.wallflow.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ammar.wallflow.MainDispatcher
import com.ammar.wallflow.R
import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.ammar.wallflow.extensions.TAG
import com.ammar.wallflow.extensions.goAsync
import com.ammar.wallflow.extensions.toast
import com.ammar.wallflow.workers.AutoWallpaperWorker
import com.ammar.wallflow.workers.AutoWallpaperWorker.Companion.Status.Failed
import com.ammar.wallflow.workers.AutoWallpaperWorker.Companion.Status.Success
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class ChangeWallpaperBroadcastReceiver : BroadcastReceiver() {
    @Inject
    lateinit var appPreferencesRepository: AppPreferencesRepository

    @Inject
    @MainDispatcher
    lateinit var mainDispatcher: CoroutineDispatcher

    override fun onReceive(context: Context?, intent: Intent?) {
        val ctx = context ?: return
        ctx.toast(ctx.getString(R.string.changing_wallpaper))
        var job: Job? = null
        job = goAsync {
            val appPrefs = appPreferencesRepository.appPreferencesFlow.firstOrNull()
                ?: return@goAsync
            val prefs = appPrefs.autoWallpaperPreferences
            if (!prefs.anySourceEnabled) {
                try {
                    withContext(mainDispatcher) {
                        ctx.toast(ctx.getString(R.string.no_sources_set))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "onReceive: ", e)
                }
                return@goAsync
            }
            val requestId = AutoWallpaperWorker.triggerImmediate(context, force = true)
            AutoWallpaperWorker.getProgress(ctx, requestId).collectLatest {
                try {
                    when (it) {
                        is Success -> withContext(mainDispatcher) {
                            ctx.toast(ctx.getString(R.string.wallpaper_changed))
                        }
                        is Failed -> withContext(mainDispatcher) {
                            ctx.toast(ctx.getString(R.string.failed_to_change_wallpaper))
                        }
                        else -> {}
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "onReceive: ", e)
                } finally {
                    if (it.isSuccessOrFail()) {
                        job?.cancel()
                    }
                }
            }
        }
    }
}
