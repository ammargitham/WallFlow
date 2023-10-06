package com.ammar.wallflow.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.ammar.wallflow.data.db.AppDatabase
import com.ammar.wallflow.data.db.entity.SearchQueryEntity
import com.ammar.wallflow.data.db.entity.SearchQueryRemoteKeyEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenSearchQueryWallpaperEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenWallpaperEntity
import com.ammar.wallflow.data.network.WallhavenNetworkDataSource
import com.ammar.wallflow.data.network.model.toWallpaperEntity
import com.ammar.wallflow.model.wallhaven.WallhavenSearchQuery
import java.io.IOException
import kotlinx.datetime.Clock
import retrofit2.HttpException

@OptIn(ExperimentalPagingApi::class)
class WallpapersRemoteMediator(
    private val searchQuery: WallhavenSearchQuery,
    private val appDatabase: AppDatabase,
    private val wallHavenNetwork: WallhavenNetworkDataSource,
    private val clock: Clock = Clock.System,
) : RemoteMediator<Int, WallhavenWallpaperEntity>() {
    private val wallpapersDao = appDatabase.wallhavenWallpapersDao()
    private val searchQueryDao = appDatabase.searchQueryDao()
    private val remoteKeysDao = appDatabase.searchQueryRemoteKeysDao()
    private val searchQueryWallpapersDao = appDatabase.wallhavenSearchQueryWallpapersDao()

    override suspend fun initialize(): InitializeAction {
        val searchQueryEntity = searchQueryDao.getBySearchQuery(searchQuery.toQueryString())
        val lastUpdatedOn =
            searchQueryEntity?.lastUpdatedOn ?: return InitializeAction.LAUNCH_INITIAL_REFRESH
        val cacheTimeout = 3 // hours
        val diffHours = (clock.now() - lastUpdatedOn).absoluteValue.inWholeMinutes / 60f
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
        state: PagingState<Int, WallhavenWallpaperEntity>,
    ): MediatorResult {
        return try {
            val searchQueryEntity = searchQueryDao.getBySearchQuery(searchQuery.toQueryString())
            val nextPage = when (loadType) {
                LoadType.REFRESH -> null
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> {
                    searchQueryEntity?.run { remoteKeysDao.getBySearchQueryId(id) }
                        ?.nextPageNumber
                        ?: return MediatorResult.Success(endOfPaginationReached = true)
                }
            }
            val response = wallHavenNetwork.search(searchQuery, nextPage)
            // if at last page, next page is null else current + 1
            val nextPageNumber = response.meta?.run {
                if (current_page != last_page) current_page + 1 else null
            }
            appDatabase.withTransaction {
                val now = clock.now()
                val searchQueryId = searchQueryEntity?.id ?: searchQueryDao.upsert(
                    SearchQueryEntity(
                        id = 0,
                        queryString = searchQuery.toQueryString(),
                        lastUpdatedOn = now,
                    ),
                ).first()

                if (loadType == LoadType.REFRESH) {
                    remoteKeysDao.deleteBySearchQueryId(searchQueryId)
                    wallpapersDao.deleteAllUniqueToSearchQueryId(searchQueryId)
                    searchQueryWallpapersDao.deleteBySearchQueryId(searchQueryId)
                    (searchQueryEntity ?: searchQueryDao.getById(searchQueryId))?.run {
                        // insert or update search query in db
                        searchQueryDao.upsert(copy(lastUpdatedOn = now))
                    }
                }

                // Update RemoteKey for this query.
                val remoteKey = remoteKeysDao.getBySearchQueryId(searchQueryId)
                val updatedRemoteKey = remoteKey?.copy(nextPageNumber = nextPageNumber)
                    ?: SearchQueryRemoteKeyEntity(
                        id = 0,
                        searchQueryId = searchQueryId,
                        nextPageNumber = nextPageNumber,
                    )
                remoteKeysDao.insertOrReplace(updatedRemoteKey)

                val networkWallpapers = response.data
                val wallhavenWallpaperIds = networkWallpapers.map { it.id }

                wallpapersDao.insert(networkWallpapers.map { it.toWallpaperEntity() })
                val wallpaperEntities = wallpapersDao.getByWallhavenIds(wallhavenWallpaperIds)

                // update mapping table
                searchQueryWallpapersDao.insert(
                    wallpaperEntities.map {
                        WallhavenSearchQueryWallpaperEntity(
                            searchQueryId = searchQueryId,
                            wallpaperId = it.id,
                        )
                    },
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
