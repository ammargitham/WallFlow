package com.ammar.wallflow.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.ammar.wallflow.data.db.AppDatabase
import com.ammar.wallflow.data.db.dao.wallpaper.RedditSearchQueryWallpapersDao
import com.ammar.wallflow.data.db.dao.wallpaper.RedditWallpapersDao
import com.ammar.wallflow.data.db.dao.wallpaper.WallhavenSearchQueryWallpapersDao
import com.ammar.wallflow.data.db.dao.wallpaper.WallhavenWallpapersDao
import com.ammar.wallflow.data.db.entity.reddit.RedditSearchQueryWallpaperEntity
import com.ammar.wallflow.data.db.entity.search.SearchQueryEntity
import com.ammar.wallflow.data.db.entity.search.SearchQueryRemoteKeyEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenSearchQueryWallpaperEntity
import com.ammar.wallflow.data.db.entity.wallpaper.OnlineSourceWallpaperEntity
import com.ammar.wallflow.data.db.entity.wallpaper.RedditWallpaperEntity
import com.ammar.wallflow.data.db.entity.wallpaper.WallhavenWallpaperEntity
import com.ammar.wallflow.data.network.OnlineSourceNetworkDataSource
import com.ammar.wallflow.data.network.RedditNetworkDataSource
import com.ammar.wallflow.data.network.WallhavenNetworkDataSource
import com.ammar.wallflow.data.network.model.OnlineSourceWallpapersNetworkResponse
import com.ammar.wallflow.data.network.model.reddit.NetworkRedditSearchResponse
import com.ammar.wallflow.data.network.model.reddit.toWallpaperEntities
import com.ammar.wallflow.data.network.model.wallhaven.NetworkWallhavenWallpapersResponse
import com.ammar.wallflow.data.network.model.wallhaven.toWallpaperEntity
import com.ammar.wallflow.extensions.indexMap
import com.ammar.wallflow.json
import com.ammar.wallflow.model.search.RedditSearch
import com.ammar.wallflow.model.search.Search
import com.ammar.wallflow.model.search.WallhavenSearch
import java.io.IOException
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import retrofit2.HttpException

@OptIn(ExperimentalPagingApi::class)
class WallpapersRemoteMediator<T : Search, U : OnlineSourceWallpaperEntity>(
    private val search: T,
    private val appDatabase: AppDatabase,
    private val network: OnlineSourceNetworkDataSource,
    private val clock: Clock = Clock.System,
) : RemoteMediator<Int, U>() {
    private val wallpapersDao = when (search) {
        is WallhavenSearch -> appDatabase.wallhavenWallpapersDao()
        is RedditSearch -> appDatabase.redditWallpapersDao()
        else -> throw RuntimeException()
    }
    private val searchQueryWallpapersDao = when (search) {
        is WallhavenSearch -> appDatabase.wallhavenSearchQueryWallpapersDao()
        is RedditSearch -> appDatabase.redditSearchQueryWallpapersDao()
        else -> throw RuntimeException()
    }
    private val searchQueryDao = appDatabase.searchQueryDao()
    private val remoteKeysDao = appDatabase.searchQueryRemoteKeysDao()

    override suspend fun initialize(): InitializeAction {
        val searchQueryString = when (search) {
            is WallhavenSearch -> json.encodeToString<WallhavenSearch>(search)
            is RedditSearch -> json.encodeToString<RedditSearch>(search)
            else -> throw RuntimeException()
        }
        val searchQueryEntity = searchQueryDao.getBySearchQuery(searchQueryString)
        val lastUpdatedOn = searchQueryEntity?.lastUpdatedOn
            ?: return InitializeAction.LAUNCH_INITIAL_REFRESH
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
        state: PagingState<Int, U>,
    ): MediatorResult {
        return try {
            val queryString = when (search) {
                is WallhavenSearch -> json.encodeToString<WallhavenSearch>(search)
                is RedditSearch -> json.encodeToString<RedditSearch>(search)
                else -> throw RuntimeException()
            }
            val searchQueryEntity = searchQueryDao.getBySearchQuery(queryString)
            val nextPage = when (loadType) {
                LoadType.REFRESH -> null
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> {
                    searchQueryEntity?.run { remoteKeysDao.getBySearchQueryId(id) }
                        ?.nextPage
                        ?: return MediatorResult.Success(endOfPaginationReached = true)
                }
            }
            val response = when (network) {
                is WallhavenNetworkDataSource -> network.search(
                    search = search as WallhavenSearch,
                    page = nextPage?.toIntOrNull(),
                )
                is RedditNetworkDataSource -> network.search(
                    search = search as RedditSearch,
                    after = nextPage,
                )
                else -> throw RuntimeException()
            }
            val nextPageStr = when (response) {
                is NetworkWallhavenWallpapersResponse -> {
                    // if at last page, next page is null else current + 1
                    response.meta?.run {
                        if (current_page != last_page) current_page + 1 else null
                    }
                }
                is NetworkRedditSearchResponse -> response.data.after
                else -> throw RuntimeException()
            }
            appDatabase.withTransaction {
                val now = clock.now()
                val searchQueryId = searchQueryEntity?.id ?: searchQueryDao.upsert(
                    SearchQueryEntity(
                        id = 0,
                        queryString = queryString,
                        lastUpdatedOn = now,
                    ),
                ).first()

                if (loadType == LoadType.REFRESH) {
                    remoteKeysDao.deleteBySearchQueryId(searchQueryId)
                    when (wallpapersDao) {
                        is WallhavenWallpapersDao -> {
                            wallpapersDao.deleteAllUniqueToSearchQueryId(searchQueryId)
                        }
                        is RedditWallpapersDao -> {
                            wallpapersDao.deleteAllUniqueToSearchQueryId(searchQueryId)
                        }
                    }
                    when (searchQueryWallpapersDao) {
                        is WallhavenSearchQueryWallpapersDao -> {
                            searchQueryWallpapersDao.deleteBySearchQueryId(searchQueryId)
                        }
                        is RedditSearchQueryWallpapersDao -> {
                            searchQueryWallpapersDao.deleteBySearchQueryId(searchQueryId)
                        }
                    }
                    (searchQueryEntity ?: searchQueryDao.getById(searchQueryId))?.run {
                        // insert or update search query in db
                        searchQueryDao.upsert(copy(lastUpdatedOn = now))
                    }
                }

                // Update RemoteKey for this query.
                val remoteKey = remoteKeysDao.getBySearchQueryId(searchQueryId)
                val updatedRemoteKey = remoteKey?.copy(nextPage = nextPageStr.toString())
                    ?: SearchQueryRemoteKeyEntity(
                        id = 0,
                        searchQueryId = searchQueryId,
                        nextPage = nextPageStr.toString(),
                    )
                remoteKeysDao.insertOrReplace(updatedRemoteKey)

                val (wallpaperEntities, idMap) = insertWallpapers(response)

                // update mapping table
                when (searchQueryWallpapersDao) {
                    is WallhavenSearchQueryWallpapersDao -> {
                        val lastOrder = if (loadType == LoadType.REFRESH) {
                            0
                        } else {
                            searchQueryWallpapersDao.getMaxOrderBySearchQueryId(
                                searchQueryId = searchQueryId,
                            ) ?: 0
                        }
                        searchQueryWallpapersDao.insert(
                            @Suppress("UNCHECKED_CAST")
                            (wallpaperEntities as List<WallhavenWallpaperEntity>)
                                .sortedBy { idMap[it.wallhavenId] }
                                .mapIndexed { i, entity ->
                                    WallhavenSearchQueryWallpaperEntity(
                                        searchQueryId = searchQueryId,
                                        wallpaperId = entity.id,
                                        order = i + lastOrder + 1,
                                    )
                                },
                        )
                    }
                    is RedditSearchQueryWallpapersDao -> {
                        val lastOrder = if (loadType == LoadType.REFRESH) {
                            0
                        } else {
                            searchQueryWallpapersDao.getMaxOrderBySearchQueryId(
                                searchQueryId = searchQueryId,
                            ) ?: 0
                        }
                        searchQueryWallpapersDao.insert(
                            @Suppress("UNCHECKED_CAST")
                            (wallpaperEntities as List<RedditWallpaperEntity>)
                                .sortedBy { idMap[it.redditId] }
                                .mapIndexed { i, entity ->
                                    RedditSearchQueryWallpaperEntity(
                                        searchQueryId = searchQueryId,
                                        wallpaperId = entity.id,
                                        order = i + lastOrder + 1,
                                    )
                                },
                        )
                    }
                }
            }

            MediatorResult.Success(endOfPaginationReached = nextPageStr == null)
        } catch (e: IOException) {
            MediatorResult.Error(e)
        } catch (e: HttpException) {
            MediatorResult.Error(e)
        }
    }

    private suspend fun insertWallpapers(
        response: OnlineSourceWallpapersNetworkResponse,
    ): Pair<List<OnlineSourceWallpaperEntity>, Map<String, Int>> {
        return when (response) {
            is NetworkWallhavenWallpapersResponse -> insertWallhavenWallpapers(response)
            is NetworkRedditSearchResponse -> insertRedditWallpapers(response)
            else -> throw RuntimeException()
        }
    }

    private suspend fun insertWallhavenWallpapers(
        response: NetworkWallhavenWallpapersResponse,
    ): Pair<List<WallhavenWallpaperEntity>, Map<String, Int>> {
        val networkWallpapers = response.data
        val wallhavenWallpaperIds = networkWallpapers.map { it.id }
        (wallpapersDao as WallhavenWallpapersDao).insert(
            networkWallpapers.map { it.toWallpaperEntity() },
        )
        val entities = wallpapersDao.getByWallhavenIds(wallhavenWallpaperIds)
        return entities to wallhavenWallpaperIds.indexMap()
    }

    private suspend fun insertRedditWallpapers(
        response: NetworkRedditSearchResponse,
    ): Pair<List<RedditWallpaperEntity>, Map<String, Int>> {
        val redditData = response.data
        val entities = redditData.children
            .filter {
                it.data.thumbnail != "default" && !it.data.is_video
            }
            .flatMap {
                // one post can contain multiple wallpapers
                it.data.toWallpaperEntities()
            }
        val redditIds = entities.map { it.redditId }
        (wallpapersDao as RedditWallpapersDao).insert(entities)
        val entities1 = wallpapersDao.getByRedditIds(redditIds)
        return entities1 to redditIds.indexMap()
    }
}
