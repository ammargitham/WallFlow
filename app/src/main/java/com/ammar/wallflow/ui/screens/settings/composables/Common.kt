package com.ammar.wallflow.ui.screens.settings.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ammar.wallflow.R
import com.ammar.wallflow.data.preferences.ObjectDetectionDelegate
import com.ammar.wallflow.model.WallpaperTarget

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
