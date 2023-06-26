package com.ammar.havenwalls.workers

import android.content.Context
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.ForegroundUpdater
import androidx.work.Logger
import androidx.work.ProgressUpdater
import androidx.work.impl.utils.SerialExecutorImpl
import androidx.work.impl.utils.SynchronousExecutor
import androidx.work.impl.utils.futures.SettableFuture
import androidx.work.impl.utils.taskexecutor.SerialExecutor
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import com.google.common.util.concurrent.ListenableFuture
import java.util.UUID
import java.util.concurrent.Executor

internal class InstantWorkTaskExecutor : TaskExecutor {
    private val synchronousExecutor: Executor = SynchronousExecutor()
    private val mSerialExecutor = SerialExecutorImpl(synchronousExecutor)
    override fun getMainThreadExecutor(): Executor {
        return synchronousExecutor
    }

    override fun getSerialTaskExecutor(): SerialExecutor {
        return mSerialExecutor
    }
}

class TestProgressUpdater : ProgressUpdater {
    override fun updateProgress(
        context: Context,
        id: UUID,
        data: Data,
    ): ListenableFuture<Void> {
        Logger.get().info(
            TAG,
            "Updating progress for $id ($data)"
        )
        val future = SettableFuture.create<Void>()
        future.set(null)
        return future
    }

    companion object {
        private val TAG = Logger.tagWithPrefix("TestProgressUpdater")
    }
}

class TestForegroundUpdater : ForegroundUpdater {
    override fun setForegroundAsync(
        context: Context,
        id: UUID,
        foregroundInfo: ForegroundInfo,
    ): ListenableFuture<Void> {
        Logger.get().info(
            TAG,
            "setForegroundAsync for $id"
        )
        val future = SettableFuture.create<Void>()
        future.set(null)
        return future
    }

    companion object {
        private val TAG = Logger.tagWithPrefix("TestForegroundUpdater")
    }
}
