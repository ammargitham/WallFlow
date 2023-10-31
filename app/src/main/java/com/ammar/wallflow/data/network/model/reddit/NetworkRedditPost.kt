package com.ammar.wallflow.data.network.model.reddit

import com.ammar.wallflow.SUPPORTED_MIME_TYPES
import com.ammar.wallflow.data.db.entity.wallpaper.RedditWallpaperEntity
import com.ammar.wallflow.data.network.model.serializers.NetworkRedditPostCreatedUtcSerializer
import com.ammar.wallflow.extensions.htmlUnescaped
import com.ammar.wallflow.extensions.parseMimeType
import com.ammar.wallflow.model.Purity
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
@Suppress("PropertyName")
data class NetworkRedditPost(
    val id: String,
    val subreddit: String,
    val is_gallery: Boolean = false,
    val title: String,
    val thumbnail: String,
    // val thumbnail_height: Int,
    // val thumbnail_width: Int,
    val over_18: Boolean = false,
    val author: String,
    val permalink: String,
    val url: String? = null,
    @Serializable(NetworkRedditPostCreatedUtcSerializer::class)
    val created_utc: Long,
    val is_video: Boolean,
    val post_hint: String? = null,
    val preview: NetworkRedditPreview? = null, // for single image
    val media_metadata: Map<String, NetworkRedditPreviewImage>? = null, // for gallery
    val gallery_data: NetworkRedditGalleryData? = null, // for gallery
)

@Serializable
data class NetworkRedditPreview(
    val images: List<NetworkRedditPreviewImage>,
)

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class NetworkRedditPreviewImage(
    val id: String? = null,
    val m: String? = null, // mime type (eg.: "image/jpg", "image/png")
    @JsonNames("s")
    val source: NetworkRedditResolution? = null,
    @JsonNames("p")
    val resolutions: List<NetworkRedditResolution>? = null,
)

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class NetworkRedditResolution(
    @JsonNames("u", "gif")
    val url: String,
    @JsonNames("x")
    val width: Int,
    @JsonNames("y")
    val height: Int,
)

@Serializable
data class NetworkRedditGalleryData(
    val items: List<NetworkRedditGalleryDataItem>,
)

@Serializable
@Suppress("PropertyName")
data class NetworkRedditGalleryDataItem(
    val id: Long,
    val media_id: String,
)

fun NetworkRedditPost.toWallpaperEntities(): List<RedditWallpaperEntity> {
    return if (is_gallery) {
        val galleryItems = gallery_data?.items ?: return emptyList()
        val mediaMetadata = media_metadata ?: return emptyList()
        galleryItems.mapIndexed { i, item ->
            val mediaId = item.media_id
            val media = mediaMetadata[mediaId] ?: return@mapIndexed null
            val source = media.source ?: return@mapIndexed null
            val thumbnailUrl = media.resolutions?.last()?.url?.htmlUnescaped()
                ?: return@mapIndexed null
            val url = source.url.htmlUnescaped()
            val mimeType = media.m ?: parseMimeType(url)
            if (mimeType !in SUPPORTED_MIME_TYPES) {
                return@mapIndexed null
            }
            RedditWallpaperEntity(
                id = 0,
                subreddit = subreddit,
                postId = id,
                postTitle = title,
                postUrl = permalink,
                purity = if (over_18) Purity.NSFW else Purity.SFW,
                author = author,
                url = url,
                thumbnailUrl = thumbnailUrl,
                width = source.width,
                height = source.height,
                mimeType = media.m,
                redditId = mediaId,
                galleryPosition = i,
            )
        }.filterNotNull()
    } else {
        val preview = preview ?: return emptyList()
        val previewImage = preview.images.first()
        val source = previewImage.source ?: return emptyList()
        val redditId = previewImage.id ?: return emptyList()
        val thumbnail = previewImage.resolutions?.last() ?: return emptyList()
        val url = source.url.htmlUnescaped()
        val mimeType = parseMimeType(url)
        if (mimeType !in SUPPORTED_MIME_TYPES) {
            return emptyList()
        }
        listOf(
            RedditWallpaperEntity(
                id = 0,
                redditId = redditId,
                subreddit = subreddit,
                postId = id,
                postTitle = title,
                postUrl = permalink,
                purity = if (over_18) Purity.NSFW else Purity.SFW,
                author = author,
                url = url,
                thumbnailUrl = thumbnail.url.htmlUnescaped(),
                width = source.width,
                height = source.height,
            ),
        )
    }
}
