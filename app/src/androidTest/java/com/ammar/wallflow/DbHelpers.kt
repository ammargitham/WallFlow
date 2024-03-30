package com.ammar.wallflow

import com.ammar.wallflow.data.db.dao.wallhaven.WallhavenTagsDao
import com.ammar.wallflow.data.db.dao.wallhaven.WallhavenUploadersDao
import com.ammar.wallflow.data.db.dao.wallpaper.RedditWallpapersDao
import com.ammar.wallflow.data.db.dao.wallpaper.WallhavenWallpapersDao
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenWallpaperTagsEntity
import com.ammar.wallflow.data.db.entity.wallpaper.RedditWallpaperEntity
import com.ammar.wallflow.data.db.entity.wallpaper.WallhavenWallpaperEntity
import com.ammar.wallflow.data.network.model.reddit.toWallpaperEntities
import com.ammar.wallflow.data.network.model.wallhaven.toEntity
import com.ammar.wallflow.data.network.model.wallhaven.toWallpaperEntity
import kotlin.random.Random
import kotlinx.datetime.Clock

suspend fun insertWallhavenEntities(
    random: Random,
    clock: Clock,
    tagsDao: WallhavenTagsDao,
    uploadersDao: WallhavenUploadersDao,
    wallhavenWallpapersDao: WallhavenWallpapersDao,
    allWallhavenUploadersNull: Boolean = false,
    count: Int = 10,
): List<WallhavenWallpaperEntity> {
    val networkWallhavenWallpapers = MockFactory.generateNetworkWallhavenWallpapers(
        size = count,
        random = random,
        clock = clock,
        allUploadersNull = allWallhavenUploadersNull,
    )
    val networkUploaders = networkWallhavenWallpapers.mapNotNull { it.uploader }
    val networkTags = networkWallhavenWallpapers.flatMap { it.tags ?: emptyList() }
    tagsDao.insert(networkTags.map { it.toEntity() })
    // need wallhaven tag id to wallhaven wallpaper id mapping
    val wallpaperTagsMap = networkWallhavenWallpapers.associate {
        it.id to it.tags?.map { t -> t.id }
    }
    val dbTagMap = tagsDao.getAll().associateBy { it.wallhavenId }

    uploadersDao.insert(networkUploaders.map { it.toEntity() })

    val wallpaperEntities = networkWallhavenWallpapers.map {
        it.toWallpaperEntity()
    }
    wallhavenWallpapersDao.insert(wallpaperEntities)

    val wallpaperMap = wallhavenWallpapersDao.getAll().associateBy { it.wallhavenId }
    val wallpaperTagEntities = wallpaperMap.flatMap {
        val whTagIds = wallpaperTagsMap[it.key] ?: emptyList()
        val tagDbIds = whTagIds.mapNotNull { tId -> dbTagMap[tId]?.id }
        tagDbIds.map { tDbId ->
            WallhavenWallpaperTagsEntity(
                wallpaperId = it.value.id,
                tagId = tDbId,
            )
        }
    }
    wallhavenWallpapersDao.insertWallpaperTagMappings(wallpaperTagEntities)
    return wallpaperEntities
}

suspend fun insertRedditEntities(
    random: Random,
    redditWallpapersDao: RedditWallpapersDao,
    count: Int = 10,
): List<RedditWallpaperEntity> {
    val networkRedditPosts = MockFactory.generateNetworkRedditPosts(
        random = random,
        size = count,
    )
    val wallpaperEntities = networkRedditPosts.flatMap {
        it.toWallpaperEntities()
    }
    redditWallpapersDao.insert(wallpaperEntities)
    return wallpaperEntities
}
