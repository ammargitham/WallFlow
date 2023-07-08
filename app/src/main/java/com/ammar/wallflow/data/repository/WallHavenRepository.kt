package com.ammar.wallflow.data.repository

import androidx.paging.PagingData
import com.ammar.wallflow.data.repository.utils.Resource
import com.ammar.wallflow.model.SearchQuery
import com.ammar.wallflow.model.Tag
import com.ammar.wallflow.model.Wallpaper
import kotlinx.coroutines.flow.Flow

interface WallHavenRepository {
    fun wallpapersPager(
        searchQuery: SearchQuery,
        pageSize: Int = 24,
        prefetchDistance: Int = pageSize,
        initialLoadSize: Int = pageSize * 3,
    ): Flow<PagingData<Wallpaper>>

    fun popularTags(): Flow<Resource<List<Tag>>>

    suspend fun refreshPopularTags()

    fun wallpaper(wallpaperWallhavenId: String): Flow<Resource<Wallpaper?>>
}
