package com.ammar.wallflow.data.repository.reddit

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.map
import com.ammar.wallflow.IoDispatcher
import com.ammar.wallflow.data.db.AppDatabase
import com.ammar.wallflow.data.db.entity.reddit.RedditWallpaperEntity
import com.ammar.wallflow.data.db.entity.reddit.toWallpaper
import com.ammar.wallflow.data.network.RedditNetworkDataSource
import com.ammar.wallflow.data.repository.WallpapersRemoteMediator
import com.ammar.wallflow.data.repository.utils.Resource
import com.ammar.wallflow.json
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.model.reddit.RedditWallpaper
import com.ammar.wallflow.model.search.RedditSearch
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString

class DefaultRedditRepository @Inject constructor(
    private val appDatabase: AppDatabase,
    private val dataSource: RedditNetworkDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : RedditRepository {
    private val wallpapersDao = appDatabase.redditWallpapersDao()

    @OptIn(ExperimentalPagingApi::class)
    override fun wallpapersPager(
        search: RedditSearch,
        pageSize: Int,
        prefetchDistance: Int,
        initialLoadSize: Int,
    ) = Pager(
        config = PagingConfig(
            pageSize = pageSize,
            prefetchDistance = prefetchDistance,
            initialLoadSize = initialLoadSize,
        ),
        remoteMediator = WallpapersRemoteMediator(
            search,
            appDatabase,
            dataSource,
        ),
        pagingSourceFactory = {
            wallpapersDao.pagingSource(queryString = json.encodeToString(search))
        },
    ).flow.map {
        it.map<RedditWallpaperEntity, Wallpaper> { entity ->
            entity.toWallpaper()
        }
    }.flowOn(ioDispatcher)

    override fun wallpaper(wallpaperId: String): Flow<Resource<RedditWallpaper?>> = flow {
        val entity = wallpapersDao.getByRedditId(wallpaperId)
        if (entity == null) {
            emit(Resource.Error(IllegalArgumentException("ID does not exist")))
            return@flow
        }
        emit(Resource.Success(entity.toWallpaper()))
    }.flowOn(ioDispatcher)

    override suspend fun insertWallpaperEntities(
        entities: Collection<RedditWallpaperEntity>,
    ): Unit = withContext(ioDispatcher) {
        val redditIds = entities.map { it.redditId }
        val existingWallpapers = wallpapersDao.getByRedditIds(redditIds)
        val existingMap = existingWallpapers.associateBy { it.redditId }
        val entitiesToInsert = entities.filter {
            // only take non-existing
            existingMap[it.redditId] == null
        }.map {
            // reset id
            it.copy(id = 0)
        }
        wallpapersDao.insert(entitiesToInsert)
    }
}
