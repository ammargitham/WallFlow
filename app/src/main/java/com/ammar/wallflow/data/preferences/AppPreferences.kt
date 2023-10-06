@file:UseSerializers(
    DateTimePeriodSerializer::class,
    ConstraintsSerializer::class,
    UUIDSerializer::class,
)

package com.ammar.wallflow.data.preferences

import androidx.annotation.IntRange
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.work.Constraints
import androidx.work.NetworkType
import com.ammar.wallflow.model.Search
import com.ammar.wallflow.model.WallpaperTarget
import com.ammar.wallflow.model.serializers.ConstraintsSerializer
import com.ammar.wallflow.model.serializers.DateTimePeriodSerializer
import com.ammar.wallflow.model.serializers.UUIDSerializer
import com.ammar.wallflow.model.wallhaven.WallhavenSearchQuery
import com.ammar.wallflow.model.wallhaven.WallhavenSorting
import com.ammar.wallflow.model.wallhaven.WallhavenTopRange
import com.ammar.wallflow.ui.screens.local.LocalSort
import java.util.UUID
import kotlinx.datetime.DateTimePeriod
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class AppPreferences(
    val wallhavenApiKey: String = "",
    val homeSearch: Search = Search(
        filters = WallhavenSearchQuery(
            sorting = WallhavenSorting.TOPLIST,
            topRange = WallhavenTopRange.ONE_DAY,
        ),
    ),
    val blurSketchy: Boolean = false,
    val blurNsfw: Boolean = false,
    val objectDetectionPreferences: ObjectDetectionPreferences = ObjectDetectionPreferences(),
    val autoWallpaperPreferences: AutoWallpaperPreferences = AutoWallpaperPreferences(),
    val lookAndFeelPreferences: LookAndFeelPreferences = LookAndFeelPreferences(),
    val changeWallpaperTileAdded: Boolean = false,
    val localWallpapersPreferences: LocalWallpapersPreferences = LocalWallpapersPreferences(),
)

enum class ObjectDetectionDelegate {
    NONE,
    NNAPI,
    GPU,
}

@Serializable
data class ObjectDetectionPreferences(
    val enabled: Boolean = false,
    val delegate: ObjectDetectionDelegate = ObjectDetectionDelegate.GPU,
    val modelId: Long = 0,
)

internal val defaultAutoWallpaperFreq = DateTimePeriod(hours = 4)
internal val defaultAutoWallpaperConstraints = Constraints.Builder().apply {
    setRequiredNetworkType(NetworkType.CONNECTED)
}.build()

@Serializable
data class AutoWallpaperPreferences(
    val enabled: Boolean = false,
    val savedSearchEnabled: Boolean = false,
    val favoritesEnabled: Boolean = false,
    val localEnabled: Boolean = false,
    val savedSearchId: Long = 0,
    val useObjectDetection: Boolean = true,
    val frequency: DateTimePeriod = defaultAutoWallpaperFreq,
    val constraints: Constraints = defaultAutoWallpaperConstraints,
    val showNotification: Boolean = false,
    val workRequestId: UUID? = null,
    val targets: Set<WallpaperTarget> = setOf(WallpaperTarget.HOME, WallpaperTarget.LOCKSCREEN),
    val markFavorite: Boolean = false,
    val download: Boolean = false,
) {
    val anySourceEnabled = (savedSearchEnabled && savedSearchId > 0) ||
        favoritesEnabled ||
        localEnabled
}

val MutableStateAutoWallpaperPreferencesSaver =
    Saver<MutableState<AutoWallpaperPreferences>, String>(
        save = {
            Json.encodeToString<AutoWallpaperPreferences>(it.value)
        },
        restore = {
            mutableStateOf(Json.decodeFromString<AutoWallpaperPreferences>(it))
        },
    )

enum class Theme {
    SYSTEM,
    LIGHT,
    DARK,
}

@Serializable
data class LookAndFeelPreferences(
    val theme: Theme = Theme.SYSTEM,
    val layoutPreferences: LayoutPreferences = LayoutPreferences(),
    val showLocalTab: Boolean = true,
)

enum class GridType {
    STAGGERED,
    FIXED_SIZE,
}

enum class GridColType {
    ADAPTIVE,
    FIXED,
}

const val MIN_GRID_COLS = 1L
const val MAX_GRID_COLS = 5L
const val MIN_GRID_COL_WIDTH_PCT = 10L
const val MAX_GRID_COL_WIDTH_PCT = 50L

@Serializable
data class LayoutPreferences(
    val gridType: GridType = GridType.STAGGERED,
    val gridColType: GridColType = GridColType.ADAPTIVE,
    @IntRange(MIN_GRID_COLS, MAX_GRID_COLS) val gridColCount: Int = 2,
    @IntRange(MIN_GRID_COL_WIDTH_PCT, MAX_GRID_COL_WIDTH_PCT) val gridColMinWidthPct: Int = 40,
    val roundedCorners: Boolean = true,
)

@Serializable
data class LocalWallpapersPreferences(
    val sort: LocalSort = LocalSort.NO_SORT,
)
