package com.ammar.wallflow.data.repository.reddit

import androidx.paging.PagingData
import com.ammar.wallflow.data.db.entity.wallpaper.RedditWallpaperEntity
import com.ammar.wallflow.data.repository.utils.Resource
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.model.reddit.RedditWallpaper
import com.ammar.wallflow.model.search.RedditSearch
import kotlinx.coroutines.flow.Flow

interface RedditRepository {
    fun wallpapersPager(
        search: RedditSearch,
        pageSize: Int = 24,
        prefetchDistance: Int = pageSize,
        initialLoadSize: Int = pageSize * 2,
    ): Flow<PagingData<Wallpaper>>

    fun wallpaper(wallpaperId: String): Flow<Resource<RedditWallpaper?>>

    suspend fun insertWallpaperEntities(
        entities: Collection<RedditWallpaperEntity>,
    )
}
