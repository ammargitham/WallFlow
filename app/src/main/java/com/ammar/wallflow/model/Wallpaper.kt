package com.ammar.wallflow.model

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.IntSize

@Stable
abstract class Wallpaper {
    abstract val source: Source
    abstract val id: String
    abstract val data: Any
    abstract val fileSize: Long
    abstract val resolution: IntSize
    abstract val mimeType: String?
    abstract val thumbData: String?
    abstract val purity: Purity
}

abstract class DownloadableWallpaper : Wallpaper() {
    abstract override val source: Source
    abstract override val data: String
}
