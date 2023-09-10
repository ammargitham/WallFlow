package com.ammar.wallflow.data.repository.local

import android.content.Context
import android.net.Uri
import androidx.paging.PagingData
import com.ammar.wallflow.MIME_TYPE_BMP
import com.ammar.wallflow.MIME_TYPE_HEIC
import com.ammar.wallflow.MIME_TYPE_JPEG
import com.ammar.wallflow.MIME_TYPE_PNG
import com.ammar.wallflow.MIME_TYPE_WEBP
import com.ammar.wallflow.data.repository.utils.Resource
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.model.local.LocalWallpaper
import kotlinx.coroutines.flow.Flow

interface LocalWallpapersRepository {
    fun wallpapersPager(
        context: Context,
        uris: Collection<Uri>,
    ): Flow<PagingData<Wallpaper>>

    fun wallpaper(
        context: Context,
        wallpaperUriStr: String,
    ): Flow<Resource<LocalWallpaper?>>

    suspend fun getRandom(
        context: Context,
        uris: Collection<Uri>,
    ): Wallpaper?

    companion object {
        val SUPPORTED_MIME_TYPES = setOf(
            MIME_TYPE_BMP,
            MIME_TYPE_HEIC,
            MIME_TYPE_JPEG,
            MIME_TYPE_PNG,
            MIME_TYPE_WEBP,
        )
    }
}
