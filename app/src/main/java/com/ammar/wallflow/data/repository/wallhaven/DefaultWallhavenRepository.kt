package com.ammar.wallflow.data.repository.wallhaven

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.room.withTransaction
import com.ammar.wallflow.IoDispatcher
import com.ammar.wallflow.data.db.AppDatabase
import com.ammar.wallflow.data.db.entity.LastUpdatedCategory
import com.ammar.wallflow.data.db.entity.LastUpdatedEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenPopularTagEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenTagEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenUploaderEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenWallpaperEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenWallpaperTagsEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallpaperWithUploaderAndTags
import com.ammar.wallflow.data.db.entity.wallhaven.asTag
import com.ammar.wallflow.data.db.entity.wallhaven.toWallpaper
import com.ammar.wallflow.data.network.WallhavenNetworkDataSource
import com.ammar.wallflow.data.network.model.NetworkWallhavenTag
import com.ammar.wallflow.data.network.model.NetworkWallhavenUploader
import com.ammar.wallflow.data.network.model.NetworkWallhavenWallpaper
import com.ammar.wallflow.data.network.model.toEntity
import com.ammar.wallflow.data.network.model.toWallpaperEntity
import com.ammar.wallflow.data.repository.WallpapersRemoteMediator
import com.ammar.wallflow.data.repository.utils.NetworkBoundResource
import com.ammar.wallflow.data.repository.utils.Resource
import com.ammar.wallflow.data.repository.utils.WallhavenTagsDocumentParser.parsePopularTags
import com.ammar.wallflow.extensions.TAG
import com.ammar.wallflow.model.Purity
import com.ammar.wallflow.model.SearchQuery
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.model.wallhaven.WallhavenTag
import com.ammar.wallflow.model.wallhaven.WallhavenWallpaper
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@OptIn(ExperimentalPagingApi::class)
class DefaultWallhavenRepository @Inject constructor(
    private val appDatabase: AppDatabase,
    private val wallHavenNetwork: WallhavenNetworkDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : WallhavenRepository {
    private val popularTagsDao = appDatabase.wallhavenPopularTagsDao()
    private val lastUpdatedDao = appDatabase.lastUpdatedDao()
    private val wallpapersDao = appDatabase.wallhavenWallpapersDao()
    private val tagsDao = appDatabase.wallhavenTagsDao()
    private val uploadersDao = appDatabase.wallhavenUploadersDao()

    private val popularWallhavenTagNetworkResource =
        object : NetworkBoundResource<
            List<WallhavenTagEntity>,
            List<WallhavenTag>,
            List<NetworkWallhavenTag>,
            >(
            initialValue = emptyList(),
            ioDispatcher = ioDispatcher,
        ) {
            override suspend fun loadFromDb(): List<WallhavenTagEntity> = popularTagsDao
                .getAllWithDetails()
                .map { it.tagEntity }

            override suspend fun shouldFetchData(dbData: List<WallhavenTagEntity>): Boolean {
                return if (dbData.isEmpty()) {
                    true
                } else {
                    val lastUpdated = lastUpdatedDao.getByKey(
                        LastUpdatedCategory.POPULAR_TAGS.key,
                    ) ?: return true
                    val duration = lastUpdated.lastUpdatedOn - Clock.System.now()
                    duration.absoluteValue.inWholeMinutes / 60f > 3
                }
            }

            override suspend fun fetchFromNetwork(
                dbData: List<WallhavenTagEntity>,
            ): List<NetworkWallhavenTag> {
                val doc = wallHavenNetwork.popularTags()
                return doc?.let(::parsePopularTags) ?: emptyList()
            }

            override suspend fun saveFetchResult(fetchResult: List<NetworkWallhavenTag>) {
                appDatabase.withTransaction {
                    popularTagsDao.deleteAll()
                    // insert non-existing tags first
                    val tagIds = upsertTags(fetchResult, false).map { it.id }
                    // create and insert PopularTagEntities
                    popularTagsDao.insert(tagIds.map { WallhavenPopularTagEntity(0, it) })
                    upsertLastUpdated(LastUpdatedCategory.POPULAR_TAGS)
                }
            }

            override fun entityConverter(dbData: List<WallhavenTagEntity>) = dbData.map {
                it.asTag()
            }

            override fun onFetchFailed(throwable: Throwable) {
                Log.e(TAG, "onFetchFailed: ", throwable)
            }
        }

    internal suspend fun upsertTags(
        tags: List<NetworkWallhavenTag>,
        shouldUpdate: Boolean,
    ) = withContext(ioDispatcher) {
        val existingTags = tagsDao.getByWallhavenIds(tags.map { it.id })

        if (shouldUpdate) {
            // update existing tags
            val tagWallhavenIdMap = tags.associateBy { it.id }
            val updatedTags = existingTags.map {
                val networkTag = tagWallhavenIdMap[it.wallhavenId] ?: return@map null
                it.copy(
                    name = networkTag.name,
                    alias = networkTag.alias,
                    categoryId = networkTag.category_id,
                    category = networkTag.category,
                    purity = Purity.fromName(networkTag.purity),
                    createdAt = networkTag.created_at,
                )
            }.filterNotNull()
            tagsDao.update(updatedTags)
        }

        // insert new tags
        val existingTagWallhavenIds = existingTags.map { it.wallhavenId }
        val newTags = tags.filter { it.id !in existingTagWallhavenIds }.map { it.toEntity() }
        tagsDao.insert(newTags)
        tagsDao.getByNames(tags.map { it.name })
    }

    private fun getWallpaperNetworkResource(
        wallpaperWallhavenId: String,
    ) = object : NetworkBoundResource<
        WallpaperWithUploaderAndTags?,
        WallhavenWallpaper?,
        NetworkWallhavenWallpaper,
        >(
        initialValue = null,
        ioDispatcher = ioDispatcher,
    ) {
        override suspend fun loadFromDb() =
            wallpapersDao.getWithUploaderAndTagsByWallhavenId(wallpaperWallhavenId)

        override suspend fun shouldFetchData(dbData: WallpaperWithUploaderAndTags?) =
            dbData?.uploader == null

        override suspend fun fetchFromNetwork(dbData: WallpaperWithUploaderAndTags?) =
            wallHavenNetwork.wallpaper(wallpaperWallhavenId).data

        override suspend fun saveFetchResult(fetchResult: NetworkWallhavenWallpaper) {
            appDatabase.withTransaction {
                var uploaderId: Long? = null
                if (fetchResult.uploader != null) {
                    // create uploader if not exists in db
                    uploaderId = insertUploader(fetchResult.uploader)
                }
                var tagsIds: List<Long>? = null
                if (fetchResult.tags != null) {
                    // create tags if not exists in db
                    tagsIds = upsertTags(fetchResult.tags, true).map { it.id }
                }
                // insert or update wallpaper in db
                val existingWallpaper = wallpapersDao.getByWallhavenId(wallpaperWallhavenId)
                val wallpaperDbId: Long
                if (existingWallpaper != null) {
                    wallpaperDbId = existingWallpaper.id
                    wallpapersDao.update(
                        fetchResult.toWallpaperEntity(
                            id = wallpaperDbId,
                            uploaderId = uploaderId,
                        ),
                    )
                } else {
                    wallpaperDbId = wallpapersDao.insert(
                        fetchResult.toWallpaperEntity(uploaderId = uploaderId),
                    ).first()
                }
                if (existingWallpaper != null) {
                    // delete existing wallpaper tag mappings
                    wallpapersDao.deleteWallpaperTagMappings(existingWallpaper.id)
                }
                if (tagsIds != null) {
                    // insert new wallpaper tag mappings
                    wallpapersDao.insertWallpaperTagMappings(
                        tagsIds.map {
                            WallhavenWallpaperTagsEntity(
                                wallpaperId = wallpaperDbId,
                                tagId = it,
                            )
                        },
                    )
                }
            }
        }

        override fun entityConverter(dbData: WallpaperWithUploaderAndTags?) =
            dbData?.let {
                val (wallpaper, uploader, tags) = it
                wallpaper.toWallpaper(uploader, tags)
            }

        override fun onFetchFailed(throwable: Throwable) {
            Log.e(TAG, "onFetchFailed: ", throwable)
        }
    }

    internal suspend fun insertUploader(
        uploader: NetworkWallhavenUploader,
    ) = withContext(ioDispatcher) {
        val existingUploader = uploadersDao.getByUsername(uploader.username)
        existingUploader?.id ?: uploadersDao.insert(uploader.toEntity()).first()
    }

    override fun wallpapersPager(
        searchQuery: SearchQuery,
        pageSize: Int,
        prefetchDistance: Int,
        initialLoadSize: Int,
    ): Flow<PagingData<Wallpaper>> = Pager(
        config = PagingConfig(
            pageSize = pageSize,
            prefetchDistance = prefetchDistance,
            initialLoadSize = initialLoadSize,
        ),
        remoteMediator = WallpapersRemoteMediator(
            searchQuery,
            appDatabase,
            wallHavenNetwork,
        ),
        pagingSourceFactory = {
            wallpapersDao.pagingSource(queryString = searchQuery.toQueryString())
        },
    ).flow.map {
        it.map<WallhavenWallpaperEntity, Wallpaper> { entity ->
            entity.toWallpaper()
        }
    }.flowOn(ioDispatcher)

    override fun popularTags() = flow {
        withContext(ioDispatcher) {
            popularWallhavenTagNetworkResource.init()
        }
        emitAll(popularWallhavenTagNetworkResource.data)
    }

    override suspend fun refreshPopularTags() {
        popularWallhavenTagNetworkResource.refresh()
    }

    override fun wallpaper(wallpaperWallhavenId: String): Flow<Resource<WallhavenWallpaper?>> {
        val resource = getWallpaperNetworkResource(wallpaperWallhavenId)
        return flow {
            withContext(ioDispatcher) {
                resource.init()
            }
            emitAll(resource.data)
        }
    }

    internal suspend fun upsertLastUpdated(
        category: LastUpdatedCategory,
        lastUpdatedOn: Instant = Clock.System.now(),
    ) {
        val lastUpdated = lastUpdatedDao.getByKey(category.key)
            ?.copy(lastUpdatedOn = lastUpdatedOn)
            ?: LastUpdatedEntity(
                id = 0,
                key = category.key,
                lastUpdatedOn = lastUpdatedOn,
            )
        lastUpdatedDao.upsert(lastUpdated)
    }

    override suspend fun insertTagEntities(
        tags: Collection<WallhavenTagEntity>,
    ): Unit = withContext(ioDispatcher) {
        val existingTags = tagsDao.getByWallhavenIds(tags.map { it.wallhavenId })
        val existingMap = existingTags.associateBy { it.wallhavenId }
        val insertTags = tags.filter {
            // only take non-existing
            existingMap[it.wallhavenId] == null
        }.map {
            // reset id
            it.copy(id = 0)
        }
        tagsDao.insert(insertTags)
    }

    override suspend fun insertUploaderEntities(
        uploaders: Collection<WallhavenUploaderEntity>,
    ): Unit = withContext(ioDispatcher) {
        val existingUploaders = uploadersDao.getByUsernames(uploaders.map { it.username })
        val existingMap = existingUploaders.associateBy { it.username }
        val insertUploaders = uploaders.filter {
            // only take non-existing
            existingMap[it.username] == null
        }.map {
            // reset id
            it.copy(id = 0)
        }
        uploadersDao.insert(insertUploaders)
    }

    override suspend fun insertWallpaperEntities(
        entities: Collection<WallhavenWallpaperEntity>,
    ): Unit = withContext(ioDispatcher) {
        val wallhavenIds = entities.map { it.wallhavenId }
        val existingWallpapers = wallpapersDao.getAllByWallhavenIds(wallhavenIds)
        val existingMap = existingWallpapers.associateBy { it.wallhavenId }
        val entitiesToInsert = entities.filter {
            // only take non-existing
            existingMap[it.wallhavenId] == null
        }.map {
            // reset id
            it.copy(id = 0)
        }
        wallpapersDao.insert(entitiesToInsert)
    }
}
