package com.ammar.havenwalls.data.di

import com.ammar.havenwalls.data.repository.DefaultWallHavenRepository
import com.ammar.havenwalls.data.repository.WallHavenRepository
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
        wallHavenRepository: DefaultWallHavenRepository,
    ): WallHavenRepository
}
