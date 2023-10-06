package com.ammar.wallflow.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ammar.wallflow.data.db.automigrations.AutoMigration4To5Spec
import com.ammar.wallflow.data.db.converters.Converters
import com.ammar.wallflow.data.db.dao.AutoWallpaperHistoryDao
import com.ammar.wallflow.data.db.dao.FavoriteDao
import com.ammar.wallflow.data.db.dao.LastUpdatedDao
import com.ammar.wallflow.data.db.dao.ObjectDetectionModelDao
import com.ammar.wallflow.data.db.dao.SavedSearchDao
import com.ammar.wallflow.data.db.dao.SearchHistoryDao
import com.ammar.wallflow.data.db.dao.SearchQueryDao
import com.ammar.wallflow.data.db.dao.SearchQueryRemoteKeysDao
import com.ammar.wallflow.data.db.dao.wallhaven.WallhavenPopularTagsDao
import com.ammar.wallflow.data.db.dao.wallhaven.WallhavenSearchQueryWallpapersDao
import com.ammar.wallflow.data.db.dao.wallhaven.WallhavenTagsDao
import com.ammar.wallflow.data.db.dao.wallhaven.WallhavenUploadersDao
import com.ammar.wallflow.data.db.dao.wallhaven.WallhavenWallpapersDao
import com.ammar.wallflow.data.db.entity.AutoWallpaperHistoryEntity
import com.ammar.wallflow.data.db.entity.FavoriteEntity
import com.ammar.wallflow.data.db.entity.LastUpdatedEntity
import com.ammar.wallflow.data.db.entity.ObjectDetectionModelEntity
import com.ammar.wallflow.data.db.entity.WallhavenSavedSearchEntity
import com.ammar.wallflow.data.db.entity.WallhavenSearchHistoryEntity
import com.ammar.wallflow.data.db.entity.WallhavenSearchQueryEntity
import com.ammar.wallflow.data.db.entity.WallhavenSearchQueryRemoteKeyEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenPopularTagEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenSearchQueryWallpaperEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenTagEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenUploaderEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenWallpaperEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenWallpaperTagsEntity

@Database(
    entities = [
        LastUpdatedEntity::class,
        WallhavenPopularTagEntity::class,
        WallhavenSearchQueryEntity::class,
        WallhavenSearchQueryRemoteKeyEntity::class,
        WallhavenSearchQueryWallpaperEntity::class,
        WallhavenWallpaperEntity::class,
        WallhavenUploaderEntity::class,
        WallhavenTagEntity::class,
        WallhavenWallpaperTagsEntity::class,
        WallhavenSearchHistoryEntity::class,
        ObjectDetectionModelEntity::class,
        WallhavenSavedSearchEntity::class,
        AutoWallpaperHistoryEntity::class,
        FavoriteEntity::class,
    ],
    version = 5,
    autoMigrations = [
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 4, to = 5, spec = AutoMigration4To5Spec::class),
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
}
