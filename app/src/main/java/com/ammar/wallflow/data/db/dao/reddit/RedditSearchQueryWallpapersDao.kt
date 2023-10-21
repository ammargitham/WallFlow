package com.ammar.wallflow.data.db.dao.reddit

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ammar.wallflow.data.db.dao.SearchQueryWallpapersDao
import com.ammar.wallflow.data.db.entity.reddit.RedditSearchQueryWallpaperEntity

@Dao
interface RedditSearchQueryWallpapersDao : SearchQueryWallpapersDao {
    @Query("SELECT * FROM reddit_search_query_wallpapers WHERE search_query_id = :searchQueryId")
    suspend fun getBySearchQueryId(searchQueryId: Long): List<RedditSearchQueryWallpaperEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(searchQueryWallpapers: Collection<RedditSearchQueryWallpaperEntity>)

    @Query("DELETE FROM reddit_search_query_wallpapers WHERE search_query_id = :searchQueryId")
    suspend fun deleteBySearchQueryId(searchQueryId: Long)
}
