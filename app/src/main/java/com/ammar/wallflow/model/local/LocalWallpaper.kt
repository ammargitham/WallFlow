package com.ammar.wallflow.model.local

import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.IntSize
import com.ammar.wallflow.model.Purity
import com.ammar.wallflow.model.Source
import com.ammar.wallflow.model.Wallpaper

@Stable
data class LocalWallpaper(
    override val source: Source = Source.LOCAL,
    override val id: String,
    override val data: Uri,
    override val fileSize: Long,
    override val resolution: IntSize,
    override val mimeType: String?,
    override val thumbData: String? = null,
    override val purity: Purity = Purity.SFW,
    val name: String,
) : Wallpaper()
