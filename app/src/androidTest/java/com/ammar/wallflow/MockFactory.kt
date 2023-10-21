package com.ammar.wallflow

import com.ammar.wallflow.data.network.model.reddit.NetworkRedditGalleryData
import com.ammar.wallflow.data.network.model.reddit.NetworkRedditGalleryDataItem
import com.ammar.wallflow.data.network.model.reddit.NetworkRedditPost
import com.ammar.wallflow.data.network.model.reddit.NetworkRedditPreview
import com.ammar.wallflow.data.network.model.reddit.NetworkRedditPreviewImage
import com.ammar.wallflow.data.network.model.reddit.NetworkRedditResolution
import com.ammar.wallflow.data.network.model.wallhaven.NetworkWallhavenTag
import com.ammar.wallflow.data.network.model.wallhaven.NetworkWallhavenThumbs
import com.ammar.wallflow.data.network.model.wallhaven.NetworkWallhavenUploader
import com.ammar.wallflow.data.network.model.wallhaven.NetworkWallhavenWallpaper
import com.ammar.wallflow.extensions.toHexString
import com.ammar.wallflow.model.search.RedditFilters
import com.ammar.wallflow.model.search.RedditSearch
import com.ammar.wallflow.model.search.RedditSort
import com.ammar.wallflow.model.search.RedditTimeRange
import com.ammar.wallflow.model.search.SavedSearch
import com.ammar.wallflow.model.search.WallhavenFilters
import com.ammar.wallflow.model.search.WallhavenSearch
import kotlin.math.absoluteValue
import kotlin.random.Random
import kotlinx.datetime.Clock

object MockFactory {
    fun generateNetworkWallhavenWallpapers(
        size: Int = 10,
        random: Random = Random,
        clock: Clock = Clock.System,
    ) = List(size) {
        generateNetworkWallhavenWallpaper(
            random = random,
            clock = clock,
        )
    }

    fun generateNetworkWallhavenWallpaper(
        random: Random = Random,
        clock: Clock = Clock.System,
        idNumber: Int = random.nextInt().absoluteValue,
    ): NetworkWallhavenWallpaper {
        val id = idNumber + 1
        return NetworkWallhavenWallpaper(
            id = "wallpaper$id",
            url = "https://example.com/wallpaper$id",
            short_url = "https://example.com/w$id",
            views = 1000,
            favorites = 50,
            source = "https://example.com/source$id",
            purity = "sfw",
            category = "landscape",
            dimension_x = 1920,
            dimension_y = 1080,
            ratio = 1920f / 1080,
            resolution = "1920 x 1080",
            file_size = 1000000,
            file_type = "jpg",
            created_at = clock.now(),
            colors = List(random.nextInt(5)) { RandomColors.nextColor(random).toHexString() },
            path = "wallpapers/wallpaper$id.jpg",
            thumbs = NetworkWallhavenThumbs(
                large = "https://example.com/wallpaper$id/large",
                original = "https://example.com/wallpaper$id/original",
                small = "https://example.com/wallpaper$id/small",
            ),
            uploader = NetworkWallhavenUploader(
                username = "uploader$id",
                group = "test",
                avatar = mapOf(
                    "large" to "https://example.com/uploader$id/large",
                    "medium" to "https://example.com/uploader$id/medium",
                    "small" to "https://example.com/uploader$id/small",
                    "tiny" to "https://example.com/uploader$id/tiny",
                ),
            ),
            tags = List(random.nextInt(5)) {
                NetworkWallhavenTag(
                    id = (it + id).toLong(),
                    name = "tag_${it + id}",
                    alias = "",
                    category_id = (it + id).toLong(),
                    category = "",
                    purity = "sfw",
                    created_at = clock.now(),
                )
            },
        )
    }

    fun generateWallhavenSavedSearches(
        size: Int = 10,
        random: Random = Random,
    ) = List(size) {
        generateWallhavenSavedSearch(
            random = random,
        )
    }

    private fun generateWallhavenSavedSearch(
        random: Random = Random,
        idNumber: Int = random.nextInt().absoluteValue,
    ): SavedSearch {
        val id = idNumber + 1
        return SavedSearch(
            id = id.toLong(),
            name = "saved_search_$id",
            search = WallhavenSearch(
                query = "test_q_$id",
                filters = WallhavenFilters(),
            ),
        )
    }

    fun generateNetworkRedditPosts(
        size: Int = 10,
        random: Random = Random,
        singleImageOnly: Boolean = false,
    ) = List(size) {
        if (singleImageOnly) {
            generateRedditImagePost(postId = random.nextInt())
        } else {
            generateNetworkRedditPost(
                random = random,
            )
        }
    }

    private fun generateNetworkRedditPost(
        random: Random = Random,
        idNumber: Int = random.nextInt().absoluteValue,
    ): NetworkRedditPost {
        val id = idNumber + 1
        val isGallery = random.nextBoolean()
        val isVideo = random.nextBoolean()
        val isNsfw = random.nextBoolean()
        return when {
            isGallery -> generateRedditGalleryPost(id)
            isNsfw -> generateRedditNSFWPost(id)
            isVideo -> generateRedditVideoPost(id)
            else -> generateRedditImagePost(id)
        }
    }

    private fun generateRedditGalleryPost(postId: Int) = NetworkRedditPost(
        id = "post_$postId",
        subreddit = "art",
        permalink = "https://example.com/permalink$postId/",
        title = "Gallery of Artwork",
        thumbnail = "https://example.com/thumbnail2_$postId.jpg",
        // thumbnail_height = 250,
        // thumbnail_width = 400,
        author = "user2",
        created_utc = 1634265000,
        is_video = false,
        is_gallery = true,
        post_hint = "image",
        media_metadata = mapOf(
            "image1_$postId" to NetworkRedditPreviewImage(
                id = "image1_$postId",
                source = NetworkRedditResolution(
                    url = "https://example.com/gallery/image1_$postId.jpg",
                    width = 1600,
                    height = 1200,
                ),
                resolutions = listOf(
                    NetworkRedditResolution(
                        url = "https://example.com/gallery/image1_small_$postId.jpg",
                        width = 400,
                        height = 300,
                    ),
                ),
            ),
            "image2_$postId" to NetworkRedditPreviewImage(
                id = "image2_$postId",
                source = NetworkRedditResolution(
                    url = "https://example.com/gallery/image2_$postId.jpg",
                    width = 1400,
                    height = 1000,
                ),
                resolutions = listOf(
                    NetworkRedditResolution(
                        url = "https://example.com/gallery/image2_small_$postId.jpg",
                        width = 350,
                        height = 250,
                    ),
                ),
            ),
        ),
        gallery_data = NetworkRedditGalleryData(
            items = listOf(
                NetworkRedditGalleryDataItem(id = 1, media_id = "image1_$postId"),
                NetworkRedditGalleryDataItem(id = 2, media_id = "image2_$postId"),
            ),
        ),
    )

    private fun generateRedditNSFWPost(postId: Int) = NetworkRedditPost(
        id = "post_3",
        subreddit = "nsfw",
        permalink = "https://example.com/permalink$postId/",
        title = "Adult Content",
        thumbnail = "https://example.com/thumbnail3_$postId.jpg",
        // thumbnail_height = 150,
        // thumbnail_width = 200,
        author = "user3",
        created_utc = 1634265250,
        is_video = false,
        over_18 = true,
        post_hint = "image",
        preview = NetworkRedditPreview(
            images = listOf(
                NetworkRedditPreviewImage(
                    id = "image1_$postId",
                    source = NetworkRedditResolution(
                        url = "https://example.com/image1_$postId.jpg",
                        width = 1200,
                        height = 800,
                    ),
                    resolutions = listOf(
                        NetworkRedditResolution(
                            url = "https://example.com/image1_small_$postId.jpg",
                            width = 300,
                            height = 200,
                        ),
                    ),
                ),
            ),
        ),
    )

    private fun generateRedditVideoPost(postId: Int) = NetworkRedditPost(
        id = "post_4",
        subreddit = "videos",
        permalink = "https://example.com/permalink$postId/",
        title = "Funny Cat Video",
        thumbnail = "https://example.com/thumbnail4_$postId.jpg",
        // thumbnail_height = 180,
        // thumbnail_width = 320,
        author = "user4",
        created_utc = 1634265500,
        is_video = true,
        url = "https://example.com/video_$postId.mp4",
    )

    fun generateRedditImagePost(postId: Int) = NetworkRedditPost(
        id = "post_1",
        subreddit = "pics",
        permalink = "https://example.com/permalink$postId/",
        title = "Beautiful Scenery",
        thumbnail = "https://example.com/thumbnail1_$postId.jpg",
        // thumbnail_height = 200,
        // thumbnail_width = 300,
        author = "user1",
        created_utc = 1634264750,
        is_video = false,
        post_hint = "image",
        preview = NetworkRedditPreview(
            images = listOf(
                NetworkRedditPreviewImage(
                    id = "image1_$postId",
                    source = NetworkRedditResolution(
                        url = "https://example.com/image1_$postId.jpg",
                        width = 1200,
                        height = 800,
                    ),
                    resolutions = listOf(
                        NetworkRedditResolution(
                            url = "https://example.com/image1_small_$postId.jpg",
                            width = 300,
                            height = 200,
                        ),
                    ),
                ),
            ),
        ),
    )

    fun generateRedditSavedSearches(
        size: Int = 10,
        random: Random = Random,
    ) = List(size) {
        generateRedditSavedSearch(
            random = random,
        )
    }

    private fun generateRedditSavedSearch(
        random: Random = Random,
        idNumber: Int = random.nextInt().absoluteValue,
    ): SavedSearch {
        val id = idNumber + 1
        return SavedSearch(
            id = id.toLong(),
            name = "saved_search_$id",
            search = RedditSearch(
                query = "test_q_$id",
                filters = RedditFilters(
                    subreddits = setOf("test_subreddit_$id"),
                    includeNsfw = false,
                    sort = RedditSort.RELEVANCE,
                    timeRange = RedditTimeRange.ALL,
                ),
            ),
        )
    }
}
