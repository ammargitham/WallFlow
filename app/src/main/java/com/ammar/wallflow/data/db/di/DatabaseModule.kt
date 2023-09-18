package com.ammar.wallflow.data.db.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ammar.wallflow.IoDispatcher
import com.ammar.wallflow.data.db.database.AppDatabase
import com.ammar.wallflow.data.db.di.ManualMigrations.MIGRATION_1_2
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

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {
    @Provides
    fun provideLastUpdatedDao(appDatabase: AppDatabase) = appDatabase.lastUpdatedDao()

    @Provides
    fun providePopularTagsDao(appDatabase: AppDatabase) = appDatabase.popularTagsDao()

    @Provides
    fun provideSearchQueryDao(appDatabase: AppDatabase) = appDatabase.searchQueryDao()

    @Provides
    fun provideSearchQueryRemoteKeysDao(appDatabase: AppDatabase) =
        appDatabase.searchQueryRemoteKeysDao()

    @Provides
    fun provideSearchQueryWallpapersDao(appDatabase: AppDatabase) =
        appDatabase.searchQueryWallpapersDao()

    @Provides
    fun provideWallpapersDao(appDatabase: AppDatabase) = appDatabase.wallpapersDao()

    @Provides
    fun provideTagsDao(appDatabase: AppDatabase) = appDatabase.tagsDao()

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
    fun providesUploadersDao(appDatabase: AppDatabase) = appDatabase.uploadersDao()

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
            addMigrations(
                MIGRATION_1_2,
            )
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
