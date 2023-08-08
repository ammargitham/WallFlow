package com.ammar.wallflow.model

import android.app.WallpaperManager
import android.os.Build
import androidx.annotation.RequiresApi

enum class WallpaperTarget {
    HOME,
    LOCKSCREEN,
}

@RequiresApi(Build.VERSION_CODES.N)
fun Set<WallpaperTarget>.toWhichInt() = this.fold(0) { acc, target ->
    acc or when (target) {
        WallpaperTarget.HOME -> WallpaperManager.FLAG_SYSTEM
        WallpaperTarget.LOCKSCREEN -> WallpaperManager.FLAG_LOCK
    }
}
