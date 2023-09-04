package com.ammar.wallflow.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.filter
import androidx.paging.map
import com.ammar.wallflow.IoDispatcher
import com.ammar.wallflow.data.db.dao.FavoriteDao
import com.ammar.wallflow.data.db.dao.WallpapersDao
import com.ammar.wallflow.data.db.entity.FavoriteEntity
import com.ammar.wallflow.data.db.entity.asWallpaper
import com.ammar.wallflow.model.Source
import com.ammar.wallflow.model.WallhavenWallpaper
import com.ammar.wallflow.model.wallhavenWallpaper1
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

@Singleton
class FavoritesRepository @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val wallpapersDao: WallpapersDao,
    @IoDispatcher val ioDispatcher: CoroutineDispatcher,
) {
    fun observeAll() = favoriteDao.observeAll()

    @OptIn(ExperimentalPagingApi::class)
    fun favoriteWallpapersPager(
        pageSize: Int = 24,
        prefetchDistance: Int = pageSize,
        initialLoadSize: Int = pageSize * 3,
    ): Flow<PagingData<WallhavenWallpaper>> = Pager(
        config = PagingConfig(
            pageSize = pageSize,
            prefetchDistance = prefetchDistance,
            initialLoadSize = initialLoadSize,
        ),
        remoteMediator = null,
        pagingSourceFactory = { favoriteDao.pagingSource() },
    ).flow.map {
        it.map { entity ->
            when (entity.source) {
                Source.WALLHAVEN -> {
                    val wallpaperEntity = wallpapersDao.getByWallhavenId(entity.sourceId)
                    wallpaperEntity?.asWallpaper() ?: wallhavenWallpaper1
                }
            }
        }.filter { wallpaper -> wallpaper != wallhavenWallpaper1 }
    }.flowOn(ioDispatcher)

    suspend fun toggleFavorite(
        sourceId: String,
        source: Source,
    ) = withContext(ioDispatcher) {
        val exists = favoriteDao.exists(
            sourceId = sourceId,
            source = source,
        )
        if (exists) {
            // delete it
            favoriteDao.deleteBySourceIdAndType(
                sourceId = sourceId,
                source = source,
            )
            return@withContext
        }
        favoriteDao.upsert(
            FavoriteEntity(
                id = 0,
                sourceId = sourceId,
                source = source,
                favoritedOn = Clock.System.now(),
            ),
        )
    }

    suspend fun getRandom() = withContext(ioDispatcher) {
        val entity = favoriteDao.getRandom() ?: return@withContext null
        when (entity.source) {
            Source.WALLHAVEN -> {
                val wallpaperEntity = wallpapersDao.getByWallhavenId(entity.sourceId)
                wallpaperEntity?.asWallpaper()
            }
        }
    }
}
