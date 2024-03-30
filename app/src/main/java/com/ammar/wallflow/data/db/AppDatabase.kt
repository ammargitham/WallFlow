package com.ammar.wallflow.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ammar.wallflow.data.db.automigrations.AutoMigration6To7
import com.ammar.wallflow.data.db.converters.Converters
import com.ammar.wallflow.data.db.dao.AutoWallpaperHistoryDao
import com.ammar.wallflow.data.db.dao.FavoriteDao
import com.ammar.wallflow.data.db.dao.LastUpdatedDao
import com.ammar.wallflow.data.db.dao.LightDarkDao
import com.ammar.wallflow.data.db.dao.ObjectDetectionModelDao
import com.ammar.wallflow.data.db.dao.RateLimitDao
import com.ammar.wallflow.data.db.dao.ViewedDao
import com.ammar.wallflow.data.db.dao.search.SavedSearchDao
import com.ammar.wallflow.data.db.dao.search.SearchHistoryDao
import com.ammar.wallflow.data.db.dao.search.SearchQueryDao
import com.ammar.wallflow.data.db.dao.search.SearchQueryRemoteKeysDao
import com.ammar.wallflow.data.db.dao.wallhaven.WallhavenPopularTagsDao
import com.ammar.wallflow.data.db.dao.wallhaven.WallhavenTagsDao
import com.ammar.wallflow.data.db.dao.wallhaven.WallhavenUploadersDao
import com.ammar.wallflow.data.db.dao.wallpaper.RedditSearchQueryWallpapersDao
import com.ammar.wallflow.data.db.dao.wallpaper.RedditWallpapersDao
import com.ammar.wallflow.data.db.dao.wallpaper.WallhavenSearchQueryWallpapersDao
import com.ammar.wallflow.data.db.dao.wallpaper.WallhavenWallpapersDao
import com.ammar.wallflow.data.db.entity.AutoWallpaperHistoryEntity
import com.ammar.wallflow.data.db.entity.FavoriteEntity
import com.ammar.wallflow.data.db.entity.LastUpdatedEntity
import com.ammar.wallflow.data.db.entity.LightDarkEntity
import com.ammar.wallflow.data.db.entity.ObjectDetectionModelEntity
import com.ammar.wallflow.data.db.entity.RateLimitEntity
import com.ammar.wallflow.data.db.entity.ViewedEntity
import com.ammar.wallflow.data.db.entity.reddit.RedditSearchQueryWallpaperEntity
import com.ammar.wallflow.data.db.entity.search.SavedSearchEntity
import com.ammar.wallflow.data.db.entity.search.SearchHistoryEntity
import com.ammar.wallflow.data.db.entity.search.SearchQueryEntity
import com.ammar.wallflow.data.db.entity.search.SearchQueryRemoteKeyEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenPopularTagEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenSearchQueryWallpaperEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenTagEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenUploaderEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenWallpaperTagsEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenWallpaperUploaderEntity
import com.ammar.wallflow.data.db.entity.wallpaper.RedditWallpaperEntity
import com.ammar.wallflow.data.db.entity.wallpaper.WallhavenWallpaperEntity

@Database(
    entities = [
        LastUpdatedEntity::class,
        WallhavenPopularTagEntity::class,
        SearchQueryEntity::class,
        SearchQueryRemoteKeyEntity::class,
        WallhavenSearchQueryWallpaperEntity::class,
        WallhavenWallpaperEntity::class,
        WallhavenUploaderEntity::class,
        WallhavenTagEntity::class,
        WallhavenWallpaperTagsEntity::class,
        WallhavenWallpaperUploaderEntity::class,
        SearchHistoryEntity::class,
        ObjectDetectionModelEntity::class,
        SavedSearchEntity::class,
        AutoWallpaperHistoryEntity::class,
        FavoriteEntity::class,
        RateLimitEntity::class,
        RedditWallpaperEntity::class,
        RedditSearchQueryWallpaperEntity::class,
        ViewedEntity::class,
        LightDarkEntity::class,
    ],
    version = 11,
    autoMigrations = [
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 4, to = 5),
        AutoMigration(
            from = 6,
            to = 7,
            spec = AutoMigration6To7::class,
        ),
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 10, to = 11),
    ],
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun lastUpdatedDao(): LastUpdatedDao
    abstract fun wallhavenPopularTagsDao(): WallhavenPopularTagsDao
    abstract fun searchQueryDao(): SearchQueryDao
    abstract fun searchQueryRemoteKeysDao(): SearchQueryRemoteKeysDao
    abstract fun wallhavenSearchQueryWallpapersDao(): WallhavenSearchQueryWallpapersDao
    abstract fun wallhavenWallpapersDao(): WallhavenWallpapersDao
    abstract fun wallhavenTagsDao(): WallhavenTagsDao
    abstract fun wallhavenUploadersDao(): WallhavenUploadersDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun objectDetectionModelDao(): ObjectDetectionModelDao
    abstract fun savedSearchDao(): SavedSearchDao
    abstract fun autoWallpaperHistoryDao(): AutoWallpaperHistoryDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun rateLimitDao(): RateLimitDao
    abstract fun redditWallpapersDao(): RedditWallpapersDao
    abstract fun redditSearchQueryWallpapersDao(): RedditSearchQueryWallpapersDao
    abstract fun viewedDao(): ViewedDao
    abstract fun lightDarkDao(): LightDarkDao
}
