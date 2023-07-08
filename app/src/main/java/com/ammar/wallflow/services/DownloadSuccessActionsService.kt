package com.ammar.wallflow.services

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.ammar.wallflow.IoDispatcher
import com.ammar.wallflow.extensions.TAG
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random


@AndroidEntryPoint
class DownloadSuccessActionsService : LifecycleService() {
    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    private val notificationManager by lazy { NotificationManagerCompat.from(this) }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent == null
            || intent.action !in setOf(Intent.ACTION_DELETE)
            || !intent.hasExtra(EXTRA_FILE_PATH)
        ) return START_NOT_STICKY
        val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
        if (filePath.isNullOrBlank()) return START_NOT_STICKY
        when (intent.action) {
            Intent.ACTION_DELETE -> delete(filePath)
            else -> return START_NOT_STICKY
        }
        dismissNotification(intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1))
        return START_REDELIVER_INTENT
    }

    private fun delete(filePath: String) {
        lifecycleScope.launch {
            withContext(ioDispatcher) {
                try {
                    val deleted = File(filePath).delete()
                    if (!deleted) {
                        Log.w(TAG, "File not deleted: $filePath")
                        return@withContext
                    }
                    Log.d(TAG, "File deleted!")
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting file: $filePath", e)
                }
            }
        }
    }

    private fun dismissNotification(notificationId: Int) {
        if (notificationId <= -1) return
        notificationManager.cancel(notificationId)
    }

    companion object {
        const val EXTRA_FILE_PATH = "file_path"
        const val EXTRA_NOTIFICATION_ID = "notification_id"

        fun getDeletePendingIntent(
            context: Context,
            file: File,
            notificationId: Int,
        ): PendingIntent = getPendingIntent(
            context,
            Intent.ACTION_DELETE,
            file,
            notificationId,
        )

        private fun getPendingIntent(
            context: Context,
            @Suppress("SameParameterValue") action: String,
            file: File,
            notificationId: Int? = null,
        ): PendingIntent = PendingIntent.getService(
            context,
            Random.nextInt(),
            Intent(
                context,
                DownloadSuccessActionsService::class.java,
            ).apply {
                this.action = action
                putExtra(EXTRA_FILE_PATH, file.absolutePath)
                if (notificationId != null) {
                    putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                }
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT,
        )
    }
}
