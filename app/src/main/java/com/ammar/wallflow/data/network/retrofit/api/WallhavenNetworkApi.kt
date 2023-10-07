package com.ammar.wallflow.data.network.retrofit.api

import com.ammar.wallflow.data.network.model.NetworkWallhavenWallpaperResponse
import com.ammar.wallflow.data.network.model.NetworkWallhavenWallpapersResponse
import org.jsoup.nodes.Document
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface WallhavenNetworkApi {
    @GET("search")
    suspend fun search(
        @Query("q") query: String? = null,
        @Query("categories") categories: String? = null,
        @Query("purity") purity: String? = null,
        @Query("sorting") sorting: String? = null,
        @Query("order") order: String? = null,
        @Query("topRange") topRange: String? = null,
        @Query("atleast") atleast: String? = null,
        @Query("resolutions") resolutions: String? = null,
        @Query("colors") colors: String? = null,
        @Query("ratios") ratios: String? = null,
        @Query("page") page: Int? = null,
        @Query("seed") seed: String? = null,
    ): NetworkWallhavenWallpapersResponse

    @GET("w/{id}")
    suspend fun wallpaper(
        @Path("id") id: String,
    ): NetworkWallhavenWallpaperResponse

    @GET("https://wallhaven.cc/tags/popular")
    suspend fun popularTags(): Document?
}
