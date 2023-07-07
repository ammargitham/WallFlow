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
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.time.Duration

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

/**
 * From https://github.com/kotest/kotest-extensions-clock
 * A mutable [Clock] that supports millisecond precision.
 */
class TestClock(
    private var now: Instant,
) : Clock {

    override fun now() = now

    /**
     * Sets the `now` instant in this test clock to the given value.
     */
    fun setNow(instant: Instant) {
        now = instant
    }

    /**
     * Adds the given [duration] from the instant in this test clock.
     */
    operator fun plus(duration: Duration) {
        setNow(now.plus(duration.inWholeMilliseconds, DateTimeUnit.MILLISECOND))
    }

    /**
     * Removes the given [duration] from the instant in this test clock.
     */
    operator fun minus(duration: Duration) {
        setNow(now.minus(duration.inWholeMilliseconds, DateTimeUnit.MILLISECOND))
    }
}
