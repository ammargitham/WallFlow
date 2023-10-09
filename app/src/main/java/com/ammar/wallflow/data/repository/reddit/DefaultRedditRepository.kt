package com.ammar.wallflow.data.repository.reddit

import androidx.paging.PagingData
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.model.search.RedditSearch
import kotlinx.coroutines.flow.Flow

class DefaultRedditRepository : RedditRepository {
    override fun wallpapersPager(
        search: RedditSearch,
        pageSize: Int,
        prefetchDistance: Int,
        initialLoadSize: Int,
    ): Flow<PagingData<Wallpaper>> {
        TODO("not implemented")
    }
}
