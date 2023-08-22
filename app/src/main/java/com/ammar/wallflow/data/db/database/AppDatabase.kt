package com.ammar.wallflow.data.db.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ammar.wallflow.data.db.converters.Converters
import com.ammar.wallflow.data.db.dao.AutoWallpaperHistoryDao
import com.ammar.wallflow.data.db.dao.FavoriteDao
import com.ammar.wallflow.data.db.dao.LastUpdatedDao
import com.ammar.wallflow.data.db.dao.ObjectDetectionModelDao
import com.ammar.wallflow.data.db.dao.PopularTagsDao
import com.ammar.wallflow.data.db.dao.SavedSearchDao
import com.ammar.wallflow.data.db.dao.SearchHistoryDao
import com.ammar.wallflow.data.db.dao.SearchQueryDao
import com.ammar.wallflow.data.db.dao.SearchQueryRemoteKeysDao
import com.ammar.wallflow.data.db.dao.SearchQueryWallpapersDao
import com.ammar.wallflow.data.db.dao.TagsDao
import com.ammar.wallflow.data.db.dao.UploadersDao
import com.ammar.wallflow.data.db.dao.WallpapersDao
import com.ammar.wallflow.data.db.entity.AutoWallpaperHistoryEntity
import com.ammar.wallflow.data.db.entity.FavoriteEntity
import com.ammar.wallflow.data.db.entity.LastUpdatedEntity
import com.ammar.wallflow.data.db.entity.ObjectDetectionModelEntity
import com.ammar.wallflow.data.db.entity.PopularTagEntity
import com.ammar.wallflow.data.db.entity.SavedSearchEntity
import com.ammar.wallflow.data.db.entity.SearchHistoryEntity
import com.ammar.wallflow.data.db.entity.SearchQueryEntity
import com.ammar.wallflow.data.db.entity.SearchQueryRemoteKeyEntity
import com.ammar.wallflow.data.db.entity.SearchQueryWallpaperEntity
import com.ammar.wallflow.data.db.entity.TagEntity
import com.ammar.wallflow.data.db.entity.UploaderEntity
import com.ammar.wallflow.data.db.entity.WallpaperEntity
import com.ammar.wallflow.data.db.entity.WallpaperTagsEntity

@Database(
    entities = [
        LastUpdatedEntity::class,
        PopularTagEntity::class,
        SearchQueryEntity::class,
        SearchQueryRemoteKeyEntity::class,
        SearchQueryWallpaperEntity::class,
        WallpaperEntity::class,
        UploaderEntity::class,
        TagEntity::class,
        WallpaperTagsEntity::class,
        SearchHistoryEntity::class,
        ObjectDetectionModelEntity::class,
        SavedSearchEntity::class,
        AutoWallpaperHistoryEntity::class,
        FavoriteEntity::class,
    ],
    version = 3,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun lastUpdatedDao(): LastUpdatedDao
    abstract fun popularTagsDao(): PopularTagsDao
    abstract fun searchQueryDao(): SearchQueryDao
    abstract fun searchQueryRemoteKeysDao(): SearchQueryRemoteKeysDao
    abstract fun searchQueryWallpapersDao(): SearchQueryWallpapersDao
    abstract fun wallpapersDao(): WallpapersDao
    abstract fun tagsDao(): TagsDao
    abstract fun uploadersDao(): UploadersDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun objectDetectionModelDao(): ObjectDetectionModelDao
    abstract fun savedSearchDao(): SavedSearchDao
    abstract fun autoWallpaperHistoryDao(): AutoWallpaperHistoryDao
    abstract fun favoriteDao(): FavoriteDao
}
