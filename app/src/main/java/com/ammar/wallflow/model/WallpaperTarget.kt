package com.ammar.wallflow.model

import android.app.WallpaperManager

enum class WallpaperTarget {
    HOME,
    LOCKSCREEN,
}

fun Set<WallpaperTarget>.toWhichInt() = this.fold(0) { acc, target ->
    acc or when (target) {
        WallpaperTarget.HOME -> WallpaperManager.FLAG_SYSTEM
        WallpaperTarget.LOCKSCREEN -> WallpaperManager.FLAG_LOCK
    }
}
