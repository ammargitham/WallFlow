package com.ammar.wallflow.ui.settings.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ammar.wallflow.R
import com.ammar.wallflow.data.preferences.ObjectDetectionDelegate

@Composable
internal fun delegateString(delegate: ObjectDetectionDelegate) = stringResource(
    when (delegate) {
        ObjectDetectionDelegate.NONE -> R.string.cpu
        ObjectDetectionDelegate.NNAPI -> R.string.nnapi
        ObjectDetectionDelegate.GPU -> R.string.gpu
    },
)
