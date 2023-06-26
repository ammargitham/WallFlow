package com.ammar.havenwalls.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.ammar.havenwalls.data.db.database.AppDatabase
import com.ammar.havenwalls.data.db.entity.SearchQueryEntity
import com.ammar.havenwalls.data.db.entity.SearchQueryRemoteKeyEntity
import com.ammar.havenwalls.data.db.entity.SearchQueryWallpaperEntity
import com.ammar.havenwalls.data.db.entity.WallpaperEntity
import com.ammar.havenwalls.data.network.WallHavenNetworkDataSource
import com.ammar.havenwalls.data.network.model.asWallpaperEntity
import com.ammar.havenwalls.model.SearchQuery
import java.io.IOException
import kotlinx.datetime.Clock
import retrofit2.HttpException

@OptIn(ExperimentalPagingApi::class)
class WallpapersRemoteMediator(
    private val searchQuery: SearchQuery,
    private val appDatabase: AppDatabase,
    private val wallHavenNetwork: WallHavenNetworkDataSource,
) : RemoteMediator<Int, WallpaperEntity>() {
    private val wallpapersDao = appDatabase.wallpapersDao()
    private val searchQueryDao = appDatabase.searchQueryDao()
    private val remoteKeyDao = appDatabase.searchQueryRemoteKeysDao()
    private val searchQueryWallpapersDao = appDatabase.searchQueryWallpapersDao()

    override suspend fun initialize(): InitializeAction {
        val searchQueryEntity = searchQueryDao.getBySearchQuery(searchQuery.toQueryString())
        val lastUpdatedOn =
            searchQueryEntity?.lastUpdatedOn ?: return InitializeAction.LAUNCH_INITIAL_REFRESH
        val cacheTimeout = 3 // hours
        val diffHours = (Clock.System.now() - lastUpdatedOn).absoluteValue.inWholeMinutes / 60f
        return if (diffHours <= cacheTimeout) {
            // Cached data is up-to-date, so there is no need to re-fetch from the network.
            InitializeAction.SKIP_INITIAL_REFRESH
        } else {
            // Need to refresh cached data from network; returning
            // LAUNCH_INITIAL_REFRESH here will also block RemoteMediator's
            // APPEND and PREPEND from running until REFRESH succeeds.
            InitializeAction.LAUNCH_INITIAL_REFRESH
        }
    }


    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, WallpaperEntity>,
    ): MediatorResult {
        return try {
            val searchQueryEntity = searchQueryDao.getBySearchQuery(searchQuery.toQueryString())
            val nextPage = when (loadType) {
                LoadType.REFRESH -> null
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> {
                    searchQueryEntity?.run { remoteKeyDao.getBySearchQueryId(id) }
                        ?.nextPageNumber
                        ?: return MediatorResult.Success(endOfPaginationReached = true)
                }
            }
            val response = wallHavenNetwork.search(searchQuery, nextPage)
            // if at last page, next page is null else current + 1
            val nextPageNumber =
                response.meta?.run { if (current_page != last_page) current_page + 1 else null }
            appDatabase.withTransaction {
                val now = Clock.System.now()
                val searchQueryId = searchQueryEntity?.id ?: searchQueryDao.upsert(
                    SearchQueryEntity(
                        id = 0,
                        queryString = searchQuery.toQueryString(),
                        lastUpdatedOn = now,
                    )
                ).first()

                if (loadType == LoadType.REFRESH) {
                    remoteKeyDao.deleteBySearchQueryId(searchQueryId)
                    wallpapersDao.deleteAllUniqueToSearchQueryId(searchQueryId)
                    searchQueryWallpapersDao.deleteBySearchQueryId(searchQueryId)
                    (searchQueryEntity ?: searchQueryDao.getById(searchQueryId))?.run {
                        // insert or update search query in db
                        searchQueryDao.upsert(copy(lastUpdatedOn = now))
                    }
                }

                // Update RemoteKey for this query.
                val remoteKey = remoteKeyDao.getBySearchQueryId(searchQueryId)
                val updatedRemoteKey = remoteKey?.copy(nextPageNumber = nextPageNumber)
                    ?: SearchQueryRemoteKeyEntity(
                        id = 0,
                        searchQueryId = searchQueryId,
                        nextPageNumber = nextPageNumber,
                    )
                remoteKeyDao.insertOrReplace(updatedRemoteKey)

                val networkWallpapers = response.data
                val wallhavenWallpaperIds = networkWallpapers.map { it.id }

                wallpapersDao.insert(networkWallpapers.map { it.asWallpaperEntity() })
                val wallpaperEntities = wallpapersDao.getByWallhavenIds(wallhavenWallpaperIds)

                // update mapping table
                searchQueryWallpapersDao.insert(
                    wallpaperEntities.map {
                        SearchQueryWallpaperEntity(
                            searchQueryId = searchQueryId,
                            wallpaperId = it.id,
                        )
                    }
                )
            }

            MediatorResult.Success(endOfPaginationReached = nextPageNumber == null)
        } catch (e: IOException) {
            MediatorResult.Error(e)
        } catch (e: HttpException) {
            MediatorResult.Error(e)
        }
    }
}
