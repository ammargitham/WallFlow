package com.ammar.wallflow.ui.screens.settings.composables

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.ammar.wallflow.R
import com.ammar.wallflow.data.preferences.ObjectDetectionDelegate
import com.ammar.wallflow.data.preferences.ViewedWallpapersLook
import com.ammar.wallflow.model.WallpaperTarget
import com.ammar.wallflow.ui.screens.settings.NextRun
import com.ammar.wallflow.utils.ExifWriteType
import java.util.Locale
import kotlinx.datetime.DateTimePeriod

@Composable
internal fun delegateString(delegate: ObjectDetectionDelegate) = stringResource(
    when (delegate) {
        ObjectDetectionDelegate.NONE -> R.string.cpu
        ObjectDetectionDelegate.NNAPI -> R.string.nnapi
        ObjectDetectionDelegate.GPU -> R.string.gpu
    },
)

@Composable
internal fun wallpaperTargetString(wallpaperTarget: WallpaperTarget) = stringResource(
    when (wallpaperTarget) {
        WallpaperTarget.HOME -> R.string.home_screen
        WallpaperTarget.LOCKSCREEN -> R.string.lock_screen
    },
)

@Suppress("SimplifiableCallChain")
@Composable
internal fun getTargetsSummary(targets: Set<WallpaperTarget>) = targets.map {
    wallpaperTargetString(it)
}.joinToString(", ")

internal enum class FrequencyChronoUnit {
    HOURS,
    MINUTES,
}

@Composable
internal fun chronoUnitString(chronoUnit: FrequencyChronoUnit) = stringResource(
    when (chronoUnit) {
        FrequencyChronoUnit.HOURS -> R.string.hours
        FrequencyChronoUnit.MINUTES -> R.string.minutes
    },
)

@Composable
internal fun getFrequencyString(
    useSameFrequency: Boolean,
    frequency: DateTimePeriod,
    lsFrequency: DateTimePeriod,
): String {
    if (useSameFrequency) {
        return getFrequencyString(frequency)
    }
    return "${stringResource(R.string.home_screen)}: ${getFrequencyString(frequency)}\n" +
        "${stringResource(R.string.lock_screen)}: ${getFrequencyString(lsFrequency)}"
}

@Composable
private fun getFrequencyString(
    frequency: DateTimePeriod,
): String {
    val hours = frequency.hours
    val minutes = frequency.minutes
    val value: Int
    val unit: FrequencyChronoUnit
    if (hours != 0 && minutes == 0) {
        value = frequency.hours
        unit = FrequencyChronoUnit.HOURS
    } else {
        value = frequency.hours * 60 + frequency.minutes
        unit = FrequencyChronoUnit.MINUTES
    }
    return pluralStringResource(
        when (unit) {
            FrequencyChronoUnit.HOURS -> R.plurals.every_x_hrs
            FrequencyChronoUnit.MINUTES -> R.plurals.every_x_mins
        },
        value,
        value,
    )
}

@Composable
internal fun exifWriteTypeString(writeType: ExifWriteType) = stringResource(
    when (writeType) {
        ExifWriteType.APPEND -> R.string.append
        ExifWriteType.OVERWRITE -> R.string.overwrite
    },
)

@Composable
internal fun viewedWallpapersLookString(
    viewedWallpapersLook: ViewedWallpapersLook,
) = stringResource(
    when (viewedWallpapersLook) {
        ViewedWallpapersLook.NONE -> R.string.none
        ViewedWallpapersLook.DIM -> R.string.dim
        ViewedWallpapersLook.DIM_WITH_LABEL -> R.string.dim_with_label
        ViewedWallpapersLook.DIM_WITH_ICON -> R.string.dim_with_icon
        ViewedWallpapersLook.LABEL -> R.string.label
        ViewedWallpapersLook.ICON -> R.string.icon
    },
)

@Composable
fun getNextRunString(
    useSameFrequency: Boolean,
    nextRun: NextRun,
    lsNextRun: NextRun,
): String {
    if (useSameFrequency) {
        return getNextRunString(nextRun)
    }
    return "${stringResource(R.string.next_run_approximate)}:\n" +
        "${stringResource(R.string.home_screen)}: ${getNextRunString(nextRun, false)}\n" +
        "${stringResource(R.string.lock_screen)}: ${getNextRunString(lsNextRun, false)}"
}

@Composable
private fun getNextRunString(
    nextRun: NextRun,
    useSameFrequency: Boolean = true,
) = when (nextRun) {
    is NextRun.NextRunTime -> if (useSameFrequency) {
        stringResource(R.string.next_run_at, getFormattedDateTime(nextRun))
    } else {
        getFormattedDateTime(nextRun)
    }
    NextRun.NotScheduled -> stringResource(R.string.not_scheduled)
    NextRun.Running -> stringResource(R.string.running)
}

@Composable
private fun getFormattedDateTime(
    nextRun: NextRun.NextRunTime,
) = DateFormat.format(
    DateFormat.getBestDateTimePattern(
        Locale.getDefault(),
        "yyyyy.MMMM.dd hh:mm",
    ),
    nextRun.instant.toEpochMilliseconds(),
).toString()

@Composable
fun getRestartReasonText(reason: RestartReason) = stringResource(
    when (reason) {
        RestartReason.ACRA_ENABLED -> R.string.acra_enabled_reason
    },
)
