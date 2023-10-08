package com.ammar.wallflow.data.network.model.reddit

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonNames

@Suppress("PropertyName")
data class NetworkRedditPost(
    val id: String,
    val subreddit: String,
    val is_gallery: Boolean = false,
    val title: String,
    val thumbnail: String,
    val thumbnail_height: Int,
    val thumbnail_width: Int,
    val over_18: Boolean = false,
    val author: String,
    val permalink: String,
    val url: String? = null,
    val created_utc: Long,
    val is_video: Boolean,
    val post_hint: String? = null,
    val preview: NetworkRedditPreview? = null, // for single image
    val media_metadata: Map<String, NetworkRedditPreviewImage>? = null, // for gallery
    // val gallery_data: GalleryData? = null, // for gallery
)

data class NetworkRedditPreview(
    val images: List<NetworkRedditPreviewImage>,
)

@OptIn(ExperimentalSerializationApi::class)
data class NetworkRedditPreviewImage(
    val m: String? = null, // mime type (eg.: "image/jpg", "image/png")
    @JsonNames("s")
    val source: NetworkRedditResolution,
    @JsonNames("p")
    val resolutions: List<NetworkRedditResolution>,
    // val id: String
)

@OptIn(ExperimentalSerializationApi::class)
data class NetworkRedditResolution(
    @JsonNames("u")
    val url: String,
    @JsonNames("x")
    val width: Long,
    @JsonNames("y")
    val height: Long,
)
