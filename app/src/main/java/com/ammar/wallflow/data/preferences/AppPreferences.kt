package com.ammar.wallflow.data.preferences

import androidx.annotation.IntRange
import androidx.work.Constraints
import androidx.work.NetworkType
import com.ammar.wallflow.model.Search
import com.ammar.wallflow.model.SearchQuery
import com.ammar.wallflow.model.Sorting
import com.ammar.wallflow.model.TopRange
import java.util.UUID
import kotlinx.datetime.DateTimePeriod
import org.tensorflow.lite.task.core.ComputeSettings.Delegate

data class AppPreferences(
    val wallhavenApiKey: String = "",
    val homeSearch: Search = Search(
        filters = SearchQuery(
            sorting = Sorting.TOPLIST,
            topRange = TopRange.ONE_DAY,
        )
    ),
    val blurSketchy: Boolean = false,
    val blurNsfw: Boolean = false,
    val objectDetectionPreferences: ObjectDetectionPreferences = ObjectDetectionPreferences(),
    val autoWallpaperPreferences: AutoWallpaperPreferences = AutoWallpaperPreferences(),
    val lookAndFeelPreferences: LookAndFeelPreferences = LookAndFeelPreferences(),
)

data class ObjectDetectionPreferences(
    val enabled: Boolean = true,
    val delegate: Delegate = Delegate.GPU,
    val modelId: Long = 0,
)

internal val defaultAutoWallpaperFreq = DateTimePeriod(hours = 4)
internal val defaultAutoWallpaperConstraints = Constraints.Builder().apply {
    setRequiredNetworkType(NetworkType.CONNECTED)
}.build()

data class AutoWallpaperPreferences(
    val enabled: Boolean = false,
    val savedSearchId: Long = 0,
    val useObjectDetection: Boolean = true,
    val frequency: DateTimePeriod = defaultAutoWallpaperFreq,
    val constraints: Constraints = defaultAutoWallpaperConstraints,
    val showNotification: Boolean = false,
    val workRequestId: UUID? = null,
)

enum class Theme {
    SYSTEM,
    LIGHT,
    DARK,
}

data class LookAndFeelPreferences(
    val theme: Theme = Theme.SYSTEM,
    val layoutPreferences: LayoutPreferences = LayoutPreferences(),
)

enum class GridType {
    STAGGERED,
    FIXED_SIZE;
}

const val minGridCols = 1L
const val maxGridCols = 5L

data class LayoutPreferences(
    val gridType: GridType = GridType.STAGGERED,
    @IntRange(minGridCols, maxGridCols) val gridColCount: Int = 2,
    val roundedCorners: Boolean = true,
)
