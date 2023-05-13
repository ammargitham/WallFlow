package com.ammar.havenwalls.data.db.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ammar.havenwalls.data.db.converters.Converters
import com.ammar.havenwalls.data.db.dao.LastUpdatedDao
import com.ammar.havenwalls.data.db.dao.ObjectDetectionModelDao
import com.ammar.havenwalls.data.db.dao.PopularTagsDao
import com.ammar.havenwalls.data.db.dao.SearchHistoryDao
import com.ammar.havenwalls.data.db.dao.SearchQueryDao
import com.ammar.havenwalls.data.db.dao.SearchQueryRemoteKeysDao
import com.ammar.havenwalls.data.db.dao.SearchQueryWallpapersDao
import com.ammar.havenwalls.data.db.dao.TagsDao
import com.ammar.havenwalls.data.db.dao.UploadersDao
import com.ammar.havenwalls.data.db.dao.WallpapersDao
import com.ammar.havenwalls.data.db.entity.LastUpdatedEntity
import com.ammar.havenwalls.data.db.entity.ObjectDetectionModelEntity
import com.ammar.havenwalls.data.db.entity.PopularTagEntity
import com.ammar.havenwalls.data.db.entity.SearchHistoryEntity
import com.ammar.havenwalls.data.db.entity.SearchQueryEntity
import com.ammar.havenwalls.data.db.entity.SearchQueryRemoteKeyEntity
import com.ammar.havenwalls.data.db.entity.SearchQueryWallpaperEntity
import com.ammar.havenwalls.data.db.entity.TagEntity
import com.ammar.havenwalls.data.db.entity.UploaderEntity
import com.ammar.havenwalls.data.db.entity.WallpaperEntity
import com.ammar.havenwalls.data.db.entity.WallpaperTagsEntity

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
    ],
    version = 1,
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
}
