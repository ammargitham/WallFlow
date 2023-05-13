package com.ammar.havenwalls.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ammar.havenwalls.data.db.entity.SearchQueryWallpaperEntity

@Dao
interface SearchQueryWallpapersDao {
    @Query("SELECT * FROM search_query_wallpapers WHERE search_query_id = :searchQueryId")
    suspend fun getBySearchQueryId(searchQueryId: Long): List<SearchQueryWallpaperEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg searchQueryWallpaper: SearchQueryWallpaperEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(searchQueryWallpapers: Collection<SearchQueryWallpaperEntity>)

    @Query("DELETE FROM search_query_wallpapers WHERE search_query_id = :searchQueryId")
    suspend fun deleteBySearchQueryId(searchQueryId: Long)
}
