package com.ammar.wallflow.data.repository.local

import android.content.Context
import android.net.Uri
import androidx.paging.PagingData
import com.ammar.wallflow.data.repository.utils.Resource
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.model.local.LocalWallpaper
import com.ammar.wallflow.ui.screens.local.LocalSort
import kotlinx.coroutines.flow.Flow

interface LocalWallpapersRepository {
    fun wallpapersPager(
        context: Context,
        uris: Collection<Uri>,
        sort: LocalSort = LocalSort.NO_SORT,
    ): Flow<PagingData<Wallpaper>>

    fun wallpaper(
        context: Context,
        wallpaperUriStr: String,
    ): Flow<Resource<LocalWallpaper?>>

    suspend fun getRandom(
        context: Context,
        uris: Collection<Uri>,
    ): Wallpaper?

    suspend fun getFirstFresh(
        context: Context,
        uris: Collection<Uri>,
        excluding: Collection<Wallpaper>,
    ): Wallpaper?

    suspend fun getByOldestSetOn(
        context: Context,
        excluding: Collection<Wallpaper>,
    ): Wallpaper?

    suspend fun getCountExcludingWallpapers(
        context: Context,
        uris: Collection<Uri>,
        excluding: Collection<Wallpaper>,
    ): Int
}
