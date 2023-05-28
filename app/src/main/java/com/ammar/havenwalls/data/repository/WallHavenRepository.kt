package com.ammar.havenwalls.data.repository

import androidx.paging.PagingData
import com.ammar.havenwalls.model.SearchQuery
import com.ammar.havenwalls.data.repository.utils.Resource
import com.ammar.havenwalls.model.Tag
import com.ammar.havenwalls.model.Wallpaper
import kotlinx.coroutines.flow.Flow

interface WallHavenRepository {
    // fun topWallpapers(topRange: TopRange = TopRange.ONE_DAY): Flow<PagingData<Wallpaper>>

    fun wallpapersPager(searchQuery: SearchQuery): Flow<PagingData<Wallpaper>>

    fun popularTags(): Flow<Resource<List<Tag>>>

    suspend fun refreshPopularTags()

    fun wallpaper(wallpaperWallhavenId: String): Flow<Resource<Wallpaper?>>
}
