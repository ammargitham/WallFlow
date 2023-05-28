package com.ammar.havenwalls.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadParams.Append
import androidx.paging.PagingSource.LoadResult.Page
import androidx.paging.PagingState
import com.ammar.havenwalls.model.SearchQuery
import com.ammar.havenwalls.model.Sorting
import com.ammar.havenwalls.model.TopRange
import com.ammar.havenwalls.data.network.WallHavenNetworkDataSource
import com.ammar.havenwalls.data.network.model.asWallpaper
import com.ammar.havenwalls.model.Wallpaper
import retrofit2.HttpException
import java.io.IOException

class TopWallpapersPageKeyedPagingSource(
    private val wallHavenNetwork: WallHavenNetworkDataSource,
    private val topRange: TopRange,
) : PagingSource<Int, Wallpaper>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Wallpaper> {
        return try {
            val response = wallHavenNetwork.search(
                searchQuery = SearchQuery(
                    sorting = Sorting.TOPLIST,
                    topRange = topRange,
                ),
                page = if (params is Append) params.key else null,
            )
            Page(
                data = response.data.map { it.asWallpaper() },
                prevKey = response.meta?.run {
                    if (current_page == 1) {
                        return@run null
                    }
                    current_page - 1
                },
                nextKey = response.meta?.run { current_page + 1 }
            )
        } catch (e: IOException) {
            LoadResult.Error(e)
        } catch (e: HttpException) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Wallpaper>) =
        state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey
        }
}
