package com.ammar.wallflow.data.di

import com.ammar.wallflow.data.repository.local.DefaultLocalWallpapersRepository
import com.ammar.wallflow.data.repository.local.LocalWallpapersRepository
import com.ammar.wallflow.data.repository.wallhaven.DefaultWallhavenRepository
import com.ammar.wallflow.data.repository.wallhaven.WallhavenRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {
    @Singleton
    @Binds
    fun bindsWallhavenRepository(
        wallHavenRepository: DefaultWallhavenRepository,
    ): WallhavenRepository

    @Singleton
    @Binds
    fun bindsLocalWallpapersRepository(
        localWallpaperRepository: DefaultLocalWallpapersRepository,
    ): LocalWallpapersRepository
}
