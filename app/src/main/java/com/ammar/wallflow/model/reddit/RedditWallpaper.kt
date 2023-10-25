package com.ammar.wallflow.model.reddit

import androidx.compose.ui.unit.IntSize
import androidx.core.util.PatternsCompat
import com.ammar.wallflow.model.DownloadableWallpaper
import com.ammar.wallflow.model.Purity
import com.ammar.wallflow.model.Source

data class RedditWallpaper(
    override val source: Source = Source.REDDIT,
    override val id: String,
    override val data: String,
    override val fileSize: Long = -1, // reddit does not provide file size in api resp
    override val resolution: IntSize,
    override val mimeType: String?,
    override val thumbData: String?,
    override val purity: Purity,
    val redditId: String? = null,
    val subreddit: String,
    val postId: String,
    val postTitle: String,
    val postUrl: String,
    val author: String,
    val galleryPosition: Int? = null,
) : DownloadableWallpaper()

// Examples
val redditWallpaper1 = RedditWallpaper(
    id = "1",
    data = "https://example.com/wallpaper1.jpg",
    resolution = IntSize(1920, 1080),
    mimeType = "image/jpeg",
    purity = Purity.SFW,
    subreddit = "EarthPorn",
    postId = "abc123",
    postTitle = "Beautiful Landscape",
    postUrl = "https://www.reddit.com/r/EarthPorn/abc123",
    author = "naturelover",
    galleryPosition = 1,
    thumbData = "https://example.com/thumb1.jpg",
)

private val WEB_URL_REGEX = PatternsCompat.WEB_URL.toRegex()

fun String.withRedditDomainPrefix(): String {
    // if already a correct url, do nothing
    if (this.matches(WEB_URL_REGEX)) {
        return this
    }
    val hasSep = this.startsWith("/")
    // prefix with reddit.com
    return "https://www.reddit.com${
        if (!hasSep) "/" else ""
    }$this"
}
