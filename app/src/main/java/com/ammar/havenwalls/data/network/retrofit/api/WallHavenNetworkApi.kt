package com.ammar.havenwalls.data.network.retrofit.api

import com.ammar.havenwalls.data.network.model.NetworkResponse
import com.ammar.havenwalls.data.network.model.NetworkWallpaper
import org.jsoup.nodes.Document
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface WallHavenNetworkApi {
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
        @Query("page") page: Int? = null,
        @Query("seed") seed: String? = null,
    ): NetworkResponse<List<NetworkWallpaper>>

    @GET("w/{id}")
    suspend fun wallpaper(
        @Path("id") id: String,
    ): NetworkResponse<NetworkWallpaper>

    @GET("https://wallhaven.cc/tags/popular")
    suspend fun popularTags(): Document?
}
