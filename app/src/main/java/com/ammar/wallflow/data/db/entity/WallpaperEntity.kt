package com.ammar.wallflow.data.db.entity

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.toColorInt
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.ammar.wallflow.model.Purity
import com.ammar.wallflow.model.WallhavenThumbs
import com.ammar.wallflow.model.WallhavenWallpaper
import kotlinx.datetime.Instant

@Entity(
    tableName = "wallpapers",
    indices = [
        Index(
            value = ["wallhaven_id"],
            unique = true,
        ),
    ],
)
data class WallpaperEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "wallhaven_id") val wallhavenId: String,
    val url: String,
    @ColumnInfo(name = "short_url") val shortUrl: String,
    @ColumnInfo(name = "uploader_id") val uploaderId: Long?,
    val views: Int,
    val favorites: Int,
    val source: String,
    val purity: Purity,
    val category: String,
    @ColumnInfo(name = "dimension_x") val dimensionX: Int,
    @ColumnInfo(name = "dimension_y") val dimensionY: Int,
    @ColumnInfo(name = "file_size") val fileSize: Long,
    @ColumnInfo(name = "file_type") val fileType: String,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    val colors: List<String>,
    val path: String,
    @Embedded(prefix = "thumb_") val thumbs: ThumbsEntity,
)

data class WallpaperWithUploaderAndTags(
    @Embedded val wallpaper: WallpaperEntity,
    @Relation(
        parentColumn = "uploader_id",
        entityColumn = "id",
    )
    val uploader: UploaderEntity?,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = WallpaperTagsEntity::class,
            parentColumn = "wallpaper_id",
            entityColumn = "tag_id",
        ),
    )
    val tags: List<TagEntity>?,
)

data class ThumbsEntity(
    val large: String,
    val original: String,
    val small: String,
)

fun WallpaperEntity.asWallpaper(
    uploader: UploaderEntity? = null,
    tags: List<TagEntity>? = null,
) = WallhavenWallpaper(
    id = wallhavenId,
    url = url,
    shortUrl = shortUrl,
    uploader = uploader?.asUploader(),
    views = views,
    favorites = favorites,
    purity = purity,
    source = source,
    category = this.category,
    resolution = IntSize(dimensionX, dimensionY),
    fileSize = fileSize,
    fileType = fileType,
    createdAt = createdAt,
    colors = colors.map { Color(it.toColorInt()) },
    path = path,
    thumbs = thumbs.asThumbs(),
    tags = tags?.map { it.asTag() },
)

private fun ThumbsEntity.asThumbs() = WallhavenThumbs(
    large = large,
    original = original,
    small = small,
)
