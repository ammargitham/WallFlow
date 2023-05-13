package com.ammar.havenwalls.utils

import android.content.Context
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.ammar.havenwalls.IoDispatcher
import com.ammar.havenwalls.workers.DownloadWorker
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HavenWallsWorkerFactory @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val okHttpClient: OkHttpClient,
) : WorkerFactory() {

    override fun createWorker(
        context: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ) = when (workerClassName) {
        DownloadWorker::class.java.name ->
            DownloadWorker(
                context,
                workerParameters,
                ioDispatcher,
                okHttpClient,
            )
        else -> null
    }
}
