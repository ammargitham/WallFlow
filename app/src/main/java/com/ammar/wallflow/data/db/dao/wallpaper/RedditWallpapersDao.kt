package com.ammar.wallflow.data.db.dao.wallpaper

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ammar.wallflow.data.db.entity.wallpaper.RedditWallpaperEntity

@Dao
interface RedditWallpapersDao : WallpapersDao {
    @Query("SELECT * FROM reddit_wallpapers WHERE id = :id")
    suspend fun getById(id: Long): RedditWallpaperEntity?

    @Query("SELECT * FROM reddit_wallpapers")
    suspend fun getAll(): List<RedditWallpaperEntity>

    @Query("SELECT id FROM reddit_wallpapers")
    suspend fun getAllIds(): List<Long>

    @Query("SELECT * FROM reddit_wallpapers WHERE post_id IN (:postIds)")
    suspend fun getByPostIds(postIds: Collection<String>): List<RedditWallpaperEntity>

    @Query("SELECT * FROM reddit_wallpapers WHERE reddit_id = :redditId")
    suspend fun getByRedditId(redditId: String): RedditWallpaperEntity?

    @Query("SELECT * FROM reddit_wallpapers WHERE reddit_id IN (:redditIds)")
    suspend fun getByRedditIds(redditIds: List<String>): List<RedditWallpaperEntity>

    @Query("SELECT reddit_id FROM reddit_wallpapers")
    suspend fun getAllRedditIds(): List<String>

    @Query(
        """
            SELECT *
            FROM reddit_wallpapers
            WHERE EXISTS (
                SELECT 1
                FROM reddit_search_query_wallpapers
                WHERE wallpaper_id = reddit_wallpapers.id
                AND search_query_id = :searchQueryId
            )
            AND NOT EXISTS (
                SELECT 1
                FROM reddit_search_query_wallpapers
                WHERE wallpaper_id = reddit_wallpapers.id
                AND search_query_id <> :searchQueryId
            )
            AND NOT EXISTS (
                SELECT 1
                FROM favorites
                WHERE source_id = reddit_wallpapers.reddit_id
                AND source = 'REDDIT'
            );
        """,
    )
    suspend fun getAllUniqueToSearchQueryId(searchQueryId: Long): List<RedditWallpaperEntity>

    @Query("SELECT COUNT(1) FROM reddit_wallpapers")
    suspend fun count(): Int

    @Query(
        """
            SELECT *
            FROM reddit_wallpapers
            WHERE id IN (
                SELECT rsqw.wallpaper_id
                FROM reddit_search_query_wallpapers rsqw
                WHERE rsqw.search_query_id = (
                    SELECT sq.id
                    FROM search_query sq
                    WHERE query_string = :queryString
                )
            )
        """,
    )
    fun pagingSource(queryString: String): PagingSource<Int, RedditWallpaperEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(wallpapers: Collection<RedditWallpaperEntity>): List<Long>

    @Query(
        """
            DELETE
            FROM reddit_wallpapers
            WHERE EXISTS (
                SELECT 1
                FROM reddit_search_query_wallpapers
                WHERE wallpaper_id = reddit_wallpapers.id
                AND search_query_id = :searchQueryId
            )
            AND NOT EXISTS (
                SELECT 1
                FROM reddit_search_query_wallpapers
                WHERE wallpaper_id = reddit_wallpapers.id
                AND search_query_id <> :searchQueryId
            )
            AND NOT EXISTS (
                SELECT 1
                FROM favorites
                WHERE source_id = reddit_wallpapers.reddit_id
                AND source = 'REDDIT'
            );
        """,
    )
    suspend fun deleteAllUniqueToSearchQueryId(searchQueryId: Long)
}
