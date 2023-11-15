package com.ammar.wallflow.data.db.entity.wallpaper

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
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenTagEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenUploaderEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenWallpaperTagsEntity
import com.ammar.wallflow.data.db.entity.wallhaven.WallhavenWallpaperUploaderEntity
import com.ammar.wallflow.data.db.entity.wallhaven.asTag
import com.ammar.wallflow.data.db.entity.wallhaven.asUploader
import com.ammar.wallflow.model.Purity
import com.ammar.wallflow.model.wallhaven.WallhavenWallpaper
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Entity(
    tableName = "wallhaven_wallpapers",
    indices = [
        Index(
            value = ["wallhaven_id"],
            unique = true,
        ),
    ],
)
@Serializable
data class WallhavenWallpaperEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "wallhaven_id") val wallhavenId: String,
    val url: String,
    @ColumnInfo(name = "short_url") val shortUrl: String,
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
) : OnlineSourceWallpaperEntity

data class WallpaperWithUploaderAndTags(
    @Embedded val wallpaper: WallhavenWallpaperEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = WallhavenWallpaperUploaderEntity::class,
            parentColumn = "wallpaper_id",
            entityColumn = "uploader_id",
        ),
    )
    val uploader: WallhavenUploaderEntity?,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = WallhavenWallpaperTagsEntity::class,
            parentColumn = "wallpaper_id",
            entityColumn = "tag_id",
        ),
    )
    val tags: List<WallhavenTagEntity>?,
)

@Serializable
data class ThumbsEntity(
    val large: String,
    val original: String,
    val small: String,
)

fun WallhavenWallpaperEntity.toWallpaper(
    uploader: WallhavenUploaderEntity? = null,
    tags: List<WallhavenTagEntity>? = null,
) = WallhavenWallpaper(
    id = wallhavenId,
    url = url,
    shortUrl = shortUrl,
    uploader = uploader?.asUploader(),
    views = views,
    favorites = favorites,
    purity = purity,
    wallhavenSource = source,
    category = this.category,
    resolution = IntSize(dimensionX, dimensionY),
    fileSize = fileSize,
    mimeType = fileType,
    createdAt = createdAt,
    colors = colors.map { Color(it.toColorInt()) },
    data = path,
    thumbData = thumbs.original,
    tags = tags?.map { it.asTag() },
)
