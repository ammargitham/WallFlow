package com.ammar.wallflow.data.db.dao.wallpaper

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ammar.wallflow.data.db.entity.reddit.RedditSearchQueryWallpaperEntity

@Dao
interface RedditSearchQueryWallpapersDao : SearchQueryWallpapersDao {
    @Query("SELECT * FROM reddit_search_query_wallpapers WHERE search_query_id = :searchQueryId")
    suspend fun getBySearchQueryId(searchQueryId: Long): List<RedditSearchQueryWallpaperEntity>

    @Query(
        """
            SELECT MAX(COALESCE(rsqw.`order`, rw.ROWID))
            FROM reddit_wallpapers rw
            INNER JOIN reddit_search_query_wallpapers rsqw
                ON rw.id = rsqw.wallpaper_id
            WHERE rsqw.search_query_id = :searchQueryId
        """,
    )
    suspend fun getMaxOrderBySearchQueryId(searchQueryId: Long): Int?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(searchQueryWallpapers: Collection<RedditSearchQueryWallpaperEntity>)

    @Query("DELETE FROM reddit_search_query_wallpapers WHERE search_query_id = :searchQueryId")
    suspend fun deleteBySearchQueryId(searchQueryId: Long)
}
