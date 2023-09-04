package com.ammar.wallflow.data.di

import com.ammar.wallflow.data.repository.DefaultWallhavenRepository
import com.ammar.wallflow.data.repository.WallhavenRepository
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
    fun bindsWallHavenRepository(
        wallHavenRepository: DefaultWallhavenRepository,
    ): WallhavenRepository
}
