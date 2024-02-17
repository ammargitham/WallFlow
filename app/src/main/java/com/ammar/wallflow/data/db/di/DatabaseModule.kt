package com.ammar.wallflow.data.db.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ammar.wallflow.IoDispatcher
import com.ammar.wallflow.data.db.AppDatabase
import com.ammar.wallflow.data.db.manualmigrations.MIGRATION_1_2
import com.ammar.wallflow.data.db.manualmigrations.MIGRATION_3_4
import com.ammar.wallflow.data.db.manualmigrations.MIGRATION_5_6
import com.ammar.wallflow.extensions.TAG
import com.ammar.wallflow.model.ObjectDetectionModel
import com.ammar.wallflow.model.toEntity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

val allManualMigrations = arrayOf(
    MIGRATION_1_2,
    MIGRATION_3_4,
    MIGRATION_5_6,
)

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {
    @Provides
    fun provideLastUpdatedDao(appDatabase: AppDatabase) = appDatabase.lastUpdatedDao()

    @Provides
    fun providesWallhavenPopularTagsDao(appDatabase: AppDatabase) =
        appDatabase.wallhavenPopularTagsDao()

    @Provides
    fun searchQueryDao(appDatabase: AppDatabase) = appDatabase.searchQueryDao()

    @Provides
    fun providesSearchQueryRemoteKeysDao(appDatabase: AppDatabase) =
        appDatabase.searchQueryRemoteKeysDao()

    @Provides
    fun providesWallhavenSearchQueryWallpapersDao(appDatabase: AppDatabase) =
        appDatabase.wallhavenSearchQueryWallpapersDao()

    @Provides
    fun providesWallhavenWallpapersDao(appDatabase: AppDatabase) =
        appDatabase.wallhavenWallpapersDao()

    @Provides
    fun providesWallhavenTagsDao(appDatabase: AppDatabase) = appDatabase.wallhavenTagsDao()

    @Provides
    fun providesSearchHistoryDao(appDatabase: AppDatabase) = appDatabase.searchHistoryDao()

    @Provides
    fun providesObjectDetectionModelDao(appDatabase: AppDatabase) =
        appDatabase.objectDetectionModelDao()

    @Provides
    fun providesSavedSearchDao(appDatabase: AppDatabase) = appDatabase.savedSearchDao()

    @Provides
    fun providesAutoWallpaperHistoryDao(appDatabase: AppDatabase) =
        appDatabase.autoWallpaperHistoryDao()

    @Provides
    fun providesFavoritesDao(appDatabase: AppDatabase) = appDatabase.favoriteDao()

    @Provides
    fun providesWallhavenUploadersDao(appDatabase: AppDatabase) =
        appDatabase.wallhavenUploadersDao()

    @Provides
    fun providesRateLimitDao(appDatabase: AppDatabase) = appDatabase.rateLimitDao()

    @Provides
    fun providesRedditWallpaperDao(appDatabase: AppDatabase) = appDatabase.redditWallpapersDao()

    @Provides
    fun providesViewedDao(appDatabase: AppDatabase) = appDatabase.viewedDao()

    @Provides
    fun providesLightDarkDao(appDatabase: AppDatabase) = appDatabase.lightDarkDao()

    lateinit var appDatabase: AppDatabase

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): AppDatabase {
        appDatabase = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app",
        ).apply {
            addMigrations(*allManualMigrations)
            addCallback(
                object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        val handler = CoroutineExceptionHandler { _, e ->
                            Log.e(TAG, "onCreate: ", e)
                        }
                        CoroutineScope(ioDispatcher).launch(handler) {
                            // insert default models
                            Log.i(TAG, "onCreate: Inserting default model")
                            appDatabase.objectDetectionModelDao().upsert(
                                ObjectDetectionModel.DEFAULT.toEntity(),
                            )
                        }
                    }
                },
            )
        }.build()
        return appDatabase
    }
}
