package com.ammar.wallflow.data.repository.wallhaven

import androidx.paging.PagingData
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenTagEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenUploaderEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenWallpaperEntity
import com.ammar.wallflow.data.repository.utils.Resource
import com.ammar.wallflow.model.WallhavenSearchQuery
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.model.wallhaven.WallhavenTag
import com.ammar.wallflow.model.wallhaven.WallhavenWallpaper
import kotlinx.coroutines.flow.Flow

interface WallhavenRepository {
    fun wallpapersPager(
        searchQuery: WallhavenSearchQuery,
        pageSize: Int = 24,
        prefetchDistance: Int = pageSize,
        initialLoadSize: Int = pageSize * 3,
    ): Flow<PagingData<Wallpaper>>

    fun popularTags(): Flow<Resource<List<WallhavenTag>>>

    suspend fun refreshPopularTags()

    fun wallpaper(wallpaperWallhavenId: String): Flow<Resource<WallhavenWallpaper?>>

    suspend fun insertTagEntities(tags: Collection<WallhavenTagEntity>)

    suspend fun insertUploaderEntities(uploaders: Collection<WallhavenUploaderEntity>)

    suspend fun insertWallpaperEntities(
        entities: Collection<WallhavenWallpaperEntity>,
    )
}
