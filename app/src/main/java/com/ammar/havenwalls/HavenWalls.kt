package com.ammar.havenwalls

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.ammar.havenwalls.utils.HavenWallsWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class HavenWalls : Application(), Configuration.Provider {
    @Inject
    lateinit var havenWallsWorkerFactory: HavenWallsWorkerFactory

    init {
        if (BuildConfig.DEBUG) {
            try {
                Class.forName("dalvik.system.CloseGuard")
                    .getMethod("setEnabled", Boolean::class.javaPrimitiveType)
                    .invoke(null, true)
            } catch (e: ReflectiveOperationException) {
                throw RuntimeException(e)
            }
        }
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(Log.WARN)
            .setWorkerFactory(havenWallsWorkerFactory)
            .build()
    }
}
