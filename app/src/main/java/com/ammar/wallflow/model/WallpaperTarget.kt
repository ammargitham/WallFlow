package com.ammar.wallflow.model

import android.app.WallpaperManager

enum class WallpaperTarget {
    HOME,
    LOCKSCREEN,
    ;

    companion object {
        val ALL = setOf(HOME, LOCKSCREEN)
    }
}

fun Set<WallpaperTarget>.toWhichInt() = this.fold(0) { acc, target ->
    acc or when (target) {
        WallpaperTarget.HOME -> WallpaperManager.FLAG_SYSTEM
        WallpaperTarget.LOCKSCREEN -> WallpaperManager.FLAG_LOCK
    }
}
