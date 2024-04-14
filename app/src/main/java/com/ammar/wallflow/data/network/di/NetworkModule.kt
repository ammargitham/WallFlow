package com.ammar.wallflow.data.network.di

import com.ammar.wallflow.BuildConfig
import com.ammar.wallflow.MIME_TYPE_JSON
import com.ammar.wallflow.REDDIT_BASE_URL
import com.ammar.wallflow.WALLHAVEN_BASE_URL
import com.ammar.wallflow.data.network.RedditNetworkDataSource
import com.ammar.wallflow.data.network.WallhavenNetworkDataSource
import com.ammar.wallflow.data.network.retrofit.DocumentConverterFactory
import com.ammar.wallflow.data.network.retrofit.RetrofitWallhavenNetwork
import com.ammar.wallflow.data.network.retrofit.WallhavenInterceptor
import com.ammar.wallflow.data.network.retrofit.api.WallhavenNetworkApi
import com.ammar.wallflow.data.network.retrofit.reddit.RedditNetworkApi
import com.ammar.wallflow.data.network.retrofit.reddit.RetrofitRedditNetwork
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun providesNetworkJson(): Json = Json {
        ignoreUnknownKeys = true
    }

    @Provides
    @Singleton
    fun providesOkHttpClient(
        wallHavenInterceptor: WallhavenInterceptor,
    ) = OkHttpClient.Builder().apply {
        readTimeout(30, TimeUnit.SECONDS)
        addInterceptor(wallHavenInterceptor)
        if (BuildConfig.DEBUG) {
            addInterceptor(
                HttpLoggingInterceptor().apply {
                    setLevel(HttpLoggingInterceptor.Level.BASIC)
                },
            )
        }
    }.build()

    @Provides
    @Singleton
    fun providesWallhavenNetworkApi(
        networkJson: Json,
        okHttpClient: OkHttpClient,
    ): WallhavenNetworkApi = Retrofit.Builder().apply {
        baseUrl(WALLHAVEN_BASE_URL)
        client(okHttpClient)
        addConverterFactory(DocumentConverterFactory())
        addConverterFactory(
            networkJson.asConverterFactory(MIME_TYPE_JSON.toMediaType()),
        )
    }
        .build()
        .create(WallhavenNetworkApi::class.java)

    @Provides
    @Singleton
    fun providesWallhavenNetworkDataSource(
        wallhavenNetworkApi: WallhavenNetworkApi,
    ): WallhavenNetworkDataSource = RetrofitWallhavenNetwork(wallhavenNetworkApi)

    @Provides
    @Singleton
    fun providesRedditNetworkApi(
        networkJson: Json,
        okHttpClient: OkHttpClient,
    ): RedditNetworkApi = Retrofit.Builder().apply {
        baseUrl(REDDIT_BASE_URL)
        client(okHttpClient)
        addConverterFactory(
            networkJson.asConverterFactory(MIME_TYPE_JSON.toMediaType()),
        )
    }
        .build()
        .create(RedditNetworkApi::class.java)

    @Provides
    @Singleton
    fun providesRedditNetworkDataSource(
        redditNetworkApi: RedditNetworkApi,
    ): RedditNetworkDataSource = RetrofitRedditNetwork(redditNetworkApi)
}
